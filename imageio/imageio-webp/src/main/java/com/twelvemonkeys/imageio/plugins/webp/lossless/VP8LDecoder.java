/*
 * Copyright (c) 2017, Harald Kuhr
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

import com.twelvemonkeys.imageio.plugins.webp.LSBBitReader;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.twelvemonkeys.imageio.util.RasterUtils.asByteRaster;
import static java.lang.Math.*;

/**
 * VP8LDecoder.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public final class VP8LDecoder {
    private final ImageInputStream imageInput;
    private final LSBBitReader lsbBitReader;

    public VP8LDecoder(final ImageInputStream imageInput, final boolean debug) {
        this.imageInput = imageInput;
        lsbBitReader = new LSBBitReader(imageInput);
    }

    public void readVP8Lossless(final WritableRaster raster, final boolean topLevel) throws IOException {
        //https://github.com/webmproject/libwebp/blob/666bd6c65483a512fe4c2eb63fbc198b6fb4fae4/src/dec/vp8l_dec.c#L1114

        int xSize = raster.getWidth();
        int ySize = raster.getHeight();

        // Read transforms
        ArrayList<Transform> transforms = new ArrayList<>();
        while (topLevel && lsbBitReader.readBit() == 1) {
            xSize = readTransform(xSize, ySize, transforms);
        }

        // Read color cache size
        int colorCacheBits = 0;
        if (lsbBitReader.readBit() == 1) {
            colorCacheBits = (int) lsbBitReader.readBits(4);
            if (colorCacheBits < 1 || colorCacheBits > 11) {
                throw new IIOException("Corrupt WebP stream, colorCacheBits < 1 || > 11: " + colorCacheBits);
            }
        }

        // Read Huffman codes
        readHuffmanCodes(colorCacheBits, topLevel);

        ColorCache colorCache = null;

        if (colorCacheBits > 0) {
            colorCache = new ColorCache(colorCacheBits);
        }

        // Use the Huffman trees to decode the LZ77 encoded data.
//        decodeImageData(raster, )
    }

    private int readTransform(int xSize, int ySize, List<Transform> transforms) throws IOException {
        int transformType = (int) lsbBitReader.readBits(2);

        // TODO: Each transform type can only be present once in the stream.

        switch (transformType) {
            case TransformType.PREDICTOR_TRANSFORM: {
                System.err.println("transformType: PREDICTOR_TRANSFORM");
//                    int sizeBits = (int) readBits(3) + 2;
                int sizeBits = (int) lsbBitReader.readBits(3) + 2;
                int size = 1 << sizeBits;

                int blockWidth = size;
                int blockHeight = size;

//                    int blockSize = divRoundUp(width, size);
                int blockSize = divRoundUp(xSize, size);

                for (int y = 0; y < ySize; y++) {
                    for (int x = 0; x < xSize; x++) {
                        int blockIndex = (y >> sizeBits) * blockSize + (x >> sizeBits);
                    }
                }

                // Special rules:
                // Top-left pixel of image is predicted BLACK
                // Rest of top pixels is predicted L
                // Rest of leftmost pixels are predicted T
                // Rightmost pixels using TR, uses LEFTMOST pixel on SAME ROW (same distance as TR in memory!)

//                    WritableRaster data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, blockWidth, blockHeight, blockWidth, 1, new int[] {0}, null);
//                    readVP8Lossless(data, false);
//
                break;
            }
            case TransformType.COLOR_TRANSFORM: {
                // The two first transforms contains the exact same data, can be combined
                System.err.println("transformType: COLOR_TRANSFORM");

                int sizeBits = (int) lsbBitReader.readBits(3) + 2;
//                int size = 1 << sizeBits;

                // TODO: Understand difference between spec divRoundUp and impl VP8LSubSampleSize

                int blockWidth = subSampleSize(xSize, sizeBits);
                int blockHeight = subSampleSize(ySize, sizeBits);
                WritableRaster data = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, blockWidth, blockHeight, blockWidth, 1, new int[] {0}, null);
                readVP8Lossless(data, false);

                transforms.add(new Transform(transformType, ((DataBufferByte) data.getDataBuffer()).getData()));

                break;
            }
            case TransformType.SUBTRACT_GREEN: {
                System.err.println("transformType: SUBTRACT_GREEN");
                // No data here

//                    addGreenToBlueAndRed();
                break;
            }
            case TransformType.COLOR_INDEXING_TRANSFORM: {
                System.err.println("transformType: COLOR_INDEXING_TRANSFORM");

                // 8 bit value for color table size
                int colorTableSize = ((int) lsbBitReader.readBits(8)) + 1; // 1-256
                System.err.println("colorTableSize: " + colorTableSize);

                // If the index is equal or larger than color_table_size,
                // the argb color value should be set to 0x00000000
                // We handle this by allocating a possibly larger buffer
                int safeColorTableSize = colorTableSize > 16 ? 256 :
                                         colorTableSize > 4 ? 16 :
                                         colorTableSize > 2 ? 4 : 2;

                System.err.println("safeColorTableSize: " + safeColorTableSize);

                int[] colorTable = new int[safeColorTableSize];

                // The color table can be obtained by reading an image,
                // without the RIFF header, image size, and transforms,
                // assuming a height of one pixel and a width of
                // color_table_size. The color table is always
                // subtraction-coded to reduce image entropy.
                // TODO: Read *without transforms*, using SUBTRACT_GREEN only!
                readVP8Lossless(asByteRaster(
                        Raster.createPackedRaster(
                                new DataBufferInt(colorTable, colorTableSize),
                                colorTableSize, 1, colorTableSize,
                                new int[] {0}, null
                        )
                ), false);

                // TODO: We may not really need this value...
                // What we need is the number of pixels packed into each green sample (byte)
                int widthBits = colorTableSize > 16 ? 0 :
                                colorTableSize > 4 ? 1 :
                                colorTableSize > 2 ? 2 : 3;

                xSize = subSampleSize(xSize, widthBits);

                /*
                // TODO: read ARGB
                int argb = 0;

                // Inverse transform
                // TODO: Expand to mutliple pixels?
                argb = colorTable[GREEN(argb)];
                */

                // TODO: Can we use this to produce an image with IndexColorModel instead of expanding the values in-memory?
                transforms.add(new Transform(transformType, colorTable));

                break;
            }
            default:
                throw new AssertionError("Invalid transformType: " + transformType);
        }

        return xSize;
    }

    private void readHuffmanCodes(int colorCacheBits, boolean allowRecursion) {

    }

    ////

    // FROM the spec
    private static int divRoundUp(final int numerator, final int denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private static int subSampleSize(final int size, final int samplingBits) {
        return (size + (1 << samplingBits) - 1) >> samplingBits;
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

    private static void addGreenToBlueAndRed(byte[] rgb) {
        rgb[0] = (byte) ((rgb[0] + rgb[1]) & 0xff);
        rgb[2] = (byte) ((rgb[2] + rgb[1]) & 0xff);
    }
}
