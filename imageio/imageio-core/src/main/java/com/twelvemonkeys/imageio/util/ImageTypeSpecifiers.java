/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.imageio.color.DiscreteAlphaIndexColorModel;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.*;
import java.awt.image.*;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * Factory class for creating {@code ImageTypeSpecifier}s.
 * Fixes some subtle bugs in {@code ImageTypeSpecifier}'s factory methods, but
 * in most cases, this class will delegate to the corresponding methods in {@link ImageTypeSpecifier}.
 *
 * @see ImageTypeSpecifier
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageTypeSpecifiers.java,v 1.0 24.01.11 17.51 haraldk Exp$
 */
public final class ImageTypeSpecifiers {

    private static final ImageTypeSpecifier TYPE_INT_RGB = createPackedOddBits(ColorSpace.getInstance(ColorSpace.CS_sRGB), 24,
                                                                               0xFF0000,
                                                                               0x00FF00,
                                                                               0x0000FF,
                                                                               0x0,
                                                                               DataBuffer.TYPE_INT,
                                                                               false);
    private static final ImageTypeSpecifier TYPE_INT_BGR = createPackedOddBits(ColorSpace.getInstance(ColorSpace.CS_sRGB), 24,
                                                                               0x0000FF,
                                                                               0x00FF00,
                                                                               0xFF0000,
                                                                               0x0,
                                                                               DataBuffer.TYPE_INT,
                                                                               false);
    private static final ImageTypeSpecifier TYPE_USHORT_565_RGB = createPackedOddBits(ColorSpace.getInstance(ColorSpace.CS_sRGB), 16,
                                                                                      0xF800,
                                                                                      0x07E0,
                                                                                      0x001F,
                                                                                      0x0,
                                                                                      DataBuffer.TYPE_USHORT,
                                                                                      false);
    private static final ImageTypeSpecifier TYPE_USHORT_555_RGB = createPackedOddBits(ColorSpace.getInstance(ColorSpace.CS_sRGB), 15,
                                                                                      0x7C00,
                                                                                      0x03E0,
                                                                                      0x001F,
                                                                                      0x0,
                                                                                      DataBuffer.TYPE_USHORT,
                                                                                      false);

    private ImageTypeSpecifiers() {}

    public static ImageTypeSpecifier createFromBufferedImageType(final int bufferedImageType) {
        switch (bufferedImageType) {
            // ImageTypeSpecifier unconditionally uses bits == 32, we'll use a workaround for the INT_RGB and USHORT types
            case BufferedImage.TYPE_INT_RGB:
                return TYPE_INT_RGB;

            case BufferedImage.TYPE_INT_BGR:
                return TYPE_INT_BGR;

            case BufferedImage.TYPE_USHORT_565_RGB:
                return TYPE_USHORT_565_RGB;

            case BufferedImage.TYPE_USHORT_555_RGB:
                return TYPE_USHORT_555_RGB;

            default:
        }

        return ImageTypeSpecifier.createFromBufferedImageType(bufferedImageType);
    }

    public static ImageTypeSpecifier createPacked(final ColorSpace colorSpace,
                                                  final int redMask, final int greenMask,
                                                  final int blueMask, final int alphaMask,
                                                  final int transferType, boolean isAlphaPremultiplied) {
        int bits = calculateRequiredBits(redMask | greenMask | blueMask | alphaMask);
        if (bits != 32) {
            // ImageTypeSpecifier unconditionally uses bits == 32, we'll use a workaround for BYTE/USHORT types
            return createPackedOddBits(colorSpace, bits, redMask, greenMask, blueMask, alphaMask, transferType, isAlphaPremultiplied);
        }

        return ImageTypeSpecifier.createPacked(colorSpace, redMask, greenMask, blueMask, alphaMask, transferType, isAlphaPremultiplied);
    }

    private static int calculateRequiredBits(int mask) {
        // See https://graphics.stanford.edu/~seander/bithacks.html#IntegerLogObvious
        int r = 1;

        while ((mask >>>= 1) != 0) {
            r++;
        }

        return r;
    }

    static ImageTypeSpecifier createPackedOddBits(final ColorSpace colorSpace, int bits,
                                                  final int redMask, final int greenMask,
                                                  final int blueMask, final int alphaMask,
                                                  final int transferType, boolean isAlphaPremultiplied) {
        // ImageTypeSpecifier unconditionally uses bits == 32, we'll use a workaround
        notNull(colorSpace, "colorSpace");
        isTrue(colorSpace.getType() == ColorSpace.TYPE_RGB, colorSpace, "ColorSpace must be TYPE_RGB");
        isTrue(redMask != 0 || greenMask != 0 || blueMask != 0 || alphaMask != 0, "No mask has at least 1 bit set");

        ColorModel colorModel = new DirectColorModel(colorSpace, bits, redMask, greenMask, blueMask, alphaMask,
                                                     isAlphaPremultiplied, transferType);

        return new ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(1, 1));
    }

    public static ImageTypeSpecifier createInterleaved(final ColorSpace colorSpace,
                                                       final int[] bandOffsets,
                                                       final int dataType,
                                                       final boolean hasAlpha,
                                                       final boolean isAlphaPremultiplied) {
        // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
        if (dataType == DataBuffer.TYPE_INT) {
            return UInt32ImageTypeSpecifier.createInterleaved(colorSpace, bandOffsets, hasAlpha, isAlphaPremultiplied);
        }

        // ...or fall back to default for anything else
        return ImageTypeSpecifier.createInterleaved(colorSpace, bandOffsets, dataType, hasAlpha, isAlphaPremultiplied);
    }

    public static ImageTypeSpecifier createBanded(final ColorSpace colorSpace,
                                                  final int[] bankIndices, final int[] bandOffsets,
                                                  final int dataType,
                                                  final boolean hasAlpha, final boolean isAlphaPremultiplied) {
        // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
        if (dataType == DataBuffer.TYPE_INT) {
            return UInt32ImageTypeSpecifier.createBanded(colorSpace, bankIndices, bandOffsets, hasAlpha, isAlphaPremultiplied);
        }

        // ...or fall back to default for anything else
        return ImageTypeSpecifier.createBanded(colorSpace, bankIndices, bandOffsets, dataType, hasAlpha, isAlphaPremultiplied);
    }

    public static ImageTypeSpecifier createGrayscale(final int bits, final int dataType) {
        if (bits == 16 && dataType == DataBuffer.TYPE_SHORT) {
            // As the ComponentColorModel is broken for 16 bit signed int, we'll use our own version
            return new Int16ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0}, false, false);
        }
        else if (bits == 32 && dataType == DataBuffer.TYPE_INT) {
            // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
            return UInt32ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0}, false, false);
        }
        else if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
            return ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0}, dataType, false, false);
        }

        // NOTE: The isSigned boolean is stored but *not used for anything* in the Grayscale ImageTypeSpecifier...
        return ImageTypeSpecifier.createGrayscale(bits, dataType, false);
    }

    public static ImageTypeSpecifier createGrayscale(final int bits, final int dataType, final boolean isAlphaPremultiplied) {
        if (bits == 16 && dataType == DataBuffer.TYPE_SHORT) {
            // As the ComponentColorModel is broken for 16 bit signed int, we'll use our own version
            return new Int16ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0, 1}, true, isAlphaPremultiplied);
        }
        else if (bits == 32 && dataType == DataBuffer.TYPE_INT) {
            // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
            return UInt32ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0, 1}, true, isAlphaPremultiplied);
        }
        else if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
            return ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0, 1}, dataType, true, isAlphaPremultiplied);
        }

        // NOTE: The isSigned boolean is stored but *not used for anything* in the Grayscale ImageTypeSpecifier...
        return ImageTypeSpecifier.createGrayscale(bits, dataType, false, isAlphaPremultiplied);
    }

    public static ImageTypeSpecifier createPackedGrayscale(final ColorSpace colorSpace, final int bits, final int dataType) {
        notNull(colorSpace, "colorSpace");
        isTrue(colorSpace.getType() == ColorSpace.TYPE_GRAY, colorSpace, "ColorSpace must be TYPE_GRAY");
        isTrue(bits == 1 || bits == 2 || bits == 4, bits, "bits must be 1, 2, or 4: %s");
        isTrue(dataType == DataBuffer.TYPE_BYTE, dataType, "dataType must be TYPE_BYTE: %s");

        int numEntries = 1 << bits;

        ColorModel colorModel;

        if (ColorSpace.getInstance(ColorSpace.CS_GRAY).equals(colorSpace)) {
            // For default gray, use linear response
            byte[] gray = new byte[numEntries];

            for (int i = 0; i < numEntries; i++) {
                gray[i] = (byte) ((i * 255) / (numEntries - 1));
            }

            colorModel = new IndexColorModel(bits, numEntries, gray, gray, gray);
        }
        else {
            byte[] r = new byte[numEntries];
            byte[] g = new byte[numEntries];
            byte[] b = new byte[numEntries];

            // Scale array values according to color profile..
            for (int i = 0; i < numEntries; i++) {
                float[] gray = new float[] { i / (float) (numEntries - 1) };
                float[] rgb = colorSpace.toRGB(gray);

                r[i] = (byte) Math.round(rgb[0] * 255);
                g[i] = (byte) Math.round(rgb[1] * 255);
                b[i] = (byte) Math.round(rgb[2] * 255);
            }

            colorModel = new IndexColorModel(bits, numEntries, r, g, b);
        }

        SampleModel sampleModel = new MultiPixelPackedSampleModel(dataType, 1, 1, bits);

        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    public static ImageTypeSpecifier createIndexed(final byte[] redLUT, final byte[] greenLUT,
                                                   final byte[] blueLUT, final byte[] alphaLUT,
                                                   final int bits, final int dataType) {
        return ImageTypeSpecifier.createIndexed(redLUT, greenLUT, blueLUT, alphaLUT, bits, dataType);
    }

    public static ImageTypeSpecifier createIndexed(final int[] colors, final boolean hasAlpha, final int transIndex,
                                                   final int bits, final int dataType) {
        return createFromIndexColorModel(new IndexColorModel(bits, colors.length, colors, 0, hasAlpha, transIndex, dataType));
    }

    public static ImageTypeSpecifier createFromIndexColorModel(final IndexColorModel colorModel) {
        return new IndexedImageTypeSpecifier(colorModel);
    }

    public static ImageTypeSpecifier createDiscreteAlphaIndexedFromIndexColorModel(final IndexColorModel colorModel) {
        ColorModel discreteAlphaIndexColorModel = new DiscreteAlphaIndexColorModel(colorModel);
        return new ImageTypeSpecifier(discreteAlphaIndexColorModel, discreteAlphaIndexColorModel.createCompatibleSampleModel(1, 1));
    }

    public static ImageTypeSpecifier createDiscreteExtraSamplesIndexedFromIndexColorModel(final IndexColorModel colorModel, int extraSamples, boolean hasAlpha) {
        ColorModel discreteAlphaIndexColorModel = new DiscreteAlphaIndexColorModel(colorModel, extraSamples, hasAlpha);
        return new ImageTypeSpecifier(discreteAlphaIndexColorModel, discreteAlphaIndexColorModel.createCompatibleSampleModel(1, 1));
    }

    public static ImageTypeSpecifier createFromRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }

        if (image instanceof BufferedImage) {
            int bufferedImageType = ((BufferedImage) image).getType();

            if (bufferedImageType != BufferedImage.TYPE_CUSTOM &&
                    // Need to retain the actual palette in the color model for IndexColorModel
                    bufferedImageType != BufferedImage.TYPE_BYTE_BINARY && bufferedImageType != BufferedImage.TYPE_BYTE_INDEXED) {
                return createFromBufferedImageType(bufferedImageType);
            }
        }

        return new ImageTypeSpecifier(image);
    }
}
