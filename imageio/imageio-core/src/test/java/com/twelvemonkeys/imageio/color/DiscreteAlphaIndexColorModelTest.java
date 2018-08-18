/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.color;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.awt.image.*;

import static org.junit.Assert.*;

public class DiscreteAlphaIndexColorModelTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        new DiscreteAlphaIndexColorModel(null);
    }

    @Test
    public void testCreateByte() {
        int[] colors = createIntLut(1 << 8);
        IndexColorModel colorModel = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        new DiscreteAlphaIndexColorModel(colorModel);
    }

    @Test
    public void testCreateUShort() {
        int[] colors = createIntLut(1 << 16);
        IndexColorModel colorModel = new IndexColorModel(16, colors.length, colors, 0, false, -1, DataBuffer.TYPE_USHORT);

        new DiscreteAlphaIndexColorModel(colorModel);
    }

    @Test
    public void testGetRed() {
        int[] colors = createIntLut(1 << 8);
        colors[0] = 0x336699;
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        DiscreteAlphaIndexColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);

        assertEquals(0x33, colorModel.getRed(0));
        assertEquals(0x33, colorModel.getRed(new byte[] {0x00, 0x45}));

        for (int i = 1; i < colors.length; i++) {
            assertEquals(i, colorModel.getRed(i));
            assertEquals(i, colorModel.getRed(new byte[] {(byte) i, (byte) 0xff}));
        }
    }

    @Test
    public void testGetGreen() {
        int[] colors = createIntLut(1 << 8);
        colors[0] = 0x336699;
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        DiscreteAlphaIndexColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);

        assertEquals(0x66, colorModel.getGreen(0));
        assertEquals(0x66, colorModel.getGreen(new byte[] {0x00, 0x45}));

        for (int i = 1; i < colors.length; i++) {
            assertEquals(i, colorModel.getGreen(i));
            assertEquals(i, colorModel.getGreen(new byte[] {(byte) i, (byte) 0xff}));
        }
    }

    @Test
    public void testGetBlue() {
        int[] colors = createIntLut(1 << 8);
        colors[0] = 0x336699;
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        DiscreteAlphaIndexColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);

        assertEquals(0x99, colorModel.getBlue(0));
        assertEquals(0x99, colorModel.getBlue(new byte[] {0x00, 0x45}));

        for (int i = 1; i < colors.length; i++) {
            assertEquals(i, colorModel.getBlue(i));
            assertEquals(i, colorModel.getBlue(new byte[] {(byte) i, (byte) 0xff}));
        }
    }

    @Test
    public void testGetAlpha() {
        int[] colors = createIntLut(1 << 8);
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        DiscreteAlphaIndexColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);

        assertEquals(0x45, colorModel.getAlpha(0x45));
        assertEquals(0x45, colorModel.getAlpha(new byte[] {0x01, 0x45}));

        for (int i = 1; i < colors.length; i++) {
            assertEquals(i, colorModel.getAlpha(i));
            assertEquals(i, colorModel.getAlpha(new byte[]{(byte) 0xff, (byte) i}));
        }
    }

    @Test
    public void testGetAlphaUShort() {
        int[] colors = createIntLut(1 << 16);
        colors[1] = 0x336699;
        IndexColorModel icm = new IndexColorModel(16, colors.length, colors, 0, false, -1, DataBuffer.TYPE_USHORT);

        DiscreteAlphaIndexColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);

        assertEquals(0x45, colorModel.getAlpha(0x4500));
        assertEquals(0x45, colorModel.getAlpha(0x457F));

        assertEquals(0x46, colorModel.getAlpha(0x45C6)); // Hmm.. This seems rather odd.. I would assume the limit should be 0x4580
        assertEquals(0x46, colorModel.getAlpha(0x45FF));

        assertEquals(0x45, colorModel.getAlpha(new short[] {0x01, 0x4500}));
        assertEquals(0x45, colorModel.getAlpha(new short[] {0x02, 0x457F}));

        assertEquals(0x46, colorModel.getAlpha(new short[] {0x03, 0x45C6}));
        assertEquals(0x46, colorModel.getAlpha(new short[] {0x04, 0x45FF}));
    }

    @Test
    public void testCreateCompatibleSampleModel() {
        int[] colors = createIntLut(1 << 8);
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        ColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(3, 2);

        assertNotNull(sampleModel);

        assertEquals(3, sampleModel.getWidth());
        assertEquals(2, sampleModel.getHeight());

        assertTrue(colorModel.isCompatibleSampleModel(sampleModel));
        assertThat(sampleModel, CoreMatchers.is(PixelInterleavedSampleModel.class));
        assertThat(sampleModel.getDataType(), CoreMatchers.equalTo(DataBuffer.TYPE_BYTE));
    }

    @Test
    public void testCreateCompatibleSampleModelUShort() {
        int[] colors = createIntLut(1 << 8);
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_USHORT);

        ColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(3, 2);

        assertNotNull(sampleModel);

        assertEquals(3, sampleModel.getWidth());
        assertEquals(2, sampleModel.getHeight());

        assertTrue(colorModel.isCompatibleSampleModel(sampleModel));
        assertThat(sampleModel, CoreMatchers.is(PixelInterleavedSampleModel.class));
        assertThat(sampleModel.getDataType(), CoreMatchers.equalTo(DataBuffer.TYPE_USHORT));
    }

    @Test
    public void testCreateCompatibleRaster() {
        int[] colors = createIntLut(1 << 8);
        IndexColorModel icm = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);

        ColorModel colorModel = new DiscreteAlphaIndexColorModel(icm);
        WritableRaster raster = colorModel.createCompatibleWritableRaster(3, 2);

        assertNotNull(raster);

        assertEquals(3, raster.getWidth());
        assertEquals(2, raster.getHeight());

        assertTrue(colorModel.isCompatibleRaster(raster));
        assertThat(raster, CoreMatchers.is(WritableRaster.class)); // Specific subclasses are in sun.awt package
        assertThat(raster.getTransferType(), CoreMatchers.equalTo(DataBuffer.TYPE_BYTE));
    }

    private static int[] createIntLut(final int count) {
        int[] lut = new int[count];

        for (int i = 0; i < count; i++) {
            lut[i] = 0xff000000 | i << 16 | i << 8 | i;
        }

        return lut;
    }
}