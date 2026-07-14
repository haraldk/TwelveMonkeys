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
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static com.twelvemonkeys.image.ImageUtilTest.createImage;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AreaAverageOpTest
 */
class AreaAverageOpTest {

    @Test
    void testFilterDifferentTypes() {
        int[] types = {
                BufferedImage.TYPE_INT_ARGB,
                BufferedImage.TYPE_INT_RGB,
                BufferedImage.TYPE_BYTE_GRAY,
                BufferedImage.TYPE_3BYTE_BGR,
                BufferedImage.TYPE_4BYTE_ABGR,
        };

        int targetW = 5;
        int targetH = 5;
        AreaAverageOp op = new AreaAverageOp(targetW, targetH);

        for (int type : types) {
            BufferedImage src = createImage(10, 10, type);
            BufferedImage dest = op.filter(src, null);

            assertNotNull(dest, "Failed for type " + type);
            assertEquals(targetW, dest.getWidth());
            assertEquals(targetH, dest.getHeight());
            assertEquals(src.getType(), dest.getType(), "Type mismatch for type " + type);
        }
    }

    @Test
    void testFilterUshort() {
        // TYPE_USHORT_GRAY and custom types using USHORT
        int targetW = 5;
        int targetH = 5;
        AreaAverageOp op = new AreaAverageOp(targetW, targetH);

        BufferedImage src = createImage(10, 10, BufferedImage.TYPE_USHORT_GRAY);
        BufferedImage dest = op.filter(src, null);

        assertNotNull(dest);
        assertEquals(targetW, dest.getWidth());
        assertEquals(targetH, dest.getHeight());
        assertEquals(BufferedImage.TYPE_USHORT_GRAY, dest.getType());

        // Test with a custom USHORT type (SinglePixelPackedSampleModel) if possible
        // For now, TYPE_USHORT_565_RGB or TYPE_USHORT_555_RGB
        int[] ushortTypes = {BufferedImage.TYPE_USHORT_565_RGB, BufferedImage.TYPE_USHORT_555_RGB};
        for (int type : ushortTypes) {
            src = createImage(10, 10, type);
            dest = op.filter(src, null);
            assertNotNull(dest);
            assertEquals(targetW, dest.getWidth());
            assertEquals(targetH, dest.getHeight());
        }
    }

    @Test
    void testFilterRaster() {
        int targetW = 10;
        int targetH = 10;
        AreaAverageOp op = new AreaAverageOp(targetW, targetH);
        BufferedImage src = createImage(20, 20);
        Raster srcRaster = src.getRaster();
        WritableRaster destRaster = op.filter(srcRaster, null);

        assertNotNull(destRaster);
        assertEquals(targetW, destRaster.getWidth());
        assertEquals(targetH, destRaster.getHeight());
    }


    @Test
    void testCreateCompatibleDestImage() {
        AreaAverageOp op = new AreaAverageOp(10, 10);
        BufferedImage src = createImage(20, 20, BufferedImage.TYPE_INT_RGB);
        BufferedImage dest = op.createCompatibleDestImage(src, null);

        assertNotNull(dest);
        assertEquals(10, dest.getWidth());
        assertEquals(10, dest.getHeight());
        assertEquals(src.getColorModel(), dest.getColorModel());
    }

    @Test
    void testCreateCompatibleDestRaster() {
        AreaAverageOp op = new AreaAverageOp(10, 10);
        BufferedImage src = createImage(20, 20);
        Raster srcRaster = src.getRaster();
        WritableRaster destRaster = op.createCompatibleDestRaster(srcRaster);

        assertNotNull(destRaster);
        assertEquals(10, destRaster.getWidth());
        assertEquals(10, destRaster.getHeight());
    }

    @Test
    void testGetBounds2D() {
        AreaAverageOp op = new AreaAverageOp(10, 10);
        BufferedImage src = createImage(20, 20);
        Rectangle2D bounds = op.getBounds2D(src);
        assertEquals(0, bounds.getX());
        assertEquals(0, bounds.getY());
        assertEquals(10, bounds.getWidth());
        assertEquals(10, bounds.getHeight());
    }

    @Test
    void testGetPoint2D() {
        int targetW = 10;
        int targetH = 10;
        AreaAverageOp op = new AreaAverageOp(targetW, targetH);
        
        Point2D srcPt = new Point2D.Double(100, 100);
        Point2D dstPt = op.getPoint2D(srcPt, null);
        
        assertNotNull(dstPt);
        assertEquals(srcPt, dstPt);
        assertNotSame(srcPt, dstPt);

        Point2D dstPt2 = new Point2D.Double();
        Point2D resultPt = op.getPoint2D(srcPt, dstPt2);
        assertSame(dstPt2, resultPt);
        assertEquals(srcPt, resultPt);
    }

    @Test
    void testGetRenderingHints() {
        AreaAverageOp op = new AreaAverageOp(10, 10);
        assertNull(op.getRenderingHints());
    }
}
