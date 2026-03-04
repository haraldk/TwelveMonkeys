/*
 * Copyright (c) 2024, Paul Allen, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.ByteOrder;

final class DDSHeader {

    // https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dx-graphics-dds-pguide
    private int flags;

    private int mipMapCount;
    private Dimension[] dimensions;

    private int pixelFormatFlags;
    private int fourCC;
    private int bitCount;
    private int redMask;
    private int greenMask;
    private int blueMask;
    private int alphaMask;

    @SuppressWarnings("unused")
    static DDSHeader read(final ImageInputStream imageInput) throws IOException {
        DDSHeader header = new DDSHeader();

        // Read MAGIC bytes [0,3]
        imageInput.setByteOrder(ByteOrder.BIG_ENDIAN);
        int magic = imageInput.readInt();
        if (magic != DDS.MAGIC) {
            throw new IIOException(String.format("Not a DDS file. Expected DDS magic 0x%8x', read 0x%8x", DDS.MAGIC, magic));
        }
        imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        // DDS_HEADER structure
        // https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-header
        int dwSize = imageInput.readInt(); // [4,7]
        if (dwSize != DDS.HEADER_SIZE) {
            throw new IIOException(String.format("Invalid DDS header size (expected %d): %d", DDS.HEADER_SIZE, dwSize));
        }

        // Verify setFlags
        header.flags = imageInput.readInt(); // [8,11]
        if (!header.getFlag(DDS.FLAG_CAPS
                | DDS.FLAG_HEIGHT
                | DDS.FLAG_WIDTH
                | DDS.FLAG_PIXELFORMAT)) {
            throw new IIOException("Required DDS Flag missing in header: " + Integer.toBinaryString(header.flags));
        }

        // Read Height & Width
        int dwHeight = imageInput.readInt(); // [12,15]
        int dwWidth = imageInput.readInt();  // [16,19]

        int dwPitchOrLinearSize = imageInput.readInt(); // [20,23]
        int dwDepth = imageInput.readInt(); // [24,27]

        // 0 = (unused) or 1 = (1 level), but still one 'base' image
        header.mipMapCount = Math.max(1, imageInput.readInt()); // [28,31]

        // build dimensions list
        header.addDimensions(dwWidth, dwHeight);

        imageInput.skipBytes(44);

        // DDS_PIXELFORMAT structure
        int px_dwSize = imageInput.readInt(); // [76,79]

        header.pixelFormatFlags = imageInput.readInt(); // [80,83]
        header.fourCC = imageInput.readInt(); // [84,87]
        header.bitCount = imageInput.readInt(); // [88,91]
        header.redMask = imageInput.readInt(); // [92,95]
        header.greenMask = imageInput.readInt(); // [96,99]
        header.blueMask = imageInput.readInt(); // [100,103]
        header.alphaMask = imageInput.readInt(); // [104,107]

        int dwCaps = imageInput.readInt(); // [108,111]
        int dwCaps2 = imageInput.readInt(); // [112,115]
        int dwCaps3 = imageInput.readInt(); // [116,119]
        int dwCaps4 = imageInput.readInt(); // [120,123]

        int dwReserved2 = imageInput.readInt(); // [124,127]

        return header;
    }

    private void addDimensions(int width, int height) {
        dimensions = new Dimension[mipMapCount];

        int w = width;
        int h = height;
        for (int i = 0; i < mipMapCount; i++) {
            dimensions[i] = new Dimension(w, h);
            w /= 2;
            h /= 2;
        }
    }

    private boolean getFlag(int mask) {
        return (flags & mask) != 0;
    }

    int getWidth(int imageIndex) {
        int lim = dimensions[imageIndex].width;
        return (lim <= 0) ? 1 : lim;
    }

    int getHeight(int imageIndex) {
        int lim =  dimensions[imageIndex].height;
        return (lim <= 0) ? 1 : lim;
    }

    int getMipMapCount() {
        return mipMapCount;
    }

    int getBitCount() {
        return bitCount;
    }

    int getFourCC() {
        return fourCC;
    }

    int getPixelFormatFlags() {
        return pixelFormatFlags;
    }

    int getRedMask() {
        return redMask;
    }

    int getGreenMask() {
        return greenMask;
    }

    int getBlueMask() {
        return blueMask;
    }

    int getAlphaMask() {
        return alphaMask;
    }
}
