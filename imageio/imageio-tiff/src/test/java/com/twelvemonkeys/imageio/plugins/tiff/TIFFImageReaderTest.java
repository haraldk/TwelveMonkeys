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

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.ImageReadParam;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
public class TIFFImageReaderTest extends ImageReaderAbstractTestCase<TIFFImageReader> {

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
                new TestData(getClassLoaderResource("/tiff/lzw-long-strings-sample.tif"), new Dimension(316, 173)) // RGBA, LZW compressed w/predictor
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
        return Arrays.asList("image/tiff");
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
        ImageInputStream stream = testData.getInputStream();

        try {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);
            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void testReadIncompatibleICCProfileIgnoredWithWarning() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/rgb-with-embedded-cmyk-icc.tif"), new Dimension(1500, 1500));

        ImageInputStream stream = testData.getInputStream();

        try {
            TIFFImageReader reader = createReader();
            reader.setInput(stream);

            IIOReadWarningListener warningListener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(warningListener);

            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(testData.getDimension(0), new Dimension(image.getWidth(), image.getHeight()));
            verify(warningListener, atLeastOnce()).warningOccurred(eq(reader), contains("ICC"));
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void testColorMap8Bit() throws IOException {
        TestData testData = new TestData(getClassLoaderResource("/tiff/scan-lzw-8bit-colormap.tiff"), new Dimension(2550, 3300));

        ImageInputStream stream = testData.getInputStream();

        try {
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
        finally {
            stream.close();
        }
    }
}
