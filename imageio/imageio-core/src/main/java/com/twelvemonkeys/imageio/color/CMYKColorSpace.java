/*
 * Copyright (c) 2011, Harald Kuhr
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

import java.awt.color.ColorSpace;

/**
 * A fallback CMYK ColorSpace, in case none can be read from disk.
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: CMYKColorSpace.java,v 1.0 Apr 30, 2008 1:38:13 PM haraldk Exp$
*/
final class CMYKColorSpace extends ColorSpace {

    static final ColorSpace INSTANCE = new CMYKColorSpace();

    final ColorSpace sRGB = getInstance(CS_sRGB);

    private CMYKColorSpace() {
        super(ColorSpace.TYPE_CMYK, 4);
    }

    public static ColorSpace getInstance() {
        return INSTANCE;
    }

    public float[] toRGB(float[] colorvalue) {
        return new float[] {
                (1 - colorvalue[0]) * (1 - colorvalue[3]),
                (1 - colorvalue[1]) * (1 - colorvalue[3]),
                (1 - colorvalue[2]) * (1 - colorvalue[3])
        };

        // TODO: Convert via CIEXYZ space using sRGB space, as suggested in docs
        // return sRGB.fromCIEXYZ(toCIEXYZ(colorvalue));
    }

    public float[] fromRGB(float[] rgbvalue) {
        // Compute CMY
        float c = 1 - rgbvalue[0];
        float m = 1 - rgbvalue[1];
        float y = 1 - rgbvalue[2];

        // Find K
        float k = Math.min(c, Math.min(m, y));

        // Convert to CMYK values
        return new float[] {(c - k), (m - k), (y - k), k};

        /*
        http://www.velocityreviews.com/forums/t127265-rgb-to-cmyk.html

        (Step 0: Normalize R,G, and B values to fit into range [0.0 ... 1.0], or
        adapt the following matrix.)

        Step 1: RGB to CMY

        | C |   | 1 |   | R |
        | M | = | 1 | - | G |
        | Y |   | 1 |   | B |

        Step 2: CMY to CMYK

        | C' |   | C |            | min(C,M,Y) |
        | M' |   | M |            | min(C,M,Y) |
        | Y' | = | Y |          - | min(C,M,Y) |
        | K' |   | min(C,M,Y) |   | 0 |

        Easier to calculate if K' is calculated first, because K' = min(C,M,Y):

        | C' |   | C |   | K' |
        | M' |   | M |   | K' |
        | Y' | = | Y | - | K' |
        | K' |   | K'|   | 0 |
         */

        //        return fromCIEXYZ(sRGB.toCIEXYZ(rgbvalue));
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        return sRGB.toCIEXYZ(toRGB(colorvalue));
    }

    public float[] fromCIEXYZ(float[] colorvalue) {
        return sRGB.fromCIEXYZ(fromRGB(colorvalue));
    }
}
