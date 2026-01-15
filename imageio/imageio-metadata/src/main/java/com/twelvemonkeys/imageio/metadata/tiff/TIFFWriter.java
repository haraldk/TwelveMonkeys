/*
 * Copyright (c) 2013, Harald Kuhr
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

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataWriter;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry.getType;
import static com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry.getValueLength;

/**
 * TIFFWriter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFWriter.java,v 1.0 17.07.13 10:20 haraldk Exp$
 */
public final class TIFFWriter extends MetadataWriter {

    private static final int WORD_LENGTH = 2;
    private static final int LONGWORD_LENGTH = 4;

    // TODO: We probably want to gloss over client code writing IFDs in BigTIFF (or vice versa) somehow... Silently convert IFD -> IFD8
    private final boolean longOffsets;
    private final int offsetSize;
    private final long entryLength;
    private final int directoryCountLength;

    public TIFFWriter() {
        this(LONGWORD_LENGTH);
    }

    public TIFFWriter(int offsetSize) {
        this.offsetSize = Validate.isTrue(offsetSize == 4 || offsetSize == 8, offsetSize, "offsetSize must be 4 for TIFF or 8 for BigTIFF");

        longOffsets = offsetSize == 8;
        directoryCountLength = longOffsets ? 8 : WORD_LENGTH;
        entryLength = 2 * WORD_LENGTH + 2 * offsetSize;
    }

    public boolean write(final Collection<? extends Entry> entries, final ImageOutputStream stream) throws IOException {
        return write(new IFD(entries), stream);
    }

    @Override
    public boolean write(final Directory directory, final ImageOutputStream stream) throws IOException {
        Validate.notNull(directory);
        Validate.notNull(stream);

        // TODO: Should probably validate that the directory contains only valid TIFF entries...
        // the writer will crash on non-Integer ids and unsupported types
        // TODO: Implement the above validation in IFD constructor?

        writeTIFFHeader(stream);

        if (directory instanceof CompoundDirectory) {
            CompoundDirectory compoundDirectory = (CompoundDirectory) directory;

            for (int i = 0; i < compoundDirectory.directoryCount(); i++) {
                writeIFD(compoundDirectory.getDirectory(i), stream, false);
            }
        }
        else {
            writeIFD(directory, stream, false);
        }

        // Offset to next IFD (EOF)
        writeOffset(stream, 0);

        return true;
    }

    public void writeTIFFHeader(final ImageOutputStream stream) throws IOException {
        // Header
        ByteOrder byteOrder = stream.getByteOrder();
        stream.writeShort(byteOrder == ByteOrder.BIG_ENDIAN ? TIFF.BYTE_ORDER_MARK_BIG_ENDIAN : TIFF.BYTE_ORDER_MARK_LITTLE_ENDIAN);
        stream.writeShort(longOffsets ? TIFF.BIGTIFF_MAGIC : TIFF.TIFF_MAGIC);

        if (longOffsets) {
            stream.writeShort(offsetSize); // Always 8 in this case
            stream.writeShort(0);
        }
    }

    public long writeIFD(final Collection<Entry> entries, final ImageOutputStream stream) throws IOException {
        Validate.notNull(entries);
        Validate.notNull(stream);

        return writeIFD(new IFD(entries), stream, false);
    }

    private long writeIFD(final Directory original, final ImageOutputStream stream, final boolean isSubIFD) throws IOException {
        // TIFF spec says tags should be in increasing order, enforce that when writing
        Directory ordered = ensureOrderedDirectory(original);

        // Compute space needed for extra storage first, then write the offset to the IFD, so that the layout is:
        // IFD offset
        // <data including sub-IFDs>
        // IFD entries (values/offsets)
        long dataOffset = stream.getStreamPosition();
        long dataSize = computeDataSize(ordered);

        // Offset to this IFD
        final long ifdOffset = stream.getStreamPosition() + dataSize + offsetSize;

        if (!isSubIFD) {
            writeOffset(stream, ifdOffset);
            dataOffset += offsetSize;

            // Seek to offset
            stream.seek(ifdOffset);
        }
        else {
            dataOffset += directoryCountLength + ordered.size() * entryLength;
        }

        // Write directory
        writeDirectoryCount(stream, ordered.size());

        for (Entry entry : ordered) {
            // Write tag id, type & value count
            stream.writeShort((Integer) entry.getIdentifier());
            stream.writeShort(getType(entry));
            writeValueCount(stream, getCount(entry));

            // Write value
            Object value = entry.getValue();
            if (value instanceof Directory) {
                if (value instanceof CompoundDirectory) {
                    // Can't have both nested and linked IFDs
                    throw new AssertionError("SubIFD cannot contain linked IFDs");
                }

                // We can't write offset here, we need to write value, as both LONG/IFD and LONG8/IFD8 is allowed
                // TODO: Or possibly gloss over, by always writing IFD8 for BigTIFF?
                long streamPosition = stream.getStreamPosition() + offsetSize;
                writeValueInline(dataOffset, getType(entry), stream);
                stream.seek(dataOffset);
                Directory subIFD = (Directory) value;
                writeIFD(subIFD, stream, true);
                dataOffset += computeDataSize(subIFD) + directoryCountLength + subIFD.size() * entryLength;
                stream.seek(streamPosition);
            }
            else {
                dataOffset += writeValue(entry, dataOffset, stream);
            }
        }

        return ifdOffset;
    }

    private void writeDirectoryCount(ImageOutputStream stream, int count) throws IOException {
        if (longOffsets) {
            stream.writeLong(count);
        }
        else {
            stream.writeShort(count);
        }
    }

    private void writeValueCount(ImageOutputStream stream, int count) throws IOException {
        if (longOffsets) {
            stream.writeLong(count);
        }
        else {
            stream.writeInt(count);
        }
    }

    public long computeIFDSize(final Collection<? extends Entry> directory) {
        return directoryCountLength + computeDataSize(new IFD(directory)) + directory.size() * entryLength;
    }

    private long computeDataSize(final Directory directory) {
        long dataSize = 0;

        for (Entry entry : directory) {
            long length = getValueLength(getType(entry), getCount(entry));

            if (length < 0) {
                throw new IllegalArgumentException(String.format("Unknown size for entry %s", entry));
            }

            if (length > offsetSize) {
                dataSize += length;
            }

            if (entry.getValue() instanceof Directory) {
                Directory subIFD = (Directory) entry.getValue();
                long subIFDSize = directoryCountLength + computeDataSize(subIFD) + subIFD.size() * entryLength;
                dataSize += subIFDSize;
            }
        }

        return dataSize;
    }

    private Directory ensureOrderedDirectory(final Directory directory) {
        if (!isSorted(directory)) {
            List<Entry> entries = new ArrayList<>(directory.size());

            for (Entry entry : directory) {
                entries.add(entry);
            }

            Collections.sort(entries, new Comparator<Entry>() {
                public int compare(Entry left, Entry right) {
                    return (Integer) left.getIdentifier() - (Integer) right.getIdentifier();
                }
            });

            return new IFD(entries);
        }

        return directory;
    }

    private boolean isSorted(final Directory directory) {
        int lastTag = 0;

        for (Entry entry : directory) {
            int tag = ((Integer) entry.getIdentifier()) & 0xffff;

            if (tag < lastTag) {
                return false;
            }

            lastTag = tag;
        }

        return true;
    }

    private long writeValue(final Entry entry, final long dataOffset, final ImageOutputStream stream) throws IOException {
        short type = getType(entry);
        long valueLength = getValueLength(type, getCount(entry));

        if (valueLength <= offsetSize) {
            writeValueInline(entry.getValue(), type, stream);

            // Pad
            for (long i = valueLength; i < offsetSize; i++) {
                stream.write(0);
            }

            return 0;
        }
        else {
            writeValueAt(dataOffset, entry.getValue(), type, stream);

            return valueLength;
        }
    }

    private int getCount(final Entry entry) {
        Object value = entry.getValue();

        if (value instanceof String) {
            return computeStringLength((String) value);
        }
        else if (value instanceof String[]) {
            return computeStringLength((String[]) value);
        }
        else {
            return entry.valueCount();
        }
    }

    private int computeStringLength(String... values) {
        int sum = 0;

        for (String value : values) {
            sum += value.getBytes(StandardCharsets.UTF_8).length + 1;
        }

        return sum;
    }

    private void writeValueInline(final Object value, final short type, final ImageOutputStream stream) throws IOException {
        if (value.getClass().isArray()) {
            switch (type) {
                case TIFF.TYPE_UNDEFINED:
                case TIFF.TYPE_BYTE:
                case TIFF.TYPE_SBYTE:
                    stream.write((byte[]) value);
                    break;

                case TIFF.TYPE_SHORT:
                case TIFF.TYPE_SSHORT:
                    short[] shorts;

                    if (value instanceof short[]) {
                        shorts = (short[]) value;
                    }
                    else if (value instanceof int[]) {
                        int[] ints = (int[]) value;
                        shorts = new short[ints.length];

                        for (int i = 0; i < ints.length; i++) {
                            shorts[i] = (short) ints[i];
                        }

                    }
                    else if (value instanceof long[]) {
                        long[] longs = (long[]) value;
                        shorts = new short[longs.length];

                        for (int i = 0; i < longs.length; i++) {
                            shorts[i] = (short) longs[i];
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Unsupported type for TIFF SHORT: " + value.getClass());
                    }

                    stream.writeShorts(shorts, 0, shorts.length);
                    break;

                case TIFF.TYPE_LONG:
                case TIFF.TYPE_SLONG:
                    int[] ints;

                    if (value instanceof int[]) {
                        ints = (int[]) value;
                    }
                    else if (value instanceof long[]) {
                        long[] longs = (long[]) value;
                        ints = new int[longs.length];

                        for (int i = 0; i < longs.length; i++) {
                            ints[i] = (int) longs[i];
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Unsupported type for TIFF LONG: " + value.getClass());
                    }

                    stream.writeInts(ints, 0, ints.length);
                    break;

                case TIFF.TYPE_RATIONAL:
                case TIFF.TYPE_SRATIONAL:
                    Rational[] rationals = (Rational[]) value;
                    for (Rational rational : rationals) {
                        stream.writeInt((int) rational.numerator());
                        stream.writeInt((int) rational.denominator());
                    }

                    break;

                case TIFF.TYPE_FLOAT:
                    float[] floats;

                    if (value instanceof float[]) {
                        floats = (float[]) value;
                    }
                    else {
                        throw new IllegalArgumentException("Unsupported type for TIFF FLOAT: " + value.getClass());
                    }

                    stream.writeFloats(floats, 0, floats.length);

                    break;

                case TIFF.TYPE_DOUBLE:
                    double[] doubles;

                    if (value instanceof double[]) {
                        doubles = (double[]) value;
                    }
                    else {
                        throw new IllegalArgumentException("Unsupported type for TIFF DOUBLE: " + value.getClass());
                    }

                    stream.writeDoubles(doubles, 0, doubles.length);

                    break;
                case TIFF.TYPE_LONG8:
                case TIFF.TYPE_SLONG8:
                    if (longOffsets) {
                        long[] longs;

                        if (value instanceof long[]) {
                            longs = (long[]) value;
                        }
                        else {
                            throw new IllegalArgumentException("Unsupported type for TIFF LONG8: " + value.getClass());
                        }

                        stream.writeLongs(longs, 0, longs.length);

                        break;
                    }
                case TIFF.TYPE_ASCII:
                    writeStrings(stream, (String[]) value);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported TIFF type: " + type);
            }
        }
        else {
            switch (type) {
                case TIFF.TYPE_BYTE:
                case TIFF.TYPE_SBYTE:
                case TIFF.TYPE_UNDEFINED:
                    stream.writeByte(((Number) value).intValue());
                    break;
                case TIFF.TYPE_ASCII:
                    writeStrings(stream, (String) value);
                    break;
                case TIFF.TYPE_SHORT:
                case TIFF.TYPE_SSHORT:
                    stream.writeShort(((Number) value).intValue());
                    break;
                case TIFF.TYPE_LONG:
                case TIFF.TYPE_SLONG:
                case TIFF.TYPE_IFD:
                    stream.writeInt(((Number) value).intValue());
                    break;
                case TIFF.TYPE_RATIONAL:
                case TIFF.TYPE_SRATIONAL:
                    Rational rational = (Rational) value;
                    stream.writeInt((int) rational.numerator());
                    stream.writeInt((int) rational.denominator());
                    break;
                case TIFF.TYPE_FLOAT:
                    stream.writeFloat(((Number) value).floatValue());
                    break;
                case TIFF.TYPE_DOUBLE:
                    stream.writeDouble(((Number) value).doubleValue());
                    break;
                case TIFF.TYPE_LONG8:
                case TIFF.TYPE_SLONG8:
                case TIFF.TYPE_IFD8:
                    if (longOffsets) {
                        stream.writeLong(((Number) value).longValue());
                        break;
                    }

                default:
                    throw new IllegalArgumentException("Unsupported TIFF type: " + type);
            }
        }
    }

    private void writeStrings(ImageOutputStream stream, String... values) throws IOException {
        for (String value : values) {
            stream.write(value.getBytes(StandardCharsets.UTF_8));
            stream.write(0);
        }
    }

    private void writeValueAt(final long dataOffset, final Object value, final short type, final ImageOutputStream stream) throws IOException {
        writeOffset(stream, dataOffset);
        long position = stream.getStreamPosition();
        stream.seek(dataOffset);
        writeValueInline(value, type, stream);
        stream.seek(position);
    }

    public void writeOffset(final ImageOutputStream output, long offset) throws IOException {
        if (longOffsets) {
            output.writeLong(assertLongOffset(offset));
        }
        else {
            output.writeInt(assertIntegerOffset(offset)); // Treated as unsigned
        }
    }

    public int offsetSize() {
        return offsetSize;
    }

    private int assertIntegerOffset(final long offset) throws IIOException {
        if (offset < 0 || offset > Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
            throw new IIOException("Integer overflow for TIFF stream");
        }

        return (int) offset;
    }

    private long assertLongOffset(final long offset) throws IIOException {
        if (offset < 0) {
            throw new IIOException("Long overflow for BigTIFF stream");
        }

        return offset;
    }
}
