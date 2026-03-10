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

import static com.twelvemonkeys.imageio.plugins.dds.BlockCompression.*;
import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.*;

/**
 * <a href="https://learn.microsoft.com/en-us/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-block-compression#compression-algorithms">Compression Algorithms</a>
 * <a href="https://github.com/microsoft/DirectXTK12/wiki/DDSTextureLoader#remarks">An extended Non-DX10 FourCC list</a>
 */
enum DDSType {
    // Compressed types
    DXT1('D' + ('X' << 8) + ('T' << 16) + ('1' << 24),  8, DXGI.DXGI_FORMAT_BC1_UNORM, BC1),
    DXT2('D' + ('X' << 8) + ('T' << 16) + ('2' << 24), 16, DXGI.DXGI_FORMAT_BC2_UNORM, BC2),
    DXT3('D' + ('X' << 8) + ('T' << 16) + ('3' << 24), 16, DXGI.DXGI_FORMAT_BC2_UNORM, BC2),
    DXT4('D' + ('X' << 8) + ('T' << 16) + ('4' << 24), 16, DXGI.DXGI_FORMAT_BC3_UNORM, BC3),
    DXT5('D' + ('X' << 8) + ('T' << 16) + ('5' << 24), 16, DXGI.DXGI_FORMAT_BC3_UNORM, BC3),

    ATI1('A' + ('T' << 8) + ('I' << 16) + ('1' << 24),  8, DXGI.DXGI_FORMAT_BC4_UNORM, BC4), // AKA BC4U
    BC4U('B' + ('C' << 8) + ('4' << 16) + ('U' << 24),  8, DXGI.DXGI_FORMAT_BC4_UNORM, BC4),
    BC4S('B' + ('C' << 8) + ('4' << 16) + ('S' << 24),  8, DXGI.DXGI_FORMAT_BC4_SNORM, BC4),
    ATI2('A' + ('T' << 8) + ('I' << 16) + ('2' << 24), 16, DXGI.DXGI_FORMAT_BC5_UNORM, BC5), // AKA BC5U
    BC5U('B' + ('C' << 8) + ('5' << 16) + ('U' << 24), 16, DXGI.DXGI_FORMAT_BC5_UNORM, BC5),
    BC5S('B' + ('C' << 8) + ('5' << 16) + ('S' << 24), 16, DXGI.DXGI_FORMAT_BC5_SNORM, BC5),

    // Special case, see DXT10Header.dxgiFormat for real format
    DXT10('D' + ('X' << 8) + ('1' << 16) + ('0' << 24), -1, DXGI.DXGI_FORMAT_UNKNOWN, null),

    // Custom uncompressed pixel formats
    // TODO: Consider swapping byte order to reflect the DXGI format?
    A1R5G5B5(2, DXGI.DXGI_FORMAT_B5G5R5A1_UNORM,    A1R5G5B5_MASKS),
    X1R5G5B5(2, DXGI.DXGI_FORMAT_UNKNOWN,           X1R5G5B5_MASKS),
    A4R4G4B4(2, DXGI.DXGI_FORMAT_B4G4R4A4_UNORM,    A4R4G4B4_MASKS),
    X4R4G4B4(2, DXGI.DXGI_FORMAT_UNKNOWN,           X4R4G4B4_MASKS),
    R5G6B5(  2, DXGI.DXGI_FORMAT_B5G6R5_UNORM,      R5G6B5_MASKS),
    R8G8B8(  3, DXGI.DXGI_FORMAT_UNKNOWN,           R8G8B8_MASKS),
    A8B8G8R8(4, DXGI.DXGI_FORMAT_R8G8B8A8_UNORM,    A8B8G8R8_MASKS),
    X8B8G8R8(4, DXGI.DXGI_FORMAT_UNKNOWN,           X8B8G8R8_MASKS),
    A8R8G8B8(4, DXGI.DXGI_FORMAT_B8G8R8A8_UNORM,    A8R8G8B8_MASKS),
    X8R8G8B8(4, DXGI.DXGI_FORMAT_B8G8R8X8_UNORM,    X8R8G8B8_MASKS);

    private final int fourCC;
    private final int blockSize;
    private final int dxgiFormat;
    final BlockCompression compression;
    final int[] rgbaMasks;

    DDSType(int fourCC, int blockSize, int dxgiFormat, BlockCompression compression) {
        this(fourCC, blockSize, dxgiFormat, compression, null);
    }

    DDSType(int blockSize, int dxgiFormat, int[] rgbaMasks) {
        this(0, blockSize, dxgiFormat, null, rgbaMasks);
    }

    DDSType(int fourCC, int blockSize, int dxgiFormat, BlockCompression compression, int[] rgbaMasks) {
        this.fourCC = fourCC;
        this.blockSize = blockSize;
        this.dxgiFormat = dxgiFormat;
        this.compression = compression;
        this.rgbaMasks = rgbaMasks;
    }

    public int fourCC() {
        return fourCC;
    }

    public int blockSize() {
        return blockSize;
    }

    public boolean isFourCC() {
        return fourCC != 0;
    }

    public boolean isBlockCompression() {
        return compression != null;
    }

    public int dxgiFormat() {
        return dxgiFormat;
    }

    public static DDSType fromFourCC(int fourCC) {
        if (fourCC != 0) {
            for (DDSType type : values()) {
                if (fourCC == type.fourCC()) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException(String.format("Unknown type: 0x%08x", fourCC));
    }

    public static DDSType fromDXGIFormat(int dxgiFormat) {
        switch (dxgiFormat) {
            case DXGI.DXGI_FORMAT_R8G8B8A8_TYPELESS:
            case DXGI.DXGI_FORMAT_R8G8B8A8_UNORM:
            case DXGI.DXGI_FORMAT_R8G8B8A8_UNORM_SRGB:
            case DXGI.DXGI_FORMAT_R8G8B8A8_UINT:
                return A8B8G8R8; // ABGR

            case DXGI.DXGI_FORMAT_B8G8R8A8_TYPELESS:
            case DXGI.DXGI_FORMAT_B8G8R8A8_UNORM:
            case DXGI.DXGI_FORMAT_B8G8R8A8_UNORM_SRGB:
                return A8R8G8B8; // ARGB

            case DXGI.DXGI_FORMAT_B8G8R8X8_TYPELESS:
            case DXGI.DXGI_FORMAT_B8G8R8X8_UNORM:
            case DXGI.DXGI_FORMAT_B8G8R8X8_UNORM_SRGB:
                return X8R8G8B8;

            case DXGI.DXGI_FORMAT_B5G5R5A1_UNORM:
                return A1R5G5B5;

            case DXGI.DXGI_FORMAT_B4G4R4A4_UNORM:
                return A4R4G4B4;

            case DXGI.DXGI_FORMAT_B5G6R5_UNORM:
                return R5G6B5;

            case DXGI.DXGI_FORMAT_BC1_TYPELESS:
            case DXGI.DXGI_FORMAT_BC1_UNORM:
            case DXGI.DXGI_FORMAT_BC1_UNORM_SRGB:
                return DXT1;

            case DXGI.DXGI_FORMAT_BC2_TYPELESS:
            case DXGI.DXGI_FORMAT_BC2_UNORM:
            case DXGI.DXGI_FORMAT_BC2_UNORM_SRGB:
                return DXT2;

            case DXGI.DXGI_FORMAT_BC3_TYPELESS:
            case DXGI.DXGI_FORMAT_BC3_UNORM:
            case DXGI.DXGI_FORMAT_BC3_UNORM_SRGB:
                return DXT4;

            case DXGI.DXGI_FORMAT_BC4_TYPELESS:
            case DXGI.DXGI_FORMAT_BC4_UNORM:
                return ATI1;

            case DXGI.DXGI_FORMAT_BC4_SNORM:
                return BC4S;

            case DXGI.DXGI_FORMAT_BC5_TYPELESS:
            case DXGI.DXGI_FORMAT_BC5_UNORM:
                return ATI2;

            case DXGI.DXGI_FORMAT_BC5_SNORM:
                return BC5S;

            default:
                throw new IllegalArgumentException("Unsupported DXGI_FORMAT: " + dxgiFormat);
        }
    }
}
