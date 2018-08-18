/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.image;

import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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

    private final int width = 30;
    private final int height = 20;

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
        BufferedImageOp jreOp = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        BufferedImageOp tmOp = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (Integer type : TYPES) {
            BufferedImage image = new BufferedImage(width, height, type);
            BufferedImage jreResult = jreOp.filter(image, null);
            BufferedImage tmResult = tmOp.filter(image, null);

            assertNotNull("No result!", tmResult);
            assertEquals("Bad type", jreResult.getType(), tmResult.getType());
            assertEquals("Incorrect color model", jreResult.getColorModel(), tmResult.getColorModel());

            assertEquals("Incorrect width", jreResult.getWidth(), tmResult.getWidth());
            assertEquals("Incorrect height", jreResult.getHeight(), tmResult.getHeight());
        }
    }

    @Test
    public void testFilterRotateBICustom() {
        BufferedImageOp jreOp = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        BufferedImageOp tmOp = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (ImageTypeSpecifier spec : SPECS) {
            BufferedImage image = spec.createBufferedImage(width, height);

            BufferedImage tmResult = tmOp.filter(image, null);
            assertNotNull("No result!", tmResult);

            BufferedImage jreResult = null;

            try {
                jreResult = jreOp.filter(image, null);
            }
            catch (ImagingOpException ignore) {
                // We expect this to fail for certain cases, that's why we crated the class in the first place
            }

            if (jreResult != null) {
                assertEquals("Bad type", jreResult.getType(), tmResult.getType());
                assertEquals("Incorrect color model", jreResult.getColorModel(), tmResult.getColorModel());

                assertEquals("Incorrect width", jreResult.getWidth(), tmResult.getWidth());
                assertEquals("Incorrect height", jreResult.getHeight(), tmResult.getHeight());
            }
            else {
                assertEquals("Bad type", spec.getBufferedImageType(), tmResult.getType());
                assertEquals("Incorrect color model", spec.getColorModel(), tmResult.getColorModel());

                assertEquals("Incorrect width", height, tmResult.getWidth());
                assertEquals("Incorrect height", width, tmResult.getHeight());
            }
        }
    }

    // Test RasterOp variants

    @Test
    public void testGetBounds2DRaster() {
        AffineTransform shearInstance = AffineTransform.getShearInstance(33.77, 77.33);
        RasterOp original = new java.awt.image.AffineTransformOp(shearInstance, null);
        RasterOp fallback = new com.twelvemonkeys.image.AffineTransformOp(shearInstance, null);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        assertEquals(original.getBounds2D(image.getRaster()), fallback.getBounds2D(image.getRaster()));
    }

    @Test
    public void testFilterRotateRasterStandard() {
        RasterOp jreOp = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        RasterOp tmOp = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (Integer type : TYPES) {
            Raster raster = new BufferedImage(width, height, type).getRaster();
            Raster jreResult = null;
            Raster tmResult = null;

            try {
                jreResult = jreOp.filter(raster, null);
            }
            catch (ImagingOpException ignore) {
                // We expect this to fail for certain cases, that's why we crated the class in the first place
            }

            try {
                tmResult = tmOp.filter(raster, null);
            }
            catch (ImagingOpException e) {
                // Only fail if JRE AffineOp produces a result and our version not
                if (jreResult != null) {
                    fail("No result!");
                }
                else {
                    System.err.println("AffineTransformOpTest.testFilterRotateRasterStandard");
                    System.err.println("type: " + type);
                    continue;
                }
            }

            if (jreResult != null) {
                assertEquals("Incorrect width", jreResult.getWidth(), tmResult.getWidth());
                assertEquals("Incorrect height", jreResult.getHeight(), tmResult.getHeight());
            }
            else {
                assertEquals("Incorrect width", height, tmResult.getWidth());
                assertEquals("Incorrect height", width, tmResult.getHeight());
            }
        }
    }

    @Test
    public void testFilterRotateRasterCustom() {
        RasterOp jreOp = new java.awt.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);
        RasterOp tmOp = new com.twelvemonkeys.image.AffineTransformOp(AffineTransform.getQuadrantRotateInstance(1, Math.min(width, height) / 2, Math.min(width, height) / 2), null);

        for (ImageTypeSpecifier spec : SPECS) {
            Raster raster = spec.createBufferedImage(width, height).getRaster();
            Raster jreResult = null;
            Raster tmResult = null;

            try {
                jreResult = jreOp.filter(raster, null);
            }
            catch (ImagingOpException ignore) {
                // We expect this to fail for certain cases, that's why we crated the class in the first place
            }

            try {
                tmResult = tmOp.filter(raster, null);
            }
            catch (ImagingOpException e) {
                // Only fail if JRE AffineOp produces a result and our version not
                if (jreResult != null) {
                    fail("No result!");
                }
                else {
                    System.err.println("AffineTransformOpTest.testFilterRotateRasterCustom");
                    System.err.println("spec: " + spec);
                    continue;
                }
            }

            if (jreResult != null) {
                assertEquals("Incorrect width", jreResult.getWidth(), tmResult.getWidth());
                assertEquals("Incorrect height", jreResult.getHeight(), tmResult.getHeight());
            }
            else {
                assertEquals("Incorrect width", height, tmResult.getWidth());
                assertEquals("Incorrect height", width, tmResult.getHeight());
            }
        }
    }
}
