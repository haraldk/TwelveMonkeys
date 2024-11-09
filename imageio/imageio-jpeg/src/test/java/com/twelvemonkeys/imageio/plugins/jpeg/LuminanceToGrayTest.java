/*
 * Copyright (c) 2021, Harald Kuhr
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


import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LuminanceToGrayTest {
    @Test
    public void testConvertByteYcc() {
        LuminanceToGray convert = new LuminanceToGray();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 3, null);
        WritableRaster result = null;

        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (127 + i)});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(1, pixel.length);
            byte[] expected = {(byte) i};
            assertArrayEquals(expected, pixel, String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)));
        }
    }

    @Test
    public void testConvertByteYccK() {
        LuminanceToGray convert = new LuminanceToGray();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = null;

        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) (255 - i), (byte) (127 + i), (byte) 255});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(1, pixel.length);
            byte[] expected = {(byte) i};
            assertArrayEquals(expected, pixel, String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)));
        }
    }

    @Test
    public void testConvertByteYccA() {
        LuminanceToGray convert = new LuminanceToGray();

        WritableRaster input = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 4, null);
        WritableRaster result = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 2, null);

        byte[] pixel = null;
        for (int i = 0; i < 255; i++) {
            input.setDataElements(0, 0, new byte[] {(byte) i, (byte) 255, (byte) (127 + i), (byte) (255 - i)});
            result = convert.filter(input, result);
            pixel = (byte[]) result.getDataElements(0, 0, pixel);

            assertNotNull(pixel);
            assertEquals(2, pixel.length);
            byte[] expected = {(byte) i, (byte) (255 - i)};
            assertArrayEquals(expected, pixel, String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)));
        }
    }
}