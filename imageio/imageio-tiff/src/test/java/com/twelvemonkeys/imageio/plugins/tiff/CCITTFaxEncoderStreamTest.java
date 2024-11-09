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

import com.twelvemonkeys.imageio.plugins.tiff.CCITTFaxEncoderStream.Code;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * CCITTFaxEncoderStreamTest
 *
 * @author <a href="mailto:mail@schmidor.de">Oliver Schmidtmer</a>
 * @author last modified by $Author$
 * @version $Id$
 */
public class CCITTFaxEncoderStreamTest {

    // Image should be (6 x 4):
    // 1 1 1 0 1 1 x x
    // 1 1 1 0 1 1 x x
    // 1 1 1 0 1 1 x x
    // 1 1 0 0 1 1 x x
    BufferedImage image;

    @BeforeEach
    public void init() {
        image = new BufferedImage(6, 4, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 6; x++) {
                image.setRGB(x, y, x != 3 ? 0xff000000 : 0xffffffff);
            }
        }

        image.setRGB(2, 3, 0xffffffff);
    }

    @Test
    public void testBuildCodes() throws IOException {
        assertTrue(CCITTFaxEncoderStream.WHITE_TERMINATING_CODES.length == 64);
        for (Code code : CCITTFaxEncoderStream.WHITE_TERMINATING_CODES) {
            assertNotNull(code);
        }
        assertTrue(CCITTFaxEncoderStream.WHITE_NONTERMINATING_CODES.length == 40);
        for (Code code : CCITTFaxEncoderStream.WHITE_NONTERMINATING_CODES) {
            assertNotNull(code);
        }
        assertTrue(CCITTFaxEncoderStream.BLACK_TERMINATING_CODES.length == 64);
        for (Code code : CCITTFaxEncoderStream.BLACK_TERMINATING_CODES) {
            assertNotNull(code);
        }
        assertTrue(CCITTFaxEncoderStream.BLACK_NONTERMINATING_CODES.length == 40);
        for (Code code : CCITTFaxEncoderStream.BLACK_NONTERMINATING_CODES) {
            assertNotNull(code);
        }
    }

    @Test
    public void testType2() throws IOException {
        testStreamEncodeDecode(TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 1, 0L);
    }

    @Test
    public void testType4() throws IOException {
        testStreamEncodeDecode(TIFFExtension.COMPRESSION_CCITT_T4, 1, 0L);
        testStreamEncodeDecode(TIFFExtension.COMPRESSION_CCITT_T4, 1, TIFFExtension.GROUP3OPT_FILLBITS);
        testStreamEncodeDecode(TIFFExtension.COMPRESSION_CCITT_T4, 1, TIFFExtension.GROUP3OPT_2DENCODING);
        testStreamEncodeDecode(TIFFExtension.COMPRESSION_CCITT_T4, 1,
                TIFFExtension.GROUP3OPT_FILLBITS | TIFFExtension.GROUP3OPT_2DENCODING);
    }

    @Test
    public void testType6() throws IOException {
        testStreamEncodeDecode(TIFFExtension.COMPRESSION_CCITT_T6, 1, 0L);
    }

    @Test
    public void testReversedFillOrder() throws IOException {
        testStreamEncodeDecode(TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, 2, 0L);
        testStreamEncodeDecode(TIFFExtension.COMPRESSION_CCITT_T6, 2, 0L);
    }

    @Test
    public void testReencodeImages() throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/fivepages-scan-causingerrors.tif").openStream())) {
            ImageReader reader = ImageIO.getImageReaders(iis).next();
            reader.setInput(iis, true);

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            ImageWriter writer = new TIFFImageWriter(new TIFFImageWriterSpi());
            BufferedImage originalImage;

            try (ImageOutputStream output = ImageIO.createImageOutputStream(outputBuffer)) {
                writer.setOutput(output);
                originalImage = reader.read(0);

                IIOImage outputImage = new IIOImage(originalImage, null, reader.getImageMetadata(0));
                writer.write(outputImage);
            }

            byte[] originalData = ((DataBufferByte) originalImage.getData().getDataBuffer()).getData();

            BufferedImage reencodedImage = ImageIO.read(new ByteArrayInputStream(outputBuffer.toByteArray()));
            byte[] reencodedData = ((DataBufferByte) reencodedImage.getData().getDataBuffer()).getData();

            assertArrayEquals(originalData, reencodedData);
        }
    }

    @Test
    public void testRunlengthIssue() throws IOException {
        // Test for "Fixed an issue with long runlengths in CCITTFax writing #188"
        byte[] data = new byte[400];
        Arrays.fill(data, (byte) 0xFF);
        data[0] = 0;
        data[399] = 0;

        ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();
        OutputStream outputSteam = new CCITTFaxEncoderStream(imageOutput, 3200, 1, TIFFExtension.COMPRESSION_CCITT_T6, 1, 0L);
        outputSteam.write(data);
        outputSteam.close();
        byte[] encodedData = imageOutput.toByteArray();

        byte[] decodedData = new byte[data.length];
        CCITTFaxDecoderStream inputStream = new CCITTFaxDecoderStream(new ByteArrayInputStream(encodedData), 3200, TIFFExtension.COMPRESSION_CCITT_T6, 0L);
        new DataInputStream(inputStream).readFully(decodedData);
        inputStream.close();

        assertArrayEquals(data, decodedData);
    }

    protected URL getClassLoaderResource(final String pName) {
        return getClass().getResource(pName);
    }

    private void testStreamEncodeDecode(int type, int fillOrder, long options) throws IOException {
        byte[] imageData = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] redecodedData = new byte[imageData.length];

        ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();
        OutputStream outputSteam = new CCITTFaxEncoderStream(imageOutput, 6, 4, type, fillOrder, options);
        outputSteam.write(imageData);
        outputSteam.close();
        byte[] encodedData = imageOutput.toByteArray();

        InputStream inStream = new ByteArrayInputStream(encodedData);
        if(fillOrder == TIFFExtension.FILL_RIGHT_TO_LEFT){
            inStream = new ReverseInputStream(inStream);
        }
        try (CCITTFaxDecoderStream inputStream =
                     new CCITTFaxDecoderStream(inStream, 6, type, options)) {
            new DataInputStream(inputStream).readFully(redecodedData);
        }

        assertArrayEquals(imageData, redecodedData);
    }

}
