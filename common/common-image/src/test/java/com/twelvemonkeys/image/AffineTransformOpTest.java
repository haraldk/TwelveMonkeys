package com.twelvemonkeys.image;

import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * AffineTransformOpTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="mailto:mail@schmidor.de">Oliver Schmidtmer</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: AffineTransformOpTest.java,v 1.0 03/06/16 harald.kuhr Exp$
 */
public class AffineTransformOpTest {
    // Some notes:
    // It would be nice to have the following classes from imageio-core available:
    // - ColorSpaces (for CMYK testing)
    // - ImageTypeSpecifiers (for correct specs)
    // Would perhaps be better to use parameterized test case
    // Is it enough to test only (quadrant) rotation? Or should we test scale/translate/arbitrary rotation etc?

    // TYPE_INT_RGB == 1 (min), TYPE_BYTE_INDEXED == 13 (max), TYPE_CUSTOM (0) excluded
    private static final List<Integer> TYPES = Arrays.asList(
            BufferedImage.TYPE_INT_RGB,
            BufferedImage.TYPE_INT_ARGB,
            BufferedImage.TYPE_INT_ARGB_PRE,
            BufferedImage.TYPE_INT_BGR,
            BufferedImage.TYPE_3BYTE_BGR,
            BufferedImage.TYPE_4BYTE_ABGR,
            BufferedImage.TYPE_4BYTE_ABGR_PRE,
            BufferedImage.TYPE_USHORT_565_RGB,
            BufferedImage.TYPE_USHORT_555_RGB,
            BufferedImage.TYPE_BYTE_GRAY,
            BufferedImage.TYPE_USHORT_GRAY,
            BufferedImage.TYPE_BYTE_BINARY,
            BufferedImage.TYPE_BYTE_BINARY
    );

    private static final ColorSpace GRAY = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    private static final ColorSpace S_RGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    // Most of these will fail using the standard Op
    private static final List<ImageTypeSpecifier> SPECS = Arrays.asList(
            ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_USHORT, true, false),
            ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_SHORT, true, false),
            ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_INT, true, false),
            ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_FLOAT, true, false),
            ImageTypeSpecifier.createInterleaved(GRAY, new int[] {0, 1}, DataBuffer.TYPE_DOUBLE, true, false),

            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2}, DataBuffer.TYPE_SHORT, false, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2}, DataBuffer.TYPE_INT, false, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2}, DataBuffer.TYPE_FLOAT, false, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2}, DataBuffer.TYPE_DOUBLE, false, false),

            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, true, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_SHORT, true, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_INT, true, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_FLOAT, true, false),
            ImageTypeSpecifier.createInterleaved(S_RGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_DOUBLE, true, false)
    );

    final int width = 30;
    final int height = 20;

    @Test
    public void testGetPoint2D() {
        AffineTransform rotateInstance = AffineTransform.getRotateInstance(2.1);
        BufferedImageOp original = new java.awt.image.AffineTransformOp(rotateInstance, null);
        BufferedImageOp fallback = new com.twelvemonkeys.image.AffineTransformOp(rotateInstance, null);

        Point2D point = new Point2D.Double(39.7, 42.91);
        assertEquals(original.getPoint2D(point, null), fallback.getPoint2D(point, null));
    }

    @Test
    public void testGetBounds2D() {
        AffineTransform shearInstance = AffineTransform.getShearInstance(33.77, 77.33);
        BufferedImageOp original = new java.awt.image.AffineTransformOp(shearInstance, null);
        BufferedImageOp fallback = new com.twelvemonkeys.image.AffineTransformOp(shearInstance, null);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        assertEquals(original.getBounds2D(image), fallback.getBounds2D(image));
    }

    // TODO: ...etc. For all delegated methods, just test that it does exactly what the original does.
    // It won't test much for now, but it will make sure we don't accidentally break things in the future.

    @Test
    public void testFilterRotateBIStandard() {
        BufferedImageOp op_jre = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        BufferedImageOp op_tm = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (Integer type : TYPES) {
            BufferedImage image = new BufferedImage(width, height, type);
            BufferedImage result_jre = op_jre.filter(image, null);
            BufferedImage result_tm = op_tm.filter(image, null);

            assertNotNull("No result!", result_tm);
            assertEquals("Bad type", result_jre.getType(), result_tm.getType());
            assertEquals("Incorrect color model", result_jre.getColorModel(), result_tm.getColorModel());

            assertEquals("Incorrect width", result_jre.getWidth(), result_tm.getWidth());
            assertEquals("Incorrect height", result_jre.getHeight(), result_tm.getHeight());
        }
    }

    @Test
    public void testFilterRotateBICustom() {
        BufferedImageOp op_jre = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        BufferedImageOp op_tm = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (ImageTypeSpecifier spec : SPECS) {
            BufferedImage image = spec.createBufferedImage(width, height);

            BufferedImage result_tm = op_tm.filter(image, null);
            assertNotNull("No result!", result_tm);

            BufferedImage result_jre;
            try {
                result_jre = op_jre.filter(image, null);

                assertEquals("Bad type", result_jre.getType(), result_tm.getType());
                assertEquals("Incorrect color model", result_jre.getColorModel(), result_tm.getColorModel());

                assertEquals("Incorrect width", result_jre.getWidth(), result_tm.getWidth());
                assertEquals("Incorrect height", result_jre.getHeight(), result_tm.getHeight());
            }
            catch (ImagingOpException e) {
                System.err.println("spec: " + spec);
                assertEquals("Bad type", spec.getBufferedImageType(), result_tm.getType());
                assertEquals("Incorrect color model", spec.getColorModel(), result_tm.getColorModel());

                assertEquals("Incorrect width", height, result_tm.getWidth());
                assertEquals("Incorrect height", width, result_tm.getHeight());
            }
        }
    }

    // TODO: Test RasterOp variants of filter too

    @Test
    public void testRasterGetBounds2D() {
        AffineTransform shearInstance = AffineTransform.getShearInstance(33.77, 77.33);
        RasterOp original = new java.awt.image.AffineTransformOp(shearInstance, null);
        RasterOp fallback = new com.twelvemonkeys.image.AffineTransformOp(shearInstance, null);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        assertEquals(original.getBounds2D(image.getRaster()), fallback.getBounds2D(image.getRaster()));
    }

    @Test
    public void testRasterRotateBIStandard() {
        RasterOp op_jre = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        RasterOp op_tm = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (Integer type : TYPES) {
            Raster raster = new BufferedImage(width, height, type).getRaster();
            Raster result_jre = null, result_tm = null;

            try {
                result_jre = op_jre.filter(raster, null);
            }
            catch (ImagingOpException e) {
                System.err.println("type: " + type);
            }

            try {
                result_tm = op_tm.filter(raster, null);
            }
            catch (ImagingOpException e) {
                // Only fail if JRE AffineOp produces a result and our version not
                if (result_jre != null) {
                    assertNotNull("No result!", result_tm);
                }
                else {
                    continue;
                }
            }

            if (result_jre != null) {
                assertEquals("Incorrect width", result_jre.getWidth(), result_tm.getWidth());
                assertEquals("Incorrect height", result_jre.getHeight(), result_tm.getHeight());
            }
            else {
                assertEquals("Incorrect width", height, result_tm.getWidth());
                assertEquals("Incorrect height", width, result_tm.getHeight());
            }
        }
    }

    @Test
    public void testRasterRotateBICustom() {
        RasterOp op_jre = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        RasterOp op_tm = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (ImageTypeSpecifier spec : SPECS) {
            Raster raster = spec.createBufferedImage(width, height).getRaster();
            Raster result_jre = null, result_tm = null;

            try {
                result_jre = op_jre.filter(raster, null);
            }
            catch (ImagingOpException e) {
                System.err.println("spec: " + spec);
            }

            try {
                result_tm = op_tm.filter(raster, null);
            }
            catch (ImagingOpException e) {
                // Only fail if JRE AffineOp produces a result and our version not
                if (result_jre != null) {
                    assertNotNull("No result!", result_tm);
                }
                else {
                    continue;
                }
            }

            if (result_jre != null) {
                assertEquals("Incorrect width", result_jre.getWidth(), result_tm.getWidth());
                assertEquals("Incorrect height", result_jre.getHeight(), result_tm.getHeight());
            }
            else {
                assertEquals("Incorrect width", height, result_tm.getWidth());
                assertEquals("Incorrect height", width, result_tm.getHeight());
            }
        }
    }
}
