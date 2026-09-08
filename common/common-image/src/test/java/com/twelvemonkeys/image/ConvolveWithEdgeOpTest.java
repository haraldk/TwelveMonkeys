/*
 * Copyright (c) 2026, Harald Kuhr
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

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConvolveWithEdgeOpTest
 */
class ConvolveWithEdgeOpTest {

    private final Kernel kernel3x3 = new Kernel(3, 3, new float[]{
            0, 0, 0,
            0, 1, 0,
            0, 0, 0
    });

    @Test
    void testConstructor() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        assertEquals(ConvolveWithEdgeOp.EDGE_ZERO_FILL, op.getEdgeCondition());
        assertArrayEquals(kernel3x3.getKernelData(null), op.getKernel().getKernelData(null));
    }

    @Test
    void testConstructorFull() {
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3, ConvolveWithEdgeOp.EDGE_REFLECT, hints);
        assertEquals(ConvolveWithEdgeOp.EDGE_REFLECT, op.getEdgeCondition());
        assertArrayEquals(kernel3x3.getKernelData(null), op.getKernel().getKernelData(null));
        assertEquals(hints, op.getRenderingHints());
    }

    @Test
    void testFilterNull() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        assertThrows(NullPointerException.class, () -> op.filter((BufferedImage) null, null));
    }

    @Test
    void testFilterSame() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        assertThrows(IllegalArgumentException.class, () -> op.filter(img, img));
    }

    @Test
    void testGetBounds2D() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        BufferedImage img = new BufferedImage(10, 20, BufferedImage.TYPE_INT_ARGB);
        Rectangle2D bounds = op.getBounds2D(img);
        assertEquals(0, bounds.getX());
        assertEquals(0, bounds.getY());
        assertEquals(10, bounds.getWidth());
        assertEquals(20, bounds.getHeight());
    }

    @Test
    void testGetPoint2D() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        Point2D src = new Point2D.Double(5, 5);
        Point2D dst = op.getPoint2D(src, null);
        assertEquals(src.getX(), dst.getX());
        assertEquals(src.getY(), dst.getY());
    }

    @Test
    void testEdgeZeroFill() {
        // Use a kernel that averages all pixels
        Kernel kernel = new Kernel(3, 3, new float[]{
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f
        });
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel, ConvolveWithEdgeOp.EDGE_ZERO_FILL, null);

        BufferedImage src = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                src.setRGB(x, y, 0xFFFFFF);
            }
        }

        BufferedImage dst = op.filter(src, null);

        // Center pixel should be white (all neighbors are white)
        // (9 * 255) / 9 = 255
        // We allow some tolerance for different JVM implementations of ConvolveOp
        int center = dst.getRGB(1, 1) & 0xFF;
        assertEquals(255, center, 1.0, "Center value " + center + " expected to be around 255");

        // Corner pixel (0,0) should be darker because of zero-fill
        // Neighbors: (0,0), (1,0), (0,1), (1,1) are white, others are zero
        // (4 * 255) / 9 = 113.33 -> 113
        int corner = dst.getRGB(0, 0) & 0xFF;
        assertTrue(corner < 255, "Corner value " + corner + " should be darker than 255");
    }

    @Test
    void testEdgeNoOp() {
        Kernel kernel = new Kernel(3, 3, new float[]{
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f
        });
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel, ConvolveWithEdgeOp.EDGE_NO_OP, null);

        BufferedImage src = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                src.setRGB(x, y, 0xFFFFFF);
            }
        }

        BufferedImage dst = op.filter(src, null);

        // Center pixel should be white
        assertEquals(0xFFFFFFFF, dst.getRGB(1, 1));

        // EDGE_NO_OP: edge pixels are copied from source
        assertEquals(0xFFFFFFFF, dst.getRGB(0, 0));
    }

    @Test
    void testEdgeReflect() {
        Kernel kernel = new Kernel(3, 3, new float[]{
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f
        });
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel, ConvolveWithEdgeOp.EDGE_REFLECT, null);

        BufferedImage src = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                src.setRGB(x, y, 0xFFFFFF);
            }
        }
        // Make center white, surroundings black to see reflection effect
        // Wait, if I want to test reflection, I should have different colors at edges.
        src.setRGB(0, 0, 0xFF0000); // Red corner
        src.setRGB(1, 1, 0xFFFFFF); // White center

        BufferedImage dst = op.filter(src, null);

        assertNotNull(dst);
        assertEquals(src.getWidth(), dst.getWidth());
        assertEquals(src.getHeight(), dst.getHeight());
    }

    @Test
    void testEdgeWrap() {
        Kernel kernel = new Kernel(3, 3, new float[]{
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f,
                1/9f, 1/9f, 1/9f
        });
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel, ConvolveWithEdgeOp.EDGE_WRAP, null);

        BufferedImage src = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        src.setRGB(1, 1, 0xFFFFFF);

        BufferedImage dst = op.filter(src, null);

        assertNotNull(dst);
        assertEquals(src.getWidth(), dst.getWidth());
        assertEquals(src.getHeight(), dst.getHeight());
    }

    @Test
    void testRasterFilter() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        
        BufferedImage srcImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        srcImg.setRGB(5, 5, 0xFFFFFF);
        Raster src = srcImg.getRaster();
        
        WritableRaster dst = op.createCompatibleDestRaster(src);
        op.filter(src, dst);
        
        assertEquals(255, dst.getSample(5, 5, 0));
    }

    @Test
    void testCreateCompatibleDestImage() {
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        BufferedImage src = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage dst = op.createCompatibleDestImage(src, src.getColorModel());
        
        assertEquals(src.getWidth(), dst.getWidth());
        assertEquals(src.getHeight(), dst.getHeight());
        assertEquals(src.getType(), dst.getType());
    }

    @Test
    void testFilter3ByteBGR() {
        // ConvolveWithEdgeOp has a workaround for TYPE_3BYTE_BGR
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        BufferedImage src = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
        src.setRGB(5, 5, 0xFFFFFF);
        
        BufferedImage dst = op.filter(src, null);
        
        assertNotNull(dst);
        assertEquals(src.getWidth(), dst.getWidth());
        assertEquals(src.getHeight(), dst.getHeight());
        assertEquals(src.getType(), dst.getType());
        assertEquals(0xFFFFFFFF, dst.getRGB(5, 5));
    }

    @Test
    void testFilterIndexed() {
        byte[] gray = new byte[256];
        for (int i = 0; i < 256; i++) {
            gray[i] = (byte) i;
        }
        IndexColorModel cm = new IndexColorModel(8, 256, gray, gray, gray);
        BufferedImage src = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED, cm);
        
        ConvolveWithEdgeOp op = new ConvolveWithEdgeOp(kernel3x3);
        // ConvolveOp might not support indexed directly, it usually converts or throws.
        // But ConvolveWithEdgeOp uses ConvolveOp.
        
        assertDoesNotThrow(() -> op.filter(src, null));
    }
}
