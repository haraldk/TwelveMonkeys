/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import javax.imageio.IIOException;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.twelvemonkeys.lang.Validate.notNull;
import static java.awt.color.ColorSpace.TYPE_GRAY;
import static java.awt.color.ColorSpace.TYPE_RGB;

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

    int getImageType() {
        return imageType;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    int getPixelDepth() {
        return pixelDepth;
    }

    int getAttributeBits() {
        return attributeBits;
    }

    int getOrigin() {
        return origin;
    }

    int getInterleave() {
        return interleave;
    }

    String getIdentification() {
        return identification;
    }

    IndexColorModel getColorMap() {
        return colorMap;
    }

    @Override
    public String toString() {
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

    static TGAHeader from(final RenderedImage image, final ImageWriteParam param)  {
        notNull(image, "image");

        ColorModel colorModel = image.getColorModel();
        IndexColorModel colorMap = colorModel instanceof IndexColorModel ? (IndexColorModel) colorModel : null;

        TGAHeader header = new TGAHeader();

        header.colorMapType = colorMap != null ? 1 : 0;
        header.imageType = getImageType(colorModel, param);
        header.colorMapStart = 0;
        header.colorMapSize = colorMap != null ? colorMap.getMapSize() : 0;
        header.colorMapDepth = colorMap != null ? (colorMap.hasAlpha() ? 32 : 24) : 0;

        header.x = 0;
        header.y = 0;

        header.width = image.getWidth(); // TODO: Param source region/subsampling might affect this
        header.height = image.getHeight(); // // TODO: Param source region/subsampling might affect this
        header.pixelDepth = colorModel.getPixelSize() == 15 ? 16 : colorModel.getPixelSize();

        header.origin = TGA.ORIGIN_UPPER_LEFT; // TODO: Allow parameter to control this?
        header.attributeBits = colorModel.hasAlpha() ? 8 : 0; // TODO: FixMe

        header.identification = null;
        header.colorMap = colorMap;

        return header;
    }

    private static int getImageType(final ColorModel colorModel, final ImageWriteParam param) {
        int uncompressedType;

        if (colorModel instanceof IndexColorModel) {
           uncompressedType = TGA.IMAGETYPE_COLORMAPPED;
        }
        else {
            switch (colorModel.getColorSpace().getType()) {
                case TYPE_RGB:
                    uncompressedType = TGA.IMAGETYPE_TRUECOLOR;
                    break;
                case TYPE_GRAY:
                    uncompressedType = TGA.IMAGETYPE_MONOCHROME;
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported color space for TGA: " + colorModel.getColorSpace());
            }
        }

        return uncompressedType | (TGAImageWriteParam.isRLE(param) ? 8 : 0);
    }

    void write(final DataOutput stream) throws IOException {
        byte[] idBytes = identification != null ? identification.getBytes(StandardCharsets.US_ASCII) : new byte[0];

        stream.writeByte(idBytes.length);
        stream.writeByte(colorMapType);
        stream.writeByte(imageType);
        stream.writeShort(colorMapStart);
        stream.writeShort(colorMapSize);
        stream.writeByte(colorMapDepth);

        stream.writeShort(x);
        stream.writeShort(y);
        stream.writeShort(width);
        stream.writeShort(height);
        stream.writeByte(pixelDepth);
        stream.writeByte(attributeBits | origin << 4 | interleave << 6);

        // Identification
        stream.write(idBytes);

        // Color map
        if (colorMap != null) {
            int[] rgb = new int[colorMap.getMapSize()];
            colorMap.getRGBs(rgb);

            int components = colorMap.hasAlpha() ? 4 : 3;
            byte[] cmap = new byte[rgb.length * components];
            for (int i = 0; i < rgb.length; i++) {
                cmap[i * components    ] = (byte) ((rgb[i] >> 16) & 0xff);
                cmap[i * components + 1] = (byte) ((rgb[i] >>  8) & 0xff);
                cmap[i * components + 2] = (byte) ((rgb[i]      ) & 0xff);

                if (components == 4) {
                    cmap[i * components + 3] = (byte) ((rgb[i] >>> 24) & 0xff);
                }
            }

            stream.write(cmap);
        }
    }

    static TGAHeader read(final ImageInputStream imageInput) throws IOException {
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

            header.identification = new String(idBytes, StandardCharsets.US_ASCII);
        }

        // Color map, not *really* part of the header
        if (header.colorMapType == TGA.COLORMAP_PALETTE) {
            header.colorMap = readColorMap(imageInput, header);
        }

        return header;
    }

    private static IndexColorModel readColorMap(final DataInput stream, final TGAHeader header) throws IOException {
        int size = header.colorMapSize;
        int depth = header.colorMapDepth;
        int bytes = (depth + 7) / 8;

        byte[] cmap = new byte[size * bytes];
        stream.readFully(cmap);

        boolean hasAlpha;

        switch (depth) {
            case 16:
                // Expand 16 (15) bit to 24 bit RGB
                byte[] temp = cmap;
                cmap = new byte[size * 3];

                for (int i = 0; i < temp.length / 2; i++) {
                    // TODO: Handle attribute bit (A)??
                    // GGGB BBBB - ARRR RRGG
                    byte low = temp[i * 2];
                    byte high = temp[i * 2 + 1];

                    cmap[i * 3    ] = (byte) (((high & 0x7C) >> 2) << 3);
                    cmap[i * 3 + 1] = (byte) (((high & 0x03) << 3 | (low & 0xE0) >> 5) << 3);
                    cmap[i * 3 + 2] = (byte) (((low & 0x1F)) << 3);
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
