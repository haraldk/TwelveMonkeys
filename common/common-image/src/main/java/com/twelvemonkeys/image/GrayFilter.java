/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.image;

import java.awt.image.RGBImageFilter;

/**
 * This class can convert a color image to grayscale.
 * <P/>
 * Uses ITU standard conversion: (222 * Red + 707 * Green + 71 * Blue) / 1000.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/GrayFilter.java#1 $
 *
 */
public class GrayFilter extends RGBImageFilter {

    // This filter can filter IndexColorModel
    {
        canFilterIndexColorModel = true;
    }

    private int low = 0;
    private float range = 1.0f;

    /**
     * Constructs a GrayFilter using ITU color-conversion.
     */
    public GrayFilter() {
    }

    /**
     * Constructs a GrayFilter using ITU color-conversion, and a dynamic range between
     * pLow and pHigh.
     *
     * @param pLow float in the range  0..1
     * @param pHigh float in the range 0..1 and >= pLow
     */
    public GrayFilter(float pLow, float pHigh) {
        if (pLow > pHigh) {
            pLow = 0f;
        }
        // Make sure high and low are inside range
        if (pLow < 0f) {
            pLow = 0f;
        }
        else if (pLow > 1f) {
            pLow = 1f;
        }
        if (pHigh < 0f) {
            pHigh = 0f;
        }
        else if (pHigh > 1f) {
            pHigh = 1f;
        }

        low = (int) (pLow * 255f);
        range = pHigh - pLow;

    }

    /**
     * Constructs a GrayFilter using ITU color-conversion, and a dynamic
     * range between pLow and pHigh.
     *
     * @param pLow integer in the range 0..255
     * @param pHigh inteeger in the range 0..255 and >= pLow
     */
    public GrayFilter(int pLow, int pHigh) {
        this(pLow / 255f, pHigh / 255f);
    }

    /**
     * Filters one pixel using ITU color-conversion.
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
        int g = pARGB >>  8 & 0xFF;
        int b = pARGB       & 0xFF;

        // ITU standard:  Gray scale=(222*Red+707*Green+71*Blue)/1000
        int gray = (222 * r + 707 * g + 71 * b) / 1000;

        //int gray = (int) ((float) (r + g + b) / 3.0f);

        if (range != 1.0f) {
            // Apply range
            gray =  low + (int) (gray * range);
        }

        // Return ARGB pixel
        return  (pARGB & 0xFF000000) | (gray << 16) | (gray << 8) | gray;
    }
}
