/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import java.awt.*;

enum TupleType {
    // Official:
    /** B/W, but uses 1 byte (8 bits) per pixel. Black is zero (oposite of PBM) */
    BLACKANDWHITE(1, 1, PNM.MAX_VAL_1BIT, Transparency.OPAQUE),
    /** B/W + bit mask, uses 2 bytes per pixel. Black is zero (oposite of PBM) */
    BLACKANDWHITE_ALPHA(2, PNM.MAX_VAL_1BIT, PNM.MAX_VAL_1BIT, Transparency.BITMASK),
    /** Grayscale, as PGM. */
    GRAYSCALE(1, 2, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** Grayscale + alpha. YA order. */
    GRAYSCALE_ALPHA(2, 2, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),
    /** RGB color, as PPM. RGB order. */
    RGB(3, 1, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** RGB color + alpha. RGBA order. */
    RGB_ALPHA(4, 1, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),

    // De facto (documented on the interwebs):
    /** CMYK color. CMYK order. */
    CMYK(4, 2, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** CMYK color + alpha. CMYKA order. */
    CMYK_ALPHA(5, 1, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),

    // Custom for PBM compatibility
    /** 1 bit B/W. White is zero (as PBM) */
    BLACKANDWHITE_WHITE_IS_ZERO(1, 1, PNM.MAX_VAL_1BIT, Transparency.OPAQUE);

    private final int samplesPerPixel;
    private final int minMaxSample;
    private final int maxMaxSample;
    private final int transparency;

    TupleType(int samplesPerPixel, int minMaxSample, int maxMaxSample, int transparency) {
        this.samplesPerPixel = samplesPerPixel;
        this.minMaxSample = minMaxSample;
        this.maxMaxSample = maxMaxSample;
        this.transparency = transparency;
    }

    public int getTransparency() {
        return transparency;
    }

    public int getSamplesPerPixel() {
        return samplesPerPixel;
    }

    public boolean isValidMaxSample(int maxSample) {
        return maxSample >= minMaxSample && maxSample <= maxMaxSample;
    }

}
