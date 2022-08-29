package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

import java.awt.image.*;

public class ColorTransform implements Transform {
    private final Raster data;
    private final byte bits;

    public ColorTransform(Raster raster, byte bits) {
        this.data = raster;
        this.bits = bits;
    }

    @Override
    public void applyInverse(WritableRaster raster) {
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

    private static void inverseTransform(final byte red, final byte green, final byte blue,
                                         final ColorTransformElement trans,
                                         final int[] newRedBlue) {
        // Applying inverse transform is just subtracting the
        // color transform deltas
        // Transformed values of red and blue components
        int tmp_red = red;
        int tmp_blue = blue;

        tmp_red -= colorTransformDelta((byte) trans.green_to_red, green);
        tmp_blue -= colorTransformDelta((byte) trans.green_to_blue, green);
        tmp_blue -= colorTransformDelta((byte) trans.red_to_blue, red); // Spec has red & 0xff

        newRedBlue[0] = tmp_red & 0xff;
        newRedBlue[1] = tmp_blue & 0xff;
    }

    private static void inverseTransform(final byte[] rgb, final ColorTransformElement trans) {
        // Applying inverse transform is just subtracting the
        // color transform deltas
        // Transformed values of red and blue components
        int tmp_red = rgb[0];
        int tmp_blue = rgb[2];

        tmp_red -= colorTransformDelta((byte) trans.green_to_red, rgb[1]);
        tmp_blue -= colorTransformDelta((byte) trans.green_to_blue, rgb[1]);
        tmp_blue -= colorTransformDelta((byte) trans.red_to_blue, rgb[0]); // Spec has red & 0xff

        rgb[0] = (byte) (tmp_red & 0xff);
        rgb[2] = (byte) (tmp_blue & 0xff);
    }

    static final class ColorTransformElement {
        final int green_to_red;
        final int green_to_blue;
        final int red_to_blue;

        ColorTransformElement(final int green_to_red, final int green_to_blue, final int red_to_blue) {
            this.green_to_red = green_to_red;
            this.green_to_blue = green_to_blue;
            this.red_to_blue = red_to_blue;
        }
    }
}
