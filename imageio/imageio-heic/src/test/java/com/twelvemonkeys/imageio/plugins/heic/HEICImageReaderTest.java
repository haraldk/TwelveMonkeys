/*
 * Copyright (c) 2015, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.heic;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HEICImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HEICImageReaderTest.java,v 1.0 03.07.14 22:28 haraldk Exp$
 */
public class HEICImageReaderTest extends ImageReaderAbstractTest<HEICImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new HEICImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Collections.singletonList(
                new TestData(getClassLoaderResource("/heic/random_collection_1440x960.heic"),
                        // NOTE: The 240x160 thumbnails are exposed through the thumbnail API
                        new Dimension(1440, 960),
                        new Dimension(1440, 960),
                        new Dimension(1440, 960),
                        new Dimension(1440, 960)
                )
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("HEIC", "heic");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("heic");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(
                "image/heic", "image/x-heic"
        );
    }

    // HEIC specific tests

    private HEICImageReader createReaderWithTestData() throws IOException {
        HEICImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/heic/random_collection_1440x960.heic")));
        return reader;
    }

    @Test
    public void testNumImagesExcludesThumbnailsAndAuxiliary() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            // The file contains 4 main images + 4 thumbnails,
            // only the main images should be exposed as images
            assertEquals(4, reader.getNumImages(true));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testThumbnails() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            assertTrue(reader.readerSupportsThumbnails());

            for (int i = 0; i < reader.getNumImages(true); i++) {
                assertEquals(1, reader.getNumThumbnails(i), "image " + i);
                assertEquals(240, reader.getThumbnailWidth(i, 0), "image " + i);
                assertEquals(160, reader.getThumbnailHeight(i, 0), "image " + i);

                BufferedImage thumbnail = reader.readThumbnail(i, 0);
                assertNotNull(thumbnail);
                assertEquals(240, thumbnail.getWidth());
                assertEquals(160, thumbnail.getHeight());
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testThumbnailIndexOutOfBounds() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            assertThrows(IndexOutOfBoundsException.class, () -> reader.readThumbnail(0, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> reader.readThumbnail(0, -1));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testThumbnailResemblesImage() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            BufferedImage image = reader.read(0);
            BufferedImage thumbnail = reader.readThumbnail(0, 0);

            // Thumbnail is a lossy 6:1 downscale, allow generous tolerance
            assertRGBEquals("Thumbnail should resemble image", image.getRGB(720, 480), thumbnail.getRGB(120, 80), 96);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadWithSourceRegion() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            BufferedImage full = reader.read(0);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(100, 100, 300, 200));
            BufferedImage region = reader.read(0, param);

            assertEquals(300, region.getWidth());
            assertEquals(200, region.getHeight());
            assertEquals(full.getRGB(100, 100), region.getRGB(0, 0));
            assertEquals(full.getRGB(399, 299), region.getRGB(299, 199));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadWithDestinationOffset() throws IOException {
        // Regression test: destination offset used to be ignored, pixels written at (0, 0) with wrong stride
        HEICImageReader reader = createReaderWithTestData();
        try {
            BufferedImage full = reader.read(0);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(0, 0, 200, 200));
            param.setDestinationOffset(new Point(50, 50));
            BufferedImage image = reader.read(0, param);

            assertEquals(250, image.getWidth());
            assertEquals(250, image.getHeight());
            assertEquals(full.getRGB(0, 0), image.getRGB(50, 50));
            assertEquals(full.getRGB(199, 199), image.getRGB(249, 249));
            assertEquals(0, image.getRGB(0, 0) & 0xffffff, "Area outside destination offset should be untouched");
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadWithUserSuppliedDestination() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            BufferedImage full = reader.read(0);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(100, 100, 300, 300));
            param.setDestinationOffset(new Point(100, 100));
            BufferedImage destination = new BufferedImage(full.getWidth(), full.getHeight(), BufferedImage.TYPE_INT_RGB);
            param.setDestination(destination);

            assertSame(destination, reader.read(0, param));
            assertEquals(full.getRGB(100, 100), destination.getRGB(100, 100));
            assertEquals(full.getRGB(399, 399), destination.getRGB(399, 399));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadWithSubsamplingAndDestinationOffset() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            BufferedImage full = reader.read(0);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(0, 0, 400, 400));
            param.setSourceSubsampling(2, 2, 0, 0);
            param.setDestinationOffset(new Point(30, 30));
            BufferedImage image = reader.read(0, param);

            assertEquals(230, image.getWidth());
            assertEquals(230, image.getHeight());
            assertEquals(full.getRGB(0, 0), image.getRGB(30, 30));
            assertEquals(full.getRGB(398, 398), image.getRGB(229, 229));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testStandardMetadataCompression() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            IIOMetadata metadata = reader.getImageMetadata(0);
            assertNotNull(metadata);
            assertTrue(metadata.isStandardMetadataFormatSupported());

            Node root = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            Element compressionTypeName = findChild(root, "Compression", "CompressionTypeName");
            assertNotNull(compressionTypeName);
            assertEquals("HEVC", compressionTypeName.getAttribute("value"));

            Element lossless = findChild(root, "Compression", "Lossless");
            assertNotNull(lossless);
            assertEquals("FALSE", lossless.getAttribute("value"));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testStandardMetadataChroma() throws IOException {
        HEICImageReader reader = createReaderWithTestData();
        try {
            Node root = reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

            Element colorSpaceType = findChild(root, "Chroma", "ColorSpaceType");
            assertNotNull(colorSpaceType);
            assertEquals("RGB", colorSpaceType.getAttribute("name"));

            Element numChannels = findChild(root, "Chroma", "NumChannels");
            assertNotNull(numChannels);
            assertEquals("3", numChannels.getAttribute("value"));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testCanDecodeRejectsNonHEICIsoBmff() throws IOException {
        // ISO BMFF container, but not a HEIC file ('isom' brand as in an MP4 file),
        // should return false without throwing exception
        byte[] isoBmff = {
                0, 0, 0, 20, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm',
                0, 0, 2, 0, 'i', 's', 'o', 'm'
        };

        try (ImageInputStream stream = new ByteArrayImageInputStream(isoBmff)) {
            assertFalse(createProvider().canDecodeInput(stream));
        }
    }

    private static Element findChild(Node root, String... path) {
        Node node = root;

        for (String name : path) {
            Node found = null;
            for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeName().equals(name)) {
                    found = child;
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            node = found;
        }

        return (Element) node;
    }
}
