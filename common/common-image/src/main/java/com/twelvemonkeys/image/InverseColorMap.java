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

/**
 * Inverse Colormap to provide efficient lookup of any given input color
 * to the closest match to the given color map.
 * <p/>
 * Based on "Efficient Inverse Color Map Computation" by Spencer W. Thomas
 * in "Graphics Gems Volume II"
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author Robin Luiten (Java port)
 * @author Spencer W. Thomas (original c version).
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/InverseColorMap.java#1 $
 */
class InverseColorMap {
    /**
     * Number of high bits of each color channel to use to lookup near match
     */
    final static int QUANTBITS = 5;

    /**
     * Truncated bits of each color channel
     */
    final static int TRUNCBITS = 8 - QUANTBITS;

    /**
     * BITMASK representing the bits for blue in the color lookup
     */
    final static int QUANTMASK_BLUE = (1 << 5) - 1;

    /**
     * BITMASK representing the bits for green in the color lookup
     */
    final static int QUANTMASK_GREEN = (QUANTMASK_BLUE << QUANTBITS);

    /**
     * BITMASK representing the bits for red in the color lookup
     */
    final static int QUANTMASK_RED = (QUANTMASK_GREEN << QUANTBITS);

    /**
     * Maximum value a quantised color channel can have
     */
    final static int MAXQUANTVAL = 1 << 5;

    byte[] rgbMapByte;
    int[] rgbMapInt;
    int numColors;
    int maxColor;
    byte[] inverseRGB;   		// inverse rgb color map
    int transparentIndex = -1;

    /**
     * @param pRGBColorMap the rgb color map to create inverse color map for.
     */
    InverseColorMap(byte[] pRGBColorMap) {
        this(pRGBColorMap, -1);
    }

    /**
     * @param pRGBColorMap the rgb color map to create inverse color map for.
     */
    // HaraldK 20040801: Added support for int[]
    InverseColorMap(int[] pRGBColorMap) {
        this(pRGBColorMap, -1);
    }

    /**
     * @param pRGBColorMap the rgb color map to create inverse color map for.
     * @param pTransparent the index of the transparent pixel in the map
     */
    InverseColorMap(byte[] pRGBColorMap, int pTransparent) {
        rgbMapByte = pRGBColorMap;
        numColors = rgbMapByte.length / 4;
        transparentIndex = pTransparent;

        inverseRGB = new byte[MAXQUANTVAL * MAXQUANTVAL * MAXQUANTVAL];
        initIRGB(new int[MAXQUANTVAL * MAXQUANTVAL * MAXQUANTVAL]);
    }

    /**
     * @param pRGBColorMap the rgb color map to create inverse color map for.
     * @param pTransparent the index of the transparent pixel in the map
     */
    InverseColorMap(int[] pRGBColorMap, int pTransparent) {
        rgbMapInt = pRGBColorMap;
        numColors = rgbMapInt.length;
        transparentIndex = pTransparent;

        inverseRGB = new byte[MAXQUANTVAL * MAXQUANTVAL * MAXQUANTVAL];
        initIRGB(new int[MAXQUANTVAL * MAXQUANTVAL * MAXQUANTVAL]);
    }


    /**
     * Simple inverse color table creation method.
     * @param pTemp temp array
     */
    void initIRGB(int[] pTemp) {
        final int x = (1 << TRUNCBITS);        // 8 the size of 1 Dimension of each quantized cell
        final int xsqr = 1 << (TRUNCBITS * 2); // 64 - twice the smallest step size vale of quantized colors
        final int xsqr2 = xsqr + xsqr;

        for (int i = 0; i < numColors; ++i) {
            if (i == transparentIndex) {
                // Skip the transparent pixel
                continue;
            }

            int red, r, rdist, rinc, rxx;
            int green, g, gdist, ginc, gxx;
            int blue, b, bdist, binc, bxx;

            // HaraldK 20040801: Added support for int[]
            if (rgbMapByte != null) {
                red = rgbMapByte[i * 4] & 0xFF;
                green = rgbMapByte[i * 4 + 1] & 0xFF;
                blue = rgbMapByte[i * 4 + 2] & 0xFF;
            }
            else if (rgbMapInt != null) {
                red = (rgbMapInt[i] >> 16) & 0xFF;
                green = (rgbMapInt[i] >> 8) & 0xFF;
                blue = rgbMapInt[i] & 0xFF;
            }
            else {
                throw new IllegalStateException("colormap == null");
            }

            rdist = red - x / 2;   // distance of red to center of current cell
            gdist = green - x / 2; // green
            bdist = blue - x / 2;  // blue
            rdist = rdist * rdist + gdist * gdist + bdist * bdist;

            rinc = 2 * (xsqr - (red << TRUNCBITS));
            ginc = 2 * (xsqr - (green << TRUNCBITS));
            binc = 2 * (xsqr - (blue << TRUNCBITS));

            int rgbI = 0;
            for (r = 0, rxx = rinc; r < MAXQUANTVAL; rdist += rxx, ++r, rxx += xsqr2) {
                for (g = 0, gdist = rdist, gxx = ginc; g < MAXQUANTVAL; gdist += gxx, ++g, gxx += xsqr2) {
                    for (b = 0, bdist = gdist, bxx = binc; b < MAXQUANTVAL; bdist += bxx, ++b, ++rgbI, bxx += xsqr2) {
                        if (i == 0 || pTemp[rgbI] > bdist) {
                            pTemp[rgbI] = bdist;
                            inverseRGB[rgbI] = (byte) i;
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the index of the nearest color to from the color map.
     *
     * @param pColor the color to get the nearest color to from color map
     *        color must be of format {@code 0x00RRGGBB} - standard default RGB
     * @return index of color which closest matches input color by using the
     *         created inverse color map.
     */
    public final int getIndexNearest(int pColor) {
        return inverseRGB[((pColor >> (3 * TRUNCBITS)) & QUANTMASK_RED) +
                ((pColor >> (2 * TRUNCBITS)) & QUANTMASK_GREEN) +
                ((pColor >> (/* 1 * */ TRUNCBITS)) & QUANTMASK_BLUE)] & 0xFF;
    }

    /**
     * Gets the index of the nearest color to from the color map.
     *
     * @param pRed red component of the color to get the nearest color to from color map
     * @param pGreen green component of the color to get the nearest color to from color map
     * @param pBlue blue component of the color to get the nearest color to from color map
     * @return index of color which closest matches input color by using the
     *         created inverse color map.
     */
    public final int getIndexNearest(int pRed, int pGreen, int pBlue) {
        // NOTE: the third line in expression for blue is shifting DOWN not UP.
        return inverseRGB[((pRed << (2 * QUANTBITS - TRUNCBITS)) & QUANTMASK_RED) +
                ((pGreen << (/* 1 * */ QUANTBITS - TRUNCBITS)) & QUANTMASK_GREEN) +
                ((pBlue >> (TRUNCBITS)) & QUANTMASK_BLUE)] & 0xFF;
    }
}

