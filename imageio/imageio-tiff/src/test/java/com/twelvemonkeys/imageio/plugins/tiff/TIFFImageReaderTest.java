package com.twelvemonkeys.imageio.plugins.tiff;/*
 * Copyright (c) 2012, Harald Kuhr
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

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * TIFFImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReaderTest.java,v 1.0 08.05.12 15:25 haraldk Exp$
 */
public class TIFFImageReaderTest extends ImageReaderAbstractTest<TIFFImageReader> {

    private static final TIFFImageReaderSpi SPI = new TIFFImageReaderSpi();

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/tiff/balloons.tif"), new Dimension(640, 480)), // RGB, uncompressed
                new TestData(getClassLoaderResource("/tiff/sm_colors_pb.tif"), new Dimension(64, 64)), // RGB, PackBits compressed
                new TestData(getClassLoaderResource("/tiff/sm_colors_tile.tif"), new Dimension(64, 64)), // RGB, uncompressed, tiled
                new TestData(getClassLoaderResource("/tiff/sm_colors_pb_tile.tif"), new Dimension(64, 64)), // RGB, PackBits compressed, tiled
                new TestData(getClassLoaderResource("/tiff/galaxy.tif"), new Dimension(965, 965)), // RGB, LZW compressed
                new TestData(getClassLoaderResource("/tiff/quad-lzw.tif"), new Dimension(512, 384)), // RGB, Old spec (reversed) LZW compressed, tiled
                new TestData(getClassLoaderResource("/tiff/bali.tif"), new Dimension(725, 489)), // Palette-based, LZW compressed
                new TestData(getClassLoaderResource("/tiff/f14.tif"), new Dimension(640, 480)), // Gray, uncompressed
                new TestData(getClassLoaderResource("/tiff/marbles.tif"), new Dimension(1419, 1001)), // RGB, LZW compressed w/predictor
                new TestData(getClassLoaderResource("/tiff/lzw-full-12-bit-table.tif"), new Dimension(874, 1240)), // Gray, LZW compressed, w/predictor
                new TestData(getClassLoaderResource("/tiff/chifley_logo.tif"), new Dimension(591, 177)), // CMYK, uncompressed
                new TestData(getClassLoaderResource("/tiff/ycbcr-cat.tif"), new Dimension(250, 325)), // YCbCr, LZW compressed
                new TestData(getClassLoaderResource("/tiff/quad-jpeg.tif"), new Dimension(512, 384)), // YCbCr, JPEG compressed, striped
                new TestData(getClassLoaderResource("/tiff/smallliz.tif"), new Dimension(160, 160)), // YCbCr, Old-Style JPEG compressed (full JFIF stream)
                new TestData(getClassLoaderResource("/tiff/zackthecat.tif"), new Dimension(234, 213)), // YCbCr, Old-Style JPEG compressed (tables, no JFIF stream)
                new TestData(getClassLoaderResource("/tiff/test-single-gray-compression-type-2.tiff"), new Dimension(1728, 1146)), // Gray, CCITT type 2 compressed
                new TestData(getClassLoaderResource("/tiff/cramps-tile.tif"), new Dimension(800, 607)), // Gray/WhiteIsZero, uncompressed, striped & tiled...
                new TestData(getClassLoaderResource("/tiff/lzw-long-strings-sample.tif"), new Dimension(316, 173)), // RGBA, LZW compressed w/predictor
                new TestData(getClassLoaderResource("/tiff/part.tif"), new Dimension(50, 50)), // Gray/BlackIsZero, uncompressed, striped signed int (SampleFormat 2)
                new TestData(getClassLoaderResource("/tiff/cmyk_jpeg_no_profile.tif"), new Dimension(150, 63)), // CMYK, JPEG compressed, no ICC profile
                new TestData(getClassLoaderResource("/tiff/cmyk_jpeg.tif"), new Dimension(100, 100)), // CMYK, JPEG compressed, with ICC profile
                new TestData(getClassLoaderResource("/tiff/grayscale-alpha.tiff"), new Dimension(248, 351)), // Gray + unassociated alpha
                new TestData(getClassLoaderResource("/tiff/signed-integral-8bit.tif"), new Dimension(439, 167)), // Gray, 8 bit *signed* integral
                new TestData(getClassLoaderResource("/tiff/floatingpoint-32bit.tif"), new Dimension(300, 100)), // RGB, 32 bit floating point
                new TestData(getClassLoaderResource("/tiff/general-cmm-error.tif"), new Dimension(1181, 860)), // RGB, LZW compression with broken/incompatible ICC profile
                new TestData(getClassLoaderResource("/tiff/lzw-rgba-padded-icc.tif"), new Dimension(19, 11)), // RGBA, LZW compression with padded ICC profile
                new TestData(getClassLoaderResource("/tiff/lzw-rgba-4444.tif"), new Dimension(64, 64)), // RGBA, LZW compression with UINT 4/4/4/4 + gray 2/2
                new TestData(getClassLoaderResource("/tiff/lzw-buffer-overflow.tif"), new Dimension(5, 49)), // RGBA, LZW compression, will throw IOOBE if small buffer
                new TestData(getClassLoaderResource("/tiff/lzw-colormap-iiobe.tif"), new Dimension(2550, 3300)), // RGBA, LZW compression, will throw IOOBE if small buffer
                new TestData(getClassLoaderResource("/tiff/scan-mono-iccgray.tif"), new Dimension(2408, 3436)), // B/W, PackBits w/gray ICC profile
                new TestData(getClassLoaderResource("/tiff/planar-striped-lzw.tif"), new Dimension(229, 229)), // RGB 8 bit/sample, planar, LZW compression
                new TestData(getClassLoaderResource("/tiff/colormap-with-extrasamples.tif"), new Dimension(10, 10)), // Palette, 8 bit/sample, 2 samples/pixel, extra samples, LZW
                // CCITT
                new TestData(getClassLoaderResource("/tiff/ccitt/group3_1d.tif"), new Dimension(6, 4)), // B/W, CCITT T4 1D
                new TestData(getClassLoaderResource("/tiff/ccitt/group3_1d_fill.tif"), new Dimension(6, 4)), // B/W, CCITT T4 1D
                new TestData(getClassLoaderResource("/tiff/ccitt/group3_2d.tif"), new Dimension(6, 4)), // B/W, CCITT T4 2D
                new TestData(getClassLoaderResource("/tiff/ccitt/group3_2d_fill.tif"), new Dimension(6, 4)), // B/W, CCITT T4 2D
                new TestData(getClassLoaderResource("/tiff/ccitt/group3_2d_lsb2msb.tif"), new Dimension(6, 4)), // B/W, CCITT T4 2D, LSB
                new TestData(getClassLoaderResource("/tiff/ccitt/group4.tif"), new Dimension(6, 4)), // B/W, CCITT T6 1D
                new TestData(getClassLoaderResource("/tiff/ccitt_tolessrows.tif"), new Dimension(6, 6)), // CCITT, metadata claiming 6 rows, stream contains only 4
                new TestData(getClassLoaderResource("/tiff/fivepages-scan-causingerrors.tif"), new Dimension(2480, 3518)), // B/W, CCITT T4
                new TestData(getClassLoaderResource("/tiff/CCITTgetNextChangingElement.tif"), new Dimension(2402,195)),
                new TestData(getClassLoaderResource("/tiff/ccitt-too-many-changes.tif"), new Dimension(24,153)),
                // CIELab
                new TestData(getClassLoaderResource("/tiff/ColorCheckerCalculator.tif"), new Dimension(798, 546)), // CIELab 8 bit/sample
                // Gray
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-02.tif"), new Dimension(73, 43)), // Gray 2 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-04.tif"), new Dimension(73, 43)), // Gray 4 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-06.tif"), new Dimension(73, 43)), // Gray 6 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-08.tif"), new Dimension(73, 43)), // Gray 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-10.tif"), new Dimension(73, 43)), // Gray 10 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-12.tif"), new Dimension(73, 43)), // Gray 12 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-14.tif"), new Dimension(73, 43)), // Gray 14 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-16.tif"), new Dimension(73, 43)), // Gray 16 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-24.tif"), new Dimension(73, 43)), // Gray 24 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-minisblack-32.tif"), new Dimension(73, 43)), // Gray 32 bit/sample
                // Palette
                new TestData(getClassLoaderResource("/tiff/depth/flower-palette-02.tif"), new Dimension(73, 43)), // Palette 2 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-palette-04.tif"), new Dimension(73, 43)), // Palette 4 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-palette-08.tif"), new Dimension(73, 43)), // Palette 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-palette-16.tif"), new Dimension(73, 43)), // Palette 16 bit/sample
                // RGB Interleaved (PlanarConfiguration: 1)
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-08.tif"), new Dimension(73, 43)), // RGB 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-10.tif"), new Dimension(73, 43)), // RGB 10 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-12.tif"), new Dimension(73, 43)), // RGB 12 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-14.tif"), new Dimension(73, 43)), // RGB 14 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-16.tif"), new Dimension(73, 43)), // RGB 16 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-24.tif"), new Dimension(73, 43)), // RGB 24 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-32.tif"), new Dimension(73, 43)), // RGB 32 bit/sample
                // RGB Planar (PlanarConfiguration: 2)
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-08.tif"), new Dimension(73, 43)), // RGB 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-10.tif"), new Dimension(73, 43)), // RGB 10 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-12.tif"), new Dimension(73, 43)), // RGB 12 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-14.tif"), new Dimension(73, 43)), // RGB 14 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-16.tif"), new Dimension(73, 43)), // RGB 16 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-24.tif"), new Dimension(73, 43)), // RGB 24 bit/sample
                // RGB Interleaved Floating point..!? We can read this one, but the samples are not normalized, so colors are way too bright...
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-32.tif"), new Dimension(73, 43)),  // RGB 32 bit FP samples (!)
                // Separated (CMYK) Interleaved (PlanarConfiguration: 1)
                new TestData(getClassLoaderResource("/tiff/depth/flower-separated-contig-08.tif"), new Dimension(73, 43)), // CMYK 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-separated-contig-16.tif"), new Dimension(73, 43)), // CMYK 16 bit/sample
                // Separated (CMYK) Planar (PlanarConfiguration: 2)
                new TestData(getClassLoaderResource("/tiff/depth/flower-separated-planar-08.tif"), new Dimension(73, 43)), // CMYK 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-separated-planar-16.tif"), new Dimension(73, 43)),  // CMYK 16 bit/sample
                // JPEG Lossless
                new TestData(getClassLoaderResource("/tiff/jpeg-lossless-8bit-gray.tif"), new Dimension(512, 512)),  // Lossless JPEG Gray, 8 bit/sample
                new TestData(getClassLoaderResource("/tiff/jpeg-lossless-12bit-gray.tif"), new Dimension(512, 512)),  // Lossless JPEG Gray, 12 bit/sample
                new TestData(getClassLoaderResource("/tiff/jpeg-lossless-16bit-gray.tif"), new Dimension(512, 512)),  // Lossless JPEG Gray, 16 bit/sample
                new TestData(getClassLoaderResource("/tiff/jpeg-lossless-24bit-rgb"), new Dimension(512, 512)),  // Lossless JPEG RGB, 8 bit/sample
                // Custom PIXTIFF ZIP (Compression: 50013)
                new TestData(getClassLoaderResource("/tiff/pixtiff/40-8bit-gray-zip.tif"), new Dimension(801, 1313))  // ZIP Gray, 8 bit/sample
        );
    }

    private List<TestData> getUnsupportedTestData() {
        return Arrays.asList(
                // RGB Interleaved (PlanarConfiguration: 1)
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-02.tif"), new Dimension(73, 43)), // RGB 2 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-04.tif"), new Dimension(73, 43)), // RGB 4 bit/sample
                // RGB Planar (PlanarConfiguration: 2)
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-02.tif"), new Dimension(73, 43)), // RGB 2 bit/sample
                new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-04.tif"), new Dimension(73, 43)) // RGB 4 bit/sample
        );
    }
    @Override
    protected ImageReaderSpi createProvider() {
        return SPI;
    }

    @Override
    protected Class<TIFFImageReader> getReaderClass() {
        return TIFFImageReader.class;
    }

    @Override
    protected TIFFImageReader createReader() {
        return SPI.createReaderInstance(null);
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("tiff", "TIFF");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("tif", "tiff");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/tiff");
    }


    @Test
    public void testReadWithSourceRegionParamEqualImageTiled() throws IOException {
        assertReadWithSourceRegionParamEqualImage(
                new Rectangle(23, 23, 15, 15),
                new TestData(getClassLoaderResource("/tiff/sm_colors_pb_tile.tif"), new Dimension(64, 64)),
                0
        );
    }

    // TODO: Should test USHORT & INT datatypes

    @Test
    public void testReadWithSourceRegionParamEqualImageJPEG() throws IOException {
        // The tiles are 512 x 16, make sure we read across tiles
        assertReadWithSourceRegionParamEqualImage(
                new Rectangle(71, 71, 17, 21),
                new TestData(getClassLoaderResource("/tiff/quad-jpeg.tif"), new Dimension(512, 384)),
                0
        );
    }

    // TODO: Test YCbCr colors

    @Test
    public void testReadOldStyleJPEGGrayscale() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/grayscale-old-style-jpeg.tiff"), new Dimension(600, 600));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);
            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));
        }
    }

    @Test
    public void testReadOldStyleJPEGIncorrectJPEGInterchangeFormatLength() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/old-style-jpeg-bogus-jpeginterchangeformatlength.tif"), new Dimension(1632, 2328));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(warningListener);

            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));
            verify(warningListener, atLeastOnce()).warningOccurred(eq(reader), contains("JPEGInterchangeFormatLength"));
        }
    }

    @Test
    public void testReadOldStyleJPEGInconsistentMetadata() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/old-style-jpeg-inconsistent-metadata.tif"), new Dimension(2483, 3515));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(warningListener);

            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));
            verify(warningListener, atLeastOnce()).warningOccurred(eq(reader), contains("JPEG"));
        }
    }

    @Test
    public void testReadIncompatibleICCProfileIgnoredWithWarning() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/rgb-with-embedded-cmyk-icc.tif"), new Dimension(1500, 1500));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(warningListener);

            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));
            verify(warningListener, atLeastOnce()).warningOccurred(eq(reader), contains("ICC"));
        }
    }

    @Test
    public void testReadYCbCrJPEGAssumedRGB() throws IOException {
        // Problematic test data, which is YCbCr encoded (as correctly specified by the PhotometricInterpretation tag,
        // but the JPEGImageReader will detect the data as RGB due to non-subsampled data and SOF ids.
        TestData testData = new TestData(getClassLoaderResource("/tiff/xerox-jpeg-ycbcr-weird-coefficients.tif"), new Dimension(2482, 3520));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            ImageReadParam param = reader.getDefaultReadParam();
            // TODO: There's a bug in reading with source region for the raster case...
//            param.setSourceRegion(new Rectangle(8, 8));
            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
//            assertEquals(new Dimension(8, 8), new Dimension(image.getWidth(), image.getHeight()));
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));

            // The pixel at 0, 0 should be white(-ish), not red!
            // NOTE: The image contains some weird custom YCbCr coefficients, which are roughly
            // 0.299, 0.587, 0.144, instead of the standard 0.299, 0.587, 0.114 (the last/blue coefficient differs)
            // this will make the background bright purple, rather than pure white as it would have been
            // with standard coefficients. Could be a typo/bug in the encoder or intentional.
            // Some/most software ignores the custom coefficients, and decodes the image as white background...
            int argb = image.getRGB(0, 0);
            assertEquals("Alpha", 0xff, (argb >>> 24) & 0xff);
            assertEquals("Red", 0xff, (argb >> 16) & 0xff);
            assertEquals("Green", 0xf2, (argb >> 8) & 0xff);
            assertEquals("Blue", 0xff, argb & 0xff);
        }
    }

    @Test
    public void testReadJPEGRasterCaseWithSrcRegion() throws IOException {
        // Problematic test data, which is YCbCr encoded (as correctly specified by the PhotometricInterpretation tag,
        // but the JPEGImageReader will detect the data as RGB due to non-subsampled data and SOF ids.
        TestData testData = new TestData(getClassLoaderResource("/tiff/xerox-jpeg-ycbcr-weird-coefficients.tif"), new Dimension(2482, 3520));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(8, 8));
            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(new Dimension(8, 8), new Dimension(image.getWidth(), image.getHeight()));
        }
    }

    @Test
    public void testColorMap8Bit() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/scan-lzw-8bit-colormap.tiff"), new Dimension(2550, 3300));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(warningListener);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(8, 8));
            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(new Dimension(8, 8), new Dimension(image.getWidth(), image.getHeight()));
            assertEquals(0xffffffff, image.getRGB(0, 0)); // The pixel at 0, 0 should be white, not black
            verify(warningListener, atLeastOnce()).warningOccurred(eq(reader), contains("ColorMap"));
        }
    }

    @Test
    public void testBadICCProfile() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/general-cmm-error.tif"), new Dimension(1181, 864));

        try (ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(warningListener);

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(8, 8));
            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(new Dimension(8, 8), new Dimension(image.getWidth(), image.getHeight()));
            verify(warningListener, atLeastOnce()).warningOccurred(eq(reader), contains("ICC profile"));
        }
    }

    @Test
    public void testPlanarEqualInterleavedRGB() throws IOException {
        TestData expectedData = new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-08.tif"), new Dimension(73, 43));
        TestData testData = new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-08.tif"), new Dimension(73, 43));

        try (ImageInputStream expectedStream = expectedData.getInputStream(); ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();

            reader.setInput(expectedStream);
            BufferedImage expected = reader.read(0, null);

            reader.setInput(stream);
            BufferedImage actual = reader.read(0, null);

            assertImageDataEquals("", expected, actual);
        }
    }

    @Test
    public void testPlanarEqualInterleavedRGB16() throws IOException {
        TestData expectedData = new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-contig-16.tif"), new Dimension(73, 43));
        TestData testData = new TestData(getClassLoaderResource("/tiff/depth/flower-rgb-planar-16.tif"), new Dimension(73, 43));

        try (ImageInputStream expectedStream = expectedData.getInputStream(); ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();

            reader.setInput(expectedStream);
            BufferedImage expected = reader.read(0, null);

            reader.setInput(stream);
            BufferedImage actual = reader.read(0, null);

            assertImageDataEquals("", expected, actual);
        }
    }

    @Test
    public void testPlanarEqualInterleavedSeparated() throws IOException {
        TestData expectedData = new TestData(getClassLoaderResource("/tiff/depth/flower-separated-contig-08.tif"), new Dimension(73, 43));
        TestData testData = new TestData(getClassLoaderResource("/tiff/depth/flower-separated-planar-08.tif"), new Dimension(73, 43));

        try (ImageInputStream expectedStream = expectedData.getInputStream(); ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();

            reader.setInput(expectedStream);
            BufferedImage expected = reader.read(0, null);

            reader.setInput(stream);
            BufferedImage actual = reader.read(0, null);

            assertImageDataEquals("", expected, actual);
        }
    }

    @Test
    public void testPlanarEqualInterleavedSeparated16() throws IOException {
        TestData expectedData = new TestData(getClassLoaderResource("/tiff/depth/flower-separated-contig-16.tif"), new Dimension(73, 43));
        TestData testData = new TestData(getClassLoaderResource("/tiff/depth/flower-separated-planar-16.tif"), new Dimension(73, 43));

        try (ImageInputStream expectedStream = expectedData.getInputStream(); ImageInputStream stream = testData.getInputStream()) {
            TIFFImageReader reader = createReader();

            reader.setInput(expectedStream);
            BufferedImage expected = reader.read(0, null);

            reader.setInput(stream);
            BufferedImage actual = reader.read(0, null);

            assertImageDataEquals("", expected, actual);
        }
    }

    @Test
    public void testPhotometricInterpretationFallback() throws IOException {
        String[] files = {
                "/tiff/guessPhotometric/group4.tif",
                "/tiff/guessPhotometric/flower-rgb-contig-08.tif",
                "/tiff/guessPhotometric/flower-separated-planar-08.tif"
        };

        final int[] results = {
                TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO,
                TIFFBaseline.PHOTOMETRIC_RGB,
                TIFFExtension.PHOTOMETRIC_SEPARATED
        };

        for (int i = 0; i < files.length; i++) {
            final AtomicBoolean foundWarning = new AtomicBoolean(false);
            final int expectedResult = results[i];

            try (ImageInputStream iis = ImageIO.createImageInputStream(getClassLoaderResource(files[i]))) {
                TIFFImageReader reader = createReader();

                reader.setInput(iis);
                reader.addIIOReadWarningListener(new IIOReadWarningListener() {
                    @Override
                    public void warningOccurred(ImageReader source, String warning) {
                        if (warning.equals("Missing PhotometricInterpretation, determining fallback: " + expectedResult)) {
                            foundWarning.set(true);
                        }
                    }
                });
                reader.read(0);
            }
            assertTrue("no correct guess for PhotometricInterpretation: " + results[i], foundWarning.get());
        }
    }

    @Test
    public void testReadWithSubsampleParamPixelsJPEG() throws IOException {
        // Tiled "new style" JPEG
        ImageReader reader = createReader();
        TestData data = new TestData(getClassLoaderResource("/tiff/quad-jpeg.tif"), new Dimension(512, 384));
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        BufferedImage image = null;
        BufferedImage subsampled = null;
        try {
            image = reader.read(0, param);

            param.setSourceSubsampling(2, 2, 0, 0);
            subsampled = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertSubsampledImageDataEquals("Subsampled image data does not match expected", image, subsampled, param);
    }

    @Test
    public void testReadWithSubsampleParamPixelsOldJPEG() throws IOException {
        ImageReader reader = createReader();
        TestData data = new TestData(getClassLoaderResource("/tiff/smallliz.tif"), new Dimension(160, 160));
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        BufferedImage image = null;
        BufferedImage subsampled = null;
        try {
            image = reader.read(0, param);

            param.setSourceSubsampling(2, 2, 0, 0);
            subsampled = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertSubsampledImageDataEquals("Subsampled image data does not match expected", image, subsampled, param);
    }

    @Test
    public void testReadWithSubsampleParamPixelsTiled() throws IOException {
        ImageReader reader = createReader();
        TestData data = new TestData(getClassLoaderResource("/tiff/cramps-tile.tif"), new Dimension(800, 607));
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        BufferedImage image = null;
        BufferedImage subsampled = null;
        try {
            image = reader.read(0, param);

            param.setSourceSubsampling(2, 2, 0, 0);
            subsampled = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertSubsampledImageDataEquals("Subsampled image data does not match expected", image, subsampled, param);
    }

    @Test
    public void testReadUnsupported() {
        ImageReader reader = createReader();

        for (TestData data : getUnsupportedTestData()) {
            reader.setInput(data.getInputStream());

            for (int i = 0; i < data.getImageCount(); i++) {
                try {
                    reader.read(i);
                    fail("Sample should be moved from unsupported to normal test case");
                }
                catch (IIOException e) {
                    assertThat(e.getMessage().toLowerCase(), containsString("unsupported"));
                }
                catch (Exception e) {
                    failBecause(String.format("Image %s index %s could not be read: %s", data.getInput(), i, e), e);
                }
            }
        }
    }

    @Test
    public void testStreamMetadataNonNull() {
        ImageReader reader = createReader();

        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());

            try {
                IIOMetadata streamMetadata = reader.getStreamMetadata();
                assertNotNull(streamMetadata);
                assertThat(streamMetadata, is(TIFFStreamMetadata.class));
            }
            catch (Exception e) {
                failBecause(String.format("Image %s could not be read: %s", data.getInput(), e), e);
            }
        }
    }

    @Test
    public void testStreamMetadataII() throws IOException {
        ImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/ccitt_tolessrows.tif"))) {
            reader.setInput(stream);
            TIFFStreamMetadata streamMetadata = (TIFFStreamMetadata) reader.getStreamMetadata();
            assertEquals(ByteOrder.LITTLE_ENDIAN, streamMetadata.byteOrder);
        }
    }

    @Test
    public void testStreamMetadataMM() throws IOException {
        ImageReader reader = createReader();

        try (ImageInputStream stream = ImageIO.createImageInputStream(getClassLoaderResource("/tiff/sm_colors_pb.tif"))) {
            reader.setInput(stream);
            TIFFStreamMetadata streamMetadata = (TIFFStreamMetadata) reader.getStreamMetadata();
            assertEquals(ByteOrder.BIG_ENDIAN, streamMetadata.byteOrder);
        }
    }

    @Test
    public void testReadRaster() {
        ImageReader reader = createReader();

        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());

            for (int i = 0; i < data.getImageCount(); i++) {
                Raster raster = null;

                try {
                    raster = reader.readRaster(i, null);
                }
                catch (Exception e) {
                    failBecause(String.format("Image %s index %s could not be read: %s", data.getInput(), i, e), e);
                }

                assertNotNull(String.format("Raster %s index %s was null!", data.getInput(), i), raster);

                assertEquals(
                        String.format("Raster %s index %s has wrong width: %s", data.getInput(), i, raster.getWidth()),
                        data.getDimension(i).width,
                        raster.getWidth()
                );
                assertEquals(
                        String.format("Raster %s index %s has wrong height: %s", data.getInput(), i, raster.getHeight()),
                        data.getDimension(i).height, raster.getHeight()
                );
            }
        }
    }
}
