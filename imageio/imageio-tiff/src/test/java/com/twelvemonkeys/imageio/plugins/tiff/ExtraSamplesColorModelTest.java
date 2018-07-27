package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.image.ResampleOp;
import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;

import static org.junit.Assert.assertEquals;

public class ExtraSamplesColorModelTest {

    private ImageTypeSpecifier createImageTypeSpecifier() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int samplesPerPixel = 5;
        int significantSamples = cs.getNumComponents() + 1;

        return new ImageTypeSpecifier(
                new ExtraSamplesColorModel(cs, true, DataBuffer.TYPE_BYTE, samplesPerPixel - significantSamples),
                new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, 1, 1, samplesPerPixel, samplesPerPixel, createOffsets(samplesPerPixel))
        );
    }

    private static int[] createOffsets(int samplesPerPixel) {
        int[] offsets = new int[samplesPerPixel];
        for (int i = 0; i < samplesPerPixel; i++) {
            offsets[i] = i;
        }
        return offsets;
    }

    @Test
    public void testImageWithExtraSamplesCanBeResampled() {
        ImageTypeSpecifier imageTypeSpecifier = createImageTypeSpecifier();
        BufferedImage bufferedImage = imageTypeSpecifier.createBufferedImage(100, 100);
        BufferedImage resampled = new ResampleOp(50, 50, ResampleOp.FILTER_LANCZOS).filter(bufferedImage, null);

        assertEquals(50, resampled.getWidth());
        assertEquals(50, resampled.getHeight());
    }
}
