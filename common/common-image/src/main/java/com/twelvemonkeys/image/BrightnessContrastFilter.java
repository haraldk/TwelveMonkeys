/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.image;

import java.awt.image.RGBImageFilter;


/**
 * Adjusts the contrast and brightness of an image.
 * <p/>
 * For brightness, the valid range is {@code -2.0,..,0.0,..,2.0}.
 * A value of {@code 0.0} means no change.
 * Negative values will make the pixels darker.
 * Maximum negative value ({@code -2}) will make all filtered pixels black.
 * Positive values will make the pixels brighter.
 * Maximum positive value ({@code 2}) will make all filtered pixels white.
 * <p/>
 * For contrast, the valid range is {@code -1.0,..,0.0,..,1.0}.
 * A value of {@code 0.0} means no change.
 * Negative values will reduce contrast.
 * Maximum negative value ({@code -1}) will make all filtered pixels grey
 * (no contrast).
 * Positive values will increase contrast.
 * Maximum positive value ({@code 1}) will make all filtered pixels primary
 * colors (either black, white, cyan, magenta, yellow, red, blue or green).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/BrightnessContrastFilter.java#1 $
 *
 * @todo consider doing something similar to http://archives.java.sun.com/cgi-bin/wa?A2=ind0302&L=jai-interest&F=&S=&P=15947
 */

public class BrightnessContrastFilter extends RGBImageFilter {

    // TODO: Replace with RescaleOp?

    // This filter can filter IndexColorModel, as it is does not depend on
    // the pixels' location
    {
        canFilterIndexColorModel = true;
    }

    // Use a pre-calculated lookup table for performance
    private final int[] LUT;

    /**
     * Creates a BrightnessContrastFilter with default values
     * ({@code brightness=0.3, contrast=0.3}).
     * <p/>
     * This will slightly increase both brightness and contrast.
     */
    public BrightnessContrastFilter() {
        this(0.3f, 0.3f);
    }

    /**
     * Creates a BrightnessContrastFilter with the given values for brightness
     * and contrast.
     * <p/>
     * For brightness, the valid range is {@code -2.0,..,0.0,..,2.0}.
     * A value of {@code 0.0} means no change.
     * Negative values will make the pixels darker.
     * Maximum negative value ({@code -2}) will make all filtered pixels black.
     * Positive values will make the pixels brighter.
     * Maximum positive value ({@code 2}) will make all filtered pixels white.
     * <p/>
     * For contrast, the valid range is {@code -1.0,..,0.0,..,1.0}.
     * A value of {@code 0.0} means no change.
     * Negative values will reduce contrast.
     * Maximum negative value ({@code -1}) will make all filtered pixels grey
     * (no contrast).
     * Positive values will increase contrast.
     * Maximum positive value ({@code 1}) will make all filtered pixels primary
     * colors (either black, white, cyan, magenta, yellow, red, blue or green).
     *
     * @param pBrightness adjust the brightness of the image, in the range
     * {@code -2.0,..,0.0,..,2.0}.
     * @param pContrast adjust the contrast of the image, in the range
     * {@code -1.0,..,0.0,..,1.0}.
     */
    public BrightnessContrastFilter(float pBrightness, float pContrast) {
        LUT = createLUT(pBrightness, pContrast);
    }

    private static int[] createLUT(float pBrightness, float pContrast) {
        int[] lut = new int[256];

        // Hmmm.. This approximates Photoshop values.. Not good though..
        double contrast = pContrast > 0 ? Math.pow(pContrast, 7.0) * 127.0 : pContrast;

        // Convert range [-1,..,0,..,1] -> [0,..,1,..,2]
        double brightness = pBrightness + 1.0;

        for (int i = 0; i < 256; i++) {
            lut[i] = clamp((int) (127.5 * brightness + (i - 127) * (contrast + 1.0)));
        }

        // Special case, to ensure only primary colors for max contrast
        if (pContrast == 1f) {
            lut[127] = lut[126];
        }

        return lut;
    }

    private static int clamp(int i) {
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }

    /**
     * Filters one pixel, adjusting brightness and contrast according to this
     * filter.
     *
     * @param pX x
     * @param pY y
     * @param pARGB pixel value in default color space
     *
     * @return the filtered pixel value in the default color space
     */
    public int filterRGB(int pX, int pY, int pARGB) {
        // Get color components
        int r = pARGB >> 16 & 0xFF;
        int g = pARGB >> 8 & 0xFF;
        int b = pARGB & 0xFF;

        // Scale to new contrast
        r = LUT[r];
        g = LUT[g];
        b = LUT[b];

        // Return ARGB pixel, leave transparency as is
        return (pARGB & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
