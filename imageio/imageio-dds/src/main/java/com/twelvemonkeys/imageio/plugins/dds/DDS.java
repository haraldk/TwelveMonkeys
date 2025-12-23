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

@SuppressWarnings("unused")
interface DDS {
    int MAGIC = ('D' << 24) + ('D' << 16) + ('S' << 8) + ' '; //Big-Endian
    int HEADER_SIZE = 124;

    // Header Flags
    int FLAG_CAPS = 0x1;              // Required in every .dds file.
    int FLAG_HEIGHT = 0x2;            // Required in every .dds file.
    int FLAG_WIDTH = 0x4;             // Required in every .dds file.
    int FLAG_PITCH = 0x8;             // Required when pitch is provided for an uncompressed texture.
    int FLAG_PIXELFORMAT = 0x1000;    // Required in every .dds file.
    int FLAG_MIPMAPCOUNT = 0x20000;   // Required in a mipmapped texture.
    int FLAG_LINEARSIZE = 0x80000;    // Required when pitch is provided for a compressed texture.
    int FLAG_DEPTH = 0x800000;        // Required in a depth texture.

    // Pixel Format Flags
    int DDSPF_SIZE = 32;
    int PIXEL_FORMAT_FLAG_ALPHAPIXELS = 0x1;
    int PIXEL_FORMAT_FLAG_ALPHA = 0x2;
    int PIXEL_FORMAT_FLAG_FOURCC = 0x04;
    int PIXEL_FORMAT_FLAG_RGB = 0x40;

    //DX10 Resource Dimensions
    int D3D10_RESOURCE_DIMENSION_TEXTURE2D = 3;

    //DXGI Formats (DX10)
    int DXGI_FORMAT_BC1_UNORM_SRGB = 72;
    int DXGI_FORMAT_BC2_UNORM_SRGB = 75;
    int DXGI_FORMAT_BC3_UNORM_SRGB = 78;
    int DXGI_FORMAT_BC4_UNORM = 80;
    int DXGI_FORMAT_BC4_SNORM = 81;
    int DXGI_FORMAT_B8G8R8A8_UNORM_SRGB = 91;
    int DXGI_FORMAT_B8G8R8X8_UNORM_SRGB = 93;
    int DXGI_FORMAT_R8G8B8A8_UNORM_SRGB = 29;

    //dwCaps
    int DDSCAPS_COMPLEX = 0x8;
    int DDSCAPS_MIPMAP = 0x400000;
    int DDSCAPS_TEXTURE = 0x1000;
}
