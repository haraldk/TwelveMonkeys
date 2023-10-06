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

package com.twelvemonkeys.imageio.plugins.webp.lossless;

import java.awt.image.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author Simon Kammermeier
 */
final class PredictorTransform implements Transform {
    private final Raster data;
    private final byte bits;

    public PredictorTransform(Raster raster, byte bits) {
        this.data = raster;
        this.bits = bits;
    }

    @Override
    public void applyInverse(WritableRaster raster) {
        int width = raster.getWidth();
        int height = raster.getHeight();

        byte[] rgba = new byte[4];

        // Handle top and left border separately

        // (0,0) Black (0x000000ff) predict
        raster.getDataElements(0, 0, rgba);
        rgba[3] += (byte) 0xff;
        raster.setDataElements(0, 0, rgba);

        byte[] predictor = new byte[4];
        byte[] predictor2 = new byte[4];
        byte[] predictor3 = new byte[4];

        // (x,0) L predict
        for (int x = 1; x < width; x++) {
            raster.getDataElements(x, 0, rgba);
            raster.getDataElements(x - 1, 0, predictor);
            addPixels(rgba, predictor);

            raster.setDataElements(x, 0, rgba);
        }

        // (0,y) T predict
        for (int y = 1; y < height; y++) {
            raster.getDataElements(0, y, rgba);
            raster.getDataElements(0, y - 1, predictor);
            addPixels(rgba, predictor);

            raster.setDataElements(0, y, rgba);
        }

        for (int y = 1; y < height; y++) {
            for (int x = 1; x < width; x++) {
                int transformType = data.getSample(x >> bits, y >> bits, 1);

                raster.getDataElements(x, y, rgba);

                int lX = x - 1; // x for left
                int tY = y - 1; // y for top

                // top right is not (x+1, tY) if last pixel in line instead (0, y)
                int trX = x == width - 1 ? 0 : x + 1;
                int trY = x == width - 1 ? y : tY;

                switch (transformType) {
                    case PredictorMode.BLACK:
                        rgba[3] += (byte) 0xff;
                        break;
                    case PredictorMode.L:
                        raster.getDataElements(lX, y, predictor);
                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.T:
                        raster.getDataElements(x, tY, predictor);
                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.TR:
                        raster.getDataElements(trX, trY, predictor);
                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.TL:
                        raster.getDataElements(lX, tY, predictor);
                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.AVG_L_TR_T:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(trX, trY, predictor2);
                        average2(predictor, predictor2);

                        raster.getDataElements(x, tY, predictor2);
                        average2(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.AVG_L_TL:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(lX, tY, predictor2);
                        average2(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.AVG_L_T:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(x, tY, predictor2);
                        average2(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.AVG_TL_T:
                        raster.getDataElements(lX, tY, predictor);
                        raster.getDataElements(x, tY, predictor2);
                        average2(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.AVG_T_TR:
                        raster.getDataElements(x, tY, predictor);
                        raster.getDataElements(trX, trY, predictor2);
                        average2(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.AVG_L_TL_T_TR:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(lX, tY, predictor2);
                        average2(predictor, predictor2);

                        raster.getDataElements(x, tY, predictor2);
                        raster.getDataElements(trX, trY, predictor3);
                        average2(predictor2, predictor3);

                        average2(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.SELECT:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(x, tY, predictor2);
                        raster.getDataElements(lX, tY, predictor3);


                        addPixels(rgba, select(predictor, predictor2, predictor3));
                        break;
                    case PredictorMode.CLAMP_ADD_SUB_FULL:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(x, tY, predictor2);
                        raster.getDataElements(lX, tY, predictor3);
                        clampAddSubtractFull(predictor, predictor2, predictor3);

                        addPixels(rgba, predictor);
                        break;
                    case PredictorMode.CLAMP_ADD_SUB_HALF:
                        raster.getDataElements(lX, y, predictor);
                        raster.getDataElements(x, tY, predictor2);
                        average2(predictor, predictor2);

                        raster.getDataElements(lX, tY, predictor2);
                        clampAddSubtractHalf(predictor, predictor2);

                        addPixels(rgba, predictor);
                        break;

                }

                raster.setDataElements(x, y, rgba);
            }
        }
    }

    private static byte[] select(final byte[] l, final byte[] t, final byte[] tl) {
        // l = left pixel, t = top pixel, tl = top left pixel.

        // ARGB component estimates for prediction.

        int pAlpha = addSubtractFull(l[3], t[3], tl[3]);
        int pRed = addSubtractFull(l[0], t[0], tl[0]);
        int pGreen = addSubtractFull(l[1], t[1], tl[1]);
        int pBlue = addSubtractFull(l[2], t[2], tl[2]);

        // Manhattan distances to estimates for left and top pixels.
        int pL = manhattanDistance(l, pAlpha, pRed, pGreen, pBlue);
        int pT = manhattanDistance(t, pAlpha, pRed, pGreen, pBlue);

        // Return either left or top, the one closer to the prediction.
        return pL < pT ? l : t;
    }

    private static int manhattanDistance(byte[] rgba, int pAlpha, int pRed, int pGreen, int pBlue) {
        return abs(pAlpha - (rgba[3] & 0xff)) + abs(pRed - (rgba[0] & 0xff)) +
                abs(pGreen - (rgba[1] & 0xff)) + abs(pBlue - (rgba[2] & 0xff));
    }

    private static void average2(final byte[] rgba1, final byte[] rgba2) {
        rgba1[0] = (byte) (((rgba1[0] & 0xff) + (rgba2[0] & 0xff)) / 2);
        rgba1[1] = (byte) (((rgba1[1] & 0xff) + (rgba2[1] & 0xff)) / 2);
        rgba1[2] = (byte) (((rgba1[2] & 0xff) + (rgba2[2] & 0xff)) / 2);
        rgba1[3] = (byte) (((rgba1[3] & 0xff) + (rgba2[3] & 0xff)) / 2);
    }

    // Clamp the input value between 0 and 255.
    private static int clamp(final int a) {
        return max(0, min(a, 255));
    }

    private static void clampAddSubtractFull(final byte[] a, final byte[] b, final byte[] c) {
        a[0] = (byte) clamp(addSubtractFull(a[0], b[0], c[0]));
        a[1] = (byte) clamp(addSubtractFull(a[1], b[1], c[1]));
        a[2] = (byte) clamp(addSubtractFull(a[2], b[2], c[2]));
        a[3] = (byte) clamp(addSubtractFull(a[3], b[3], c[3]));
    }

    private static void clampAddSubtractHalf(final byte[] a, final byte[] b) {
        a[0] = (byte) clamp(addSubtractHalf(a[0], b[0]));
        a[1] = (byte) clamp(addSubtractHalf(a[1], b[1]));
        a[2] = (byte) clamp(addSubtractHalf(a[2], b[2]));
        a[3] = (byte) clamp(addSubtractHalf(a[3], b[3]));
    }

    private static int addSubtractFull(byte a, byte b, byte c) {
        return (a & 0xff) + (b & 0xff) - (c & 0xff);
    }

    private static int addSubtractHalf(byte a, byte b) {
        return (a & 0xff) + ((a & 0xff) - (b & 0xff)) / 2;
    }

    private static void addPixels(byte[] rgba, byte[] predictor) {
        rgba[0] += predictor[0];
        rgba[1] += predictor[1];
        rgba[2] += predictor[2];
        rgba[3] += predictor[3];
    }

}
