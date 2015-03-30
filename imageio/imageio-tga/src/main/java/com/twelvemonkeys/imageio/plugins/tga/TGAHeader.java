/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.IndexColorModel;
import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.Charset;

final class TGAHeader {

    private int colorMapType;
    private int imageType;
    private int colorMapStart;
    private int colorMapSize;
    private int colorMapDepth;
    private int x;
    private int y;
    private int width;
    private int height;
    private int pixelDepth;
    private int attributeBits;
    private int origin;
    private int interleave;
    private String identification;
    private IndexColorModel colorMap;

    public int getImageType() {
        return imageType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPixelDepth() {
        return pixelDepth;
    }

    public int getAttributeBits() {
        return attributeBits;
    }

    public int getOrigin() {
        return origin;
    }

    public int getInterleave() {
        return interleave;
    }

    public String getIdentification() {
        return identification;
    }

    public IndexColorModel getColorMap() {
        return colorMap;
    }

    @Override public String toString() {
        return "TGAHeader{" +
                "colorMapType=" + colorMapType +
                ", imageType=" + imageType +
                ", colorMapStart=" + colorMapStart +
                ", colorMapSize=" + colorMapSize +
                ", colorMapDepth=" + colorMapDepth +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", pixelDepth=" + pixelDepth +
                ", attributeBits=" + attributeBits +
                ", origin=" + origin +
                ", interleave=" + interleave +
                (identification != null ? ", identification='" + identification + '\'' : "") +
                '}';
    }

    public static TGAHeader read(final ImageInputStream imageInput) throws IOException {
//        typedef struct _TgaHeader
//        {
//            BYTE IDLength;        /* 00h  Size of Image ID field */
//            BYTE ColorMapType;    /* 01h  Color map type */
//            BYTE ImageType;       /* 02h  Image type code */
//            WORD CMapStart;       /* 03h  Color map origin */
//            WORD CMapLength;      /* 05h  Color map length */
//            BYTE CMapDepth;       /* 07h  Depth of color map entries */
//            WORD XOffset;         /* 08h  X origin of image */
//            WORD YOffset;         /* 0Ah  Y origin of image */
//            WORD Width;           /* 0Ch  Width of image */
//            WORD Height;          /* 0Eh  Height of image */
//            BYTE PixelDepth;      /* 10h  Image pixel size */
//            BYTE ImageDescriptor; /* 11h  Image descriptor byte */
//        } TGAHEAD;
        TGAHeader header = new TGAHeader();

        int imageIdLength = imageInput.readUnsignedByte();
        header.colorMapType = imageInput.readUnsignedByte(); // 1: palette, 0: no palette, other: Unspecified... (< 127 reserved, 128-256: free for devs)
        header.imageType = imageInput.readUnsignedByte();    // 0: no image data, 1: Colormap, 2: Truecolor, 3: Monochrome, 9: Colormap + RLE, 10: Truecolor + RLE, 11: Monochrome + RLE, other: Unspecified.

        // Color map specification
        header.colorMapStart = imageInput.readUnsignedShort();
        header.colorMapSize = imageInput.readUnsignedShort(); // number of colors, not bytes..?
        header.colorMapDepth = imageInput.readUnsignedByte(); // 15, 16, 24 or 32!

        // Image specification
        header.x = imageInput.readUnsignedShort();
        header.y = imageInput.readUnsignedShort();
        header.width = imageInput.readUnsignedShort();
        header.height = imageInput.readUnsignedShort();

        header.pixelDepth = imageInput.readUnsignedByte();
        int imageDescriptor = imageInput.readUnsignedByte();
        header.attributeBits = imageDescriptor & 0xf; // Bit 0-3: number of "attribute bits" per pixel
        header.origin = (imageDescriptor & 0x30) >> 4; // Bit 4-6: origin 0: lower left, 2: upper left
        header.interleave = (imageDescriptor & 0xC0) >> 6; // Bit 7-8: interleave 0: non-interleaved, 1: two-way, 2: four way, 3: reserved

        // Image ID section, not *really* part of the header, but let's get rid of it...
        if (imageIdLength > 0) {
            byte[] idBytes = new byte[imageIdLength];
            imageInput.readFully(idBytes);

            header.identification = new String(idBytes, Charset.forName("US-ASCII"));
        }

        // Color map, not *really* part of the header
        if (header.colorMapType == TGA.COLORMAP_PALETTE) {
            header.colorMap = readColorMap(imageInput, header);
        }

        return header;
    }

    static IndexColorModel readColorMap(final DataInput stream, final TGAHeader header) throws IOException {
        int size = header.colorMapSize;
        int depth = header.colorMapDepth;
        int bytes = (depth + 7) / 8;

        byte[] cmap = new byte[size * bytes];
        stream.readFully(cmap);

        boolean hasAlpha;

        switch (depth) {
            case 16:
                // Expand 16 bit to 24 bit RGB
                byte[] temp = cmap;
                cmap = new byte[size * 3];

                for (int i = 0; i < temp.length / 2; i++) {
                    // TODO: Handle attribute bit (A)??
                    // GGGB BBBB - ARRR RRGG
                    byte low = temp[i * 2];
                    byte high = temp[i * 2 + 1];

                    byte r = (byte) (8  * ((high & 0x7C) >> 2));
                    byte g = (byte) (8 * ((high & 0x03) << 3 | (low & 0xE0) >> 5));
                    byte b = (byte) (8 * ((low & 0x1F)));

                    cmap[i * 3    ] = r;
                    cmap[i * 3 + 1] = g;
                    cmap[i * 3 + 2] = b;
                }

                hasAlpha = false;
                break;
            case 24:
                hasAlpha = false;
                break;
            case 32:
                hasAlpha = true;
                break;
            default:
                throw new IIOException("Unsupported color map depth: " + header.colorMapDepth);
        }

        return new IndexColorModel(header.pixelDepth, size, cmap, header.colorMapStart, hasAlpha);
    }
}
