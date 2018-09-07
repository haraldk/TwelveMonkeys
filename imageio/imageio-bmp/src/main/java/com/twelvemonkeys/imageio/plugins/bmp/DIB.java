/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

/**
 * DIB
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DIB.java,v 1.0 Apr 8, 2008 1:43:04 PM haraldk Exp$
 *
 * @see <a href="http://en.wikipedia.org/wiki/BMP_file_format">BMP file format (Wikipedia)</a>
 * @see <a href="http://en.wikipedia.org/wiki/ICO_(icon_image_file_format)">ICO file format (Wikipedia)</a>
 */
interface DIB {
    int TYPE_ICO = 1;
    int TYPE_CUR = 2;

    int BMP_FILE_HEADER_SIZE = 14;

    /** BITMAPCOREHEADER size, OS/2 V1 */
    int BITMAP_CORE_HEADER_SIZE = 12;

    /** Strange BITMAPCOREHEADER size, OS/2 V2, but only first 16 bytes... */
    int OS2_V2_HEADER_16_SIZE = 16;

    /** BITMAPCOREHEADER size, OS/2 V2 */
    int OS2_V2_HEADER_SIZE = 64;

    /**
     * BITMAPINFOHEADER size, Windows 3.0 and later.
     * This is the most commonly used header for persistent bitmaps.
     */
    int BITMAP_INFO_HEADER_SIZE = 40;

    int BITMAP_V2_INFO_HEADER_SIZE = 52; // Undocumented, written by Photoshop

    int BITMAP_V3_INFO_HEADER_SIZE = 56;  // Undocumented, written by Photoshop

    /** BITMAPV4HEADER size, Windows 95/NT4 and later. */
    int BITMAP_V4_INFO_HEADER_SIZE = 108;

    /** BITMAPV5HEADER size, Windows 98/2000 and later. */
    int BITMAP_V5_INFO_HEADER_SIZE = 124;

    /** BI_RGB: No compression. Default. */
    int COMPRESSION_RGB = 0;
    /** BI_RLE8: 8 bit run-length encoding (RLE). */
    int COMPRESSION_RLE8 = 1;
    /** BI_RLE4: 4 bit run-length encoding (RLE). */
    int COMPRESSION_RLE4 = 2;
    /** BI_BITFIELDS, OS2_V2: Huffman 1D compression. V2: RGB bit field masks, V3+: RGBA. */
    int COMPRESSION_BITFIELDS = 3;
    int COMPRESSION_JPEG = 4;
    int COMPRESSION_PNG = 5;
    /** RGBA bitfield masks. */
    int COMPRESSION_ALPHA_BITFIELDS = 6;

    // Unused for Windows Metafiles using CMYK colorspace:
    // int COMPRESSION_CMYK = 11;
    // int COMPRESSION_CMYK_RLE8 = 12;
    // int COMPRESSION_CMYK_RLE5 = 13;

    /* Color space types. */
    int LCS_CALIBRATED_RGB = 0;
    int LCS_sRGB = 's' << 24 | 'R' << 16 | 'G' << 8 | 'B'; // 0x73524742
    int LCS_WINDOWS_COLOR_SPACE = 'W' << 24 | 'i' << 16 | 'n' << 8 | ' '; // 0x57696e20
    int PROFILE_LINKED = 'L' << 24 | 'I' << 16 | 'N' << 8 | 'K'; // 0x4c494e4b
    int PROFILE_EMBEDDED = 'M' << 24 | 'B' << 16 | 'E' << 8 | 'D'; // 0x4d424544

    /** PNG "magic" identifier */
    long PNG_MAGIC = 0x89l << 56 | (long) 'P' << 48 | (long) 'N' << 40 | (long) 'G' << 32 | 0x0dl << 24 | 0x0al << 16 | 0x1al << 8 | 0x0al;
}
