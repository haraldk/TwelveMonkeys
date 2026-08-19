package com.twelvemonkeys.imageio.plugins.hdr;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class RGBETest {

    // A scanline whose leading 4 bytes mark it as "not run length encoded" must be read flat and
    // must not fall through into the RLE path, which decodes a second time past the output array.
    @Test
    public void testNonRLEScanlineDoesNotOverflow() throws Exception {
        int width = 8;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        // Leading quad: first byte != 2 selects the non-RLE branch, last two bytes match the width
        bytes.write(new byte[] {1, 0, 0, (byte) width});
        // Flat pixel data for the remaining (width - 1) pixels
        for (int i = 0; i < (width - 1) * 4; i++) {
            bytes.write(0x11);
        }
        // Well-formed RLE channel data that the buggy fall-through would decode on top of the row
        for (int c = 0; c < 4; c++) {
            bytes.write(width);
            bytes.write(0x22);
            for (int i = 0; i < width - 1; i++) {
                bytes.write(0x33);
            }
        }

        byte[] row = new byte[width * 4];
        DataInput input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));

        assertDoesNotThrow(() -> RGBE.readPixelsRawRLE(input, row, 0, width, 1));

        byte[] expected = new byte[width * 4];
        expected[0] = 1;
        expected[1] = 0;
        expected[2] = 0;
        expected[3] = (byte) width;
        for (int i = 4; i < expected.length; i++) {
            expected[i] = 0x11;
        }
        assertArrayEquals(expected, row);
    }

    // Widths below the RLE minimum are read flat and must likewise not continue into the RLE path.
    @Test
    public void testSubMinimumWidthDoesNotOverflow() throws Exception {
        int width = 4;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < width * 4; i++) {
            bytes.write(0x44);
        }

        byte[] row = new byte[width * 4];
        DataInput input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));

        assertDoesNotThrow(() -> RGBE.readPixelsRawRLE(input, row, 0, width, 1));

        byte[] expected = new byte[width * 4];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 0x44;
        }
        assertArrayEquals(expected, row);
    }
}
