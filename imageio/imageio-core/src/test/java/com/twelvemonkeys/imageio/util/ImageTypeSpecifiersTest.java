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

import com.twelvemonkeys.lang.Validate;
import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.*;

import static org.junit.Assert.assertEquals;

public class ImageTypeSpecifiersTest {

    private static final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    private static final ColorSpace GRAY = ColorSpace.getInstance(ColorSpace.CS_GRAY);

    private static final int DCM_RED_MASK = 0x00ff0000;
    private static final int DCM_GREEN_MASK = 0x0000ff00;
    private static final int DCM_BLUE_MASK = 0x000000ff;
    private static final int DCM_ALPHA_MASK = 0xff000000;
    private static final int DCM_565_RED_MASK = 0xf800;
    private static final int DCM_565_GRN_MASK = 0x07E0;
    private static final int DCM_565_BLU_MASK = 0x001F;
    private static final int DCM_555_RED_MASK = 0x7C00;
    private static final int DCM_555_GRN_MASK = 0x03E0;
    private static final int DCM_555_BLU_MASK = 0x001F;
    private static final int DCM_BGR_RED_MASK = 0x0000ff;
    private static final int DCM_BGR_GRN_MASK = 0x00ff00;
    private static final int DCM_BGR_BLU_MASK = 0xff0000;

    @Test
    public void testCreateFromBufferedImageType() {
        for (int type = BufferedImage.TYPE_INT_RGB; type < BufferedImage.TYPE_BYTE_INDEXED; type++) {
            assertEquals(
                    ImageTypeSpecifier.createFromBufferedImageType(type),
                    ImageTypeSpecifiers.createFromBufferedImageType(type)
            );
        }
    }

    @Test
    public void testCreatePacked32() {
        // TYPE_INT_RGB
        assertEquals(
                ImageTypeSpecifier.createPacked(sRGB, DCM_RED_MASK, DCM_GREEN_MASK, DCM_BLUE_MASK, 0, DataBuffer.TYPE_INT, false),
                ImageTypeSpecifiers.createPacked(sRGB, DCM_RED_MASK, DCM_GREEN_MASK, DCM_BLUE_MASK, 0, DataBuffer.TYPE_INT, false)
        );
        // TYPE_INT_ARGB
        assertEquals(
                ImageTypeSpecifier.createPacked(sRGB, DCM_RED_MASK, DCM_GREEN_MASK, DCM_BLUE_MASK, DCM_ALPHA_MASK, DataBuffer.TYPE_INT, false),
                ImageTypeSpecifiers.createPacked(sRGB, DCM_RED_MASK, DCM_GREEN_MASK, DCM_BLUE_MASK, DCM_ALPHA_MASK, DataBuffer.TYPE_INT, false)
        );
        // TYPE_INT_ARGB_PRE
        assertEquals(
                ImageTypeSpecifier.createPacked(sRGB, DCM_RED_MASK, DCM_GREEN_MASK, DCM_BLUE_MASK, DCM_ALPHA_MASK, DataBuffer.TYPE_INT, true),
                ImageTypeSpecifiers.createPacked(sRGB, DCM_RED_MASK, DCM_GREEN_MASK, DCM_BLUE_MASK, DCM_ALPHA_MASK, DataBuffer.TYPE_INT, true)
        );
        // TYPE_INT_BGR
        assertEquals(
                ImageTypeSpecifier.createPacked(sRGB, DCM_BGR_RED_MASK, DCM_BGR_GRN_MASK, DCM_BGR_BLU_MASK, 0, DataBuffer.TYPE_INT, false),
                ImageTypeSpecifiers.createPacked(sRGB, DCM_BGR_RED_MASK, DCM_BGR_GRN_MASK, DCM_BGR_BLU_MASK, 0, DataBuffer.TYPE_INT, false)
        );
    }

    @Test
    public void testCreatePacked16() {
        // TYPE_USHORT_555_RGB
        assertEquals(
                createPacked(sRGB, DCM_555_RED_MASK, DCM_555_GRN_MASK, DCM_555_BLU_MASK, 0, DataBuffer.TYPE_USHORT, false),
                ImageTypeSpecifiers.createPacked(sRGB, DCM_555_RED_MASK, DCM_555_GRN_MASK, DCM_555_BLU_MASK, 0, DataBuffer.TYPE_USHORT, false)
        );
        // "SHORT 555 RGB" (impossible, only BYTE, USHORT, INT supported)

        // TYPE_USHORT_565_RGB
        assertEquals(
                createPacked(sRGB, DCM_565_RED_MASK, DCM_565_GRN_MASK, DCM_565_BLU_MASK, 0, DataBuffer.TYPE_USHORT, false),
                ImageTypeSpecifiers.createPacked(sRGB, DCM_565_RED_MASK, DCM_565_GRN_MASK, DCM_565_BLU_MASK, 0, DataBuffer.TYPE_USHORT, false)
        );
        // "USHORT 4444 ARGB"
        assertEquals(
                createPacked(sRGB, 0xf00, 0xf0, 0xf, 0xf000, DataBuffer.TYPE_USHORT, false),
                ImageTypeSpecifiers.createPacked(sRGB, 0xf00, 0xf0, 0xf, 0xf000, DataBuffer.TYPE_USHORT, false)
        );
        // "USHORT 4444 ARGB PRE"
        assertEquals(
                createPacked(sRGB, 0xf00, 0xf0, 0xf, 0xf000, DataBuffer.TYPE_USHORT, true),
                ImageTypeSpecifiers.createPacked(sRGB, 0xf00, 0xf0, 0xf, 0xf000, DataBuffer.TYPE_USHORT, true)
        );

        // Extra: Make sure color models bits is actually 16 (ImageTypeSpecifier equivalent returns 32)
        assertEquals(16, ImageTypeSpecifiers.createPacked(sRGB, DCM_565_RED_MASK, DCM_565_GRN_MASK, DCM_565_BLU_MASK, 0, DataBuffer.TYPE_USHORT, false).getColorModel().getPixelSize());
   }

    @Test
    public void testCreatePacked8() {
        // "BYTE 332 RGB"
        assertEquals(
                createPacked(sRGB, 0xe0, 0x1c, 0x03, 0x0, DataBuffer.TYPE_BYTE, false),
                ImageTypeSpecifiers.createPacked(sRGB, 0xe0, 0x1c, 0x3, 0x0, DataBuffer.TYPE_BYTE, false)
        );
        // "BYTE 2222 ARGB"
        assertEquals(
                createPacked(sRGB, 0xc0, 0x30, 0x0c, 0x03, DataBuffer.TYPE_BYTE, false),
                ImageTypeSpecifiers.createPacked(sRGB, 0xc0, 0x30, 0x0c, 0x03, DataBuffer.TYPE_BYTE, false)
        );
        // "BYTE 2222 ARGB PRE"
        assertEquals(
                createPacked(sRGB, 0xc0, 0x30, 0x0c, 0x03, DataBuffer.TYPE_BYTE, true),
                ImageTypeSpecifiers.createPacked(sRGB, 0xc0, 0x30, 0x0c, 0x03, DataBuffer.TYPE_BYTE, true)
        );

        // Extra: Make sure color models bits is actually 8 (ImageTypeSpecifiers equivalent returns 32)
        assertEquals(8, ImageTypeSpecifiers.createPacked(sRGB, 0xc0, 0x30, 0x0c, 0x03, DataBuffer.TYPE_BYTE, false).getColorModel().getPixelSize());
    }

    private ImageTypeSpecifier createPacked(final ColorSpace colorSpace,
                                            final int redMask, final int greenMask, final int blueMask, final int alphaMask,
                                            final int transferType, final boolean isAlphaPremultiplied) {
        Validate.isTrue(transferType == DataBuffer.TYPE_BYTE || transferType == DataBuffer.TYPE_USHORT, transferType, "transferType: %s");

        int bits = transferType == DataBuffer.TYPE_BYTE ? 8 : 16;

        ColorModel colorModel =
                new DirectColorModel(colorSpace, bits, redMask, greenMask, blueMask, alphaMask, isAlphaPremultiplied, transferType);

        return new ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(1, 1));
    }

    @Test
    public void testCreateInterleaved8() {
        // 8 bits/sample
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_BYTE, false, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_BYTE, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_BYTE, true, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_BYTE, true, false)
        );

        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, true),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, true)
        );
    }

    @Test
    public void testCreateInterleaved16() {
        // 16 bits/sample
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_USHORT, false, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_USHORT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_USHORT, true, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_USHORT, true, false)
        );

        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, true, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, true, true),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, true, true)
        );
    }

    @Test
    public void testCreateInterleaved32() {
        // 32 bits/sample
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, false, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_INT, false, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, true, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_INT, true, false)
        );

        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2}, false, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_INT, false, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, true, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_INT, true, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, true, true),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_INT, true, true)
        );
    }

    @Test
    public void testCreateInterleaved32fp() {
        // 32 bits/sample
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_FLOAT, false, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_FLOAT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_FLOAT, true, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_FLOAT, true, false)
        );

        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_FLOAT, false, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_FLOAT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_FLOAT, true, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_FLOAT, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_FLOAT, true, true),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_FLOAT, true, true)
        );
    }

    @Test
    public void testCreateInterleaved64fp() {
        // 64 bits/sample
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_DOUBLE, false, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_DOUBLE, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_DOUBLE, true, false),
                ImageTypeSpecifiers.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_DOUBLE, true, false)
        );

        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_DOUBLE, false, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_DOUBLE, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_DOUBLE, true, false),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_DOUBLE, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_DOUBLE, true, true),
                ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_DOUBLE, true, true)
        );
    }

    @Test
    public void testCreateBanded8() {
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_BYTE, false, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_BYTE, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_BYTE, true, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_BYTE, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_BYTE, true, true),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_BYTE, true, true)
        );
    }

    @Test
    public void testCreateBanded16() {
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_USHORT, false, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_USHORT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_USHORT, true, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_USHORT, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_USHORT, true, true),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_USHORT, true, true)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_SHORT, false, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_SHORT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_SHORT, true, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_SHORT, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_SHORT, true, true),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_SHORT, true, true)
        );
    }

    @Test
    public void testCreateBanded32() {
        assertEquals(
                UInt32ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, false, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_INT, false, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, true, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_INT, true, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, true, true),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_INT, true, true)
        );
    }

    @Test
    public void testCreateBanded32fp() {
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_FLOAT, false, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_FLOAT, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_FLOAT, true, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_FLOAT, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_FLOAT, true, true),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_FLOAT, true, true)
        );
    }

    @Test
    public void testCreateBanded64fp() {
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_DOUBLE, false, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_DOUBLE, false, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_DOUBLE, true, false),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_DOUBLE, true, false)
        );
        assertEquals(
                ImageTypeSpecifier.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_DOUBLE, true, true),
                ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[] {0, 1000, 2000, 3000}, DataBuffer.TYPE_DOUBLE, true, true)
        );
    }

    @Test
    public void testCreateGrayscale1to8() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            assertEquals(
                    ImageTypeSpecifier.createGrayscale(bits, DataBuffer.TYPE_BYTE, false),
                    ImageTypeSpecifiers.createGrayscale(bits, DataBuffer.TYPE_BYTE)
            );
            assertEquals(
                    ImageTypeSpecifier.createGrayscale(bits, DataBuffer.TYPE_BYTE, true),
                    ImageTypeSpecifiers.createGrayscale(bits, DataBuffer.TYPE_BYTE)
            );
        }

    }

    @Test
    public void testCreateGrayscale16() {
        assertEquals(
                ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, false),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_USHORT)
        );
        assertEquals(
                ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, true), // NOTE: Signed TYPE_USHORT makes no sense...
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_USHORT)
        );

        assertEquals(
                new Int16ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0}, false, false),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_SHORT)
        );
    }

    @Test
    public void testCreateGrayscale32() {
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, false, false),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_INT)
        );
    }

    @Test
    public void testCreateGrayscaleFloat() {
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_FLOAT, false, false),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_FLOAT)
        );
    }

    @Test
    public void testCreateGrayscaleDouble() {
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0}, DataBuffer.TYPE_DOUBLE, false, false),
                ImageTypeSpecifiers.createGrayscale(64, DataBuffer.TYPE_DOUBLE)
        );
    }

    @Test
    public void testCreateGrayscaleAlpha1to8() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            assertEquals(
                    ImageTypeSpecifier.createGrayscale(bits, DataBuffer.TYPE_BYTE, false, false),
                    ImageTypeSpecifiers.createGrayscale(bits, DataBuffer.TYPE_BYTE, false)
            );
            assertEquals(
                    ImageTypeSpecifier.createGrayscale(bits, DataBuffer.TYPE_BYTE, false, true),
                    ImageTypeSpecifiers.createGrayscale(bits, DataBuffer.TYPE_BYTE, true)
            );
            assertEquals(
                    ImageTypeSpecifier.createGrayscale(bits, DataBuffer.TYPE_BYTE, true, false),
                    ImageTypeSpecifiers.createGrayscale(bits, DataBuffer.TYPE_BYTE, false)
            );
            assertEquals(
                    ImageTypeSpecifier.createGrayscale(bits, DataBuffer.TYPE_BYTE, true, true),
                    ImageTypeSpecifiers.createGrayscale(bits, DataBuffer.TYPE_BYTE, true)
            );
        }
    }

    @Test
    public void testCreateGrayscaleAlpha16() {
        assertEquals(
                ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, false, false),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_USHORT, false)
        );
        assertEquals(
                ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, false, true),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_USHORT, true)
        );
        assertEquals(
                ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, true, false),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_USHORT, false)
        );
        assertEquals(
                ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, true, true),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_USHORT, true)
        );

        assertEquals(
                new Int16ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0, 1}, true, false),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_SHORT, false)
        );
        assertEquals(
                new Int16ImageTypeSpecifier(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {0, 1}, true, true),
                ImageTypeSpecifiers.createGrayscale(16, DataBuffer.TYPE_SHORT, true)
        );
    }

    @Test
    public void testCreateGrayscaleAlpha32() {
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, true, false),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_INT, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, true, false),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_INT, false)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, true, true),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_INT, true)
        );
        assertEquals(
                UInt32ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, true, true),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_INT, true)
        );
    }

    @Test
    public void testCreateGrayscaleAlphaFloat() {
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_FLOAT, true, false),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_FLOAT, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_FLOAT, true, true),
                ImageTypeSpecifiers.createGrayscale(32, DataBuffer.TYPE_FLOAT, true)
        );
    }

    @Test
    public void testCreateGrayscaleAlphaDouble() {
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_DOUBLE, true, false),
                ImageTypeSpecifiers.createGrayscale(64, DataBuffer.TYPE_DOUBLE, false)
        );
        assertEquals(
                ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_DOUBLE, true, true),
                ImageTypeSpecifiers.createGrayscale(64, DataBuffer.TYPE_DOUBLE, true)
        );
    }

    @Test
    public void testCreatePackedGrayscale1() {
        assertEquals(
                ImageTypeSpecifier.createGrayscale(1, DataBuffer.TYPE_BYTE, false),
                ImageTypeSpecifiers.createPackedGrayscale(GRAY, 1, DataBuffer.TYPE_BYTE)
        );
    }

    @Test
    public void testCreatePackedGrayscale2() {
        assertEquals(
                ImageTypeSpecifier.createGrayscale(2, DataBuffer.TYPE_BYTE, false),
                ImageTypeSpecifiers.createPackedGrayscale(GRAY, 2, DataBuffer.TYPE_BYTE)
        );
    }

    @Test
    public void testCreatePackedGrayscale4() {
        assertEquals(
                ImageTypeSpecifier.createGrayscale(4, DataBuffer.TYPE_BYTE, false),
                ImageTypeSpecifiers.createPackedGrayscale(GRAY, 4, DataBuffer.TYPE_BYTE)
        );
    }

    @Test
    public void testCreateIndexedByteArrays1to8() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            byte[] lut = createByteLut(1 << bits);

            assertEquals(
                    ImageTypeSpecifier.createIndexed(lut, lut, lut, null, bits, DataBuffer.TYPE_BYTE),
                    ImageTypeSpecifiers.createIndexed(lut, lut, lut, null, bits, DataBuffer.TYPE_BYTE)
            );
            assertEquals(
                    ImageTypeSpecifier.createIndexed(lut, lut, lut, lut, bits, DataBuffer.TYPE_BYTE),
                    ImageTypeSpecifiers.createIndexed(lut, lut, lut, lut, bits, DataBuffer.TYPE_BYTE)
            );
        }
    }

    @Test
    public void testCreateIndexedByteArrays16() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            byte[] lut = createByteLut(1 << bits);

            assertEquals(
                    ImageTypeSpecifier.createIndexed(lut, lut, lut, null, bits, DataBuffer.TYPE_USHORT),
                    ImageTypeSpecifiers.createIndexed(lut, lut, lut, null, bits, DataBuffer.TYPE_USHORT)
            );
            assertEquals(
                    ImageTypeSpecifier.createIndexed(lut, lut, lut, lut, bits, DataBuffer.TYPE_USHORT),
                    ImageTypeSpecifiers.createIndexed(lut, lut, lut, lut, bits, DataBuffer.TYPE_USHORT)
            );

            // TYPE_SHORT is unsupported to MultiPixelPacked format (MultiPixelPackedSampleModel)
        }

        byte[] lut = createByteLut(1 << 16); // This is stupid, but ImageTypeSpecifier enforces lut.length == 1 << bits

        assertEquals(
                ImageTypeSpecifier.createIndexed(lut, lut, lut, null, 16, DataBuffer.TYPE_USHORT),
                ImageTypeSpecifiers.createIndexed(lut, lut, lut, null, 16, DataBuffer.TYPE_USHORT)
        );
        assertEquals(
                ImageTypeSpecifier.createIndexed(lut, lut, lut, lut, 16, DataBuffer.TYPE_USHORT),
                ImageTypeSpecifiers.createIndexed(lut, lut, lut, lut, 16, DataBuffer.TYPE_USHORT)
        );

        assertEquals(
                ImageTypeSpecifier.createIndexed(lut, lut, lut, null, 16, DataBuffer.TYPE_SHORT),
                ImageTypeSpecifiers.createIndexed(lut, lut, lut, null, 16, DataBuffer.TYPE_SHORT)
        );
        assertEquals(
                ImageTypeSpecifier.createIndexed(lut, lut, lut, lut, 16, DataBuffer.TYPE_SHORT),
                ImageTypeSpecifiers.createIndexed(lut, lut, lut, lut, 16, DataBuffer.TYPE_SHORT)
        );
    }

    @Test
    public void testCreateIndexedByteArrays32() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            byte[] lut = createByteLut(1 << bits);

            assertEquals(
                    ImageTypeSpecifier.createIndexed(lut, lut, lut, null, bits, DataBuffer.TYPE_INT),
                    ImageTypeSpecifiers.createIndexed(lut, lut, lut, null, bits, DataBuffer.TYPE_INT)
            );
            assertEquals(
                    ImageTypeSpecifier.createIndexed(lut, lut, lut, lut, bits, DataBuffer.TYPE_INT),
                    ImageTypeSpecifiers.createIndexed(lut, lut, lut, lut, bits, DataBuffer.TYPE_INT)
            );
        }

        byte[] lut = createByteLut(1 << 16); // This is stupid, but ImageTypeSpecifier enforces lut.length == 1 << bits

        assertEquals(
                ImageTypeSpecifier.createIndexed(lut, lut, lut, null, 16, DataBuffer.TYPE_INT),
                ImageTypeSpecifiers.createIndexed(lut, lut, lut, null, 16, DataBuffer.TYPE_INT)
        );
        assertEquals(
                ImageTypeSpecifier.createIndexed(lut, lut, lut, lut, 16, DataBuffer.TYPE_INT),
                ImageTypeSpecifiers.createIndexed(lut, lut, lut, lut, 16, DataBuffer.TYPE_INT)
        );
    }

    @Test
    public void testCreateIndexedIntArray1to8() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            int[] colors = createIntLut(1 << bits);
            assertEquals(
                    IndexedImageTypeSpecifier.createFromIndexColorModel(new IndexColorModel(bits, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE)),
                    ImageTypeSpecifiers.createIndexed(colors, false, -1, bits, DataBuffer.TYPE_BYTE)
            );
        }
    }

    @Test
    public void testCreateIndexedIntArray16() {
        int[] colors = createIntLut(1 << 16);
        assertEquals(
                IndexedImageTypeSpecifier.createFromIndexColorModel(new IndexColorModel(16, colors.length, colors, 0, false, -1, DataBuffer.TYPE_USHORT)),
                ImageTypeSpecifiers.createIndexed(colors, false, -1, 16, DataBuffer.TYPE_USHORT)
        );

    }

    @Test
    public void testCreateFromIndexedColorModel1to8() {
        for (int bits = 1; bits <= 8; bits <<= 1) {
            int[] colors = createIntLut(1 << bits);
            IndexColorModel colorModel = new IndexColorModel(bits, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);
            assertEquals(
                    IndexedImageTypeSpecifier.createFromIndexColorModel(colorModel),
                    ImageTypeSpecifiers.createFromIndexColorModel(colorModel)
            );
        }
    }

    @Test
    public void testCreateFromIndexedColorModel16() {
        int[] colors = createIntLut(1 << 16);
        IndexColorModel colorModel = new IndexColorModel(16, colors.length, colors, 0, false, -1, DataBuffer.TYPE_USHORT);
        assertEquals(
                IndexedImageTypeSpecifier.createFromIndexColorModel(colorModel),
                ImageTypeSpecifiers.createFromIndexColorModel(colorModel)
        );
    }

    @Test
    public void testCreateDiscreteAlphaIndexedFromIndexColorModel8() {
        int[] colors = createIntLut(1 << 8);
        IndexColorModel colorModel = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);
        assertEquals(
                new ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(1, 1)),
                ImageTypeSpecifiers.createFromIndexColorModel(colorModel)
        );
    }

    @Test
    public void testCreateDiscreteAlphaIndexedFromIndexColorModel16() {
        int[] colors = createIntLut(1 << 16);
        IndexColorModel colorModel = new IndexColorModel(16, colors.length, colors, 0, false, -1, DataBuffer.TYPE_USHORT);
        assertEquals(
                new ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(1, 1)),
                ImageTypeSpecifiers.createFromIndexColorModel(colorModel)
        );
    }

    private static byte[] createByteLut(final int count) {
        byte[] lut = new byte[count];
        for (int i = 0; i < count; i++) {
            lut[i] = (byte) count;
        }
        return lut;
    }

    private static int[] createIntLut(final int count) {
        int[] lut = new int[count];

        for (int i = 0; i < count; i++) {
            lut[i] = 0xff000000 | count << 16 | count << 8 | count;
        }

        return lut;
    }
}
