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

package com.twelvemonkeys.imageio.plugins.sgi;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

final class SGIHeader {
    private int compression;
    private int bytesPerPixel;
    private int dimensions;
    private int width;
    private int height;
    private int channels;
    private int minValue;
    private int maxValue;
    private String name;
    private int colorMode;

    public int getCompression() {
        return compression;
    }

    public int getBytesPerPixel() {
        return bytesPerPixel;
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

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public String getName() {
        return name;
    }

    public int getColorMode() {
        return colorMode;
    }

    @Override public String toString() {
        return "SGIHeader{" +
                "compression=" + compression +
                ", bytesPerPixel=" + bytesPerPixel +
                ", dimensions=" + dimensions +
                ", width=" + width +
                ", height=" + height +
                ", channels=" + channels +
                ", minValue=" + minValue +
                ", maxValue=" + maxValue +
                ", name='" + name + '\'' +
                ", colorMode=" + colorMode +
                '}';
    }

    public static SGIHeader read(final ImageInputStream imageInput) throws IOException {
//        typedef struct _SGIHeader
//        {
//            SHORT Magic;          /* Identification number (474) */
//            CHAR Storage;         /* Compression flag */
//            CHAR Bpc;             /* Bytes per pixel */
//            WORD Dimension;       /* Number of image dimensions */
//            WORD XSize;           /* Width of image in pixels */
//            WORD YSize;           /* Height of image in pixels */
//            WORD ZSize;           /* Number of bit channels */
//            LONG PixMin;          /* Smallest pixel value */
//            LONG PixMax;          /* Largest pixel value */
//            CHAR Dummy1[4];       /* Not used */
//            CHAR ImageName[80];   /* Name of image */
//            LONG ColorMap;        /* Format of pixel data */
//            CHAR Dummy2[404];     /* Not used */
//        } SGIHEAD;
        short magic = imageInput.readShort();
        if (magic != SGI.MAGIC) {
            throw new IIOException(String.format("Not an SGI image. Expected SGI magic %04x, read %04x", SGI.MAGIC, magic));
        }

        SGIHeader header = new SGIHeader();

        header.compression = imageInput.readUnsignedByte();
        header.bytesPerPixel = imageInput.readUnsignedByte();

        header.dimensions = imageInput.readUnsignedShort();
        header.width = imageInput.readUnsignedShort();
        header.height = imageInput.readUnsignedShort();
        header.channels = imageInput.readUnsignedShort();

        header.minValue = imageInput.readInt();
        header.maxValue = imageInput.readInt();

        imageInput.readInt(); // Ignore

        byte[] nameBytes = new byte[80];
        imageInput.readFully(nameBytes);
        header.name = toAsciiString(nameBytes);

        header.colorMode = imageInput.readInt();

        imageInput.skipBytes(404);

        return header;
    }

    private static String toAsciiString(final byte[] bytes) {
        // Find null-terminator
        int len = bytes.length;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                len = i;
                break;
            }
        }

        return new String(bytes, 0, len, Charset.forName("ASCII"));
    }
}
