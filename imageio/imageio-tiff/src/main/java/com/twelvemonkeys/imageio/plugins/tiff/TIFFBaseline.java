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
 * TIFFBaseline
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFBaseline.java,v 1.0 08.05.12 16:43 haraldk Exp$
 */
public interface TIFFBaseline {
    int COMPRESSION_NONE = 1;
    int COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE = 2;
    int COMPRESSION_PACKBITS = 32773;

    int PHOTOMETRIC_WHITE_IS_ZERO = 0;
    int PHOTOMETRIC_BLACK_IS_ZERO = 1;
    int PHOTOMETRIC_RGB = 2;
    int PHOTOMETRIC_PALETTE = 3;
    int PHOTOMETRIC_MASK = 4;

    int SAMPLEFORMAT_UINT = 1; // Spec says only UINT required for baseline

    int PLANARCONFIG_CHUNKY = 1;

    int EXTRASAMPLE_UNSPECIFIED = 0;
    int EXTRASAMPLE_ASSOCIATED_ALPHA = 1;
    int EXTRASAMPLE_UNASSOCIATED_ALPHA = 2;

    int PREDICTOR_NONE = 1;

    int RESOLUTION_UNIT_NONE = 1;
    int RESOLUTION_UNIT_DPI = 2; // Default
    int RESOLUTION_UNIT_CENTIMETER = 3;

    int FILL_LEFT_TO_RIGHT = 1; // Default

    // NOTE: These are bit flags that can be ORed together!
    int FILETYPE_REDUCEDIMAGE = 1;
    int FILETYPE_PAGE = 2;
    int FILETYPE_MASK = 4;

    int ORIENTATION_TOPLEFT = 1;
}
