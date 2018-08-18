/*
 * Copyright (c) 2009, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.tiff;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;

import static com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry.getValueLength;

/**
 * TIFFReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFReader.java,v 1.0 Nov 13, 2009 5:42:51 PM haraldk Exp$
 */
public final class TIFFReader extends MetadataReader {

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.metadata.exif.debug"));

    static final Collection<Integer> KNOWN_IFDS = Collections.unmodifiableCollection(Arrays.asList(TIFF.TAG_EXIF_IFD, TIFF.TAG_GPS_IFD, TIFF.TAG_INTEROP_IFD, TIFF.TAG_SUB_IFD));
    private boolean longOffsets;
    private int offsetSize;

    @Override
    public Directory read(final ImageInputStream input) throws IOException {
        Validate.notNull(input, "input");

        byte[] bom = new byte[2];
        input.readFully(bom);

        if (bom[0] == 'I' && bom[1] == 'I') {
            input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
        else if (bom[0] == 'M' && bom[1] == 'M') {
            input.setByteOrder(ByteOrder.BIG_ENDIAN);
        }
        else {
            throw new IIOException(String.format("Invalid TIFF byte order mark '%s', expected: 'II' or 'MM'", StringUtil.decode(bom, 0, bom.length, "ASCII")));
        }

        // BigTiff uses version 43 instead of TIFF's 42, and header is slightly different, see
        // http://www.awaresystems.be/imaging/tiff/bigtiff.html
        int magic = input.readUnsignedShort();
        if (magic == TIFF.TIFF_MAGIC) {
            longOffsets = false;
            offsetSize = 4;
        }
        else if (magic == TIFF.BIGTIFF_MAGIC) {
            longOffsets = true;
            offsetSize = 8;

            // Just validate we're ok
            int offsetSize = input.readUnsignedShort();
            if (offsetSize != 8) {
                throw new IIOException(String.format("Unexpected BigTIFF offset size: %04x, expected: %04x", offsetSize, 8));
            }

            int padding = input.readUnsignedShort();
            if (padding != 0) {
                throw new IIOException(String.format("Unexpected BigTIFF padding: %04x, expected: %04x", padding, 0));
            }
        }
        else {
            throw new IIOException(String.format("Wrong TIFF magic in input data: %04x, expected: %04x", magic, TIFF.TIFF_MAGIC));
        }

        return readLinkedIFDs(input);
    }

    private TIFFDirectory readLinkedIFDs(final ImageInputStream input) throws IOException {
        long nextOffset = readOffset(input);

        List<IFD> ifds = new ArrayList<>();

        // Read linked IFDs
        while (nextOffset != 0) {
            try {
                ifds.add(readIFD(input, nextOffset));

                nextOffset = readOffset(input);
            }
            catch (EOFException eof) {
                // catch EOF here as missing EOF marker
                nextOffset = 0;
            }
        }

        return new TIFFDirectory(ifds);
    }

    private long readOffset(final ImageInputStream input) throws IOException {
        return longOffsets ? input.readLong() : input.readUnsignedInt();
    }

    private IFD readIFD(final ImageInputStream pInput, final long pOffset) throws IOException {
        pInput.seek(pOffset);

        long entryCount = readEntryCount(pInput);

        List<TIFFEntry> entries = new ArrayList<>();

        for (int i = 0; i < entryCount; i++) {
            try {
                TIFFEntry entry = readEntry(pInput);

                if (entry != null) {
                    entries.add(entry);
                }
            }
            catch (IIOException e) {
                break;
            }
        }

        // TODO: Consider leaving to client code what sub-IFDs to parse (but always parse TAG_SUB_IFD).
        readSubIFDs(pInput, entries,
                Arrays.asList(TIFF.TAG_EXIF_IFD, TIFF.TAG_GPS_IFD, TIFF.TAG_INTEROP_IFD, TIFF.TAG_SUB_IFD)
        );

        return new IFD(entries);
    }

    private long readEntryCount(final ImageInputStream pInput) throws IOException {
        return longOffsets ? pInput.readLong() : pInput.readUnsignedShort();
    }

    private void readSubIFDs(ImageInputStream input, List<TIFFEntry> entries, List<Integer> subIFDIds) throws IOException {
        if (subIFDIds == null || subIFDIds.isEmpty()) {
            return;
        }

        long initialPosition = input.getStreamPosition();

        for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
            TIFFEntry entry = entries.get(i);
            int tagId = (Integer) entry.getIdentifier();

            if (subIFDIds.contains(tagId)) {
                try {
                    if (KNOWN_IFDS.contains(tagId)) {
                        long[] pointerOffsets = getPointerOffsets(entry);
                        List<IFD> subIFDs = new ArrayList<>(pointerOffsets.length);

                        for (long pointerOffset : pointerOffsets) {
                            try {
                                subIFDs.add(readIFD(input, pointerOffset));
                            }
                            catch (EOFException ignore) {
                                // TODO: Issue warning
                                if (DEBUG) {
                                    ignore.printStackTrace();
                                }
                            }
                        }

                        if (subIFDs.size() == 1) {
                            // Replace the entry with parsed data
                            entries.set(i, new TIFFEntry(tagId, entry.getType(), subIFDs.get(0)));
                        }
                        else if (!subIFDs.isEmpty()) {
                            // Replace the entry with parsed data
                            entries.set(i, new TIFFEntry(tagId, entry.getType(), subIFDs.toArray(new IFD[subIFDs.size()])));
                        }
                    }
                }
                catch (IIOException e) {
                    if (DEBUG) {
                        // TODO: Issue warning without crashing...?
                        System.err.println("Error parsing sub-IFD: " + tagId);
                        e.printStackTrace();
                    }
                }
            }
        }

        // Restore initial position
        input.seek(initialPosition);
    }

    private long[] getPointerOffsets(final Entry entry) throws IIOException {
        long[] offsets;
        Object value = entry.getValue();

        if (value instanceof Byte) {
            offsets = new long[] {(Byte) value & 0xff};
        }
        else if (value instanceof Short) {
            offsets = new long[] {(Short) value & 0xffff};
        }
        else if (value instanceof Integer) {
            offsets = new long[] {(Integer) value & 0xffffffffL};
        }
        else if (value instanceof Long) {
            offsets = new long[] {(Long) value};
        }
        else if (value instanceof long[]) {
            offsets = (long[]) value;
        }
        else {
            throw new IIOException(String.format("Unknown pointer type: %s", value != null ? value.getClass() : null));
        }

        return offsets;
    }

    private TIFFEntry readEntry(final ImageInputStream pInput) throws IOException {
        int tagId = pInput.readUnsignedShort();
        short type = pInput.readShort();
        int count = readValueCount(pInput); // Number of values

        // It's probably a spec violation to have count 0, but we'll be lenient about it
        if (count < 0) {
            throw new IIOException(String.format("Illegal count %d for tag %s type %s @%08x", count, tagId, type, pInput.getStreamPosition()));
        }

        if (!isValidType(type)) {
            pInput.skipBytes(4); // read Value

            // Invalid tag, this is just for debugging
            long offset = pInput.getStreamPosition() - 12L;

            if (DEBUG) {
                System.err.printf("Bad EXIF data @%08x\n", pInput.getStreamPosition());
                System.err.println("tagId: " + tagId + (tagId <= 0 ? " (INVALID)" : ""));
                System.err.println("type: " + type + " (INVALID)");
                System.err.println("count: " + count);

                pInput.mark();
                pInput.seek(offset);

                try {
                    byte[] bytes = new byte[8 + Math.min(120, Math.max(24, count))];
                    int len = pInput.read(bytes);

                    if (DEBUG) {
                        System.err.print(HexDump.dump(offset, bytes, 0, len));
                        System.err.println(len < count ? "[...]" : "");
                    }
                }
                finally {
                    pInput.reset();
                }
            }
            return null;
        }

        long valueLength = getValueLength(type, count);

        Object value;
        if (valueLength > 0 && valueLength <= offsetSize) {
            value = readValueInLine(pInput, type, count);
            pInput.skipBytes(offsetSize - valueLength);
        }
        else {
            long valueOffset = readOffset(pInput); // This is the *value* iff the value size is <= 4 bytes
            value = readValueAt(pInput, valueOffset, type, count);
        }

        return new TIFFEntry(tagId, type, value);
    }

    private boolean isValidType(final short type) {
        return type > 0 && type < TIFF.TYPE_LENGTHS.length && TIFF.TYPE_LENGTHS[type] > 0;
    }

    private int readValueCount(final ImageInputStream pInput) throws IOException {
        return assertIntCount(longOffsets ? pInput.readLong() : pInput.readUnsignedInt());
    }

    private int assertIntCount(final long count) throws IOException {
        if (count > Integer.MAX_VALUE) {
            throw new IIOException(String.format("Unsupported TIFF value count value: %s > Integer.MAX_VALUE", count));
        }

        return (int) count;
    }

    private Object readValueAt(final ImageInputStream pInput, final long pOffset, final short pType, final int pCount) throws IOException {
        long pos = pInput.getStreamPosition();
        try {
            pInput.seek(pOffset);
            return readValue(pInput, pType, pCount);
        }
        catch (EOFException e) {
            // TODO: Add warning listener API and report problem to client code
            return e;
        }
        finally {
            pInput.seek(pos);
        }
    }

    private Object readValueInLine(final ImageInputStream pInput, final short pType, final int pCount) throws IOException {
        return readValue(pInput, pType, pCount);
    }

    private static Object readValue(final ImageInputStream pInput, final short pType, final int pCount) throws IOException {
        // TODO: Review value "widening" for the unsigned types. Right now it's inconsistent. Should we leave it to client code?
        // TODO: New strategy: Leave data as is, instead perform the widening in TIFFEntry.getValue.
        // TODO: Add getValueByte/getValueUnsignedByte/getValueShort/getValueUnsignedShort/getValueInt/etc... in API.

        long pos = pInput.getStreamPosition();

        switch (pType) {
            case TIFF.TYPE_ASCII:
                // TODO: This might be UTF-8 or ISO-8859-x, even though spec says NULL-terminated 7 bit ASCII
                // TODO: Fail if unknown chars, try parsing with ISO-8859-1 or file.encoding
                if (pCount == 0) {
                    return "";
                }
                byte[] ascii = new byte[pCount];
                pInput.readFully(ascii);
                int len = ascii[ascii.length - 1] == 0 ? ascii.length - 1 : ascii.length;
                return StringUtil.decode(ascii, 0, len, "UTF-8"); // UTF-8 is ASCII compatible
            case TIFF.TYPE_BYTE:
                if (pCount == 1) {
                    return pInput.readUnsignedByte();
                }
                // else fall through
            case TIFF.TYPE_SBYTE:
                if (pCount == 1) {
                    return pInput.readByte();
                }
                // else fall through
            case TIFF.TYPE_UNDEFINED:
                byte[] bytes = new byte[pCount];
                pInput.readFully(bytes);

                // NOTE: We don't change (unsigned) BYTE array wider Java type, as most often BYTE array means
                // binary data and we want to keep that as a byte array for clients to parse further

                return bytes;
            case TIFF.TYPE_SHORT:
                if (pCount == 1) {
                    return pInput.readUnsignedShort();
                }
            case TIFF.TYPE_SSHORT:
                if (pCount == 1) {
                    return pInput.readShort();
                }

                short[] shorts = new short[pCount];
                pInput.readFully(shorts, 0, shorts.length);

                if (pType == TIFF.TYPE_SHORT) {
                    int[] ints = new int[pCount];
                    for (int i = 0; i < pCount; i++) {
                        ints[i] = shorts[i] & 0xffff;
                    }

                    return ints;
                }

                return shorts;
            case TIFF.TYPE_IFD:
            case TIFF.TYPE_LONG:
                if (pCount == 1) {
                    return pInput.readUnsignedInt();
                }
            case TIFF.TYPE_SLONG:
                if (pCount == 1) {
                    return pInput.readInt();
                }

                int[] ints = new int[pCount];
                pInput.readFully(ints, 0, ints.length);

                if (pType == TIFF.TYPE_LONG || pType == TIFF.TYPE_IFD) {
                    long[] longs = new long[pCount];
                    for (int i = 0; i < pCount; i++) {
                        longs[i] = ints[i] & 0xffffffffL;
                    }

                    return longs;
                }

                return ints;
            case TIFF.TYPE_FLOAT:
                if (pCount == 1) {
                    return pInput.readFloat();
                }

                float[] floats = new float[pCount];
                pInput.readFully(floats, 0, floats.length);
                return floats;
            case TIFF.TYPE_DOUBLE:
                if (pCount == 1) {
                    return pInput.readDouble();
                }

                double[] doubles = new double[pCount];
                pInput.readFully(doubles, 0, doubles.length);
                return doubles;

            case TIFF.TYPE_RATIONAL:
                if (pCount == 1) {
                    return createSafeRational(pInput.readUnsignedInt(), pInput.readUnsignedInt());
                }

                Rational[] rationals = new Rational[pCount];
                for (int i = 0; i < rationals.length; i++) {
                    rationals[i] = createSafeRational(pInput.readUnsignedInt(), pInput.readUnsignedInt());
                }

                return rationals;
            case TIFF.TYPE_SRATIONAL:
                if (pCount == 1) {
                    return createSafeRational(pInput.readInt(), pInput.readInt());
                }

                Rational[] srationals = new Rational[pCount];
                for (int i = 0; i < srationals.length; i++) {
                    srationals[i] = createSafeRational(pInput.readInt(), pInput.readInt());
                }

                return srationals;

            // BigTiff:
            case TIFF.TYPE_LONG8:
            case TIFF.TYPE_SLONG8:
            case TIFF.TYPE_IFD8:
                // TODO: Assert BigTiff (version == 43)

                if (pCount == 1) {
                    long val = pInput.readLong();
                    if (pType != TIFF.TYPE_SLONG8 && val < 0) {
                        throw new IIOException(String.format("Value > %s", Long.MAX_VALUE));
                    }

                    return val;
                }

                long[] longs = new long[pCount];
                for (int i = 0; i < pCount; i++) {
                    longs[i] = pInput.readLong();
                }

                return longs;

            default:
                // Spec says skip unknown values
                return new Unknown(pType, pCount, pos);
        }
    }

    private static Rational createSafeRational(final long numerator, final long denominator) throws IOException {
        if (denominator == 0) {
            // Bad data.
            return Rational.NaN;
        }

        return new Rational(numerator, denominator);
    }

    public static void main(String[] args) throws IOException {
//        if (true) {
//            ImageInputStream stream = ImageIO.createImageInputStream(new File(args[0]));
//
//            byte[] b = new byte[Math.min((int) stream.length(), 1024)];
//            stream.readFully(b);
//
//            System.err.println(HexDump.dump(b));
//
//            return;
//        }
//
        TIFFReader reader = new TIFFReader();
        ImageInputStream stream = ImageIO.createImageInputStream(new File(args[0]));

        long pos = 0;
        if (args.length > 1) {
            if (args[1].startsWith("0x")) {
                pos = Integer.parseInt(args[1].substring(2), 16);
            }
            else {
                pos = Long.parseLong(args[1]);
            }

            stream.setByteOrder(pos < 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

            pos = Math.abs(pos);

            stream.seek(pos);
        }

        try {
            Directory directory;

            if (args.length > 1) {
                directory = reader.readIFD(stream, pos);
            }
            else {
                directory = reader.read(stream);
            }

            for (Entry entry : directory) {
                System.err.println(entry);

                Object value = entry.getValue();
                if (value instanceof byte[]) {
                    byte[] bytes = (byte[]) value;
                    System.err.println(HexDump.dump(0, bytes, 0, Math.min(bytes.length, 128)));
                }
            }
        }
        finally {
            stream.close();
        }
    }

    //////////////////////
    // TODO: Stream based hex dump util?
    public static class HexDump {
        private HexDump() {
        }

        private static final int WIDTH = 32;

        public static String dump(byte[] bytes) {
            return dump(0, bytes, 0, bytes.length);
        }

        public static String dump(long offset, byte[] bytes, int off, int len) {
            StringBuilder builder = new StringBuilder();

            int i;
            for (i = 0; i < len; i++) {
                if (i % WIDTH == 0) {
                    if (i > 0) {
                        builder.append("\n");
                    }
                    builder.append(String.format("%08x: ", i + off + offset));
                }
                else if (i > 0 && i % 2 == 0) {
                    builder.append(" ");
                }

                builder.append(String.format("%02x", bytes[i + off]));

                int next = i + 1;
                if (next % WIDTH == 0 || next == len) {
                    int leftOver = (WIDTH - (next % WIDTH)) % WIDTH;

                    if (leftOver != 0) {
                        // Pad: 5 spaces for every 2 bytes... Special care if padding is non-even.
                        int pad = leftOver / 2;

                        if (len % 2 != 0) {
                            builder.append("  ");
                        }

                        for (int j = 0; j < pad; j++) {
                            builder.append("     ");
                        }
                    }

                    builder.append("  ");
                    builder.append(toAsciiString(bytes, next - (WIDTH - leftOver) + off, next + off));
                }
            }

            return builder.toString();
        }

        private static String toAsciiString(final byte[] bytes, final int from, final int to) {
            byte[] range = Arrays.copyOfRange(bytes, from, to);

            for (int i = 0; i < range.length; i++) {
                if (range[i] < 32 || range[i] > 126) {
                    range[i] = '.'; // Unreadable char
                }
            }

            return new String(range, Charset.forName("ascii"));
        }
    }
}
