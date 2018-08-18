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
}