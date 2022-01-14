/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * CCITTFaxDecoderStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CCITTFaxDecoderStreamTest.java,v 1.0 09.03.13 14:44 haraldk
 *          Exp$
 */
public class CCITTFaxDecoderStreamTest {

    // group3_1d.tif: EOL|3W|1B|2W|EOL|3W|1B|2W|EOL|3W|1B|2W|EOL|2W|2B|2W|5*F
    static final byte[] DATA_G3_1D = { 0x00, 0x18, 0x4E, 0x00, 0x30, (byte) 0x9C, 0x00, 0x61, 0x38, 0x00, (byte) 0xBE,
            (byte) 0xE0 };

    // group3_1d_fill.tif
    static final byte[] DATA_G3_1D_FILL = { 0x00, 0x01, (byte) 0x84, (byte) 0xE0, 0x01, (byte) 0x84, (byte) 0xE0, 0x01,
            (byte) 0x84, (byte) 0xE0, 0x1, 0x7D, (byte) 0xC0 };

    // group3_1d_premature_eol.tif
    // 0011 0101 | 0000 0010 1011 | 0110 0111 | 0010 1001 | 0100
    // 0W | 59B | 640W | 40W
    static final byte[] DATA_G3_1D_PREMATURE_EOL = {
            0x35, 0x02, (byte) 0xB6, 0x72, (byte) 0x94, (byte) 0xE8, 0x74, 0x38, 0x1C, (byte) 0x81, 0x64, (byte) 0xD4,
            0x0A, (byte) 0xD9, (byte) 0xD2, 0x27, 0x50, (byte) 0x90, (byte) 0xA6, (byte) 0x87, 0x43, (byte) 0xE3
    };

    // group3_2d.tif: EOL|k=1|3W|1B|2W|EOL|k=0|V|V|V|EOL|k=1|3W|1B|2W|EOL|k=0|V-1|V|V|6*F
    static final byte[] DATA_G3_2D = {0x00, 0x1C, 0x27, 0x00, 0x17, 0x00, 0x1C, 0x27, 0x00, 0x12, (byte) 0xC0};

    // group3_2d_fill.tif
    static final byte[] DATA_G3_2D_FILL = {0x00, 0x01, (byte) 0xC2, 0x70, 0x01, 0x70, 0x01, (byte) 0xC2,
                                           0x70, 0x01, 0x2C};

    static final byte[] DATA_G3_2D_lsb2msb = {0x00, 0x38, (byte) 0xE4, 0x00, (byte) 0xE8, 0x00, 0x38, (byte) 0xE4,
                                              0x00, 0x48, 0x03};

    static final byte[] DATA_G3_LONG = {0x00, 0x68, 0x0A, (byte) 0xC9, 0x3A, 0x3A, 0x00, 0x68,
                                        (byte) 0x8A, (byte) 0xD8, 0x3A, 0x35, 0x00, 0x68, 0x0A, 0x06,
                                        (byte) 0xDD, 0x3A, 0x19, 0x00, 0x68, (byte) 0x8A, (byte) 0x9E, 0x75,
                                        0x08, 0x00, 0x68};

    // group4.tif:
    // Line 1: V-3, V-2, V0
    // Line 2: V0 V0 V0
    // Line 3: V0 V0 V0
    // Line 4: V-1, V0, V0 EOL EOL
    static final byte[] DATA_G4 = { 0x04, 0x17, (byte) 0xF5, (byte) 0x80, 0x08, 0x00, (byte) 0x80 };

    static final byte[] DATA_G4_ALIGNED = {
            0x04, 0x14, // 00000100 000101(00)
            (byte) 0xE0,    // 111 (00000)
            (byte) 0xE0,   // 111 (00000)
            0x58 // 01011 (000)
    };

    // TODO: Better tests (full A4 width scan lines?)

    // From http://www.mikekohn.net/file_formats/tiff.php
    static final byte[] DATA_TYPE_2 = { (byte) 0x84, (byte) 0xe0, // 10000100
                                                                  // 11100000
            (byte) 0x84, (byte) 0xe0, // 10000100 11100000
            (byte) 0x84, (byte) 0xe0, // 10000100 11100000
            (byte) 0x7d, (byte) 0xc0, // 01111101 11000000
    };

    static final byte[] DATA_TYPE_3 = { 0x00, 0x01, (byte) 0xc2, 0x70, // 00000000
                                                                       // 00000001
                                                                       // 11000010
                                                                       // 01110000
            0x00, 0x01, 0x78, // 00000000 00000001 01111000
            0x00, 0x01, 0x78, // 00000000 00000001 01110000
            0x00, 0x01, 0x56, // 00000000 00000001 01010110
            // 0x01, // 00000001

    };

    // 001 00110101 10 000010 1 1 1 1 1 1 1 1 1 1 010 11 (000000 padding)
    static final byte[] DATA_TYPE_4 = { 0x26, // 001 00110
            (byte) 0xb0, // 101 10 000
            0x5f, // 010 1 1 1 1 1
            (byte) 0xfa, // 1 1 1 1 1 010
            (byte) 0xc0 // 11 (000000 padding)
    };

    // 3W|1B|2W| 3W|1B|2W| 3W|1B|2W| 2W|2B|2W
    // 1000|010|0111| 1000|010|0111| 1000|010|0111| 0111|11|0111
    static final byte[] DATA_RLE_UNALIGNED = {
            (byte)0x84, (byte)0xF0, (byte)0x9E,0x13, (byte)0xBE,(byte) 0xE0
    };

    // Image should be (6 x 4):
    // 1 1 1 0 1 1 x x
    // 1 1 1 0 1 1 x x
    // 1 1 1 0 1 1 x x
    // 1 1 0 0 1 1 x x
    final BufferedImage image = new BufferedImage(6, 4, BufferedImage.TYPE_BYTE_BINARY);

    @Before
    public void init() {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 6; x++) {
                image.setRGB(x, y, x != 3 ? 0xff000000 : 0xffffffff);
            }
        }

        image.setRGB(2, 3, 0xffffffff);
    }

    @Test
    public void testDecodeType2() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_TYPE_2), 6,
                TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 0L);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_1D() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_1D), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, 0L);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_1D_FILL() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_1D_FILL), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, TIFFExtension.GROUP3OPT_FILLBITS);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testFindCompressionType() throws IOException {
        // RLE
        assertEquals(TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, CCITTFaxDecoderStream.findCompressionType(TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, new ByteArrayInputStream(DATA_RLE_UNALIGNED)));

        // Group 3/CCITT_T4
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ByteArrayInputStream(DATA_G3_1D)));
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ByteArrayInputStream(DATA_G3_1D_FILL)));
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ByteArrayInputStream(DATA_G3_2D)));
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ByteArrayInputStream(DATA_G3_2D_FILL)));
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ReverseInputStream(new ByteArrayInputStream(DATA_G3_2D_lsb2msb))));
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ReverseInputStream(new ByteArrayInputStream(DATA_G3_LONG))));

        // Group 4/CCITT_T6
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T6, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T6, new ByteArrayInputStream(DATA_G4)));
        assertEquals(TIFFExtension.COMPRESSION_CCITT_T6, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T6, new ByteArrayInputStream(DATA_G4_ALIGNED)));

        // From sample file encoded with RLE, but with CCITT_T4 compression tag
        assertEquals(TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ByteArrayInputStream(DATA_G3_1D_PREMATURE_EOL)));
        assertEquals(TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, new ByteArrayInputStream(DATA_RLE_UNALIGNED)));
    }

    @Test
    public void testDecodeType3_2D() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_2D), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, TIFFExtension.GROUP3OPT_2DENCODING);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_2D_FILL() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_2D_FILL), 6,
                TIFFExtension.COMPRESSION_CCITT_T4,
                TIFFExtension.GROUP3OPT_2DENCODING | TIFFExtension.GROUP3OPT_FILLBITS);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_2D_REVERSED() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ReverseInputStream(new ByteArrayInputStream(DATA_G3_2D_lsb2msb)), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, TIFFExtension.GROUP3OPT_2DENCODING);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType4() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G4), 6,
                TIFFExtension.COMPRESSION_CCITT_T6, 0L);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeMissingRows() throws IOException {
        // See https://github.com/haraldk/TwelveMonkeys/pull/225 and https://github.com/haraldk/TwelveMonkeys/issues/232
        InputStream inputStream = getResourceAsStream("/tiff/ccitt_tolessrows.tif");

        // Skip until StripOffsets: 8
        for (int i = 0; i < 8; i++) {
            inputStream.read();
        }

        // Read until StripByteCounts: 7
        byte[] data = new byte[7];
        new DataInputStream(inputStream).readFully(data);

        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(data),
                6, TIFFExtension.COMPRESSION_CCITT_T6, 0L);

        byte[] bytes = new byte[6]; // 6 x 6 pixel, 1 bpp => 6 bytes
        new DataInputStream(stream).readFully(bytes);

        // Pad image data with 0s
        byte[] imageData = Arrays.copyOf(((DataBufferByte) image.getData().getDataBuffer()).getData(), 6);
        assertArrayEquals(imageData, bytes);

        // Ideally, we should have no more data now, but the stream don't know that...
        // assertEquals("Should contain no more data", -1, stream.read());
    }

    @Test
    public void testMoreChangesThanColumns() throws IOException {
        // Produces an CCITT Stream with 9 changes on 8 columns.
        byte[] data = new byte[] {(byte) 0b10101010};
        ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();
        OutputStream outputSteam = new CCITTFaxEncoderStream(imageOutput,
                8, 1, TIFFExtension.COMPRESSION_CCITT_T6, 1, 0L);
        outputSteam.write(data);
        outputSteam.close();

        byte[] encoded = imageOutput.toByteArray();
        InputStream inputStream = new CCITTFaxDecoderStream(new ByteArrayInputStream(encoded), 8,
                TIFFExtension.COMPRESSION_CCITT_T6, 0L);
        byte decoded = (byte) inputStream.read();
        assertEquals(data[0], decoded);
    }

    @Test
    public void testMoreChangesThanColumnsFile() throws IOException {
        // See https://github.com/haraldk/TwelveMonkeys/issues/328
        // 26 changes on 24 columns: H0w1b, H1w1b, ..., H1w0b
        InputStream stream = getResourceAsStream("/tiff/ccitt-too-many-changes.tif");

        // Skip bytes before StripOffsets: 86
        for (int i = 0; i < 86; i++) {
            stream.read();
        }

        InputStream inputStream = new CCITTFaxDecoderStream(stream,
                24, TIFFExtension.COMPRESSION_CCITT_T6, 0L);
        byte decoded = (byte) inputStream.read();
        assertEquals((byte) 0b10101010, decoded);
    }

    @Test
    public void testDecodeType4ByteAligned() throws IOException {
        CCITTFaxDecoderStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G4_ALIGNED), 6,
                TIFFExtension.COMPRESSION_CCITT_T6, 0L, true);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType2NotByteAligned() throws IOException {
        CCITTFaxDecoderStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_RLE_UNALIGNED), 6,
                TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 0L, false);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testG3AOE() throws IOException {
        InputStream inputStream = getResourceAsStream("/tiff/ccitt/g3aoe.tif");

        // Skip until StripOffsets: 8
        for (int i = 0; i < 8; i++) {
            inputStream.read();
        }

        // Read until StripByteCounts: 20050
        byte[] data = new byte[20050];
        new DataInputStream(inputStream).readFully(data);

        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(data),
                1728, TIFFExtension.COMPRESSION_CCITT_T4, TIFFExtension.GROUP3OPT_FILLBITS);

        byte[] bytes = new byte[216 * 1168]; // 1728 x 1168 pixel, 1 bpp => 216 bytes * 1168
        new DataInputStream(stream).readFully(bytes);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test(expected = IOException.class)
    public void testAIOBEInCorruptStreamShouldThrowIOException() throws IOException {
        // From #645
        try (InputStream ccittFaxDecoderStream = new CCITTFaxDecoderStream(getResourceAsStream("/ccitt/645.ccitt"), 7, 4, 0, false)) {
            while(ccittFaxDecoderStream.read() != -1); // Just read until the end
        }
    }

    @Test
    public void testFindCompressionTypeForMissingStartEOL() throws IOException {
        // Type 4, missing leading EOL code
        // starts with 1728px long white lines
        byte[] data = new byte[]{
                0x4d, (byte) 0x9a, (byte) 0x80, 0x01, 0x4d, (byte) 0x9a, (byte) 0x80, 0x01, 0x4d, (byte) 0x9a, (byte) 0x80, 0x01, 0x4d, (byte) 0x9a, (byte) 0x80, 0x01,
                (byte) 0x91, 0x3c, 0x17, 0x6d, 0x02, (byte) 0xf2, (byte) 0xb0, 0x20, (byte) 0x01, (byte) 0xda, (byte) 0xa8, (byte) 0xb3, 0x17, 0x4e, 0x62, (byte) 0xcd, (byte) 0xa7
        };
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            int detectedType = CCITTFaxDecoderStream.findCompressionType(TIFFExtension.COMPRESSION_CCITT_T4, is);
            assertEquals(TIFFExtension.COMPRESSION_CCITT_T4, detectedType);
        }
    }

    private InputStream getResourceAsStream(String name) {
        return getClass().getResourceAsStream(name);
    }
}
