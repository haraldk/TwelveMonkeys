package com.twelvemonkeys.imageio.color;

import org.junit.Test;

import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;

import static org.junit.Assert.assertEquals;

public class UInt32ColorModelTest {

    private static final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    private static final ColorSpace GRAY = ColorSpace.getInstance(ColorSpace.CS_GRAY);

    @Test
    public void testGetNormalizedComponentsRGBBlack() {
        ComponentColorModel model = new UInt32ColorModel(sRGB, true, false);
        float[] normalized = model.getNormalizedComponents(new int[]{0, 0, 0, 0}, null, 0);
        for (float norm : normalized) {
            assertEquals(0, norm, 0);
        }
    }

    @Test
    public void testGetNormalizedComponentsRGBGray() {
        ComponentColorModel model = new UInt32ColorModel(sRGB, true, false);
        float[] normalized = model.getNormalizedComponents(new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}, null, 0);
        for (float norm : normalized) {
            assertEquals(0.5f, norm, 0);
        }
    }

    @Test
    public void testGetNormalizedComponentsRGBWhite() {
        ComponentColorModel model = new UInt32ColorModel(sRGB, true, false);
        float[] normalized = model.getNormalizedComponents(new int[]{-1, -1, -1, -1}, null, 0);
        for (float norm : normalized) {
            assertEquals(1, norm, 0);
        }
    }

    @Test
    public void testGetNormalizedComponentsRGB() {
        ComponentColorModel model = new UInt32ColorModel(sRGB, true, false);
        int[] pixel = new int[4];

        float[] normalized = null;
        for (long pix = 0; pix < 1l << 32; pix += Short.MAX_VALUE) {
            float expected = ((float) (pix & 0xffffffffl)) / ((float) ((1l << 32) - 1));

            for (int i = 0; i < pixel.length; i++) {
                pixel[i] = (int) pix;
            }

            normalized = model.getNormalizedComponents(pixel, normalized, 0);

            for (float norm : normalized) {
                assertEquals(expected, norm, 0);
            }
        }
    }

    @Test
    public void testGetNormalizedComponentsGrayBlack() {
        ComponentColorModel model = new UInt32ColorModel(GRAY, false, false);
        float[] normalized = model.getNormalizedComponents(new int[]{0}, null, 0);
        for (float norm : normalized) {
            assertEquals(0, norm, 0);
        }
    }

    @Test
    public void testGetNormalizedComponentsGrayGray() {
        ComponentColorModel model = new UInt32ColorModel(GRAY, false, false);
        float[] normalized = model.getNormalizedComponents(new int[]{Integer.MIN_VALUE}, null, 0);
        for (float norm : normalized) {
            assertEquals(0.5f, norm, 0);
        }
    }

    @Test
    public void testGetNormalizedComponentsGrayWhite() {
        ComponentColorModel model = new UInt32ColorModel(GRAY, false, false);
        float[] normalized = model.getNormalizedComponents(new int[]{-1}, null, 0);
        for (float norm : normalized) {
            assertEquals(1, norm, 0);
        }
    }

    @Test
    public void testGetNormalizedComponentsGray() {
        ComponentColorModel model = new UInt32ColorModel(GRAY, false, false);
        int[] pixel = new int[1];

        float[] normalized = null;
        for (long pix = 0; pix < 1l << 32; pix += Short.MAX_VALUE) {
            float expected = ((float) (pix & 0xffffffffl)) / ((float) ((1l << 32) - 1));

            pixel[0] = (int) pix;

            normalized = model.getNormalizedComponents(pixel, normalized, 0);

            assertEquals(expected, normalized[0], 0);
        }
    }
}
