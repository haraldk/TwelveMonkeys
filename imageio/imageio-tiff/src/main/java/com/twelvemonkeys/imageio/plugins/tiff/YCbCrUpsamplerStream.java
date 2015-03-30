/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.lang.Validate;

import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Input stream that provides on-the-fly conversion and upsampling of TIFF subsampled YCbCr samples to (raw) RGB samples.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: YCbCrUpsamplerStream.java,v 1.0 31.01.13 09:25 haraldk Exp$
 */
final class YCbCrUpsamplerStream extends FilterInputStream {
    // NOTE: DO NOT MODIFY OR EXPOSE THIS ARRAY OUTSIDE PACKAGE!
    static final double[] CCIR_601_1_COEFFICIENTS = new double[] {299.0 / 1000.0, 587.0 / 1000.0, 114.0 / 1000.0};

    private final int horizChromaSub;
    private final int vertChromaSub;
    private final int yCbCrPos;
    private final int columns;
    private final double[] coefficients;

    private final int units;
    private final int unitSize;
    private final int padding;
    private final byte[] decodedRows;
    int decodedLength;
    int decodedPos;

    private final byte[] buffer;
    int bufferLength;
    int bufferPos;

    public YCbCrUpsamplerStream(final InputStream stream, final int[] chromaSub, final int yCbCrPos, final int columns, final double[] coefficients) {
        super(Validate.notNull(stream, "stream"));

        Validate.notNull(chromaSub, "chromaSub");
        Validate.isTrue(chromaSub.length == 2, "chromaSub.length != 2");

        this.horizChromaSub = chromaSub[0];
        this.vertChromaSub = chromaSub[1];
        this.yCbCrPos = yCbCrPos;
        this.columns = columns;
        this.coefficients = Arrays.equals(CCIR_601_1_COEFFICIENTS, coefficients) ? null : coefficients;

        // In TIFF, subsampled streams are stored in "units" of horiz * vert pixels.
        // For a 4:2 subsampled stream like this:
        //
        //   Y0 Y1 Y2 Y3   Cb0   Cr0   Y8 Y9 Y10 Y11   Cb1   Cr1
        //   Y4 Y5 Y6 Y7               Y12Y13Y14 Y15
        //
        // In the stream, the order is: Y0,Y1,Y2..Y7,Cb0,Cr0, Y8...Y15,Cb1,Cr1, Y16...

        unitSize = horizChromaSub * vertChromaSub + 2;
        units = (columns + horizChromaSub - 1) / horizChromaSub;    // If columns % horizChromasSub != 0...
        padding = units * horizChromaSub - columns;                 // ...each coded row will be padded to fill unit
        decodedRows = new byte[columns * vertChromaSub * 3];
        buffer = new byte[unitSize * units];
    }

    private void fetch() throws IOException {
        if (bufferPos >= bufferLength) {
            int pos = 0;
            int read;

            // This *SHOULD* read an entire row of units into the buffer, otherwise decodeRows will throw EOFException
            while (pos < buffer.length && (read = in.read(buffer, pos, buffer.length - pos)) > 0) {
                pos += read;
            }

            bufferLength = pos;
            bufferPos = 0;
        }

        if (bufferLength > 0) {
            decodeRows();
        }
        else {
            decodedLength = -1;
        }
    }

    private void decodeRows() throws EOFException {
        decodedLength = decodedRows.length;

        for (int u = 0; u < units; u++) {
            if (bufferPos >= bufferLength) {
                throw new EOFException("Unexpected end of stream");
            }

            // Decode one unit
            byte cb = buffer[bufferPos + unitSize - 2];
            byte cr = buffer[bufferPos + unitSize - 1];

            for (int y = 0; y < vertChromaSub; y++) {
                for (int x = 0; x < horizChromaSub; x++) {
                    // Skip padding at end of row
                    int column = horizChromaSub * u + x;
                    if (column >= columns) {
                        bufferPos += padding;
                        break;
                    }

                    int pixelOff = 3 * (column + columns * y);

                    decodedRows[pixelOff] = buffer[bufferPos++];
                    decodedRows[pixelOff + 1] = cb;
                    decodedRows[pixelOff + 2] = cr;

                    // Convert to RGB
                    if (coefficients == null) {
                        YCbCrConverter.convertYCbCr2RGB(decodedRows, decodedRows, pixelOff);
                    }
                    else {
                        convertYCbCr2RGB(decodedRows, decodedRows, coefficients, pixelOff);
                    }
                }
            }

            bufferPos += 2; // Skip CbCr bytes at end of unit
        }

        bufferPos = bufferLength;
        decodedPos = 0;
    }

    @Override
    public int read() throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        return decodedRows[decodedPos++] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        int read = Math.min(decodedLength - decodedPos, len);
        System.arraycopy(decodedRows, decodedPos, b, off, read);
        decodedPos += read;

        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        int skipped = (int) Math.min(decodedLength - decodedPos, n);
        decodedPos += skipped;

        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    private void convertYCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final double[] coefficients, final int offset) {
        double y  = (yCbCr[offset    ] & 0xff);
        double cb = (yCbCr[offset + 1] & 0xff) - 128;
        double cr = (yCbCr[offset + 2] & 0xff) - 128;

        double lumaRed   = coefficients[0];
        double lumaGreen = coefficients[1];
        double lumaBlue  = coefficients[2];

        int red = (int) Math.round(cr * (2 - 2 * lumaRed) + y);
        int blue = (int) Math.round(cb * (2 - 2 * lumaBlue) + y);
        int green = (int) Math.round((y - lumaRed * red - lumaBlue * blue) / lumaGreen);

        rgb[offset    ] = clamp(red);
        rgb[offset + 2] = clamp(blue);
        rgb[offset + 1] = clamp(green);
    }

    private static byte clamp(int val) {
        return (byte) Math.max(0, Math.min(255, val));
    }

    // TODO: This code is copied from JPEG package, make it "more" public: com.tm.imageio.color package?
    /**
     * Static inner class for lazy-loading of conversion tables.
     */
    static final class YCbCrConverter {
        /** Define tables for YCC->RGB color space conversion. */
        private final static int SCALEBITS = 16;
        private final static int MAXJSAMPLE = 255;
        private final static int CENTERJSAMPLE = 128;
        private final static int ONE_HALF = 1 << (SCALEBITS - 1);

        private final static int[] Cr_R_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_B_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cr_G_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_G_LUT = new int[MAXJSAMPLE + 1];

        /**
         * Initializes tables for YCC->RGB color space conversion.
         */
        private static void buildYCCtoRGBtable() {
            if (TIFFImageReader.DEBUG) {
                System.err.println("Building YCC conversion table");
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

        static void convertYCbCr2RGB(final Raster raster) {
            final int height = raster.getHeight();
            final int width = raster.getWidth();
            final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    convertYCbCr2RGB(data, data, (x + y * width) * 3);
                }
            }
        }

        static void convertYCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final int offset) {
            int y  = yCbCr[offset    ] & 0xff;
            int cr = yCbCr[offset + 2] & 0xff;
            int cb = yCbCr[offset + 1] & 0xff;

            rgb[offset    ] = clamp(y + Cr_R_LUT[cr]);
            rgb[offset + 1] = clamp(y + (Cb_G_LUT[cb] + Cr_G_LUT[cr] >> SCALEBITS));
            rgb[offset + 2] = clamp(y + Cb_B_LUT[cb]);
        }

        static void convertYCCK2CMYK(final Raster raster) {
            final int height = raster.getHeight();
            final int width = raster.getWidth();
            final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    convertYCCK2CMYK(data, data, (x + y * width) * 4);
                }
            }
        }

        private static void convertYCCK2CMYK(byte[] ycck, byte[] cmyk, int offset) {
            // Inverted
            int y  = 255 - ycck[offset    ] & 0xff;
            int cb = 255 - ycck[offset + 1] & 0xff;
            int cr = 255 - ycck[offset + 2] & 0xff;
            int k  = 255 - ycck[offset + 3] & 0xff;

            int cmykC = MAXJSAMPLE - (y + Cr_R_LUT[cr]);
            int cmykM = MAXJSAMPLE - (y + (Cb_G_LUT[cb] + Cr_G_LUT[cr] >> SCALEBITS));
            int cmykY = MAXJSAMPLE - (y + Cb_B_LUT[cb]);

            cmyk[offset    ] = clamp(cmykC);
            cmyk[offset + 1] = clamp(cmykM);
            cmyk[offset + 2] = clamp(cmykY);
            cmyk[offset + 3] = (byte) k; // K passes through unchanged
        }
    }
}
