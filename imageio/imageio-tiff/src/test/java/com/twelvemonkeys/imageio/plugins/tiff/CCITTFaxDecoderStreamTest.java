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
 * @version $Id: CCITTFaxDecoderStreamTest.java,v 1.0 09.03.13 14:44 haraldk Exp$
 */
public class CCITTFaxDecoderStreamTest {

    // TODO: Better tests (full A4 width scan lines?)

    // From http://www.mikekohn.net/file_formats/tiff.php
    static final byte[] DATA_TYPE_2 = {
            (byte) 0x84, (byte) 0xe0, // 10000100 11100000
            (byte) 0x84, (byte) 0xe0, // 10000100 11100000
            (byte) 0x84, (byte) 0xe0, // 10000100 11100000
            (byte) 0x7d, (byte) 0xc0, // 01111101 11000000
    };

    static final byte[] DATA_TYPE_3 = {
            0x00, 0x01, (byte) 0xc2, 0x70,
            0x00, 0x01, 0x70,
            0x01,

    };

    static final byte[] DATA_TYPE_4 = {
            0x26, (byte) 0xb0, 95, (byte) 0xfa, (byte) 0xc0
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
    public void testReadCountType2() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_TYPE_2), 6, TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 1);

        int count = 0;
        int read;
        while ((read = stream.read()) >= 0) {
            count++;
        }

        // Just make sure we'll have 4 bytes
        assertEquals(4, count);

        // Verify that we don't return arbitrary values
        assertEquals(-1, read);
    }

    @Test
    public void testDecodeType2() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_TYPE_2), 6, TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 1);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        new DataInputStream(stream).readFully(bytes);

//        JPanel panel = new JPanel();
//        panel.add(new JLabel("Expected", new BufferedImageIcon(image, 300, 300, true), JLabel.CENTER));
//        panel.add(new JLabel("Actual", new BufferedImageIcon(new BufferedImage(image.getColorModel(), Raster.createPackedRaster(new DataBufferByte(bytes, bytes.length), 6, 4, 1, null), false, null), 300, 300, true), JLabel.CENTER));
//        JOptionPane.showConfirmDialog(null, panel);

        assertArrayEquals(imageData, bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeType3() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_TYPE_3), 6, TIFFExtension.COMPRESSION_CCITT_T4, 1);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        DataInputStream dataInput = new DataInputStream(stream);

        for (int y = 0; y < image.getHeight(); y++) {
            System.err.println("y: " + y);
            dataInput.readFully(bytes, y * image.getWidth(), image.getWidth());
        }

//        JPanel panel = new JPanel();
//        panel.add(new JLabel("Expected", new BufferedImageIcon(image, 300, 300, true), JLabel.CENTER));
//        panel.add(new JLabel("Actual", new BufferedImageIcon(new BufferedImage(image.getColorModel(), Raster.createPackedRaster(new DataBufferByte(bytes, bytes.length), 6, 4, 1, null), false, null), 300, 300, true), JLabel.CENTER));
//        JOptionPane.showConfirmDialog(null, panel);

        assertArrayEquals(imageData, bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeType4() throws IOException {
        InputStream stream = new CCITTFaxDecoderStream(new ByteArrayInputStream(DATA_TYPE_4), 6, TIFFExtension.COMPRESSION_CCITT_T6, 1);

        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] bytes = new byte[imageData.length];
        DataInputStream dataInput = new DataInputStream(stream);

        for (int y = 0; y < image.getHeight(); y++) {
            System.err.println("y: " + y);
            dataInput.readFully(bytes, y * image.getWidth(), image.getWidth());
        }

//        JPanel panel = new JPanel();
//        panel.add(new JLabel("Expected", new BufferedImageIcon(image, 300, 300, true), JLabel.CENTER));
//        panel.add(new JLabel("Actual", new BufferedImageIcon(new BufferedImage(image.getColorModel(), Raster.createPackedRaster(new DataBufferByte(bytes, bytes.length), 6, 4, 1, null), false, null), 300, 300, true), JLabel.CENTER));
//        JOptionPane.showConfirmDialog(null, panel);

        assertArrayEquals(imageData, bytes);
    }
}
