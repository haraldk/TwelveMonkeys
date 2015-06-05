/*
 * Copyright (c) 2013, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.exif;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataWriter;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;

/**
 * EXIFWriter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFWriter.java,v 1.0 17.07.13 10:20 haraldk Exp$
 */
public final class EXIFWriter extends MetadataWriter {

    static final int WORD_LENGTH = 2;
    static final int LONGWORD_LENGTH = 4;
    static final int ENTRY_LENGTH = 12;

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

    public long writeIFD(final Collection<Entry> entries, ImageOutputStream stream) throws IOException {
        return writeIFD(new IFD(entries), stream, false);
    }

    private long writeIFD(final Directory original, ImageOutputStream stream, boolean isSubIFD) throws IOException {
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
            int length = EXIFReader.getValueLength(getType(entry), getCount(entry));

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
            List<Entry> entries = new ArrayList<Entry>(directory.size());

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

    private long writeValue(Entry entry, long dataOffset, ImageOutputStream stream) throws IOException {
        short type = getType(entry);
        int valueLength = EXIFReader.getValueLength(type, getCount(entry));

        if (valueLength <= LONGWORD_LENGTH) {
            writeValueInline(entry.getValue(), type, stream);

            // Pad
            for (int i = valueLength; i < LONGWORD_LENGTH; i++) {
                stream.write(0);
            }

            return 0;
        }
        else {
            writeValueAt(dataOffset, entry.getValue(), type, stream);

            return valueLength;
        }
    }

    private int getCount(Entry entry) {
        Object value = entry.getValue();
        return value instanceof String ? ((String) value).getBytes(Charset.forName("UTF-8")).length + 1 : entry.valueCount();
    }

    private void writeValueInline(Object value, short type, ImageOutputStream stream) throws IOException {
        if (value.getClass().isArray()) {
            switch (type) {
                case TIFF.TYPE_BYTE:
                    stream.write((byte[]) value);
                    break;
                case TIFF.TYPE_SHORT:
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
                        throw new IllegalArgumentException("Unsupported type for TIFF SHORT: " + value.getClass());
                    }

                    stream.writeInts(ints, 0, ints.length);

                    break;

                case TIFF.TYPE_RATIONAL:
                    Rational[] rationals = (Rational[]) value;
                    for (Rational rational : rationals) {
                        stream.writeInt((int) rational.numerator());
                        stream.writeInt((int) rational.denominator());
                    }

                // TODO: More types

                default:
                    throw new IllegalArgumentException("Unsupported TIFF type: " + type);
            }
        }
//        else if (value instanceof Directory) {
//            writeIFD((Directory) value, stream, false);
//        }
        else {
            switch (type) {
                case TIFF.TYPE_BYTE:
                    stream.writeByte((Integer) value);
                    break;
                case TIFF.TYPE_ASCII:
                    byte[] bytes = ((String) value).getBytes(Charset.forName("UTF-8"));
                    stream.write(bytes);
                    stream.write(0);
                    break;
                case TIFF.TYPE_SHORT:
                    stream.writeShort((Integer) value);
                    break;
                case TIFF.TYPE_LONG:
                    stream.writeInt(((Number) value).intValue());
                    break;
                case TIFF.TYPE_RATIONAL:
                    Rational rational = (Rational) value;
                    stream.writeInt((int) rational.numerator());
                    stream.writeInt((int) rational.denominator());
                    break;
                    // TODO: More types

                default:
                    throw new IllegalArgumentException("Unsupported TIFF type: " + type);
            }
        }
    }

    private void writeValueAt(long dataOffset, Object value, short type, ImageOutputStream stream) throws IOException {
        stream.writeInt(assertIntegerOffset(dataOffset));
        long position = stream.getStreamPosition();
        stream.seek(dataOffset);
        writeValueInline(value, type, stream);
        stream.seek(position);
    }

    private short getType(Entry entry) {
        if (entry instanceof EXIFEntry) {
            EXIFEntry exifEntry = (EXIFEntry) entry;
            return exifEntry.getType();
        }

        Object value = Validate.notNull(entry.getValue());

        boolean array = value.getClass().isArray();
        if (array) {
            value = Array.get(value, 0);
        }

        // Note: This "narrowing" is to keep data consistent between read/write.
        // TODO: Check for negative values and use signed types?
        if (value instanceof Byte) {
            return TIFF.TYPE_BYTE;
        }
        if (value instanceof Short) {
            if (!array && (Short) value < Byte.MAX_VALUE) {
                return TIFF.TYPE_BYTE;
            }

            return TIFF.TYPE_SHORT;
        }
        if (value instanceof Integer) {
            if (!array && (Integer) value < Short.MAX_VALUE) {
                return TIFF.TYPE_SHORT;
            }

            return TIFF.TYPE_LONG;
        }
        if (value instanceof Long) {
            if (!array && (Long) value < Integer.MAX_VALUE) {
                return TIFF.TYPE_LONG;
            }
        }

        if (value instanceof Rational) {
            return TIFF.TYPE_RATIONAL;
        }

        if (value instanceof String) {
            return TIFF.TYPE_ASCII;
        }

        // TODO: More types

        throw new UnsupportedOperationException(String.format("Method getType not implemented for entry of type %s/value of type %s", entry.getClass(), value.getClass()));
    }

    private int assertIntegerOffset(long offset) throws IIOException {
        if (offset > Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
            throw new IIOException("Integer overflow for TIFF stream");
        }

        return (int) offset;
    }
}
