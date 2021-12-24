/*
 * Copyright (c) 2018, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.image.ResampleOp;
import com.twelvemonkeys.imageio.color.ColorSpaces;

import org.junit.Test;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExtraSamplesColorModelTest {

    private BufferedImage createExtraSamplesImage(int w, int h, ColorSpace cs, boolean hasAlpha, int extraComponents) {
        int samplesPerPixel = cs.getNumComponents() + (hasAlpha ? 1 : 0) + extraComponents;

        ExtraSamplesColorModel colorModel = new ExtraSamplesColorModel(cs, hasAlpha, true, DataBuffer.TYPE_BYTE, extraComponents);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, h, samplesPerPixel, samplesPerPixel * w, createOffsets(samplesPerPixel));

        WritableRaster raster = Raster.createWritableRaster(sampleModel, new Point(0, 0));

        return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), new Hashtable());
    }

    private static int[] createOffsets(int samplesPerPixel) {
        int[] offsets = new int[samplesPerPixel];
        for (int i = 0; i < samplesPerPixel; i++) {
            offsets[i] = i;
        }
        return offsets;
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampledGray() {
        for (int i = 1; i < 8; i++) {
            BufferedImage bufferedImage = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpace.CS_GRAY), false, i);
            BufferedImage resampled = new ResampleOp(5, 5, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

            assertNotNull(resampled);
            assertEquals(5, resampled.getWidth());
            assertEquals(5, resampled.getHeight());
        }
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampledGrayAlpha() {
        for (int i = 1; i < 8; i++) {
            BufferedImage bufferedImage = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpace.CS_GRAY), true, i);
            BufferedImage resampled = new ResampleOp(5, 5, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

            assertNotNull(resampled);
            assertEquals(5, resampled.getWidth());
            assertEquals(5, resampled.getHeight());
        }
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampledRGB() {
        for (int i = 1; i < 8; i++) {
            BufferedImage bufferedImage = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpace.CS_sRGB), false, i);
            BufferedImage resampled = new ResampleOp(5, 5, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

            assertNotNull(resampled);
            assertEquals(5, resampled.getWidth());
            assertEquals(5, resampled.getHeight());
        }
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampledRGBAlpha() {
        for (int i = 1; i < 8; i++) {
            BufferedImage bufferedImage = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpace.CS_sRGB), true, i);
            BufferedImage resampled = new ResampleOp(5, 5, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

            assertNotNull(resampled);
            assertEquals(5, resampled.getWidth());
            assertEquals(5, resampled.getHeight());
        }
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampledCMYK() {
        for (int i = 1; i < 8; i++) {
            BufferedImage bufferedImage = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), false, i);
            BufferedImage resampled = new ResampleOp(5, 5, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

            assertNotNull(resampled);
            assertEquals(5, resampled.getWidth());
            assertEquals(5, resampled.getHeight());
        }
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampledCMYKAlpha() {
        for (int i = 1; i < 8; i++) {
            BufferedImage bufferedImage = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), true, i);
            BufferedImage resampled = new ResampleOp(5, 5, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

            assertNotNull(resampled);
            assertEquals(5, resampled.getWidth());
            assertEquals(5, resampled.getHeight());
        }
    }

    @Test
    public void testSetRGB() {
        BufferedImage image = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpace.CS_sRGB), false, 1);

        image.setRGB(0, 0, Color.BLACK.getRGB());
        assertEquals(Color.BLACK.getRGB(), image.getRGB(0, 0));
    }

    @Test
    public void testSetRGBs() {
        BufferedImage image = createExtraSamplesImage(10, 10, ColorSpaces.getColorSpace(ColorSpace.CS_sRGB), false, 1);
        image.setRGB(0, 0, 2, 1, new int[]{Color.BLACK.getRGB(), Color.WHITE.getRGB()}, 0, 2);
        assertEquals(Color.BLACK.getRGB(), image.getRGB(0, 0));
        assertEquals(Color.WHITE.getRGB(), image.getRGB(1, 0));
    }
}