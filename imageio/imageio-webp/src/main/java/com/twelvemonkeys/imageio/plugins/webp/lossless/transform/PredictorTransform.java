package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

import java.awt.image.*;

import static java.lang.Math.*;

public class PredictorTransform implements Transform {
    private final Raster data;
    private final byte bits;

    public PredictorTransform(Raster raster, byte bits) {
        this.data = raster;
        this.bits = bits;
    }

    @Override
    public void applyInverse(WritableRaster raster) {
    }

    private static int ALPHA(final int ARGB) {
        return ARGB >>> 24;
    }

    private static int RED(final int ARGB) {
        return (ARGB >> 16) & 0xff;
    }

    private static int GREEN(final int ARGB) {
        return (ARGB >> 8) & 0xff;
    }

    private static int BLUE(final int ARGB) {
        return ARGB & 0xff;
    }

    private static int select(final int L, final int T, final int TL) {
        // L = left pixel, T = top pixel, TL = top left pixel.

        // ARGB component estimates for prediction.
        int pAlpha = ALPHA(L) + ALPHA(T) - ALPHA(TL);
        int pRed = RED(L) + RED(T) - RED(TL);
        int pGreen = GREEN(L) + GREEN(T) - GREEN(TL);
        int pBlue = BLUE(L) + BLUE(T) - BLUE(TL);

        // Manhattan distances to estimates for left and top pixels.
        int pL = abs(pAlpha - ALPHA(L)) + abs(pRed - RED(L)) +
                 abs(pGreen - GREEN(L)) + abs(pBlue - BLUE(L));
        int pT = abs(pAlpha - ALPHA(T)) + abs(pRed - RED(T)) +
                 abs(pGreen - GREEN(T)) + abs(pBlue - BLUE(T));

        // Return either left or top, the one closer to the prediction.
        return pL < pT ? L : T;
    }

    private static int average2(final int a, final int b) {
        return (a + b) / 2;
    }

    // Clamp the input value between 0 and 255.
    private static int clamp(final int a) {
        return max(0, min(a, 255));
    }

    private static int clampAddSubtractFull(final int a, final int b, final int c) {
        return clamp(a + b - c);
    }

    private static int clampAddSubtractHalf(final int a, final int b) {
        return clamp(a + (a - b) / 2);
    }

}
