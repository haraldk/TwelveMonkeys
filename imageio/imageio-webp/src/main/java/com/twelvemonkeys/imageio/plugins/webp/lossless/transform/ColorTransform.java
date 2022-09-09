/*
 * Copyright (c) 2022, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

import java.awt.image.*;

/**
 * @author Simon Kammermeier
 */
public class ColorTransform implements Transform {
    private final Raster data;
    private final byte bits;

    public ColorTransform(Raster raster, byte bits) {
        this.data = raster;
        this.bits = bits;
    }

    @Override
    public void applyInverse(WritableRaster raster) {
        int width = raster.getWidth();
        int height = raster.getHeight();

        byte[] rgba = new byte[4];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                data.getDataElements(x >> bits, y >> bits, rgba);
                ColorTransformElement trans = new ColorTransformElement(rgba);

                raster.getDataElements(x, y, rgba);

                trans.inverseTransform(rgba);

                raster.setDataElements(x, y, rgba);
            }
        }
    }

    // NOTE: For encoding!
    private static void colorTransform(final int red, final int blue, final int green,
                                       final ColorTransformElement trans,
                                       final int[] newRedBlue) {
        // Transformed values of red and blue components
        int tmp_red = red;
        int tmp_blue = blue;

        // Applying transform is just adding the transform deltas
        tmp_red += colorTransformDelta((byte) trans.green_to_red, (byte) green);
        tmp_blue += colorTransformDelta((byte) trans.green_to_blue, (byte) green);
        tmp_blue += colorTransformDelta((byte) trans.red_to_blue, (byte) red);

        // No pointer dereferences in Java...
        // TODO: Consider passing an offset too, so we can modify in-place
        newRedBlue[0] = tmp_red & 0xff;
        newRedBlue[1] = tmp_blue & 0xff;
    }

    // A conversion from the 8-bit unsigned representation (uint8) to the 8-bit
    // signed one (int8) is required before calling ColorTransformDelta(). It
    // should be performed using 8-bit two's complement (that is: uint8 range
    // [128-255] is mapped to the [-128, -1] range of its converted int8
    // value).
    private static byte colorTransformDelta(final byte t, final byte c) {
        return (byte) ((t * c) >> 5);
    }

    private static final class ColorTransformElement {

        final int green_to_red;
        final int green_to_blue;
        final int red_to_blue;

        ColorTransformElement(final byte[] rgba) {
            this.green_to_red = rgba[2];
            this.green_to_blue = rgba[1];
            this.red_to_blue = rgba[0];
        }

        private void inverseTransform(final byte[] rgb) {
            // Applying inverse transform is just adding (!, different from specification) the
            // color transform deltas 3

            // Transformed values of red and blue components
            int tmp_red = rgb[0];
            int tmp_blue = rgb[2];

            tmp_red += colorTransformDelta((byte) this.green_to_red, rgb[1]);
            tmp_blue += colorTransformDelta((byte) this.green_to_blue, rgb[1]);
            tmp_blue += colorTransformDelta((byte) this.red_to_blue, (byte) tmp_red); // Spec has red & 0xff

            rgb[0] = (byte) (tmp_red & 0xff);
            rgb[2] = (byte) (tmp_blue & 0xff);
        }
    }

}
