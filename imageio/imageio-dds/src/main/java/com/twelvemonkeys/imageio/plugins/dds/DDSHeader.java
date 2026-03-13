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

import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.A1R5G5B5_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.A4R4G4B4_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.A8B8G8R8_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.A8R8G8B8_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.R5G6B5_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.R8G8B8_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.X1R5G5B5_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.X4R4G4B4_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.X8B8G8R8_MASKS;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.X8R8G8B8_MASKS;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;

/**
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-header">DDS_HEADER structure</a>
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dx-graphics-dds-pguide">Programming Guide for DDS</a>
 */
final class DDSHeader {

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

    DXT10Header dxt10Header;

    @SuppressWarnings("unused")
    static DDSHeader read(final ImageInputStream imageInput) throws IOException {
        DDSHeader header = new DDSHeader();

        int dwSize = imageInput.readInt(); // [4,7]
        if (dwSize != DDS.HEADER_SIZE) {
            throw new IIOException(String.format("Invalid DDS header size (expected %d): %d", DDS.HEADER_SIZE, dwSize));
        }

        // Verify flags
        header.flags = imageInput.readInt(); // [8,11]
        if (!header.hasFlag(DDS.FLAG_CAPS | DDS.FLAG_HEIGHT | DDS.FLAG_WIDTH | DDS.FLAG_PIXELFORMAT)) {
            // NOTE: The Microsoft DDS documentation mention that readers should not rely on these flags...
            throw new IIOException("Required DDS flag missing in header: " + Integer.toBinaryString(header.flags));
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
        if (px_dwSize != DDS.PIXELFORMAT_SIZE) {
            throw new IIOException(String.format("Invalid DDS pixel format structure size (expected %d): %d", DDS.PIXELFORMAT_SIZE, dwSize));
        }

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

        if (header.fourCC == DDSType.DXT10.fourCC()) {
            // If DXT10, the DXT10 header will follow immediately
            header.dxt10Header = DXT10Header.read(imageInput);
        }

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

    private boolean hasFlag(int mask) {
        return (flags & mask) == mask;
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

    DDSType getType() throws IIOException {
        if (dxt10Header != null) {
            return dxt10Header.getType();
        }

        return getRawType();
    }

    DDSType getRawType() throws IIOException {
        if ((pixelFormatFlags & DDS.PIXEL_FORMAT_FLAG_FOURCC) != 0) {
            // DXT
            return DDSType.fromFourCC(fourCC);
        }
        else if ((pixelFormatFlags & DDS.PIXEL_FORMAT_FLAG_RGB) != 0) {
            // RGB
            int alphaMask = ((pixelFormatFlags & 0x01) != 0) ? this.alphaMask : 0; // 0x01 alpha

            if (bitCount == 16) {
                if (redMask == A1R5G5B5_MASKS[0] && greenMask == A1R5G5B5_MASKS[1] && blueMask == A1R5G5B5_MASKS[2] && alphaMask == A1R5G5B5_MASKS[3]) {
                    // A1R5G5B5
                    return DDSType.A1R5G5B5;
                }
                else if (redMask == X1R5G5B5_MASKS[0] && greenMask == X1R5G5B5_MASKS[1] && blueMask == X1R5G5B5_MASKS[2] && alphaMask == X1R5G5B5_MASKS[3]) {
                    // X1R5G5B5
                    return DDSType.X1R5G5B5;
                }
                else if (redMask == A4R4G4B4_MASKS[0] && greenMask == A4R4G4B4_MASKS[1] && blueMask == A4R4G4B4_MASKS[2] && alphaMask == A4R4G4B4_MASKS[3]) {
                    // A4R4G4B4
                    return DDSType.A4R4G4B4;
                }
                else if (redMask == X4R4G4B4_MASKS[0] && greenMask == X4R4G4B4_MASKS[1] && blueMask == X4R4G4B4_MASKS[2] && alphaMask == X4R4G4B4_MASKS[3]) {
                    // X4R4G4B4
                    return DDSType.X4R4G4B4;
                }
                else if (redMask == R5G6B5_MASKS[0] && greenMask == R5G6B5_MASKS[1] && blueMask == R5G6B5_MASKS[2] && alphaMask == R5G6B5_MASKS[3]) {
                    // R5G6B5
                    return DDSType.R5G6B5;
                }

                throw new IIOException("Unsupported 16bit RGB image.");
            }
            else if (bitCount == 24) {
                if (redMask == R8G8B8_MASKS[0] && greenMask == R8G8B8_MASKS[1] && blueMask == R8G8B8_MASKS[2] && alphaMask == R8G8B8_MASKS[3]) {
                    // R8G8B8
                    return DDSType.R8G8B8;
                }

                throw new IIOException("Unsupported 24bit RGB image.");
            }
            else if (bitCount == 32) {
                if (redMask == A8B8G8R8_MASKS[0] && greenMask == A8B8G8R8_MASKS[1] && blueMask == A8B8G8R8_MASKS[2] && alphaMask == A8B8G8R8_MASKS[3]) {
                    // A8B8G8R8
                    return DDSType.A8B8G8R8;
                }
                else if (redMask == X8B8G8R8_MASKS[0] && greenMask == X8B8G8R8_MASKS[1] && blueMask == X8B8G8R8_MASKS[2] && alphaMask == X8B8G8R8_MASKS[3]) {
                    // X8B8G8R8
                    return DDSType.X8B8G8R8;
                }
                else if (redMask == A8R8G8B8_MASKS[0] && greenMask == A8R8G8B8_MASKS[1] && blueMask == A8R8G8B8_MASKS[2] && alphaMask == A8R8G8B8_MASKS[3]) {
                    // A8R8G8B8
                    return DDSType.A8R8G8B8;
                }
                else if (redMask == X8R8G8B8_MASKS[0] && greenMask == X8R8G8B8_MASKS[1] && blueMask == X8R8G8B8_MASKS[2] && alphaMask == X8R8G8B8_MASKS[3]) {
                    // X8R8G8B8
                    return DDSType.X8R8G8B8;
                }

                throw new IIOException("Unsupported 32bit RGB image.");
            }

            throw new IIOException("Unsupported bit count: " + bitCount);
        }

        throw new IIOException("Unsupported YUV or LUMINANCE image.");
    }

    @Override
    public String toString() {
        return "DDSHeader{" +
            "flags=" + Integer.toBinaryString(flags) +
            ", mipMapCount=" + mipMapCount +
            ", dimensions=" + Arrays.toString(Arrays.stream(dimensions)
                                                    .map(DDSHeader::dimensionToString)
                                                    .toArray(String[]::new)) +
            ", pixelFormatFlags=" + Integer.toBinaryString(pixelFormatFlags) +
            ", fourCC=" + fourCC +
            ", bitCount=" + bitCount +
            ", redMask=" + redMask +
            ", greenMask=" + greenMask +
            ", blueMask=" + blueMask +
            ", alphaMask=" + alphaMask +
            '}';
    }

    private static String dimensionToString(Dimension dimension) {
        return String.format("%dx%d", dimension.width, dimension.height);
    }
}
