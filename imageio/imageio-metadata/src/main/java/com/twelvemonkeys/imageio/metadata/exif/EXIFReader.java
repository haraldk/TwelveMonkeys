/*
 * Copyright (c) 2009, Harald Kuhr
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

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * EXIFReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFReader.java,v 1.0 Nov 13, 2009 5:42:51 PM haraldk Exp$
 */
public final class EXIFReader extends MetadataReader {

    @Override
    public Directory read(final ImageInputStream pInput) throws IOException {
        byte[] bom = new byte[2];
        pInput.readFully(bom);
        if (bom[0] == 'I' && bom[1] == 'I') {
            pInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
        else if (!(bom[0] == 'M' && bom[1] == 'M')) {
            throw new IIOException(String.format("Invalid TIFF byte order mark '%s', expected: 'II' or 'MM'", StringUtil.decode(bom, 0, bom.length, "ASCII")));
        }

        int magic = pInput.readUnsignedShort();
        if (magic != TIFF.TIFF_MAGIC) {
            throw new IIOException(String.format("Wrong TIFF magic in EXIF data: %04x, expected: %04x", magic,  TIFF.TIFF_MAGIC));
        }

        long directoryOffset = pInput.readUnsignedInt();

        return readDirectory(pInput, directoryOffset);
    }

    private EXIFDirectory readDirectory(final ImageInputStream pInput, final long pOffset) throws IOException {
        List<Entry> entries = new ArrayList<Entry>();

        pInput.seek(pOffset);
        int entryCount = pInput.readUnsignedShort();

        for (int i = 0; i < entryCount; i++) {
            entries.add(readEntry(pInput));
        }

        long nextOffset = pInput.readUnsignedInt();

        if (nextOffset != 0) {
            EXIFDirectory next = readDirectory(pInput, nextOffset);

            for (Entry entry : next) {
                entries.add(entry);
            }
        }

        return new EXIFDirectory(entries);
    }

    private EXIFEntry readEntry(final ImageInputStream pInput) throws IOException {
        int tagId = pInput.readUnsignedShort();

        short type = pInput.readShort();
        int count = pInput.readInt(); // Number of values

        Object value;

        if (tagId == TIFF.IFD_EXIF || tagId == TIFF.IFD_GPS || tagId == TIFF.IFD_INTEROP) {
            // Parse sub IFDs
            long offset = pInput.readUnsignedInt();
            pInput.mark();

            try {
                value = readDirectory(pInput, offset);
            }
            finally {
                pInput.reset();
            }
        }
        else {
            int valueLength = getValueLength(type, count);

            if (valueLength > 0 && valueLength <= 4) {
                value = readValueInLine(pInput, type, count);
                pInput.skipBytes(4 - valueLength);
            }
            else {
                long valueOffset = pInput.readUnsignedInt(); // This is the *value* iff the value size is <= 4 bytes
                value = readValue(pInput, valueOffset, type, count);
            }
        }

        return new EXIFEntry(tagId, value, type);
    }

    private Object readValue(final ImageInputStream pInput, final long pOffset, final short pType, final int pCount) throws IOException {
        long pos = pInput.getStreamPosition();
        try {
            pInput.seek(pOffset);
            return readValueInLine(pInput, pType, pCount);
        }
        finally {
            pInput.seek(pos);
        }
    }

    private Object readValueInLine(final ImageInputStream pInput, final short pType, final int pCount) throws IOException {
        return readValueDirect(pInput, pType, pCount);
    }

    private static Object readValueDirect(final ImageInputStream pInput, final short pType, final int pCount) throws IOException {
        switch (pType) {
            case 2:
                // TODO: This might be UTF-8 or ISO-8859-1, even though spec says ASCII
                byte[] ascii = new byte[pCount];
                pInput.readFully(ascii);
                return StringUtil.decode(ascii, 0, ascii.length, "UTF-8"); // UTF-8 is ASCII compatible
            case 1:
                if (pCount == 1) {
                    return pInput.readUnsignedByte();
                }
            case 6:
                if (pCount == 1) {
                    return pInput.readByte();
                }
            case 7:
                byte[] bytes = new byte[pCount];
                pInput.readFully(bytes);
                return bytes;
            case 3:
                if (pCount == 1) {
                    return pInput.readUnsignedShort();
                }
            case 8:
                if (pCount == 1) {
                    return pInput.readShort();
                }

                short[] shorts = new short[pCount];
                pInput.readFully(shorts, 0, shorts.length);
                return shorts;
            case 4:
                if (pCount == 1) {
                    return pInput.readUnsignedInt();
                }
            case 9:
                if (pCount == 1) {
                    return pInput.readInt();
                }

                int[] ints = new int[pCount];
                pInput.readFully(ints, 0, ints.length);
                return ints;
            case 11:
                if (pCount == 1) {
                    return pInput.readFloat();
                }

                float[] floats = new float[pCount];
                pInput.readFully(floats, 0, floats.length);
                return floats;
            case 12:
                if (pCount == 1) {
                    return pInput.readDouble();
                }

                double[] doubles = new double[pCount];
                pInput.readFully(doubles, 0, doubles.length);
                return doubles;

            case 5:
                if (pCount == 1) {
                    return new Rational(pInput.readUnsignedInt(), pInput.readUnsignedInt());
                }

                Rational[] rationals = new Rational[pCount];
                for (int i = 0; i < rationals.length; i++) {
                    rationals[i] = new Rational(pInput.readUnsignedInt(), pInput.readUnsignedInt());
                }

                return rationals;
            case 10:
                if (pCount == 1) {
                    return new Rational(pInput.readInt(), pInput.readInt());
                }

                Rational[] srationals = new Rational[pCount];
                for (int i = 0; i < srationals.length; i++) {
                    srationals[i] = new Rational(pInput.readInt(), pInput.readInt());
                }

                return srationals;

            default:
                throw new IIOException(String.format("Unknown EXIF type '%s'", pType));
        }
    }

    private int getValueLength(final int pType, final int pCount) {
        if (pType > 0 && pType <= TIFF.TYPE_LENGTHS.length) {
            return TIFF.TYPE_LENGTHS[pType - 1] * pCount;
        }

        return -1;
    }
}
