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

package com.twelvemonkeys.imageio.plugins.psd;

import java.awt.color.ColorSpace;

/**
 * YCbCrColorSpace
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: YCbCrColorSpace.java,v 1.0 Jun 28, 2008 3:30:50 PM haraldk Exp$
 */
// TODO: Move to com.twlevemonkeys.image?
// TODO: Read an ICC YCbCr profile from classpath resource? Is there such a thing?
final class YCbCrColorSpace extends ColorSpace {

    static final ColorSpace INSTANCE = new CMYKColorSpace();
    final ColorSpace sRGB = getInstance(CS_sRGB);

    YCbCrColorSpace() {
        super(ColorSpace.TYPE_YCbCr, 3);
    }

    public static ColorSpace getInstance() {
        return INSTANCE;
    }

    // http://www.w3.org/Graphics/JPEG/jfif.txt
    /*
    Conversion to and from RGB

    Y, Cb, and Cr are converted from R, G, and B as defined in CCIR Recommendation 601
    but are normalized so as to occupy the full 256 levels of a 8-bit binary encoding.  More
    precisely:

    Y   = 256 * E'y
    Cb  = 256 * [ E'Cb ] + 128
    Cr  = 256 * [ E'Cr ] + 128

    where the E'y, E'Cb and E'Cb are defined as in CCIR 601.  Since values of E'y have a
    range of 0 to 1.0 and those for  E'Cb and E'Cr have a range of -0.5 to +0.5,  Y, Cb, and Cr
    must be clamped to 255 when they are maximum value.

    RGB to YCbCr Conversion

    YCbCr (256 levels) can be computed directly from 8-bit RGB as follows:

    Y   =     0.299  R + 0.587  G + 0.114  B
    Cb  =   - 0.1687 R - 0.3313 G + 0.5    B + 128
    Cr  =     0.5    R - 0.4187 G - 0.0813 B + 128

    NOTE - Not all image file formats store image samples in the order R0, G0,
    B0, ... Rn, Gn, Bn.  Be sure to verify the sample order before converting an
    RGB file to JFIF.

    YCbCr to RGB Conversion

    RGB can be computed directly from YCbCr (256 levels) as follows:

    R = Y                    + 1.402   (Cr-128)
    G = Y - 0.34414 (Cb-128) - 0.71414 (Cr-128)
    B = Y + 1.772   (Cb-128)
     */
    public float[] toRGB(float[] colorvalue) {
//        R = Y                    + 1.402   (Cr-128)
//        G = Y - 0.34414 (Cb-128) - 0.71414 (Cr-128)
//        B = Y + 1.772   (Cb-128)
        return new float[] {
                colorvalue[0]                                    + 1.402f   * (colorvalue[2] - .5f),
                colorvalue[0] - 0.34414f * (colorvalue[1] - .5f) - 0.71414f * (colorvalue[2] - .5f),
                colorvalue[0] + 1.772f   * (colorvalue[1] - .5f),
        };
        // TODO: Convert via CIEXYZ space using sRGB space, as suggested in docs
        // return sRGB.fromCIEXYZ(toCIEXYZ(colorvalue));
    }

    public float[] fromRGB(float[] rgbvalue) {
//        Y   =     0.299  R + 0.587  G + 0.114  B
//        Cb  =   - 0.1687 R - 0.3313 G + 0.5    B + 128
//        Cr  =     0.5    R - 0.4187 G - 0.0813 B + 128
        return new float[] {
                 0.299f  * rgbvalue[0] + 0.587f  * rgbvalue[1] + 0.114f  * rgbvalue[2],
                -0.1687f * rgbvalue[0] - 0.3313f * rgbvalue[1] + 0.5f    * rgbvalue[2] + .5f,
                 0.5f    * rgbvalue[0] - 0.4187f * rgbvalue[1] - 0.0813f * rgbvalue[2] + .5f
        };
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        throw new UnsupportedOperationException("Method toCIEXYZ not implemented"); // TODO: Implement
    }

    public float[] fromCIEXYZ(float[] colorvalue) {
        throw new UnsupportedOperationException("Method fromCIEXYZ not implemented"); // TODO: Implement
    }
}
