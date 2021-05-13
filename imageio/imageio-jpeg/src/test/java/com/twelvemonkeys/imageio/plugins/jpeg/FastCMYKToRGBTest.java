/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * FastCMYKToRGBTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: FastCMYKToRGBTest.java,v 1.0 22.02.11 16.22 haraldk Exp$
 */
public class FastCMYKToRGBTest {
    @Test
    public void testCreate() {
        new FastCMYKToRGB();
    }

    @Test
    public void testConvertByteRGBWhite() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = convert.filter(input, null);
        byte[] pixel = (byte[]) result.getDataElements(0, 0, null);
        assertNotNull(pixel);
        assertEquals(3, pixel.length);
        byte[] expected = {(byte) 255, (byte) 255, (byte) 255};
        assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
    }

    @Test
    public void testConvertIntRGBWhite() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = convert.filter(input, new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getRaster());
        int[] pixel = (int[]) result.getDataElements(0, 0, null);
        assertNotNull(pixel);
        assertEquals(1, pixel.length);
        int expected = 0xFFFFFF;
        int rgb = pixel[0] & 0xFFFFFF;
        assertEquals(String.format("Was: 0x%08x, expected: 0x%08x", rgb, expected), expected, rgb);
    }

    @Test
    public void testConvertByteRGBBlack() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = null;
        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (127 + i), (byte) 255});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(3, pixel.length);
            byte[] expected = {(byte) 0, (byte) 0, (byte) 0};
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
        }
    }

    @Test
    public void testConvertIntRGBBlack() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getRaster();
        int[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (127 + i), (byte) 255});
            result = convert.filter(input, result);
            pixel = (int[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(1, pixel.length);
            int expected = 0x0;
            int rgb = pixel[0] & 0xFFFFFF;
            assertEquals(String.format("Was: 0x%08x, expected: 0x%08x", rgb, expected), expected, rgb);
        }
    }

    @Test
    public void testConvertByteRGBColors() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = null;
        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (128 + i), 0});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(3, pixel.length);
            byte[] expected = {(byte) (255 - i), (byte) i, (byte) (127 - i)};
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
        }
    }

    @Test
    public void testConvertByteBGRColors() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = null;
        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (128 + i), 0});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(3, pixel.length);
            byte[] expected = {(byte) (255 - i), (byte) i, (byte) (127 - i)};
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
        }
    }

    @Test
    public void testConvertByteABGRColors() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR).getRaster();
        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (128 + i), 0});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(4, pixel.length);
            byte[] expected = {(byte) (255 - i), (byte) i, (byte) (127 - i), (byte) 0xff};
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
        }
    }

    @Test
    public void testConvertIntRGBColors() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getRaster();
        int[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (128 + i), 0});
            result = convert.filter(input, result);
            pixel = (int[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(1, pixel.length);
            int expected = (((byte) (255 - i)) & 0xFF) << 16 | (((byte) i) & 0xFF) << 8 | ((byte) (127 - i)) & 0xFF;
            int rgb = pixel[0] & 0xFFFFFF;
            assertEquals(String.format("Was: 0x%08x, expected: 0x%08x", rgb, expected), expected, rgb);
        }
    }

    @Test
    public void testConvertIntBGRColors() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR).getRaster();
        int[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (128 + i), 0});
            result = convert.filter(input, result);
            pixel = (int[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(1, pixel.length);
            int expected = (((byte) (127 - i)) & 0xFF) << 16 | (((byte) i) & 0xFF) << 8 | ((byte) (255 - i)) & 0xFF;
            int rgb = pixel[0] & 0xFFFFFF;
            assertEquals(String.format("Was: 0x%08x, expected: 0x%08x", rgb, expected), expected, rgb);
        }
    }

    @Test
    public void testConvertIntARGBColors() {
        FastCMYKToRGB convert = new FastCMYKToRGB();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getRaster();
        int[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[]{(byte) i, (byte) (255 - i), (byte) (128 + i), 0});
            result = convert.filter(input, result);
            pixel = (int[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(1, pixel.length);
            int expected = 0xFF << 24 | (((byte) (255 - i)) & 0xFF) << 16 | (((byte) i) & 0xFF) << 8 | ((byte) (127 - i)) & 0xFF;
            assertEquals(String.format("Was: 0x%08x, expected: 0x%08x", pixel[0], expected), expected, pixel[0]);
        }
    }
}
