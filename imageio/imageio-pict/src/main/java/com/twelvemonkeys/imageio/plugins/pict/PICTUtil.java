/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pict;

import javax.imageio.IIOException;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * PICTUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PICTUtil.java,v 1.0 Feb 16, 2009 8:46:27 PM haraldk Exp$
 */
final class PICTUtil {

    private static final String ENC_MAC_ROMAN = "MacRoman";

    public static final Charset ENCODING = initEncoding();

    private static Charset initEncoding() {
        try {
            return Charset.forName(ENC_MAC_ROMAN);
        }
        catch (UnsupportedCharsetException e) {
            return Charset.forName("ISO-8859-1");
        }
    }

    /**
     * Reads a fixed point number from the given stream.
     *
     * @param pStream the input stream
     * @return the number as a {@code double}.
     *
     * @throws java.io.IOException if an I/O error occurs during read
     */
    public static double readFixedPoint(final DataInput pStream) throws IOException {
        return pStream.readInt() / (double) (1 << 16);
    }

    static String readIdString(final DataInput pStream) throws IOException {
        byte[] bytes = new byte[4];
        pStream.readFully(bytes);
        return new String(bytes, "ASCII");
    }

    /**
     * Reads a dimension from the given stream.
     *
     * @param pStream the input stream
     * @return the dimension read
     *
     * @throws java.io.IOException if an I/O error occurs during read
     */
    public static Dimension readDimension(final DataInput pStream) throws IOException {
        int h = pStream.readShort();
        int v = pStream.readShort();
        return new Dimension(h, v);
    }

    /**
     * Reads a 32 byte fixed length Pascal string from the given input.
     * The input stream must be positioned at the length byte of the text,
     * the text will be no longer than 31 characters long.
     *
     * @param pStream the input stream
     * @return the text read
     *
     * @throws IOException if an I/O exception occurs during reading
     */
    public static String readStr31(final DataInput pStream) throws IOException {
        String text = readPascalString(pStream);
        int length = 31 - text.length();
        if (length < 0) {
            throw new IOException("String length exceeds maximum (31): " + text.length());
        }
        pStream.skipBytes(length);
        return text;
    }

    /**
     * Reads a Pascal String from the given stream.
     * The input stream must be positioned at the length byte of the text,
     * which can thus be a maximum of 255 characters long.
     *
     * @param pStream the input stream
     * @return the text read
     *
     * @throws IOException if an I/O exception occurs during reading
     */
    public static String readPascalString(final DataInput pStream) throws IOException {
        // Get as many bytes as indicated by byte count
        int length = pStream.readUnsignedByte();

        byte[] bytes = new byte[length];
        pStream.readFully(bytes, 0, length);

        return new String(bytes, ENCODING);
    }

    /**
     * Reads a {@link Pattern pattern} from the given stream.
     *
     * @param pStream the input stream
     * @return the pattern read
     *
     * @throws java.io.IOException if an I/O error occurs during read
     */
    public static Pattern readPattern(final DataInput pStream) throws IOException {
        // Get the data (8 bytes)
        byte[] data = new byte[8];
        pStream.readFully(data);
        return new BitMapPattern(data);
    }

    // TODO: Refactor, don't need both readPattern methods
    public static Pattern readPattern(final DataInput pStream, final Color fg, final Color bg) throws IOException {
        // Get the data (8 bytes)
        byte[] data = new byte[8];
        pStream.readFully(data);
        return new BitMapPattern(data, fg, bg);
    }

    /**
     * Reads a variable width {@link Pattern color pattern} from the given stream
     *
     * @param pStream the input stream
     * @return the pattern read
     *
     * @throws java.io.IOException if an I/O error occurs during read
     */
    /*
    http://developer.apple.com/DOCUMENTATION/mac/QuickDraw/QuickDraw-461.html#MARKER-9-243
    IF patType = ditherPat
    THEN
       PatType:    word;       {pattern type = 2}
       Pat1Data:   Pattern;    {old pattern data}
       RGB:        RGBColor;   {desired RGB for pattern}
    ELSE
       PatType:    word;       {pattern type = 1}
       Pat1Data:   Pattern;    {old pattern data}
       PixMap:     PixMap;
       ColorTable: ColorTable;
       PixData:    PixData;
    END;
     */
    public static Pattern readColorPattern(final DataInput pStream) throws IOException {
        short type = pStream.readShort();

        Pattern pattern;
        Pattern fallback = readPattern(pStream);

        if (type == 1) {
            // TODO: This is foobar...
            // PixMap
            // ColorTable
            // PixData
            throw new IIOException(String.format("QuickDraw pattern type '0x%04x' not implemented (yet)", type));
        }
        else if (type == 2) {
            Color color = readRGBColor(pStream);
            pattern = new PixMapPattern(color, fallback);
        }
        else {
            throw new IIOException(String.format("Unknown QuickDraw pattern type '0x%04x'", type));
        }

        return pattern;
    }

    /**
     * Reads an {@link RGBColor} record from the given stream.
     *
     * @param pStream the input stream
     * @return the color read
     *
     * @throws java.io.IOException if an I/O error occurs during read
     */
    /*
    http://developer.apple.com/DOCUMENTATION/mac/QuickDraw/QuickDraw-269.html#HEADING269-11
    RGBColor       =
    RECORD
       red:     Integer;    {red component}
       green:   Integer;    {green component}
       blue:    Integer;    {blue component}
    END;
     */
    public static Color readRGBColor(final DataInput pStream) throws IOException {
        short r = pStream.readShort();
        short g = pStream.readShort();
        short b = pStream.readShort();

        return new RGBColor(r, g, b);
    }

    /**
     * Reads a {@code ColorTable} data structure from the given stream.
     *
     * @param pStream    the input stream
     * @param pPixelSize the pixel size
     * @return the indexed color model created from the {@code ColorSpec} records read.
     *
     * @throws java.io.IOException if an I/O error occurs during read
     */
    /*
    http://developer.apple.com/DOCUMENTATION/mac/QuickDraw/QuickDraw-269.html#HEADING269-11
    ColorSpec      =
    RECORD
       value:   Integer;    {index or other value}
       rgb:     RGBColor;   {true color}
    END;

    ColorTable     =
    RECORD
       ctSeed:  LongInt;    {unique identifier from table}
       ctFlags: Integer;    {contains flags describing the ctTable field; }
                            { clear for a PixMap record}
       ctSize:  Integer;    {number of entries in the next field minus 1}
       ctTable: cSpecArray; {an array of ColorSpec records}
    END;
     */
    public static IndexColorModel readColorTable(final DataInput pStream, final int pPixelSize) throws IOException {
        // TODO: Do we need to support these?
        /*int seed = */pStream.readInt();
        /*int flags = */pStream.readUnsignedShort();
        int size = pStream.readUnsignedShort() + 1; // data is size - 1

        int[] colors = new int[size];

        for (int i = 0; i < size; i++) {
            // Read ColorSpec records
            int index = pStream.readUnsignedShort();
            Color color = readRGBColor(pStream);
            colors[index] = color.getRGB();
        }

        return new IndexColorModel(pPixelSize, size, colors, 0, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
    }
}
