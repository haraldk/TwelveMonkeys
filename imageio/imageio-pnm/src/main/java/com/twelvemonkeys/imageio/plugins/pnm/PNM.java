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

package com.twelvemonkeys.imageio.plugins.pnm;

/**
 * @see <a href="http://netpbm.sourceforge.net/doc/index.html#formats">The Netpbm Formats</a>.
 */
interface PNM {
    /** 1 bit per sample, ASCII format, white is zero. */
    short PBM_PLAIN = 'P' << 8 | '1';
    /** Grayscale up to 16 bits per sample, ASCII format. */
    short PGM_PLAIN = 'P' << 8 | '2';
    /** Color (RGB) up to 16 bits per sample, ASCII format. */
    short PPM_PLAIN = 'P' << 8 | '3';
    /** 1 bit per sample, RAW format, white is zero. */
    short PBM = 'P' << 8 | '4';
    /** Grayscale up to 16 bits per sample, RAW format. */
    short PGM = 'P' << 8 | '5';
    /** Color (RGB) up to 16 bits per sample, RAW format. */
    short PPM = 'P' << 8 | '6';

    /**
     * PAM format, may contain data in same formats as the above, has extended header.
     * Always 1-16 bits per sample, RAW format.
     * @see <a href="http://netpbm.sourceforge.net/doc/pam.html">PAM format</a>
     */
    short PAM = 'P' << 8 | '7';

    // Consider these for a future PFM (floating point) format
    short PFM_RGB = 'P' << 8 | 'F'; // PPM_FLOAT? PFM?
    short PFM_GRAY = 'P' << 8 | 'f'; // PGM_FLOAT? PfM?

    /** Max value for 1 bit rasters (1). */
    int MAX_VAL_1BIT = 1;
    /** Max value for 8 bit rasters (255). */
    int MAX_VAL_8BIT = 255;
    /** Max value for 16 bit rasters (65535). */
    int MAX_VAL_16BIT = 65535;
    /** Max value for 32 bit rasters (4294967295). Experimental, not supported by the "spec". */
    long MAX_VAL_32BIT = 4294967295L;

    /** In order to not confuse PAM ("P7") with xv thumbnails. */
    int XV_THUMBNAIL_MAGIC = (' ' << 24 | '3' << 16 | '3' << 8 | '2');;
}
