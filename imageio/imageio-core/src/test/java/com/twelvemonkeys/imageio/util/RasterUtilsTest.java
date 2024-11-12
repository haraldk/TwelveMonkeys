package com.twelvemonkeys.imageio.util;

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RasterUtilsTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RasterUtilsTest.java,v 1.0 05/05/2021 haraldk Exp$
 */
public class RasterUtilsTest {
    @Test
    public void testAsByteRasterFromNull() {
        assertThrows(NullPointerException.class, () -> RasterUtils.asByteRaster((Raster) null));
    }

    @SuppressWarnings("RedundantCast")
    @Test
    public void testAsByteRasterWritableFromNull() {
        assertThrows(NullPointerException.class, () -> RasterUtils.asByteRaster((WritableRaster) null));
    }

    @Test
    public void testAsByteRasterPassThrough() {
        WritableRaster[] rasters = new WritableRaster[] {
                new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR).getRaster(),
                new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR).getRaster(),
                new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR_PRE).getRaster(),
                new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY).getRaster(),
                Raster.createBandedRaster(DataBuffer.TYPE_BYTE, 1, 1, 7, null),
                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 2, null),
                new WritableRaster(new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, 1, 1, 1, 1, new int[1]), new Point(0, 0)) {}
        };

        for (Raster raster : rasters) {
            assertSame(raster, RasterUtils.asByteRaster(raster));
        }

        for (WritableRaster raster : rasters) {
            assertSame(raster, RasterUtils.asByteRaster(raster));
        }
    }

    @Test
    public void testAsByteRasterWritableFromTYPE_INT_RGB() {
        BufferedImage image = new BufferedImage(9, 11, BufferedImage.TYPE_INT_RGB);

        WritableRaster raster = RasterUtils.asByteRaster(image.getRaster());

        assertEquals(DataBuffer.TYPE_BYTE, raster.getTransferType());
        assertEquals(PixelInterleavedSampleModel.class, raster.getSampleModel().getClass());
        assertEquals(image.getWidth(), raster.getWidth());
        assertEquals(image.getHeight(), raster.getHeight());

        assertEquals(3, raster.getNumBands());
        assertEquals(3, raster.getNumDataElements());

        assertImageRasterEquals(image, raster);
    }

    @Test
    public void testAsByteRasterWritableFromTYPE_INT_ARGB() {
        BufferedImage image = new BufferedImage(9, 11, BufferedImage.TYPE_INT_ARGB);

        WritableRaster raster = RasterUtils.asByteRaster(image.getRaster());

        assertEquals(DataBuffer.TYPE_BYTE, raster.getTransferType());
        assertEquals(PixelInterleavedSampleModel.class, raster.getSampleModel().getClass());
        assertEquals(image.getWidth(), raster.getWidth());
        assertEquals(image.getHeight(), raster.getHeight());

        assertEquals(4, raster.getNumBands());
        assertEquals(4, raster.getNumDataElements());

        assertImageRasterEquals(image, raster);
    }

    @Test
    public void testAsByteRasterWritableFromTYPE_INT_ARGB_PRE() {
        BufferedImage image = new BufferedImage(9, 11, BufferedImage.TYPE_INT_ARGB_PRE);

        WritableRaster raster = RasterUtils.asByteRaster(image.getRaster());

        assertEquals(DataBuffer.TYPE_BYTE, raster.getTransferType());
        assertEquals(PixelInterleavedSampleModel.class, raster.getSampleModel().getClass());
        assertEquals(image.getWidth(), raster.getWidth());
        assertEquals(image.getHeight(), raster.getHeight());

        assertEquals(4, raster.getNumBands());
        assertEquals(4, raster.getNumDataElements());

        // We don't assert on values here, as the premultiplied values makes it hard...
    }

    @Test
    public void testAsByteRasterWritableFromTYPE_INT_BGR() {
        BufferedImage image = new BufferedImage(9, 11, BufferedImage.TYPE_INT_BGR);

        WritableRaster raster = RasterUtils.asByteRaster(image.getRaster());

        assertEquals(DataBuffer.TYPE_BYTE, raster.getTransferType());
        assertEquals(PixelInterleavedSampleModel.class, raster.getSampleModel().getClass());
        assertEquals(image.getWidth(), raster.getWidth());
        assertEquals(image.getHeight(), raster.getHeight());

        assertEquals(3, raster.getNumBands());
        assertEquals(3, raster.getNumDataElements());

        assertImageRasterEquals(image, raster);
    }

    @Test
    public void testAsByteRasterWritableFromTYPE_CUSTOM_GRAB() {
        BufferedImage image = ImageTypeSpecifier.createPacked(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                0x00FF0000,
                0xFF000000,
                0x000000FF,
                0x0000FF00,
                DataBuffer.TYPE_INT, false).createBufferedImage(7, 13);

        WritableRaster raster = RasterUtils.asByteRaster(image.getRaster());

        assertEquals(DataBuffer.TYPE_BYTE, raster.getTransferType());
        assertEquals(PixelInterleavedSampleModel.class, raster.getSampleModel().getClass());
        assertEquals(image.getWidth(), raster.getWidth());
        assertEquals(image.getHeight(), raster.getHeight());

        assertEquals(4, raster.getNumBands());
        assertEquals(4, raster.getNumDataElements());

        assertImageRasterEquals(image, raster);
    }

    @Test
    public void testAsByteRasterWritableFromTYPE_CUSTOM_BxRG() {
        BufferedImage image = ImageTypeSpecifier.createPacked(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                0x0000FF00,
                0x000000FF,
                0xFF000000,
                0,
                DataBuffer.TYPE_INT, false).createBufferedImage(7, 13);

        WritableRaster raster = RasterUtils.asByteRaster(image.getRaster());

        assertEquals(DataBuffer.TYPE_BYTE, raster.getTransferType());
        assertEquals(PixelInterleavedSampleModel.class, raster.getSampleModel().getClass());
        assertEquals(image.getWidth(), raster.getWidth());
        assertEquals(image.getHeight(), raster.getHeight());

        assertEquals(3, raster.getNumBands());
        assertEquals(3, raster.getNumDataElements());

        assertImageRasterEquals(image, raster);
    }

    private static void assertImageRasterEquals(BufferedImage image, WritableRaster raster) {
        // NOTE: This is NOT necessarily how the values are stored in the data buffer
        int[] argbOffs = new int[] {16, 8, 0, 24};

        Raster imageRaster = image.getRaster();

        Random rng = new Random(27365481723L);

        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                int argb = 0;

                for (int b = 0; b < raster.getNumBands(); b++) {
                    int s = rng.nextInt(0xFF);
                    raster.setSample(x, y, b, s);

                    assertEquals(s, raster.getSample(x, y, b));
                    assertEquals(s, imageRaster.getSample(x, y, b));

                    argb |= (s << argbOffs[b]);
                }

                if (raster.getNumBands() < 4) {
                    argb |= 0xFF000000;
                }

                int expectedArgb = image.getRGB(x, y);
                if (argb != expectedArgb) {
                    assertEquals(x + ", " + y + ": ",  String.format("#%08x", expectedArgb), String.format("#%08x", argb));
                }
            }
        }
    }
}