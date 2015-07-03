/*
 * Copyright (c) 2013, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

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

    // group3_2d.tif: EOL|k=1|3W|1B|2W|EOL|k=0|V|V|V|EOL|k=1|3W|1B|2W|EOL|k=0|V-1|V|V|6*F
    static final byte[] DATA_G3_2D = { 0x00, 0x1C, 0x27, 0x00, 0x17, 0x00, 0x1C, 0x27, 0x00, 0x12, (byte) 0xC0 };

    // group3_2d_fill.tif
    static final byte[] DATA_G3_2D_FILL = { 0x00, 0x01, (byte) 0xC2, 0x70, 0x01, 0x70, 0x01, (byte) 0xC2, 0x70, 0x01,
            0x2C };

    static final byte[] DATA_G3_2D_lsb2msb = { 0x00, 0x38, (byte) 0xE4, 0x00, (byte) 0xE8, 0x00, 0x38, (byte) 0xE4,
            0x00, 0x48, 0x03 };

    // group4.tif:
    // Line 1: V-3, V-2, V0
    // Line 2: V0 V0 V0
    // Line 3: V0 V0 V0
    // Line 4: V-1, V0, V0 EOL EOL
    static final byte[] DATA_G4 = { 0x04, 0x17, (byte) 0xF5, (byte) 0x80, 0x08, 0x00, (byte) 0x80 };

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

    // Image should be (6 x 4):
    // 1 1 1 0 1 1 x x
    // 1 1 1 0 1 1 x x
    // 1 1 1 0 1 1 x x
    // 1 1 0 0 1 1 x x
    BufferedImage image;

    @Before
    public void init() {
        image = new BufferedImage(6, 4, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 6; x++) {
                image.setRGB(x, y, x == 3 ? 0xff000000 : 0xffffffff);
            }
        }

        image.setRGB(2, 3, 0xff000000);
    }

    @Test
    public void testDecodeType2() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_TYPE_2), 6,
                TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 1, 0L);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_1D() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_1D), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, 1, 0L);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_1D_FILL() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_1D_FILL), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, 1, TIFFExtension.GROUP3OPT_FILLBITS);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_2D() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_2D), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, 1, TIFFExtension.GROUP3OPT_2DENCODING);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_2D_FILL() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_2D_FILL), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, 1,
                TIFFExtension.GROUP3OPT_2DENCODING | TIFFExtension.GROUP3OPT_FILLBITS);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType3_2D_REVERSED() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G3_2D_lsb2msb), 6,
                TIFFExtension.COMPRESSION_CCITT_T4, 2, TIFFExtension.GROUP3OPT_2DENCODING);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }

    @Test
    public void testDecodeType4() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_G4), 6,
                TIFFExtension.COMPRESSION_CCITT_T6, 1, 0L);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);
        assertArrayEquals(imageData, bytes);
    }
}
