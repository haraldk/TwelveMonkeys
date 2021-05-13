/*
 * Copyright (c) 2015, Harald Kuhr
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

package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.lang.Validate;

/**
 * Converts between CIE L*a*b* and sRGB color spaces.
 */
// Code adapted from ImageJ's Color_Space_Converter.java (Public Domain):
// http://rsb.info.nih.gov/ij/plugins/download/Color_Space_Converter.java
public final class CIELabColorConverter {
    // TODO: Create interface in the color package?
    // TODO: Make YCbCr/YCCK -> RGB/CMYK implement same interface?

    public enum Illuminant {
        D50(new float[] {96.4212f, 100.0f, 82.5188f}),
        D65(new float[] {95.0429f, 100.0f, 108.8900f});

        private final float[] whitePoint;

        Illuminant(final float[] wp) {
            whitePoint = Validate.isTrue(wp != null && wp.length == 3, wp, "Bad white point definition: %s");
        }

        public float[] getWhitePoint() {
            return whitePoint;
        }
    }

    private final float[] whitePoint;

    public CIELabColorConverter(final Illuminant illuminant) {
        whitePoint = Validate.notNull(illuminant, "illuminant").getWhitePoint();
    }

    private float clamp(float x) {
        if (x < 0.0f) {
            return 0.0f;
        }
        else if (x > 255.0f) {
            return 255.0f;
        }
        else {
            return x;
        }
    }

    public void toRGB(float l, float a, float b, float[] rgbResult) {
        XYZtoRGB(LABtoXYZ(l, a, b, rgbResult), rgbResult);
    }

    /**
     * Convert LAB to XYZ.
     * @param L
     * @param a
     * @param b
     * @return XYZ values
     */
    private float[] LABtoXYZ(float L, float a, float b, float[] xyzResult) {
        // Significant speedup: Removing Math.pow
        float y = (L + 16.0f) / 116.0f;
        float y3 = y * y * y; // Math.pow(y, 3.0);
        float x = (a / 500.0f) + y;
        float x3 = x * x * x; // Math.pow(x, 3.0);
        float z = y - (b / 200.0f);
        float z3 = z * z * z; // Math.pow(z, 3.0);

        if (y3 > 0.008856f) {
            y = y3;
        }
        else {
            y = (y - (16.0f / 116.0f)) / 7.787f;
        }

        if (x3 > 0.008856f) {
            x = x3;
        }
        else {
            x = (x - (16.0f / 116.0f)) / 7.787f;
        }

        if (z3 > 0.008856f) {
            z = z3;
        }
        else {
            z = (z - (16.0f / 116.0f)) / 7.787f;
        }

        xyzResult[0] = x * whitePoint[0];
        xyzResult[1] = y * whitePoint[1];
        xyzResult[2] = z * whitePoint[2];

        return xyzResult;
    }

    /**
     * Convert XYZ to RGB
     * @param xyz
     * @return RGB values
     */
    private float[] XYZtoRGB(final float[] xyz, final float[] rgbResult) {
        return XYZtoRGB(xyz[0], xyz[1], xyz[2], rgbResult);
    }

    private float[] XYZtoRGB(final float X, final float Y, final float Z, float[] rgbResult) {
        float x = X / 100.0f;
        float y = Y / 100.0f;
        float z = Z / 100.0f;

        float r = x * 3.2406f + y * -1.5372f + z * -0.4986f;
        float g = x * -0.9689f + y * 1.8758f + z * 0.0415f;
        float b = x * 0.0557f + y * -0.2040f + z * 1.0570f;

        // assume sRGB
        if (r > 0.0031308f) {
            r = ((1.055f * (float) pow(r, 1.0 / 2.4)) - 0.055f);
        }
        else {
            r = (r * 12.92f);
        }

        if (g > 0.0031308f) {
            g = ((1.055f * (float) pow(g, 1.0 / 2.4)) - 0.055f);
        }
        else {
            g = (g * 12.92f);
        }

        if (b > 0.0031308f) {
            b = ((1.055f * (float) pow(b, 1.0 / 2.4)) - 0.055f);
        }
        else {
            b = (b * 12.92f);
        }

        // convert 0..1 into 0..255
        rgbResult[0] = clamp(r * 255);
        rgbResult[1] = clamp(g * 255);
        rgbResult[2] = clamp(b * 255);

        return rgbResult;
    }

    // TODO: Test, to figure out if accuracy is good enough.
    // Visual inspection looks good! The author claims 5-12% error, worst case up to 25%...
    // http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
    static double pow(final double a, final double b) {
        long tmp = Double.doubleToLongBits(a);
        long tmp2 = (long) (b * (tmp - 4606921280493453312L)) + 4606921280493453312L;
        return Double.longBitsToDouble(tmp2);
    }
}
