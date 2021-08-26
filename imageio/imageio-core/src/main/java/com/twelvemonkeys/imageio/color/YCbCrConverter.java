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

/**
 * Fast YCbCr to RGB conversion.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author Original code by Werner Randelshofer (used by permission).
 */
public final class YCbCrConverter {
    /**
     * Define tables for YCC->RGB color space conversion.
     */
    private final static int SCALEBITS = 16;
    private final static int MAXJSAMPLE = 255;
    private final static int CENTERJSAMPLE = 128;
    private final static int ONE_HALF = 1 << (SCALEBITS - 1);

    private final static class JPEG {
        private final static int[] Cr_R_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_B_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cr_G_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_G_LUT = new int[MAXJSAMPLE + 1];

        /**
         * Initializes tables for YCC->RGB color space conversion.
         */
        private static void buildYCCtoRGBtable() {
            if (ColorSpaces.DEBUG) {
                System.err.println("Building JPEG YCbCr conversion table");
            }

            for (int i = 0, x = -CENTERJSAMPLE; i <= MAXJSAMPLE; i++, x++) {
                // i is the actual input pixel value, in the range 0..MAXJSAMPLE
                // The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE
                // Cr=>R value is nearest int to 1.40200 * x
                Cr_R_LUT[i] = (int) ((1.40200 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF) >> SCALEBITS;
                // Cb=>B value is nearest int to 1.77200 * x
                Cb_B_LUT[i] = (int) ((1.77200 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF) >> SCALEBITS;
                // Cr=>G value is scaled-up -0.71414 * x
                Cr_G_LUT[i] = -(int) (0.71414 * (1 << SCALEBITS) + 0.5) * x;
                // Cb=>G value is scaled-up -0.34414 * x
                // We also add in ONE_HALF so that need not do it in inner loop
                Cb_G_LUT[i] = -(int) ((0.34414) * (1 << SCALEBITS) + 0.5) * x + ONE_HALF;
            }
        }

        static {
            buildYCCtoRGBtable();
        }
    }

    private final static class ITU_R_601 {
        private final static int[] Cr_R_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_B_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cr_G_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_G_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Y_LUT = new int[MAXJSAMPLE + 1];

        /**
         * Initializes tables for YCC->RGB color space conversion.
         */
        private static void buildYCCtoRGBtable() {
            if (ColorSpaces.DEBUG) {
                System.err.println("Building ITU-R REC.601 YCbCr conversion table");
            }

            for (int i = 0, x = -CENTERJSAMPLE; i <= MAXJSAMPLE; i++, x++) {
                // i is the actual input pixel value, in the range 0..MAXJSAMPLE
                // The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE

                // Y'CbCr to RGB conversion, using values from BT.601 specification:
                //   R = 1.16438 * (Y'-16)                      + 1.59603 * (Cr-128)
                //   G = 1.16438 * (Y'-16) - 0.39176 * (Cb-128) - 0.81297 * (Cr-128)
                //   B = 1.16438 * (Y'-16) + 2.01723 * (Cb-128)

                // Cr=>R value is nearest int to 1.59603 * x
                Cr_R_LUT[i] = ((int) (1.59603 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF) >> SCALEBITS;
                // Cb=>B value is nearest int to 2.01723 * x
                Cb_B_LUT[i] = ((int) (2.01723 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF) >> SCALEBITS;
                // Cr=>G value is scaled-up -0.81297 * x
                Cr_G_LUT[i] = -(int) (0.81297 * (1 << SCALEBITS) + 0.5) * x;
                // Cb=>G value is scaled-up -0.39176 * x
                // We also add in ONE_HALF so that need not do it in inner loop
                Cb_G_LUT[i] = -(int) ((0.39176) * (1 << SCALEBITS) + 0.5) * x + ONE_HALF;

                // Y`=>RGB
                Y_LUT[i] = ((int) (1.16438 * (1 << SCALEBITS) + 0.5) * (i - 16) + ONE_HALF) >> SCALEBITS;
            }
        }

        static {
            buildYCCtoRGBtable();
        }
    }

    public static void convertYCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final double[] coefficients, double[] referenceBW, final int offset) {
        double y;
        double cb;
        double cr;

        if (referenceBW == null) {
            // Default case
            y = (yCbCr[offset] & 0xff);
            cb = (yCbCr[offset + 1] & 0xff) - 128;
            cr = (yCbCr[offset + 2] & 0xff) - 128;
        }
        else {
            // Custom values
            y = ((yCbCr[offset] & 0xff) - referenceBW[0]) * 255.0 / (referenceBW[1] - referenceBW[0]);
            cb = ((yCbCr[offset + 1] & 0xff) - referenceBW[2]) * 127.0 / (referenceBW[3] - referenceBW[2]);
            cr = ((yCbCr[offset + 2] & 0xff) - referenceBW[4]) * 127.0 / (referenceBW[5] - referenceBW[4]);
        }

        double lumaRed = coefficients[0];
        double lumaGreen = coefficients[1];
        double lumaBlue = coefficients[2];

        int red = (int) Math.round(cr * (2.0 - 2.0 * lumaRed) + y);
        int blue = (int) Math.round(cb * (2.0 - 2.0 * lumaBlue) + y);
        int green = (int) Math.round((y - lumaRed * red - lumaBlue * blue) / lumaGreen);

        rgb[offset] = clamp(red);
        rgb[offset + 2] = clamp(blue);
        rgb[offset + 1] = clamp(green);
    }

    public static void convertJPEGYCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final int offset) {
        int y  = yCbCr[offset    ] & 0xff;
        int cb = yCbCr[offset + 1] & 0xff;
        int cr = yCbCr[offset + 2] & 0xff;

        rgb[offset    ] = clamp(y + JPEG.Cr_R_LUT[cr]);
        rgb[offset + 1] = clamp(y + (JPEG.Cb_G_LUT[cb] + JPEG.Cr_G_LUT[cr] >> SCALEBITS));
        rgb[offset + 2] = clamp(y + JPEG.Cb_B_LUT[cb]);
    }

    public static void convertRec601YCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final int offset) {
        int y =  yCbCr[offset    ] & 0xff;
        int cb = yCbCr[offset + 1] & 0xff;
        int cr = yCbCr[offset + 2] & 0xff;

        rgb[offset    ] = clamp(ITU_R_601.Y_LUT[y] + ITU_R_601.Cr_R_LUT[cr]);
        rgb[offset + 1] = clamp(ITU_R_601.Y_LUT[y] + (ITU_R_601.Cr_G_LUT[cr] + ITU_R_601.Cb_G_LUT[cb] >> SCALEBITS));
        rgb[offset + 2] = clamp(ITU_R_601.Y_LUT[y] + ITU_R_601.Cb_B_LUT[cb]);
    }

    private static byte clamp(final int val) {
        return (byte) Math.max(0, Math.min(255, val));
    }
}
