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

package com.twelvemonkeys.imageio.plugins.pcx;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.Arrays;

final class PCXHeader {
    private static final IndexColorModel MONOCHROME = new IndexColorModel(1, 2, new int[] {0, -1}, 0, false, -1, DataBuffer.TYPE_BYTE);

    private int version;
    private int compression;
    private int bitsPerPixel;
    private int width;
    private int height;
    private int hdpi;
    private int vdpi;
    private byte[] palette;
    private int channels;
    private int bytesPerLine;
    private int paletteInfo;
    private int hScreenSize;
    private int vScreenSize;

    public int getVersion() {
        return version;
    }

    public int getCompression() {
        return compression;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getChannels() {
        return channels;
    }

    public int getBytesPerLine() {
        return bytesPerLine;
    }

    public IndexColorModel getEGAPalette() {
        // Test for CGA modes
        if (isCGAVideoMode4() || isCGAVideoMode5() || isCGAVideoMode6()) {
            return CGAColorModel.create(palette, bitsPerPixel);
        }

        // Test if we should use a default B/W palette
        if (bitsPerPixel == 1 && channels == 1 && (version < PCX.VERSION_2_X_WINDOWS || isDummyPalette())) {
            return MONOCHROME;
        }

        int bits = channels * bitsPerPixel;
        return new IndexColorModel(bits, Math.min(16, 1 << bits), palette, 0, false);
    }

    private boolean isCGAVideoMode4() {
        return bitsPerPixel * channels == 2 && width == 320 && hdpi == 320 && height == 200 && vdpi == 200;
    }

    private boolean isCGAVideoMode5() {
        return bitsPerPixel == 1 && channels == 1 && width == 320 && hdpi == 320 && height == 200 && vdpi == 200;
    }

    private boolean isCGAVideoMode6() {
        return bitsPerPixel == 1 && channels == 1 && width == 640 && hdpi == 640 && height == 200 && vdpi == 200;
    }

    private boolean isDummyPalette() {
        return isEmptyPalette() || isPhotoshopPalette();
    }

    private boolean isEmptyPalette() {
        // All black
        for (int i = 0; i < 48; i++) {
            if (palette[i] != 0) {
                return false;
            }
        }

        return true;
    }

    private boolean isPhotoshopPalette() {
        // Written by Photoshop: 15,15,15, 14,14,14, ... 0,0,0
        for (int i = 0; i < 16; i++) {
            int off = i * 3;

            if (palette[off] != 15 - i || palette[off + 1] != 15 - i || palette[off + 2] != 15 - i) {
                return false;
            }
        }

        return true;
    }

    @Override public String toString() {
        return "PCXHeader[" +
                "version=" + version +
                ", compression=" + compression +
                ", bitsPerPixel=" + bitsPerPixel +
                ", width=" + width +
                ", height=" + height +
                ", hdpi=" + hdpi +
                ", vdpi=" + vdpi +
                ", channels=" + channels +
                ", bytesPerLine=" + bytesPerLine +
                ", paletteInfo=" + paletteInfo +
                ", hScreenSize=" + hScreenSize +
                ", vScreenSize=" + vScreenSize +
                ", palette=" + Arrays.toString(palette) +
                ']';
    }

    public static PCXHeader read(final ImageInputStream imageInput) throws IOException {
//        typedef struct _PcxHeader
//        {
//            BYTE	Identifier;        /* PCX Id Number (Always 0x0A) */
//            BYTE	Version;           /* Version Number */
//            BYTE	Encoding;          /* Encoding Format */
//            BYTE	BitsPerPixel;      /* Bits per Pixel */
//            WORD	XStart;            /* Left of image */
//            WORD	YStart;            /* Top of Image */
//            WORD	XEnd;              /* Right of Image
//            WORD	YEnd;              /* Bottom of image */
//            WORD	HorzRes;           /* Horizontal Resolution */
//            WORD	VertRes;           /* Vertical Resolution */
//            BYTE	Palette[48];       /* 16-Color EGA Palette */
//            BYTE	Reserved1;         /* Reserved (Always 0) */
//            BYTE	NumBitPlanes;      /* Number of Bit Planes */
//            WORD	BytesPerLine;      /* Bytes per Scan-line */
//            WORD	PaletteType;       /* Palette Type */
//            WORD	HorzScreenSize;    /* Horizontal Screen Size */
//            WORD	VertScreenSize;    /* Vertical Screen Size */
//            BYTE	Reserved2[54];     /* Reserved (Always 0) */
//        } PCXHEAD;

        byte magic = imageInput.readByte();
        if (magic != PCX.MAGIC) {
            throw new IIOException(String.format("Not a PCX image. Expected PCX magic 0x%02x: 0x%02x", PCX.MAGIC, magic));
        }

        PCXHeader header = new PCXHeader();

        header.version = imageInput.readUnsignedByte();
        header.compression = imageInput.readUnsignedByte();
        header.bitsPerPixel = imageInput.readUnsignedByte();

        int xStart = imageInput.readUnsignedShort();
        int yStart = imageInput.readUnsignedShort();
        header.width = imageInput.readUnsignedShort() - xStart + 1;
        header.height = imageInput.readUnsignedShort() - yStart + 1;

        header.hdpi = imageInput.readUnsignedShort();
        header.vdpi = imageInput.readUnsignedShort();

        byte[] palette = new byte[48];
        imageInput.readFully(palette); // 16 RGB triplets
        header.palette = palette;

        imageInput.readUnsignedByte(); // Reserved, should be 0

        header.channels = imageInput.readUnsignedByte();      // Channels or Bit planes
        header.bytesPerLine = imageInput.readUnsignedShort(); // Must be even!

        header.paletteInfo = imageInput.readUnsignedShort() & 0x2; // 1 == Color/BW, 2 == Gray. Ignored

        header.hScreenSize = imageInput.readUnsignedShort();
        header.vScreenSize = imageInput.readUnsignedShort();

        imageInput.skipBytes(PCX.HEADER_SIZE - imageInput.getStreamPosition());

        return header;
    }
}
