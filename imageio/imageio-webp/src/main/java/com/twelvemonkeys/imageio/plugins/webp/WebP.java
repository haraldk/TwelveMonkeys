/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp;

/**
 * WebP
 */
interface WebP {
    int RIFF_MAGIC = 'R' | 'I' << 8 | 'F' << 16 | 'F' << 24;
    int WEBP_MAGIC = 'W' | 'E' << 8 | 'B' << 16 | 'P' << 24;

    int CHUNK_VP8_ = 'V' | 'P' << 8 | '8' << 16 | ' ' << 24;
    int CHUNK_VP8L = 'V' | 'P' << 8 | '8' << 16 | 'L' << 24;
    int CHUNK_VP8X = 'V' | 'P' << 8 | '8' << 16 | 'X' << 24;

    int CHUNK_ALPH = 'A' | 'L' << 8 | 'P' << 16 | 'H' << 24;
    int CHUNK_ANIM = 'A' | 'N' << 8 | 'I' << 16 | 'M' << 24;
    int CHUNK_ANMF = 'A' | 'N' << 8 | 'M' << 16 | 'F' << 24;
    int CHUNK_ICCP = 'I' | 'C' << 8 | 'C' << 16 | 'P' << 24;
    int CHUNK_EXIF = 'E' | 'X' << 8 | 'I' << 16 | 'F' << 24;
    int CHUNK_XMP_ = 'X' | 'M' << 8 | 'P' << 16 | ' ' << 24;

    byte LOSSLESSS_SIG = 0x2f;
}
