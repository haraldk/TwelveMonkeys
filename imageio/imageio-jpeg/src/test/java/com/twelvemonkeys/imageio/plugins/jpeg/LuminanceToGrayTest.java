package com.twelvemonkeys.imageio.plugins.jpeg;

import org.junit.Test;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * LumaToGrayTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LumaToGrayTest.java,v 1.0 10/04/2021 haraldk Exp$
 */
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
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
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
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
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
            assertArrayEquals(String.format("Was: %s, expected: %s", Arrays.toString(pixel), Arrays.toString(expected)), expected, pixel);
        }
    }
}