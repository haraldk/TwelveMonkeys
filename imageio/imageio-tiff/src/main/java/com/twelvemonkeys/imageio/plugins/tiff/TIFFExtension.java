/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

/**
 * TIFFExtension
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFExtension.java,v 1.0 08.05.12 16:45 haraldk Exp$
 */
interface TIFFExtension {
    /** CCITT T.4/Group 3 Fax compression. */
    int COMPRESSION_CCITT_T4 = 3;
    /** CCITT T.6/Group 4 Fax compression. */
    int COMPRESSION_CCITT_T6 = 4;
    /** LZW Compression. Was baseline, but moved to extension due to license issues in the LZW algorithm. */
    int COMPRESSION_LZW = 5;
    /** Deprecated. */
    int COMPRESSION_OLD_JPEG = 6;
    /** JPEG Compression (lossy). */
    int COMPRESSION_JPEG = 7;
    /** Custom: PKZIP-style Deflate. */
    int COMPRESSION_DEFLATE = 32946;
    /** Adobe-style Deflate. */
    int COMPRESSION_ZLIB = 8;

    /*
    LibTIFF:
    COMPRESSION_NONE = 1;
COMPRESSION_CCITTRLE = 2;
COMPRESSION_CCITTFAX3 = COMPRESSION_CCITT_T4 = 3;
COMPRESSION_CCITTFAX4 = COMPRESSION_CCITT_T6 = 4;
COMPRESSION_LZW = 5;
COMPRESSION_OJPEG = 6;
COMPRESSION_JPEG = 7;
COMPRESSION_NEXT = 32766;
COMPRESSION_CCITTRLEW = 32771;
COMPRESSION_PACKBITS = 32773;
COMPRESSION_THUNDERSCAN = 32809;
COMPRESSION_IT8CTPAD = 32895;
COMPRESSION_IT8LW = 32896;
COMPRESSION_IT8MP = 32897;
COMPRESSION_IT8BL = 32898;
COMPRESSION_PIXARFILM = 32908;
COMPRESSION_PIXARLOG = 32909;
COMPRESSION_DEFLATE = 32946;
COMPRESSION_ADOBE_DEFLATE = 8;
COMPRESSION_DCS = 32947;
COMPRESSION_JBIG = 34661;
COMPRESSION_SGILOG = 34676;
COMPRESSION_SGILOG24 = 34677;
COMPRESSION_JP2000 = 34712;
     */

    int PHOTOMETRIC_SEPARATED = 5;
    int PHOTOMETRIC_YCBCR = 6;
    int PHOTOMETRIC_CIELAB = 8;
    int PHOTOMETRIC_ICCLAB = 9;
    int PHOTOMETRIC_ITULAB = 10;

    int PLANARCONFIG_PLANAR = 2;

    int PREDICTOR_NONE = 1;
    int PREDICTOR_HORIZONTAL_DIFFERENCING = 2;
    int PREDICTOR_HORIZONTAL_FLOATINGPOINT = 3;
}
