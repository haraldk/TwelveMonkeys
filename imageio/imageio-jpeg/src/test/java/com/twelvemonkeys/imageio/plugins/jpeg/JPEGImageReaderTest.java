/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  Neither the name "TwelveMonkeys" nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterThan;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * JPEGImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReaderTest.java,v 1.0 24.01.11 22.04 haraldk Exp$
 */
public class JPEGImageReaderTest extends ImageReaderAbstractTest<JPEGImageReader> {

    protected static final JPEGImageReaderSpi SPI = new JPEGImageReaderSpi(lookupDelegateProvider());

    protected static ImageReaderSpi lookupDelegateProvider() {
        return JPEGImageReaderSpi.lookupDelegateProvider(IIORegistry.getDefaultInstance());
    }

    @Override
    protected List<TestData> getTestData() {
        // While a lot of these files don't conform to any spec (Exif/JFIF), we will read these.
        return Arrays.asList(
                new TestData(getClassLoaderResource("/jpeg/cmm-exception-adobe-rgb.jpg"), new Dimension(626, 76)),
                new TestData(getClassLoaderResource("/jpeg/cmm-exception-srgb.jpg"), new Dimension(1800, 1200)),
                new TestData(getClassLoaderResource("/jpeg/corrupted-icc-srgb.jpg"), new Dimension(1024, 685)),
                new TestData(getClassLoaderResource("/jpeg/gray-sample.jpg"), new Dimension(386, 396)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample.jpg"), new Dimension(160, 227)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), new Dimension(2707, 3804)),
                new TestData(getClassLoaderResource("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg"), new Dimension(640, 480)),
                new TestData(getClassLoaderResource("/jpeg/jfif-padded-segments.jpg"), new Dimension(20, 45)),
                new TestData(getClassLoaderResource("/jpeg/0x00-to-0xFF-between-segments.jpg"), new Dimension(16, 16)),
                new TestData(getClassLoaderResource("/jpeg/jfif-bogus-empty-jfif-segment.jpg"), new Dimension(942, 714)),
                new TestData(getClassLoaderResource("/jpeg/jfif-16bit-dqt.jpg"), new Dimension(204, 131))
        );

        // More test data in specific tests below
    }

    protected List<TestData> getBrokenTestData() {
        // These files are considered too broken to be read (ie. most other software does not read them either).
        return Arrays.asList(
                new TestData(getClassLoaderResource("/broken-jpeg/broken-bogus-segment-length.jpg"), new Dimension(467, 612)), // Semi-readable, parts missing
                new TestData(getClassLoaderResource("/broken-jpeg/broken-adobe-marker-bad-length.jpg"), new Dimension(1800, 1200)), // Unreadable, segment lengths are wrong
                new TestData(getClassLoaderResource("/broken-jpeg/broken-invalid-adobe-ycc-gray.jpg"), new Dimension(11, 440)), // Image readable, broken metadata (fixable?)
                new TestData(getClassLoaderResource("/broken-jpeg/broken-no-sof-ascii-transfer-mode.jpg"), new Dimension(-1, -1)), // Unreadable, can't find SOFn marker
                new TestData(getClassLoaderResource("/broken-jpeg/broken-sos-before-sof.jpg"), new Dimension(-1, -1)), // Unreadable, can't find SOFn marker
                new TestData(getClassLoaderResource("/broken-jpeg/broken-adobe-segment-length-beyond-eof.jpg"), new Dimension(-1, -1)) // Unreadable, no EOI
        );

        // More test data in specific tests below
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return SPI;
    }

    @Override
    protected JPEGImageReader createReader() {
        try {
            return (JPEGImageReader) SPI.createReaderInstance();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Class<JPEGImageReader> getReaderClass() {
        return JPEGImageReader.class;
    }

    @Override
    protected boolean allowsNullRawImageType() {
        return true;
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("JPEG", "jpeg", "JPG", "jpg");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("jpeg", "jpg");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/jpeg");
    }

    // TODO: Test that subsampling is actually reading something

    // Special cases found in the wild below

    @Test
    public void testICCProfileCMYKClassOutputColors() throws IOException {
        // Make sure ICC profile with class output isn't converted to too bright values
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cmyk-sample-custom-icc-bright.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(800, 800, 64, 8));
        param.setSourceSubsampling(8, 8, 2, 2);

        BufferedImage image = reader.read(0, param);
        assertNotNull(image);

        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        byte[] expectedData = {34, 37, 34, 47, 47, 44, 22, 26, 28, 23, 26, 28, 20, 23, 26, 20, 22, 25, 22, 25, 27, 18, 21, 24};

        assertEquals(expectedData.length, data.length);

        assertJPEGPixelsEqual(expectedData, data, 0);

        reader.dispose();
    }

    private static void assertJPEGPixelsEqual(byte[] expected, byte[] actual, int actualOffset) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(String.format("Difference in pixel %d", i), expected[i], actual[i + actualOffset], 5);
        }
    }

    @Test
    public void testICCDuplicateSequence() throws IOException {
        // Variation of the above, file contains multiple ICC chunks, with all counts and sequence numbers == 1
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-icc-duplicate-sequence-numbers-rgb-internal-kodak-srgb-jfif.jpg")));

        assertEquals(345, reader.getWidth(0));
        assertEquals(540, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(345, image.getWidth());
        assertEquals(540, image.getHeight());

        reader.dispose();
    }

    @Test
    public void testICCDuplicateSequenceZeroBased() throws IOException {
        // File contains multiple ICC chunks, with all counts and sequence numbers == 0
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-icc-duplicate-sequence-numbers-rgb-xerox-dc250-heavyweight-1-progressive-jfif.jpg")));

        assertEquals(3874, reader.getWidth(0));
        assertEquals(5480, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(0, 0, 3874, 16)); // Save some memory
        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(3874, image.getWidth());
        assertEquals(16, image.getHeight());

        reader.dispose();
    }

    @Test
    public void testTruncatedICCProfile() throws IOException {
        // File contains single 'ICC_PROFILE' chunk, with a truncated (32 000 bytes) "Europe ISO Coated FOGRA27" ICC profile (by Adobe).
        // Profile should have been about 550 000 bytes, split into multiple chunks. Written by GIMP 2.6.11
        // See: https://bugzilla.redhat.com/show_bug.cgi?id=695246
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cmm-exception-invalid-icc-profile-data.jpg")));

        assertEquals(1993, reader.getWidth(0));
        assertEquals(1038, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(reader.getWidth(0), 8));
        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(1993, image.getWidth());
        assertEquals(8, image.getHeight());

        reader.dispose();
    }

    @Test
    public void testCCOIllegalArgument() throws IOException {
        // File contains CMYK ICC profile ("Coated FOGRA27 (ISO 12647-2:2004)"), but image data is 3 channel YCC/RGB
        // JFIF 1.1 with unknown origin.
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cco-illegalargument-rgb-coated-fogra27.jpg")));

        assertEquals(281, reader.getWidth(0));
        assertEquals(449, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(281, image.getWidth());
        assertEquals(449, image.getHeight());

        // TODO: Need to test colors!
        reader.dispose();
    }

    @Test
    public void testNoImageTypesRGBWithCMYKProfile() throws IOException {
        // File contains CMYK ICC profile ("U.S. Web Coated (SWOP) v2") AND Adobe App14 specifying YCCK conversion (!),
        // but image data is plain 3 channel YCC/RGB.
        // EXIF/TIFF metadata says Software: "Microsoft Windows Photo Gallery 6.0.6001.18000"...
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/no-image-types-rgb-us-web-coated-v2-ms-photogallery-exif.jpg")));

        assertEquals(1743, reader.getWidth(0));
        assertEquals(2551, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(0, 0, 1743, 16)); // Save some memory
        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(1743, image.getWidth());
        assertEquals(16, image.getHeight());

        // TODO: Need to test colors!

        assertTrue(reader.hasThumbnails(0)); // Should not blow up!
    }

    @Test
    public void testCMYKWithRGBProfile() throws IOException {
        // File contains JFIF (!), RGB ICC profile AND Adobe App14 specifying unknown conversion,
        // but image data is 4 channel CMYK (from SOF0 channel Ids 'C', 'M', 'Y', 'K').
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-cmyk-invalid-icc-profile-srgb.jpg")));

        assertEquals(493, reader.getWidth(0));
        assertEquals(500, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(0, 0, 493, 16)); // Save some memory
        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(493, image.getWidth());
        assertEquals(16, image.getHeight());

        // TODO: Need to test colors!

        assertFalse(reader.hasThumbnails(0)); // Should not blow up!
    }

    @Test
    public void testWarningEmbeddedColorProfileInvalidIgnored() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/warning-embedded-color-profile-invalid-ignored-cmyk.jpg")));

        assertEquals(183, reader.getWidth(0));
        assertEquals(283, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(183, image.getWidth());
        assertEquals(283, image.getHeight());

        // TODO: Need to test colors!
    }

    @Test
    public void testEOFSOSSegment() throws IOException {
        // Regression...
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/eof-sos-segment-bug.jpg")));

        assertEquals(266, reader.getWidth(0));
        assertEquals(400, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(266, image.getWidth());
        assertEquals(400, image.getHeight());
    }

    @Test
    public void testInvalidICCSingleChunkBadSequence() throws IOException {
        // Regression
        // Single segment ICC profile, with chunk index/count == 0

        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-icc-single-chunk-bad-sequence-number.jpg")));

        assertEquals(1772, reader.getWidth(0));
        assertEquals(2126, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(reader.getWidth(0), 8));

        IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
        reader.addIIOReadWarningListener(warningListener);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(1772, image.getWidth());
        assertEquals(8, image.getHeight());

        verify(warningListener, atLeast(1)).warningOccurred(eq(reader), anyString());
    }

    @Test
    public void testYCbCrNotSubsampledNonstandardChannelIndexes() throws IOException {
        // Regression: Make sure 3 channel, non-subsampled JFIF, defaults to YCbCr, even if unstandard channel indexes
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-ycbcr-no-subsampling-intel.jpg")));

        assertEquals(600, reader.getWidth(0));
        assertEquals(600, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(8, 8));

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(8, image.getWidth());
        assertEquals(8, image.getHeight());

        // QnD test: Make sure all pixels are white (if treated as RGB, they will be pink-ish)
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0xffffff, image.getRGB(x, y) & 0xffffff);
            }
        }
    }

    @Test
    public void testCorbisRGB() throws IOException {
        // Special case, throws exception below without special treatment
        // java.awt.color.CMMException: General CMM error517
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cmm-exception-corbis-rgb.jpg")));

        assertEquals(512, reader.getWidth(0));
        assertEquals(384, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(512, image.getWidth());
        assertEquals(384, image.getHeight());

        reader.dispose();
    }

    @Ignore("Known issue in com.sun...JPEGMetadata")
    @Test
    public void testStandardMetadataColorSpaceTypeRGBForYCbCr() {
        // These reports RGB in standard metadata, while the data is really YCbCr.
        // Exif files are always YCbCr AFAIK.
        fail("/jpeg/exif-jpeg-thumbnail-sony-dsc-p150-inverted-colors.jpg");
        fail("/jpeg/exif-pspro-13-inverted-colors.jpg");
        // Not Exif, but same issue: SOF comp ids are JFIF standard 1-3 and
        // *should* be interpreted as YCbCr but isn't.
        // Possible fix for this, is to insert a fake JFIF segment, as this image
        // conforms to the JFIF spec (but it won't work for the Exif samples)
        fail("/jpeg/no-jfif-ycbcr.jpg");
    }

    @Test
    public void testBrokenReadRasterAfterGetMetadataException() throws IOException {
        // See issue 107, from PDFBox team
        JPEGImageReader reader = createReader();

        try {
            for (TestData broken : getBrokenTestData()) {
                reader.setInput(broken.getInputStream());

                try {
                    reader.getImageMetadata(0);
                }
                catch (IOException ignore) {
                    // Expected IOException here, due to broken file
//                    ignore.printStackTrace();
                }

                try {
                    reader.readRaster(0, null);
                }
                catch (IOException expected) {
                    // Should not throw anything other than IOException here
                    if (!(expected instanceof EOFException)) {
                        assertNotNull(expected.getMessage());
                    }
                }
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testBrokenRead() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            for (TestData broken : getBrokenTestData()) {
                reader.setInput(broken.getInputStream());

                try {
                    reader.read(0);
                }
                catch (IIOException expected) {
                    assertNotNull(expected.getMessage());
                }
                catch (IOException expected) {
                    if (!(expected instanceof EOFException)) {
                        assertNotNull(expected.getMessage());
                    }
                }
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testBrokenGetDimensions() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            for (TestData broken : getBrokenTestData()) {
                reader.setInput(broken.getInputStream());

                Dimension exptectedSize = broken.getDimension(0);

                try {
                    assertEquals(exptectedSize.width, reader.getWidth(0));
                    assertEquals(exptectedSize.height, reader.getHeight(0));
                }
                catch (IIOException expected) {
                    assertNotNull(expected.getMessage());
                }
                catch (IOException expected) {
                    if (!(expected instanceof EOFException)) {
                        assertNotNull(expected.getMessage());
                    }
                }
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testBrokenGetImageMetadata() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            for (TestData broken : getBrokenTestData()) {
                reader.setInput(broken.getInputStream());

                try {
                    reader.getImageMetadata(0);
                }
                catch (IIOException expected) {
                    assertNotNull(expected.getMessage());
                }
                catch (IOException expected) {
                    if (!(expected instanceof EOFException)) {
                        assertNotNull(expected.getMessage());
                    }
                }
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testImageMetadata1ChannelGrayWithBogusAdobeYCC() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            // Any sample should do here
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-adobe-ycc-gray-with-metadata.jpg")));
            IIOMetadata metadata = reader.getImageMetadata(0);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);

            IIOMetadataNode chroma = getSingleElementByName(root, "Chroma");
            IIOMetadataNode numChannels = getSingleElementByName(chroma, "NumChannels");
            assertEquals("1", numChannels.getAttribute("value"));
            IIOMetadataNode colorSpaceType = getSingleElementByName(chroma, "ColorSpaceType");
            assertEquals("GRAY", colorSpaceType.getAttribute("name"));
        }
        finally {
            reader.dispose();
        }
    }

    private IIOMetadataNode getSingleElementByName(final IIOMetadataNode root, final String name) {
        NodeList elements = root.getElementsByTagName(name);
        assertEquals(1, elements.getLength());
        return (IIOMetadataNode) elements.item(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetImageMetadataOutOfBounds() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            // Any sample should do here
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/gray-sample.jpg")));
            reader.getImageMetadata(-1);
        }
        finally {
            reader.dispose();
        }
    }

    @Test(expected = IIOException.class)
    public void testBrokenBogusSegmentLengthReadWithDestination() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/broken-jpeg/broken-bogus-segment-length.jpg")));

            assertEquals(467, reader.getWidth(0));
            assertEquals(612, reader.getHeight(0));

            ImageTypeSpecifier type = reader.getImageTypes(0).next();
            BufferedImage image = type.createBufferedImage(reader.getWidth(0), reader.getHeight(0));

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestination(image);

            try {
                reader.read(0, param);
            }
            catch (IOException e) {
                // Even if we get an exception here, the image should contain 10-15% of the image
                assertRGBEquals(0xffffffff, image.getRGB(0, 0));   // white area
                assertRGBEquals(0xff0000ff, image.getRGB(67, 22)); // blue area
                assertRGBEquals(0xffff00ff, image.getRGB(83, 22)); // purple area

                throw e;
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testHasThumbnailNoIFD1() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/srgb-exif-no-ifd1.jpg")));

        assertEquals(150, reader.getWidth(0));
        assertEquals(207, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(150, image.getWidth());
        assertEquals(207, image.getHeight());

        assertFalse(reader.hasThumbnails(0)); // Should just not blow up, even if the EXIF IFD1 is missing
    }

    @Test
    public void testJFIFRawRGBThumbnail() throws IOException {
        // JFIF with raw RGB thumbnail (+ EXIF thumbnail)
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-jfif-and-exif-thumbnail-sharpshot-iphone.jpg")));

        assertTrue(reader.hasThumbnails(0));
        assertEquals(2, reader.getNumThumbnails(0));

        // RAW JFIF
        assertEquals(131, reader.getThumbnailWidth(0, 0));
        assertEquals(122, reader.getThumbnailHeight(0, 0));

        BufferedImage rawJFIFThumb = reader.readThumbnail(0, 0);
        assertNotNull(rawJFIFThumb);
        assertEquals(131, rawJFIFThumb.getWidth());
        assertEquals(122, rawJFIFThumb.getHeight());

        // Exif (old thumbnail, from original image, should probably been removed by the software...)
        assertEquals(160, reader.getThumbnailWidth(0, 1));
        assertEquals(120, reader.getThumbnailHeight(0, 1));

        BufferedImage exifThumb = reader.readThumbnail(0, 1);
        assertNotNull(exifThumb);
        assertEquals(160, exifThumb.getWidth());
        assertEquals(120, exifThumb.getHeight());
    }

    // TODO: Test JFXX indexed thumbnail
    // TODO: Test JFXX RGB thumbnail

    @Test
    public void testJFXXJPEGThumbnail() throws IOException {
        // JFIF with JFXX JPEG encoded thumbnail
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg")));

        assertTrue(reader.hasThumbnails(0));
        assertEquals(1, reader.getNumThumbnails(0));
        assertEquals(80, reader.getThumbnailWidth(0, 0));
        assertEquals(60, reader.getThumbnailHeight(0, 0));

        BufferedImage thumbnail = reader.readThumbnail(0, 0);
        assertNotNull(thumbnail);
        assertEquals(80, thumbnail.getWidth());
        assertEquals(60, thumbnail.getHeight());
    }

    @Test
    public void testEXIFJPEGThumbnail() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg")));

        assertTrue(reader.hasThumbnails(0));
        assertEquals(1, reader.getNumThumbnails(0));
        assertEquals(114, reader.getThumbnailWidth(0, 0));
        assertEquals(160, reader.getThumbnailHeight(0, 0));

        BufferedImage thumbnail = reader.readThumbnail(0, 0);
        assertNotNull(thumbnail);
        assertEquals(114, thumbnail.getWidth());
        assertEquals(160, thumbnail.getHeight());
    }

    @Test
    public void testEXIFRawThumbnail() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/exif-rgb-thumbnail-sony-d700.jpg")));

        assertTrue(reader.hasThumbnails(0));
        assertEquals(1, reader.getNumThumbnails(0));
        assertEquals(80, reader.getThumbnailWidth(0, 0));
        assertEquals(60, reader.getThumbnailHeight(0, 0));

        BufferedImage thumbnail = reader.readThumbnail(0, 0);
        assertNotNull(thumbnail);
        assertEquals(80, thumbnail.getWidth());
        assertEquals(60, thumbnail.getHeight());
    }

    @Test
    public void testBadEXIFRawThumbnail() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg")));

        assertTrue(reader.hasThumbnails(0));
        assertEquals(1, reader.getNumThumbnails(0));
        assertEquals(96, reader.getThumbnailWidth(0, 0));
        assertEquals(72, reader.getThumbnailHeight(0, 0));

        BufferedImage thumbnail = reader.readThumbnail(0, 0);
        assertNotNull(thumbnail);
        assertEquals(96, thumbnail.getWidth());
        assertEquals(72, thumbnail.getHeight());
    }

    @Test
    public void testInvertedColors() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/exif-jpeg-thumbnail-sony-dsc-p150-inverted-colors.jpg")));

        assertEquals(2437, reader.getWidth(0));
        assertEquals(1662, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(0, 0, reader.getWidth(0), 8));
        BufferedImage strip = reader.read(0, param);

        assertNotNull(strip);
        assertEquals(2437, strip.getWidth());
        assertEquals(8, strip.getHeight());

        int[] expectedRGB = new int[] {
                0xffe9d0bc, 0xfff3decd, 0xfff5e6d3, 0xfff8ecdc, 0xfff8f0e5, 0xffe3ceb9, 0xff6d3923, 0xff5a2d18,
                0xff00170b, 0xff131311, 0xff52402c, 0xff624a30, 0xff6a4f34, 0xfffbf8f1, 0xfff4efeb, 0xffefeae6,
                0xffebe6e2, 0xffe3e0d9, 0xffe1d6d0, 0xff10100e
        };

        // Validate strip colors
        for (int i = 0; i < strip.getWidth() / 128; i++) {
            int actualRGB = strip.getRGB(i * 128, 4);
            assertRGBEquals(expectedRGB[i], actualRGB);
        }
    }

    @Test
    public void testThumbnailInvertedColors() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/exif-jpeg-thumbnail-sony-dsc-p150-inverted-colors.jpg")));

        assertTrue(reader.hasThumbnails(0));
        assertEquals(1, reader.getNumThumbnails(0));
        assertEquals(160, reader.getThumbnailWidth(0, 0));
        assertEquals(109, reader.getThumbnailHeight(0, 0));

        BufferedImage thumbnail = reader.readThumbnail(0, 0);
        assertNotNull(thumbnail);
        assertEquals(160, thumbnail.getWidth());
        assertEquals(109, thumbnail.getHeight());

        int[] expectedRGB = new int[] {
                0xffefd5c4, 0xffead3b1, 0xff55392d, 0xff55403b, 0xff6d635a, 0xff7b726b, 0xff68341f, 0xff5c2f1c,
                0xff250f12, 0xff6d7c77, 0xff414247, 0xff6a4f3a, 0xff6a4e39, 0xff564438, 0xfffcf7f1, 0xffefece7,
                0xfff0ebe7, 0xff464040, 0xffe3deda, 0xffd4cfc9,
        };

        // Validate strip colors
        for (int i = 0; i < thumbnail.getWidth() / 8; i++) {
            int actualRGB = thumbnail.getRGB(i * 8, 4);
            assertRGBEquals(expectedRGB[i], actualRGB);
        }
    }

    private List<TestData> getCMYKData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample.jpg"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-custom-icc-bright.jpg"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-no-icc.jpg"), new Dimension(100, 100))
        );
    }

    @Test
    public void testGetImageTypesCMYK() throws IOException {
        // Make sure CMYK images will report their embedded color profile among image types
        JPEGImageReader reader = createReader();

        List<TestData> cmykData = getCMYKData();

        for (TestData data : cmykData) {
            reader.setInput(data.getInputStream());
            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

            assertTrue(data + " has no image types", types.hasNext());

            boolean hasRGBType = false;
            boolean hasCMYKType = false;

            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();

                int csType = type.getColorModel().getColorSpace().getType();
                if (csType == ColorSpace.TYPE_RGB) {
                    hasRGBType = true;
                }
                else if (csType == ColorSpace.TYPE_CMYK) {
                    assertTrue("CMYK types should be delivered after RGB types (violates \"contract\" of more \"natural\" type first) for " + data, hasRGBType);

                    hasCMYKType = true;
                    break;
                }
            }

            assertTrue("No RGB types for " + data, hasRGBType);
            assertTrue("No CMYK types for " + data, hasCMYKType);
        }

        reader.dispose();
    }

    @Test
    public void testGetRawImageTypeCMYK() throws IOException {
        // Make sure images that are encoded as CMYK (not YCCK) actually return non-null for getRawImageType
        JPEGImageReader reader = createReader();

        List<TestData> cmykData = Arrays.asList(
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample.jpg"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-no-icc.jpg"), new Dimension(100, 100))
        );

        for (TestData data : cmykData) {
            reader.setInput(data.getInputStream());

            ImageTypeSpecifier rawType = reader.getRawImageType(0);
            assertNotNull("No raw type for " + data, rawType);
        }
    }

    @Test
    public void testReadCMYKAsCMYK() throws IOException {
        // Make sure CMYK images can be read and still contain their original (embedded) color profile
        JPEGImageReader reader = createReader();

        List<TestData> cmykData = getCMYKData();

        for (TestData data : cmykData) {
            reader.setInput(data.getInputStream());
            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

            assertTrue(data + " has no image types", types.hasNext());

            ImageTypeSpecifier cmykType = null;

            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();

                int csType = type.getColorModel().getColorSpace().getType();
                if (csType == ColorSpace.TYPE_CMYK) {
                    cmykType = type;
                    break;
                }
            }

            assertNotNull("No CMYK types for " + data, cmykType);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestinationType(cmykType);
            param.setSourceRegion(new Rectangle(reader.getWidth(0), 8)); // We don't really need to read it all

            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(ColorSpace.TYPE_CMYK, image.getColorModel().getColorSpace().getType());
        }

        reader.dispose();
    }

    @Test
    public void testReadCMYKAsCMYKSameRGBasRGB() throws IOException {
        // Make sure CMYK images can be read and still contain their original (embedded) color profile
        JPEGImageReader reader = createReader();

        List<TestData> cmykData = getCMYKData();

        for (TestData data : cmykData) {
            reader.setInput(data.getInputStream());
            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

            assertTrue(data + " has no image types", types.hasNext());

            ImageTypeSpecifier cmykType = null;
            ImageTypeSpecifier rgbType = null;

            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();

                int csType = type.getColorModel().getColorSpace().getType();
                if (rgbType == null && csType == ColorSpace.TYPE_RGB) {
                    rgbType = type;
                }
                else if (cmykType == null && csType == ColorSpace.TYPE_CMYK) {
                    cmykType = type;
                }

                if (rgbType != null && cmykType != null) {
                    break;
                }
            }

            assertNotNull("No RGB types for " + data, rgbType);
            assertNotNull("No CMYK types for " + data, cmykType);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(reader.getWidth(0), 8)); // We don't really need to read it all

            param.setDestinationType(cmykType);
            BufferedImage imageCMYK = reader.read(0, param);

            param.setDestinationType(rgbType);
            BufferedImage imageRGB = reader.read(0, param);

            assertNotNull(imageCMYK);
            assertEquals(ColorSpace.TYPE_CMYK, imageCMYK.getColorModel().getColorSpace().getType());

            assertNotNull(imageRGB);
            assertEquals(ColorSpace.TYPE_RGB, imageRGB.getColorModel().getColorSpace().getType());

            for (int y = 0; y < imageCMYK.getHeight(); y++) {
                for (int x = 0; x < imageCMYK.getWidth(); x++) {
                    int cmykAsRGB = imageCMYK.getRGB(x, y);
                    int rgb = imageRGB.getRGB(x, y);

                    if (rgb != cmykAsRGB) {
                        assertRGBEquals(String.format("Diff at [%d, %d]: %s != %s", x, y, String.format("#%04x", cmykAsRGB), String.format("#%04x", rgb)), rgb, cmykAsRGB, 2);
                    }
                }
            }
        }

        reader.dispose();
    }

    @Test
    public void testReadNoJFIFYCbCr() throws IOException {
        // Basically the same issue as http://stackoverflow.com/questions/9340569/jpeg-image-with-wrong-colors
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/no-jfif-ycbcr.jpg")));

        assertEquals(310, reader.getWidth(0));
        assertEquals(206, reader.getHeight(0));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(0, 0, 310, 8));
        BufferedImage image = reader.read(0, param);
        assertNotNull(image);
        assertEquals(310, image.getWidth());
        assertEquals(8, image.getHeight());

        int[] expectedRGB = new int[] {
                0xff3c1b14, 0xff35140b, 0xff4b2920, 0xff3b160e, 0xff49231a, 0xff874e3d, 0xff563d27, 0xff926c61,
                0xff350005, 0xff84432d, 0xff754f46, 0xff2c2223, 0xff422016, 0xff220f0b, 0xff251812, 0xff1c1209,
                0xff483429, 0xff1b140c, 0xff231c16, 0xff2f261f, 0xff2e2923, 0xff170c08, 0xff383025, 0xff443b34,
                0xff574a39, 0xff3b322b, 0xffeee1d0, 0xffebdecd, 0xffe9dccb, 0xffe8dbca, 0xffe7dcca,
        };

        // Validate strip colors
        for (int i = 0; i < image.getWidth() / 10; i++) {
            int actualRGB = image.getRGB(i * 10, 7);
            assertRGBEquals(expectedRGB[i], actualRGB);
        }
    }

    /**
     * Slightly fuzzy RGB equals method. Tolerance +/-5 steps.
     */
    private void assertRGBEquals(int expectedRGB, int actualRGB) {
        assertRGBEquals("RGB values differ", expectedRGB, actualRGB, 5);
    }

    private void assertRGBEquals(String message, int expectedRGB, int actualRGB, int tolerance) {
        assertEquals(message, (expectedRGB >> 16) & 0xff, (actualRGB >> 16) & 0xff, tolerance);
        assertEquals(message, (expectedRGB >>  8) & 0xff, (actualRGB >>  8) & 0xff, tolerance);
        assertEquals(message, (expectedRGB      ) & 0xff, (actualRGB      ) & 0xff, tolerance);
    }

    // Regression: Test subsampling offset within  of bounds
    // NOTE: These tests assumes the reader will read at least 1024 scanlines (if available) each iteration,
    //       this might change in the future. If so, the tests will no longer test what tey are supposed to....
    @Test
    public void testReadSubsamplingBounds1028() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1028.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 1, 1);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
    }

    @Test
    public void testReadSubsamplingNotSkippingLines1028() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1028.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 1, 1);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);

        // Make sure correct color is actually read, not just left empty
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 2));
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 1));
    }

    @Test
    public void testReadSubsamplingBounds1027() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1027.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 2, 2);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);

        // Make sure correct color is actually read, not just left empty
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 2));
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 1));
    }

    @Test
    public void testReadSubsamplingBounds1026() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1026.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 1, 1);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);

        // Make sure correct color is actually read, not just left empty
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 2));
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 1));
    }

    @Test
    public void testReadSubsamplingBounds1025() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1025.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 1, 1);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
    }

    @Test
    public void testReadSubsamplingNotSkippingLines1025() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1025.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 1, 1);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);

        // Make sure correct color is actually read, not just left empty
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 2));
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 1));
    }

    @Test
    public void testReadSubsamplingBounds1024() throws IOException {
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/read-error1024.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceSubsampling(3, 3, 1, 1);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);

        // Make sure correct color is actually read, not just left empty
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 2));
        assertRGBEquals(0xfefefd, image.getRGB(0, image.getHeight() - 1));
    }

    @Test
    public void testXDensityOutOfRangeIssue() throws IOException {
        // Image has JFIF with x/y density 0
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/xdensity-out-of-range-zero.jpg")));

        IIOMetadata imageMetadata = reader.getImageMetadata(0);
        assertNotNull(imageMetadata);

        // Assume that the aspect ratio is 1 if both x/y density is 0.
        IIOMetadataNode tree = (IIOMetadataNode) imageMetadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        NodeList dimensions = tree.getElementsByTagName("Dimension");
        assertEquals(1, dimensions.getLength());
        assertEquals("PixelAspectRatio", dimensions.item(0).getFirstChild().getNodeName());
        assertEquals("1.0", ((Element) dimensions.item(0).getFirstChild()).getAttribute("value"));
    }

    // TODO: Test RGBA/YCbCrA handling

    @Test
    public void testReadMetadata() throws IOException {
        // Just test that we can read the metadata without exceptions
        JPEGImageReader reader = createReader();

        for (TestData testData : getTestData()) {
            reader.setInput(testData.getInputStream());

            for (int i = 0; i < reader.getNumImages(true); i++) {
                try {
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    assertNotNull(String.format("Image metadata null for %s image %s", testData, i), metadata);

                    Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                    assertNotNull(tree);
                    assertThat(tree, new IsInstanceOf(IIOMetadataNode.class));

                    IIOMetadataNode iioTree = (IIOMetadataNode) tree;
                    assertEquals(1, iioTree.getElementsByTagName("JPEGvariety").getLength());
                    Node jpegVariety = iioTree.getElementsByTagName("JPEGvariety").item(0);
                    assertNotNull(jpegVariety);

                    Node app0JFIF = jpegVariety.getFirstChild();
                    if (app0JFIF != null) {
                        assertEquals("app0JFIF", app0JFIF.getLocalName());
                    }

                    NodeList markerSequences = iioTree.getElementsByTagName("markerSequence");
                    assertTrue(markerSequences.getLength() == 1 || markerSequences.getLength() == 2); // In case of JPEG encoded thumbnail, there will be 2
                    IIOMetadataNode markerSequence = (IIOMetadataNode) markerSequences.item(0);
                    assertNotNull(markerSequence);
                    assertThat(markerSequence.getChildNodes().getLength(), new GreaterThan<>(0));

                    NodeList unknowns = markerSequence.getElementsByTagName("unknown");
                    for (int j = 0; j < unknowns.getLength(); j++) {
                        IIOMetadataNode unknown = (IIOMetadataNode) unknowns.item(j);
                        assertNotNull(unknown.getUserObject()); // All unknowns must have user object (data array)
                    }
                }
                catch (IIOException e) {
                    fail(String.format("Reading metadata failed for %s image %s: %s", testData, i, e.getMessage()));
                }
            }
        }
    }

    @Test
    public void testReadInconsistentMetadata() throws IOException {
        // A collection of JPEG files that makes the JPEGImageReader throw exception "Inconsistent metadata read from stream"...
        List<String> resources = Arrays.asList(
                "/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg", // Ok
                "/jpeg/gray-sample.jpg", // Ok
                "/jpeg/cmyk-sample.jpg",
                "/jpeg/cmyk-sample-multiple-chunk-icc.jpg",
                "/jpeg/invalid-icc-duplicate-sequence-numbers-rgb-xerox-dc250-heavyweight-1-progressive-jfif.jpg",
                "/jpeg/no-image-types-rgb-us-web-coated-v2-ms-photogallery-exif.jpg"
        );

        for (String resource : resources) {
            // Just test that we can read the metadata without exceptions
            JPEGImageReader reader = createReader();
            ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource(resource));

            try {
                reader.setInput(stream);
                IIOMetadata metadata = reader.getImageMetadata(0);
                assertNotNull(String.format("%s: null metadata", resource), metadata);

                Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                assertNotNull(tree);
//                new XMLSerializer(System.err, System.getProperty("file.encoding")).serialize(tree, false);

            }
            catch (IIOException e) {
                AssertionError fail = new AssertionError(String.format("Reading metadata failed for %ss: %s", resource, e.getMessage()));
                fail.initCause(e);
                throw fail;
            }
            finally {
                stream.close();
            }
        }
    }

    @Test
    public void testReadMetadataEqualReference() throws IOException {
        // Compares the metadata for JFIF-conformant files with metadata from com.sun...JPEGImageReader
        JPEGImageReader reader = createReader();
        ImageReader referenceReader = createReferenceReader();

        for (TestData testData : getTestData()) {
            reader.setInput(testData.getInputStream());
            referenceReader.setInput(testData.getInputStream());

            for (int i = 0; i < reader.getNumImages(true); i++) {
                try {
                    IIOMetadata reference = referenceReader.getImageMetadata(i);

                    try {
                        IIOMetadata metadata = reader.getImageMetadata(i);

                        String[] formatNames = reference.getMetadataFormatNames();
                        for (String formatName : formatNames) {
                            Node referenceTree = reference.getAsTree(formatName);
                            Node actualTree = metadata.getAsTree(formatName);

//                            new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(actualTree, false);
                            assertTreesEquals(String.format("Metadata differs for %s image %s ", testData, i), referenceTree, actualTree);
                        }
                    }
                    catch (IIOException e) {
                        AssertionError fail = new AssertionError(String.format("Reading metadata failed for %s image %s: %s", testData, i, e.getMessage()));
                        fail.initCause(e);
                        throw fail;
                    }
                }
                catch (IIOException ignore) {
                    // The reference reader will fail on certain images, we'll just ignore that
                    System.err.println(String.format("WARNING: Reading reference metadata failed for %s image %s: %s", testData, i, ignore.getMessage()));
                }
            }
        }
    }

    private ImageReader createReferenceReader() {
        try {
            @SuppressWarnings("unchecked")
            Class<ImageReaderSpi> spiClass = (Class<ImageReaderSpi>) Class.forName("com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi");
            ImageReaderSpi provider = spiClass.newInstance();

            ImageReader reader = provider.createReaderInstance();
            assumeNotNull(reader);
            return reader;
        }
        catch (Throwable t) {
            assumeNoException(t);
        }

        return null;
    }

    private void assertTreesEquals(String message, Node expectedTree, Node actualTree) {
        if (expectedTree == actualTree) {
            return;
        }

        if (expectedTree == null) {
            assertNull(actualTree);
            return;
        }

        assertEquals(String.format("%s: Node names differ", message), expectedTree.getNodeName(), actualTree.getNodeName());

        NamedNodeMap expectedAttributes = expectedTree.getAttributes();
        NamedNodeMap actualAttributes = actualTree.getAttributes();
        assertEquals(String.format("%s: Number of attributes for <%s> differ", message, expectedTree.getNodeName()), expectedAttributes.getLength(), actualAttributes.getLength());
        for (int i = 0; i < expectedAttributes.getLength(); i++) {
            Node item = expectedAttributes.item(i);
            assertEquals(String.format("%s: \"%s\" attribute for <%s> differ", message, item.getNodeName(), expectedTree.getNodeName()), item.getNodeValue(), actualAttributes.getNamedItem(item.getNodeName()).getNodeValue());
        }

        // Test for equal user objects.
        // - array equals or reflective equality... Most user objects does not have a decent equals method.. :-P
        if (expectedTree instanceof IIOMetadataNode) {
            assertTrue(String.format("%s: %s not an IIOMetadataNode", message, expectedTree.getNodeName()), actualTree instanceof IIOMetadataNode);

            Object expectedUserObject = ((IIOMetadataNode) expectedTree).getUserObject();

            if (expectedUserObject != null) {
                Object actualUserObject = ((IIOMetadataNode) actualTree).getUserObject();
                assertNotNull(String.format("%s: User object missing for <%s>", message, expectedTree.getNodeName()), actualUserObject);
                assertEqualUserObjects(String.format("%s: User objects for <%s MarkerTag\"%s\"> differ", message, expectedTree.getNodeName(), ((IIOMetadataNode) expectedTree).getAttribute("MarkerTag")), expectedUserObject, actualUserObject);
            }
        }

        // Sort nodes to make sure that sequence of equally named tags does not matter
        List<IIOMetadataNode> expectedChildren = sortNodes(expectedTree.getChildNodes());
        List<IIOMetadataNode> actualChildren = sortNodes(actualTree.getChildNodes());

        assertEquals(String.format("%s: Number of child nodes for %s differ", message, expectedTree.getNodeName()), expectedChildren.size(), actualChildren.size());

        for (int i = 0; i < expectedChildren.size(); i++) {
            assertTreesEquals(message + "<" + expectedTree.getNodeName() + ">", expectedChildren.get(i), actualChildren.get(i));
        }
    }

    private void assertEqualUserObjects(String message, Object expectedUserObject, Object actualUserObject) {
        if (expectedUserObject.equals(actualUserObject)) {
            return;
        }

        if (expectedUserObject instanceof ICC_Profile) {
            if (actualUserObject instanceof ICC_Profile) {
                assertArrayEquals(message, ((ICC_Profile) expectedUserObject).getData(), ((ICC_Profile) actualUserObject).getData());
                return;
            }
        }
        else if (expectedUserObject instanceof byte[]) {
            if (actualUserObject instanceof byte[]) {
                assertArrayEquals(message, (byte[]) expectedUserObject, (byte[]) actualUserObject);
                return;
            }
        }
        else if (expectedUserObject instanceof JPEGHuffmanTable) {
            if (actualUserObject instanceof JPEGHuffmanTable) {
                assertArrayEquals(message, ((JPEGHuffmanTable) expectedUserObject).getLengths(), ((JPEGHuffmanTable) actualUserObject).getLengths());
                assertArrayEquals(message, ((JPEGHuffmanTable) expectedUserObject).getValues(), ((JPEGHuffmanTable) actualUserObject).getValues());
                return;
            }
        }
        else if (expectedUserObject instanceof JPEGQTable) {
            if (actualUserObject instanceof JPEGQTable) {
                assertArrayEquals(message, ((JPEGQTable) expectedUserObject).getTable(), ((JPEGQTable) actualUserObject).getTable());
                return;
            }
        }

        fail(expectedUserObject.getClass().getName());
    }

    private List<IIOMetadataNode> sortNodes(final NodeList nodes) {
        ArrayList<IIOMetadataNode> sortedNodes = new ArrayList<>(new AbstractList<IIOMetadataNode>() {
            @Override
            public IIOMetadataNode get(int index) {
                return (IIOMetadataNode) nodes.item(index);
            }

            @Override
            public int size() {
                return nodes.getLength();
            }
        });

        Collections.sort(
                sortedNodes,
                new Comparator<IIOMetadataNode>() {
                    public int compare(IIOMetadataNode left, IIOMetadataNode right) {
                        int res = left.getNodeName().compareTo(right.getNodeName());
                        if (res != 0) {
                            return res;
                        }

                        // Compare attribute values
                        NamedNodeMap leftAttributes = left.getAttributes(); // TODO: We should sort left's attributes as well, for stable sorting + handle diffs in attributes
                        NamedNodeMap rightAttributes = right.getAttributes();

                        for (int i = 0; i < leftAttributes.getLength(); i++) {
                            Node leftAttribute = leftAttributes.item(i);
                            Node rightAttribute = rightAttributes.getNamedItem(leftAttribute.getNodeName());

                            if (rightAttribute == null) {
                                return 1;
                            }

                            res = leftAttribute.getNodeValue().compareTo(rightAttribute.getNodeValue());
                            if (res != 0) {
                                return res;
                            }
                        }

                        if (left.getUserObject() instanceof byte[] && right.getUserObject() instanceof byte[]) {
                            byte[] leftBytes = (byte[]) left.getUserObject();
                            byte[] rightBytes = (byte[]) right.getUserObject();

                            if (leftBytes.length < rightBytes.length) {
                                return 1;
                            }

                            if (leftBytes.length > rightBytes.length) {
                                return -1;
                            }

                            if (leftBytes.length > 0) {
                                for (int i = 0; i < leftBytes.length; i++) {
                                    if (leftBytes[i] < rightBytes[i]) {
                                        return -1;
                                    }
                                    if (leftBytes[i] > rightBytes[i]) {
                                        return 1;
                                    }
                                }
                            }
                        }

                        return 0;
                    }
                }
        );

        return sortedNodes;
    }

    @Test
    public void testGetNumImagesBogusDataPrepended() throws IOException {
        // The JPEGImageReader (incorrectly) interprets this image to be a "tables only" image.

        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/broken-jpeg/broken-bogus-data-prepended-real-jfif-start-at-4801.jpg")));
            assertEquals(-1, reader.getNumImages(false)); // Ok
            assertEquals(0, reader.getNumImages(true));  // Should throw IIOException or return 0
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testNegativeSOSComponentCount() throws IOException {
        // The data in the stream looks like this:
        // FF DA 00 08 01 01 01 06 3F 02 0E 70 9A A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 A2 64 05 5D ...
        // ..but the JPEGBuffer class contains:
        // FF DA 00 08 A2 A2 A2 A2 A2 64 05 5D 02 87 FC 5B 5C E1 0E BD ...
        //             *****************??
        // 15 bytes missing in action! Why?
        // There's a bug in com.sun.imageio.plugins.jpeg.AdobeMarkerSegment when parsing non-standard length
        // APP14/Adobe segments (i.e. lengths other than 14) that causes the
        // com.sun.imageio.plugins.jpeg.JPEGBuffer#loadBuf() method to overwrite parts of the input data
        // (the difference between the real length and 14, at the end of the stream). This can cause all
        // sorts of weird problems later, and is a pain to track down (it is probably the real cause for
        // many of the other issues we've found in the set).
        // See also: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6355567

        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-exif-xmp-adobe-progressive-negative-component-count.jpg")));

            IIOMetadata metadata = reader.getImageMetadata(0);
            assertNotNull(metadata);

            Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            assertNotNull(tree);
            assertThat(tree, new IsInstanceOf(IIOMetadataNode.class));
        }
        catch (IIOException knownIssue) {
            // This shouldn't fail, but the bug is most likely in the JPEGBuffer class
            assertNotNull(knownIssue.getCause());
            assertThat(knownIssue.getCause(), new IsInstanceOf(NegativeArraySizeException.class));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testInconsistentSOSBandCountExceedsSOFBandCount() throws IOException {
        // Last SOS segment contains (FF DA) 00 08 01 03 03 01 3F 10  (... 18 more ...  F0 7D FB FB 6D)
        // (14th)                    (SOS)   len 8 |  |  |  |  |  approx high: 1, approx low: 0
        //                                         |  |  |  |  end spectral selection:
        //                                         |  |  |  start spectral selection: 1
        //                                         |  |  dc: 0, ac: 3
        //                                         |  selector: 3
        //                                         1 component
        // Metadata reads completely different values...
        // FF DA 00 08 01 F0 7D FB FB 6D
        //                \_ there's 24 bytes MIA (skipped) here, between the length and the actual data read...

        // Seems to be a bug in the AdobeMarkerSegment, it reads 12 bytes always,
        // then subtracting length from bufferAvail, but *does not update bufPtr to skip the remaining*.
        // This causes trouble for subsequent JPEGBuffer.loadBuf() calls, because it will overwrite the same
        // number of bytes *at the end* of the buffer.
        // This image has a 38 (36) byte App14/Adobe segment.
        // The length 36 - 12 = 24 (the size of the missing bytes!)

        // TODO: Report bug!

        ImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/progressive-adobe-sof-bands-dont-match-sos-band-count.jpg")));

            IIOMetadata metadata = reader.getImageMetadata(0);
            assertNotNull(metadata);

            Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            assertNotNull(tree);
            assertThat(tree, new IsInstanceOf(IIOMetadataNode.class));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testInvalidDHTIssue() throws IOException {
        // Image has empty (!) DHT that is okay on read, but not when you set back from tree...
        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-progressive-invalid-dht.jpg")));

            IIOMetadata metadata = reader.getImageMetadata(0);
            assertNotNull(metadata);

            Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            assertNotNull(tree);
            assertThat(tree, new IsInstanceOf(IIOMetadataNode.class));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testComponentIdOutOfRange() throws IOException {
        // Image has SOF and SOS component ids that are negative, setFromTree chokes on this...
        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-component-id-out-of-range.jpg")));

            IIOMetadata metadata = reader.getImageMetadata(0);
            assertNotNull(metadata);

            Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            assertNotNull(tree);
            assertThat(tree, new IsInstanceOf(IIOMetadataNode.class));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetRawImageTypeAdobeAPP14CMYKAnd3channelData() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/exif-jfif-app13-app14ycck-3channel.jpg")));

            ImageTypeSpecifier rawType = reader.getRawImageType(0);
            assertNull(rawType); // But no exception, please...
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadAdobeAPP14CMYKAnd3channelData() throws IOException {
        JPEGImageReader reader = createReader();

        try {
            reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/exif-jfif-app13-app14ycck-3channel.jpg")));

            assertEquals(310, reader.getWidth(0));
            assertEquals(384, reader.getHeight(0));

            BufferedImage image = reader.read(0, null);
            assertNotNull(image);
            assertEquals(310, image.getWidth());
            assertEquals(384, image.getHeight());
            assertEquals(ColorSpace.TYPE_RGB, image.getColorModel().getColorSpace().getType());
        }
        finally {
            reader.dispose();
        }
    }
}
