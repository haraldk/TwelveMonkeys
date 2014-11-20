/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * Factory class for creating {@code ImageTypeSpecifier}s.
 * In most cases, this class will delegate to the corresponding methods in {@link ImageTypeSpecifier}.
 *
 * @see javax.imageio.ImageTypeSpecifier
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageTypeSpecifiers.java,v 1.0 24.01.11 17.51 haraldk Exp$
 */
public final class ImageTypeSpecifiers {

    private ImageTypeSpecifiers() {}

    public static ImageTypeSpecifier createFromBufferedImageType(final int bufferedImageType) {
        return ImageTypeSpecifier.createFromBufferedImageType(bufferedImageType);
    }

    public static ImageTypeSpecifier createPacked(final ColorSpace colorSpace,
                                                  final int redMask, final int greenMask,
                                                  final int blueMask, final int alphaMask,
                                                  final int transferType, boolean isAlphaPremultiplied) {
        return ImageTypeSpecifier.createPacked(colorSpace, redMask, greenMask, blueMask, alphaMask, transferType, isAlphaPremultiplied);
    }

    public static ImageTypeSpecifier createInterleaved(final ColorSpace colorSpace,
                                                       final int[] bandOffsets,
                                                       final int dataType,
                                                       final boolean hasAlpha,
                                                       final boolean isAlphaPremultiplied) {
        // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
        if (dataType == DataBuffer.TYPE_INT) {
            return new UInt32ImageTypeSpecifier(colorSpace, bandOffsets, hasAlpha, isAlphaPremultiplied);
        }

        // ...or fall back to default for anything else
        return ImageTypeSpecifier.createInterleaved(colorSpace, bandOffsets, dataType, hasAlpha, isAlphaPremultiplied);
    }

    public static ImageTypeSpecifier createBanded(final ColorSpace colorSpace,
                                                  final int[] bankIndices, final int[] bandOffsets,
                                                  final int dataType,
                                                  final boolean hasAlpha, final boolean isAlphaPremultiplied) {
        return ImageTypeSpecifier.createBanded(colorSpace, bankIndices, bandOffsets, dataType, hasAlpha, isAlphaPremultiplied);
    }

    public static ImageTypeSpecifier createGrayscale(final int bits, final int dataType) {
        if (bits == 32 && dataType == DataBuffer.TYPE_INT) {
            // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
            return new UInt32ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0}, false, false);
        }

        // NOTE: The isSigned boolean is stored but *not used for anything* in the Grayscale ImageTypeSpecifier...
        return ImageTypeSpecifier.createGrayscale(bits, dataType, false);
    }

    public static ImageTypeSpecifier createGrayscale(final int bits, final int dataType, final boolean isAlphaPremultiplied) {
        if (bits == 32 && dataType == DataBuffer.TYPE_INT) {
            // As the ComponentColorModel is broken for 32 bit unsigned int, we'll use our own version
            return new UInt32ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0, 1}, true, isAlphaPremultiplied);
        }

        // NOTE: The isSigned boolean is stored but *not used for anything* in the Grayscale ImageTypeSpecifier...
        return ImageTypeSpecifier.createGrayscale(bits, dataType, false, isAlphaPremultiplied);
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

    public static ImageTypeSpecifier createFromIndexColorModel(final IndexColorModel pColorModel) {
        return new IndexedImageTypeSpecifier(pColorModel);
    }
}
