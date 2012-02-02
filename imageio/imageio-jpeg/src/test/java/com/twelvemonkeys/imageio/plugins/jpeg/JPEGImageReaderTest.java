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

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
                new TestData(getClassLoaderResource("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg"), new Dimension(640, 480))
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
        
        for (int i = 0; i < expectedData.length; i++) {
            assertEquals(expectedData[i], data[i], 5);
        }
    }

    @Test
    public void testICCDuplicateSequence() throws IOException {
        // Variation of the above, file contains multiple ICC chunks, with all counts and sequence numbers == 1

        // TODO: As the IIOException is thrown even from the readRaster method (ends up in readImageHeader native
        // method), we could probably intercept at the byte/stream level, and insert correct count/sequence numbers,
        // as seen by the native code.
        // Should be doable, but will make reading slower. We want to avoid that in the common case.

        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-icc-duplicate-sequence-numbers-rgb-internal-kodak-srgb-jfif.jpg")));

        assertEquals(345, reader.getWidth(0));
        assertEquals(540, reader.getHeight(0));

        BufferedImage image = reader.read(0);

        assertNotNull(image);
        assertEquals(345, image.getWidth());
        assertEquals(540, image.getHeight());
    }

    @Test
    public void testICCDuplicateSequenceZeroBased() throws IOException {
        // File contains multiple ICC chunks, with all counts and sequence numbers == 0

        // TODO: As the IIOException is thrown even from the readRaster method (ends up in readImageHeader native
        // method), we could probably intercept at the byte/stream level, and insert correct count/sequence numbers,
        // as seen by the native code.
        // Should be doable, but will make reading slower. We want to avoid that in the common case.

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

    // TODO: Test JFIF raw thumbnail
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
}
