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

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JPEGImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReaderTest.java,v 1.0 24.01.11 22.04 haraldk Exp$
 */
public class JPEGImageReaderTest extends ImageReaderAbstractTestCase<JPEGImageReader> {

    private static final JPEGImageReaderSpi SPI = new JPEGImageReaderSpi(lookupDelegateProvider());

    private static ImageReaderSpi lookupDelegateProvider() {
        return JPEGImageReaderSpi.lookupDelegateProvider(IIORegistry.getDefaultInstance());
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/jpeg/cmm-exception-adobe-rgb.jpg"), new Dimension(626, 76)),
                new TestData(getClassLoaderResource("/jpeg/cmm-exception-srgb.jpg"), new Dimension(1800, 1200)),
                new TestData(getClassLoaderResource("/jpeg/gray-sample.jpg"), new Dimension(386, 396)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample.jpg"), new Dimension(160, 227)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), new Dimension(2707, 3804)),
                new TestData(getClassLoaderResource("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg"), new Dimension(640, 480)),
                new TestData(getClassLoaderResource("/jpeg/jfif-padded-segments.jpg"), new Dimension(20, 45))
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

    @SuppressWarnings({"unchecked"})
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
        return Arrays.asList("image/jpeg");
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
        param.setSourceSubsampling(8, 8, 1, 1);

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
            assertEquals(expected[i], actual[i + actualOffset], 5);
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

        verify(warningListener).warningOccurred(eq(reader), anyString());
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
            assertEquals((actualRGB >> 16) & 0xff, (expectedRGB[i] >> 16) & 0xff, 5);
            assertEquals((actualRGB >> 8) & 0xff, (expectedRGB[i] >> 8) & 0xff, 5);
            assertEquals((actualRGB) & 0xff, (expectedRGB[i]) & 0xff, 5);
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
            assertEquals((actualRGB >> 16) & 0xff, (expectedRGB[i] >> 16) & 0xff, 5);
            assertEquals((actualRGB >> 8) & 0xff, (expectedRGB[i] >> 8) & 0xff, 5);
            assertEquals((actualRGB) & 0xff, (expectedRGB[i]) & 0xff, 5);
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
            assertEquals((actualRGB >> 16) & 0xff, (expectedRGB[i] >> 16) & 0xff, 5);
            assertEquals((actualRGB >> 8) & 0xff, (expectedRGB[i] >> 8) & 0xff, 5);
            assertEquals((actualRGB) & 0xff, (expectedRGB[i]) & 0xff, 5);
        }
    }

    // TODO: Test RGBA/YCbCrA handling

    @Test
    public void testReadMetadataMaybeNull() throws IOException {
        // Just test that we can read the metadata without exceptions
        JPEGImageReader reader = createReader();

        for (TestData testData : getTestData()) {
            reader.setInput(testData.getInputStream());

            for (int i = 0; i < reader.getNumImages(true); i++) {
                try {
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    assertNotNull(String.format("Image metadata null for %s image %s", testData, i), metadata);
                }
                catch (IIOException e) {
                    System.err.println(String.format("WARNING: Reading metadata failed for %s image %s: %s", testData, i, e.getMessage()));
                }
            }
        }
    }
}
