/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TGAImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TGAImageReaderTest.java,v 1.0 03.07.14 22:28 haraldk Exp$
 */
public class TGAImageReaderTest extends ImageReaderAbstractTest<TGAImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new TGAImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/tga/MARBLES.TGA"), new Dimension(1419, 1001)), // Uncompressed BGR

                // Original TrueVision sample suite
                new TestData(getClassLoaderResource("/tga/UBW8.TGA"), new Dimension(128, 128)), // Uncompressed monochrome
                new TestData(getClassLoaderResource("/tga/UCM8.TGA"), new Dimension(128, 128)), // Uncompressed  8 bit colormapped, 16 bit palette
                new TestData(getClassLoaderResource("/tga/UTC16.TGA"), new Dimension(128, 128)), // Uncompressed  16 bit BGR
                new TestData(getClassLoaderResource("/tga/UTC24.TGA"), new Dimension(128, 128)), // Uncompressed  24 bit BGR
                new TestData(getClassLoaderResource("/tga/UTC32.TGA"), new Dimension(128, 128)), // Uncompressed  32 bit BGRA

                new TestData(getClassLoaderResource("/tga/CBW8.TGA"), new Dimension(128, 128)), // RLE compressed monochrome
                new TestData(getClassLoaderResource("/tga/CCM8.TGA"), new Dimension(128, 128)), // RLE compressed 8 bit colormapped, 16 bit palette
                new TestData(getClassLoaderResource("/tga/CTC16.TGA"), new Dimension(128, 128)), // RLE compressed 16 bit BGR
                new TestData(getClassLoaderResource("/tga/CTC24.TGA"), new Dimension(128, 128)), // RLE compressed 24 bit BGR
                new TestData(getClassLoaderResource("/tga/CTC32.TGA"), new Dimension(128, 128)), // RLE compressed 32 bit BGRA

                // More samples from http://www.fileformat.info/format/tga/sample/index.htm
                new TestData(getClassLoaderResource("/tga/FLAG_B16.TGA"), new Dimension(124, 124)), // Uncompressed 16 bit BGR bottom/up
                new TestData(getClassLoaderResource("/tga/FLAG_B24.TGA"), new Dimension(124, 124)), // Uncompressed 24 bit BGR bottom/up
                new TestData(getClassLoaderResource("/tga/FLAG_B32.TGA"), new Dimension(124, 124)), // Uncompressed 32 bit BGRX bottom/up
                new TestData(getClassLoaderResource("/tga/FLAG_T16.TGA"), new Dimension(124, 124)), // Uncompressed 16 bit BGR top/down
//                new TestData(getClassLoaderResource("/tga/FLAG_T24.TGA"), new Dimension(124, 124)), // Uncompressed 24 bit BGR top/down (missing from file set)
                new TestData(getClassLoaderResource("/tga/FLAG_T32.TGA"), new Dimension(124, 124)), // Uncompressed 32 bit BGRX top/down

                new TestData(getClassLoaderResource("/tga/XING_B16.TGA"), new Dimension(240, 164)), // Uncompressed 16 bit BGR bottom/up
                new TestData(getClassLoaderResource("/tga/XING_B24.TGA"), new Dimension(240, 164)), // Uncompressed 24 bit BGR bottom/up
                new TestData(getClassLoaderResource("/tga/XING_B32.TGA"), new Dimension(240, 164)), // Uncompressed 32 bit BGRX bottom/up
                new TestData(getClassLoaderResource("/tga/XING_T16.TGA"), new Dimension(240, 164)), // Uncompressed 16 bit BGR top/down
                new TestData(getClassLoaderResource("/tga/XING_T24.TGA"), new Dimension(240, 164)), // Uncompressed 24 bit BGR top/down
                new TestData(getClassLoaderResource("/tga/XING_T32.TGA"), new Dimension(240, 164)), // Uncompressed 32 bit BGRX top/down

                new TestData(getClassLoaderResource("/tga/autodesk-3dsmax-extsize494.tga"), new Dimension(440, 200)),  // RLE compressed 32 bit BGRA bottom/up, with extension area size 494

                new TestData(getClassLoaderResource("/tga/monochrome16_top_left.tga"), new Dimension(64, 64)),  // Uncompressed 16 bit monochrome
                new TestData(getClassLoaderResource("/tga/monochrome16_top_left_rle.tga"), new Dimension(64, 64)),  // RLE compressed 16 bit monochrome

                new TestData(getClassLoaderResource("/tga/692c33d1-d0c3-4fe2-a059-f199d063bc7a.tga"), new Dimension(256, 256)), // Uncompressed BGR, with colorMapDepth set to 24
                new TestData(getClassLoaderResource("/tga/0112eccd-2c29-4368-bcef-59c823b6e5d1.tga"), new Dimension(256, 256)), // RLE compressed BGR, with extension area size 0
                new TestData(getClassLoaderResource("/tga/alpha-no-extension.tga"), new Dimension(167, 64)) // Uncompressed BGRA, without extension area
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("TGA", "tga", "TARGA", "targa");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("tga", "tpic");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(
                "image/tga", "image/x-tga",
                "image/targa", "image/x-targa"
        );
    }

    @Test
    public void testSubsampling() throws IOException {
        ImageReader reader = createReader();
        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 5, 0, 0);

        for (TestData testData : getTestData()) {
            try (ImageInputStream input = testData.getInputStream()) {
                reader.setInput(input);
                assertNotNull(reader.read(0, param));
            }
            finally {
                reader.reset();
            }
        }

        reader.dispose();
    }

    @Test
    public void testAlpha() throws IOException {
        ImageReader reader = createReader();

        // These samples have "attribute bits" that are *NOT* alpha, according to the extension area
        for (String sample : Arrays.asList("/tga/UTC16.TGA", "/tga/UTC32.TGA", "/tga/CTC16.TGA", "/tga/CTC32.TGA")) {
            try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource(sample))) {
                reader.setInput(input);

                BufferedImage image = reader.read(0);

                String message = String.format("RGB value differs for sample '%s'", sample);
                assertRGBEquals(message, 0xffff0000, image.getRGB(0, 0), 0);
                assertRGBEquals(message, 0xff00ff00, image.getRGB(8, 0), 0);
                assertRGBEquals(message, 0xff0000ff, image.getRGB(16, 0), 0);
                assertRGBEquals(message, 0xff000000, image.getRGB(24, 0), 0);
                assertRGBEquals(message, 0xffff0000, image.getRGB(32, 0), 0);
                assertRGBEquals(message, 0xff00ff00, image.getRGB(40, 0), 0);
                assertRGBEquals(message, 0xff0000ff, image.getRGB(48, 0), 0);
                assertRGBEquals(message, 0xffffffff, image.getRGB(56, 0), 0);
            }
        }

        // This sample has "attribute bits" that *ARE* alpha, and there is no extension area
        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tga/alpha-no-extension.tga"))) {
            reader.setInput(input);

            BufferedImage image = reader.read(0);

            String message = "RGB value differs for sample '/tga/alpha-no-extension.tga'";
            assertRGBEquals(message, 0x00ffffff, image.getRGB(0, 0), 0);
            assertRGBEquals(message, 0xffffffff, image.getRGB(48, 14), 0);
        }

        reader.dispose();
    }

    @Test
    public void testMetadataTextEntries() throws IOException {
        ImageReader reader = createReader();

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/tga/autodesk-3dsmax-extsize494.tga"))) {
            reader.setInput(input);
            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

            NodeList textEntries = root.getElementsByTagName("TextEntry");
            boolean softwareFound = false;

            for (int i = 0; i < root.getLength(); i++) {
                IIOMetadataNode software = (IIOMetadataNode) textEntries.item(i);

                if ("Software".equals(software.getAttribute("keyword"))) {
                    assertEquals("Autodesk 3ds max 1.0", software.getAttribute("value"));
                    softwareFound = true;
                    break;
                }
            }

            assertTrue("No Software TextEntry", softwareFound);
        }
        finally {
            reader.dispose();
        }
    }
}
