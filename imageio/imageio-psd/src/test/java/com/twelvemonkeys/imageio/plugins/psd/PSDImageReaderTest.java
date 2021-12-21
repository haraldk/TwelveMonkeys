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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;

import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static org.junit.Assert.*;

/**
 * PSDImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class PSDImageReaderTest extends ImageReaderAbstractTest<PSDImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new PSDImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                // 5 channel, RGB
                new TestData(getClassLoaderResource("/psd/photoshopping.psd"), new Dimension(300, 225)),
                // 1 channel, gray, 8 bit samples
                new TestData(getClassLoaderResource("/psd/buttons.psd"), new Dimension(20, 20)),
                // 5 channel, CMYK
                new TestData(getClassLoaderResource("/psd/escenic-liquid-logo.psd"), new Dimension(595, 420)),
                // 3 channel RGB, "no composite layer"
                new TestData(getClassLoaderResource("/psd/jugware-icon.psd"), new Dimension(128, 128)),
                // 3 channel RGB, old data, no layer info/mask
                new TestData(getClassLoaderResource("/psd/MARBLES.PSD"), new Dimension(1419, 1001)),
                // 1 channel, indexed color
                new TestData(getClassLoaderResource("/psd/coral_fish.psd"), new Dimension(800, 800)),
                // 1 channel, bitmap, 1 bit samples
                new TestData(getClassLoaderResource("/psd/test_bitmap.psd"), new Dimension(710, 512)),
                // 1 channel, gray, 16 bit samples
                new TestData(getClassLoaderResource("/psd/test_gray16.psd"), new Dimension(710, 512)),
                // 4 channel, CMYK, 16 bit samples
                new TestData(getClassLoaderResource("/psd/cmyk_16bits.psd"), new Dimension(1000, 275)),
                // 3 channel, RGB, 8 bit samples ("Large Document Format" aka PSB)
                new TestData(getClassLoaderResource("/psb/test_original.psb"), new Dimension(710, 512)),
                // From http://telegraphics.com.au/svn/psdparse/trunk/psd/
                new TestData(getClassLoaderResource("/psd/adobehq.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq_ind.psd"), new Dimension(341, 512)),
                // Contains a shorter than normal PrintFlags chunk
                new TestData(getClassLoaderResource("/psd/adobehq-2.5.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq-3.0.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq-5.5.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq-7.0.psd"), new Dimension(341, 512)),
                // From https://github.com/kmike/psd-tools/tree/master/tests/psd_files
                new TestData(getClassLoaderResource("/psd/masks2.psd"), new Dimension(640, 1136)),
                // RGB, multiple alpha channels, no transparency
                new TestData(getClassLoaderResource("/psd/rgb-multichannel-no-transparency.psd"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/psb/rgb-multichannel-no-transparency.psb"), new Dimension(100, 100)),
                // CMYK, uncompressed + contains some uncommon MeSa (instead of 8BIM) resource blocks
                new TestData(getClassLoaderResource("/psd/fruit-cmyk-MeSa-resource.psd"), new Dimension(400, 191)),
                // 3 channel, RGB, 32 bit samples
                new TestData(getClassLoaderResource("/psd/32bit5x5.psd"), new Dimension(5, 5))
                // TODO: Need more recent ZIP compressed PSD files from CS2/CS3+
        );
    }

    @Override
    protected List<TestData> getTestDataForAffineTransformOpCompatibility() {
        return Arrays.asList(
                // 5 channel, RGB
                new TestData(getClassLoaderResource("/psd/photoshopping.psd"), new Dimension(300, 225)),
                // 1 channel, gray, 8 bit samples
                new TestData(getClassLoaderResource("/psd/buttons.psd"), new Dimension(20, 20)),
                // 3 channel RGB, "no composite layer"
                new TestData(getClassLoaderResource("/psd/jugware-icon.psd"), new Dimension(128, 128)),
                // 3 channel RGB, old data, no layer info/mask
                new TestData(getClassLoaderResource("/psd/MARBLES.PSD"), new Dimension(1419, 1001)),
                // 1 channel, indexed color
                new TestData(getClassLoaderResource("/psd/coral_fish.psd"), new Dimension(800, 800)),
                // 1 channel, bitmap, 1 bit samples
                new TestData(getClassLoaderResource("/psd/test_bitmap.psd"), new Dimension(710, 512)),
                // 1 channel, gray, 16 bit samples
                new TestData(getClassLoaderResource("/psd/test_gray16.psd"), new Dimension(710, 512)),
                // 3 channel, RGB, 8 bit samples ("Large Document Format" aka PSB)
                new TestData(getClassLoaderResource("/psb/test_original.psb"), new Dimension(710, 512)),
                // From http://telegraphics.com.au/svn/psdparse/trunk/psd/
                new TestData(getClassLoaderResource("/psd/adobehq.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq_ind.psd"), new Dimension(341, 512)),
                // Contains a shorter than normal PrintFlags chunk
                new TestData(getClassLoaderResource("/psd/adobehq-2.5.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq-3.0.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq-5.5.psd"), new Dimension(341, 512)),
                new TestData(getClassLoaderResource("/psd/adobehq-7.0.psd"), new Dimension(341, 512)),
                // From https://github.com/kmike/psd-tools/tree/master/tests/psd_files
                new TestData(getClassLoaderResource("/psd/masks2.psd"), new Dimension(640, 1136)),
                // RGB, multiple alpha channels, no transparency
                new TestData(getClassLoaderResource("/psd/rgb-multichannel-no-transparency.psd"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/psb/rgb-multichannel-no-transparency.psb"), new Dimension(100, 100))
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Collections.singletonList("psd");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("psd");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(
                "image/vnd.adobe.photoshop",
                "application/vnd.adobe.photoshop",
                "image/x-psd"
        );
    }

    @Test
    public void testSupportsThumbnail() throws IOException {
        PSDImageReader imageReader = createReader();
        assertTrue(imageReader.readerSupportsThumbnails());
    }

    @Test
    public void testThumbnailReading() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(0).getInputStream()) {
            imageReader.setInput(stream);

            assertEquals(1, imageReader.getNumThumbnails(0));

            BufferedImage thumbnail = imageReader.readThumbnail(0, 0);
            assertNotNull(thumbnail);

            assertEquals(128, thumbnail.getWidth());
            assertEquals(96, thumbnail.getHeight());
        }
    }

    @Test
    public void testThumbnailReadingNoInput() throws IOException {
        PSDImageReader imageReader = createReader();

        try {
            imageReader.getNumThumbnails(0);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("input"));
        }

        try {
            imageReader.getThumbnailWidth(0, 0);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("input"));
        }

        try {
            imageReader.getThumbnailHeight(0, 0);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("input"));
        }

        try {
            imageReader.readThumbnail(0, 0);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("input"));
        }
    }

    @Test
    public void testThumbnailReadingOutOfBounds() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(0).getInputStream()) {
            imageReader.setInput(stream);

            int numImages = imageReader.getNumImages(true);

            try {
                imageReader.getNumThumbnails(numImages + 1);
                fail("Expected IndexOutOfBoundsException");
            }
            catch (IndexOutOfBoundsException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().toLowerCase().contains("index"));
            }

            try {
                imageReader.getThumbnailWidth(-1, 0);
                fail("Expected IndexOutOfBoundsException");
            }
            catch (IndexOutOfBoundsException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().toLowerCase().contains("index"));
            }

            try {
                imageReader.getThumbnailHeight(0, -2);
                fail("Expected IndexOutOfBoundsException");
            }
            catch (IndexOutOfBoundsException expected) {
                // Sloppy...
                assertTrue(expected.getMessage(), expected.getMessage().toLowerCase().contains("-2"));
            }

            try {
                imageReader.readThumbnail(numImages + 99, 42);
                fail("Expected IndexOutOfBoundsException");
            }
            catch (IndexOutOfBoundsException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().toLowerCase().contains("index"));
            }
        }
    }

    @Test
    public void testThumbnailDimensions() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(0).getInputStream()) {
            imageReader.setInput(stream);

            assertEquals(1, imageReader.getNumThumbnails(0));

            assertEquals(128, imageReader.getThumbnailWidth(0, 0));
            assertEquals(96, imageReader.getThumbnailHeight(0, 0));
        }
    }

    @Test
    public void testThumbnailReadListeners() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(0).getInputStream()) {
            imageReader.setInput(stream);

            final List<Object> seqeunce = new ArrayList<>();
            imageReader.addIIOReadProgressListener(new ProgressListenerBase() {
                private float lastPercentageDone = 0;

                @Override
                public void thumbnailStarted(final ImageReader pSource, final int pImageIndex, final int pThumbnailIndex) {
                    seqeunce.add("started");
                }

                @Override
                public void thumbnailComplete(final ImageReader pSource) {
                    seqeunce.add("complete");
                }

                @Override
                public void thumbnailProgress(final ImageReader pSource, final float pPercentageDone) {
                    // Optional
                    assertEquals("Listener invoked out of sequence", 1, seqeunce.size());
                    assertTrue(pPercentageDone >= lastPercentageDone);
                    lastPercentageDone = pPercentageDone;
                }
            });

            BufferedImage thumbnail = imageReader.readThumbnail(0, 0);
            assertNotNull(thumbnail);

            assertEquals("Listeners not invoked", 2, seqeunce.size());
            assertEquals("started", seqeunce.get(0));
            assertEquals("complete", seqeunce.get(1));
        }
    }

    @Test
    public void testReadLayers() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(3).getInputStream()) {
            imageReader.setInput(stream);

            int numImages = imageReader.getNumImages(true);

            assertEquals(3, numImages);

            for (int i = 0; i < numImages; i++) {
                BufferedImage image = imageReader.read(i);
                assertNotNull(image);

                // Make sure layers are correct size
                assertEquals(image.getWidth(), imageReader.getWidth(i));
                assertEquals(image.getHeight(), imageReader.getHeight(i));
            }
        }
    }

    @Test
    public void testImageTypesLayers() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(3).getInputStream()) {
            imageReader.setInput(stream);

            int numImages = imageReader.getNumImages(true);
            for (int i = 0; i < numImages; i++) {
                ImageTypeSpecifier rawType = imageReader.getRawImageType(i);
                assertNotNull(rawType);

                Iterator<ImageTypeSpecifier> types = imageReader.getImageTypes(i);

                assertNotNull(types);
                assertTrue(types.hasNext());

                boolean found = false;

                while (types.hasNext()) {
                    ImageTypeSpecifier type = types.next();

                    if (!found && (rawType == type || rawType.equals(type))) {
                        found = true;
                    }
                }

                assertTrue("RAW image type not in type iterator", found);
            }
        }
    }

    @Test
    public void testReadLayersExplicitType() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(3).getInputStream()) {
            imageReader.setInput(stream);

            int numImages = imageReader.getNumImages(true);
            for (int i = 0; i < numImages; i++) {
                Iterator<ImageTypeSpecifier> types = imageReader.getImageTypes(i);

                while (types.hasNext()) {
                    ImageTypeSpecifier type = types.next();
                    ImageReadParam param = imageReader.getDefaultReadParam();
                    param.setDestinationType(type);
                    BufferedImage image = imageReader.read(i, param);

                    assertEquals(type.getBufferedImageType(), image.getType());

                    if (type.getBufferedImageType() == 0) {
                        // TODO: If type.getBIT == 0, test more
                        // Compatible color model
                        assertEquals(type.getNumComponents(), image.getColorModel().getNumComponents());

                        // Same color space
                        assertEquals(type.getColorModel().getColorSpace(), image.getColorModel().getColorSpace());

                        // Same number of samples
                        assertEquals(type.getNumBands(), image.getSampleModel().getNumBands());

                        // Same number of bits/sample
                        for (int j = 0; j < type.getNumBands(); j++) {
                            assertEquals(type.getBitsPerBand(j), image.getSampleModel().getSampleSize(j));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testReadLayersExplicitDestination() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = getTestData().get(3).getInputStream()) {
            imageReader.setInput(stream);

            int numImages = imageReader.getNumImages(true);
            for (int i = 0; i < numImages; i++) {
                Iterator<ImageTypeSpecifier> types = imageReader.getImageTypes(i);
                int width = imageReader.getWidth(i);
                int height = imageReader.getHeight(i);

                while (types.hasNext()) {
                    ImageTypeSpecifier type = types.next();
                    ImageReadParam param = imageReader.getDefaultReadParam();
                    BufferedImage destination = type.createBufferedImage(width, height);
                    param.setDestination(destination);

                    BufferedImage image = imageReader.read(i, param);

                    assertSame(destination, image);
                }
            }
        }
    }

    @Test
    public void testGrayAlphaLayers() throws IOException {
        PSDImageReader imageReader = createReader();

        // The expected colors for each layer
        int[] colors = new int[] {
                -1, // Don't care
                0xff000000,
                0xffffffff,
                0xff737373,
                0xff3c3c3c,
                0xff656565,
                0xffc9c9c9,
                0xff979797,
                0xff5a5a5a
        };

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/psd/test_grayscale_boxes.psd"))) {
            imageReader.setInput(stream);

            int numImages = imageReader.getNumImages(true);
            assertEquals(colors.length, numImages);

            // Skip reading the merged composite image
            for (int i = 1; i < numImages; i++) {
                Iterator<ImageTypeSpecifier> types = imageReader.getImageTypes(i);
                int width = imageReader.getWidth(i);
                int height = imageReader.getHeight(i);

                while (types.hasNext()) {
                    ImageTypeSpecifier type = types.next();

                    ImageReadParam param = imageReader.getDefaultReadParam();
                    BufferedImage destination = type.createBufferedImage(width, height);
                    param.setDestination(destination);

                    BufferedImage image = imageReader.read(i, param);

                    assertSame(destination, image);

                    // NOTE: Allow some slack, as Java 1.7 and 1.8 color management differs slightly
                    int rgb = image.getRGB(0, 0);
                    assertRGBEquals("Colors differ", colors[i], rgb, 1);
                }
            }
        }
    }

    @Test
    public void testMultiChannelNoTransparencyPSD() throws IOException {
        PSDImageReader imageReader = createReader();

        // The following PSD is RGB, has 4 channels (1 alpha/auxillary channel), but should be treated as opaque
        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/psd/rgb-multichannel-no-transparency.psd"))) {
            imageReader.setInput(stream);

            BufferedImage image = imageReader.read(0);

            assertEquals(Transparency.OPAQUE, image.getTransparency());
        }
    }

    @Test
    public void testMultiChannelNoTransparencyPSB() throws IOException {
        PSDImageReader imageReader = createReader();

        // The following PSB is RGB, has 4 channels (1 alpha/auxiliary channel), but should be treated as opaque
        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/psb/rgb-multichannel-no-transparency.psb"))) {
            imageReader.setInput(stream);

            BufferedImage image = imageReader.read(0);

            assertEquals(Transparency.OPAQUE, image.getTransparency());
        }
    }

    @Test
    public void testReadUnicodeLayerName() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/psd/long-layer-names.psd"))) {
            imageReader.setInput(stream);

            IIOMetadata metadata = imageReader.getImageMetadata(0);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(PSDMetadata.NATIVE_METADATA_FORMAT_NAME);
            NodeList layerInfo = root.getElementsByTagName("LayerInfo");

            assertEquals(1, layerInfo.getLength()); // Sanity
            assertEquals("If_The_Layer_Name_Is_Really_Long_Oh_No_What_Do_I_Do", ((IIOMetadataNode) layerInfo.item(0)).getAttribute("name"));
        }
    }

    @Test
    public void testGroupLayerRead() throws IOException {
        PSDImageReader imageReader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/psd/layer_group_32bit5x5.psd"))) {
            imageReader.setInput(stream);

            IIOMetadata metadata = imageReader.getImageMetadata(0);
            List<PSDLayerInfo> layerInfos = ((PSDMetadata) metadata).layerInfo;

            assertEquals(layerInfos.size(), 5);

            PSDLayerInfo groupedLayer = null;
            PSDLayerInfo groupLayer = null;
            PSDLayerInfo sectionDividerLayer = null;
            for (PSDLayerInfo layerInfo : layerInfos) {
                if (layerInfo.getLayerId() == 5) {
                    groupedLayer = layerInfo;
                }

                if (layerInfo.getLayerId() == 6) {
                    groupLayer = layerInfo;
                }

                if (layerInfo.getLayerId() == 7) {
                    sectionDividerLayer = layerInfo;
                }
            }

            assertNotNull(groupedLayer);
            assertEquals((int) groupedLayer.groupLayerId, 6);
            assertEquals(groupedLayer.group, false);
            assertEquals(groupedLayer.sectionDivider, false);

            assertNotNull(groupLayer);
            assertNull(groupLayer.groupLayerId);
            assertEquals(groupLayer.group, true);
            assertEquals(groupLayer.sectionDivider, false);

            assertNotNull(sectionDividerLayer);
            assertNull(sectionDividerLayer.groupLayerId);
            assertEquals(sectionDividerLayer.group, false);
            assertEquals(sectionDividerLayer.sectionDivider, true);

        }
    }
}