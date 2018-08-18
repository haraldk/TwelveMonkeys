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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private static final int ENTRY_LENGTH = 12;

    public boolean write(final Collection<Entry> entries, final ImageOutputStream stream) throws IOException {
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
        stream.writeInt(0);

        return true;
    }

    public void writeTIFFHeader(final ImageOutputStream stream) throws IOException {
        // Header
        ByteOrder byteOrder = stream.getByteOrder();
        stream.writeShort(byteOrder == ByteOrder.BIG_ENDIAN ? TIFF.BYTE_ORDER_MARK_BIG_ENDIAN : TIFF.BYTE_ORDER_MARK_LITTLE_ENDIAN);
        stream.writeShort(42);
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
        final long ifdOffset = stream.getStreamPosition() + dataSize + LONGWORD_LENGTH;

        if (!isSubIFD) {
            stream.writeInt(assertIntegerOffset(ifdOffset));
            dataOffset += LONGWORD_LENGTH;

            // Seek to offset
            stream.seek(ifdOffset);
        }
        else {
            dataOffset += WORD_LENGTH + ordered.size() * ENTRY_LENGTH;
        }

        // Write directory
        stream.writeShort(ordered.size());

        for (Entry entry : ordered) {
            // Write tag id
            stream.writeShort((Integer) entry.getIdentifier());
            // Write tag type
            stream.writeShort(getType(entry));
            // Write value count
            stream.writeInt(getCount(entry));

            // Write value
            if (entry.getValue() instanceof Directory) {
                // TODO: This could possibly be a compound directory, in which case the count should be > 1
                stream.writeInt(assertIntegerOffset(dataOffset));
                long streamPosition = stream.getStreamPosition();
                stream.seek(dataOffset);
                Directory subIFD = (Directory) entry.getValue();
                writeIFD(subIFD, stream, true);
                dataOffset += computeDataSize(subIFD);
                stream.seek(streamPosition);
            }
            else {
                dataOffset += writeValue(entry, dataOffset, stream);
            }
        }

        return ifdOffset;
    }

    public long computeIFDSize(final Collection<Entry> directory) {
        return WORD_LENGTH + computeDataSize(new IFD(directory)) + directory.size() * ENTRY_LENGTH;
    }

    private long computeDataSize(final Directory directory) {
        long dataSize = 0;

        for (Entry entry : directory) {
            long length = getValueLength(getType(entry), getCount(entry));

            if (length < 0) {
                throw new IllegalArgumentException(String.format("Unknown size for entry %s", entry));
            }

            if (length > LONGWORD_LENGTH) {
                dataSize += length;
            }

            if (entry.getValue() instanceof Directory) {
                Directory subIFD = (Directory) entry.getValue();
                long subIFDSize = WORD_LENGTH + subIFD.size() * ENTRY_LENGTH + computeDataSize(subIFD);
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

        if (valueLength <= LONGWORD_LENGTH) {
            writeValueInline(entry.getValue(), type, stream);

            // Pad
            for (long i = valueLength; i < LONGWORD_LENGTH; i++) {
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
        return value instanceof String ? ((String) value).getBytes(Charset.forName("UTF-8")).length + 1 : entry.valueCount();
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
                        throw new IllegalArgumentException("Unsupported type for TIFF FLOAT: " + value.getClass());
                    }

                    stream.writeDoubles(doubles, 0, doubles.length);

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
                    byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                    stream.write(bytes);
                    stream.write(0);
                    break;
                case TIFF.TYPE_SHORT:
                case TIFF.TYPE_SSHORT:
                    stream.writeShort(((Number) value).intValue());
                    break;
                case TIFF.TYPE_LONG:
                case TIFF.TYPE_SLONG:
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

                default:
                    throw new IllegalArgumentException("Unsupported TIFF type: " + type);
            }
        }
    }

    private void writeValueAt(final long dataOffset, final Object value, final short type, final ImageOutputStream stream) throws IOException {
        stream.writeInt(assertIntegerOffset(dataOffset));
        long position = stream.getStreamPosition();
        stream.seek(dataOffset);
        writeValueInline(value, type, stream);
        stream.seek(position);
    }

    private int assertIntegerOffset(long offset) throws IIOException {
        if (offset > Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
            throw new IIOException("Integer overflow for TIFF stream");
        }

        return (int) offset;
    }
}
