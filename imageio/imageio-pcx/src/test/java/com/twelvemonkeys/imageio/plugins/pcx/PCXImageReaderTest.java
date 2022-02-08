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

package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * PCXImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PCXImageReaderTest.java,v 1.0 03.07.14 22:28 haraldk Exp$
 */
public class PCXImageReaderTest extends ImageReaderAbstractTest<PCXImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new PCXImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/pcx/input.pcx"), new Dimension(70, 46)), // RLE encoded RGB, v5
                new TestData(getClassLoaderResource("/pcx/rose.pcx"), new Dimension(38, 48)), // RLE encoded, 16 color indexed (1 bps/4 channels), v5
                new TestData(getClassLoaderResource("/pcx/animals.pcx"), new Dimension(239, 157)), // RLE encoded, 8 color indexed (1 bps/3 channels), v3
                // TODO: Find good test images for the various combinations of bits/sample & channels
                new TestData(getClassLoaderResource("/pcx/DARKSTAR.PCX"), new Dimension(88, 52)), // RLE encoded monochrome (1 bps/1 channel)
                new TestData(getClassLoaderResource("/pcx/MARBLES.PCX"), new Dimension(1419, 1001)), // RLE encoded RGB
                new TestData(getClassLoaderResource("/pcx/no-palette-monochrome.pcx"), new Dimension(128, 152)), // RLE encoded monochrome (1 bps/1 channel)
                // See cga-pcx.txt, however, the text seems to be in error, I don't see how the bits can be as described
                new TestData(getClassLoaderResource("/pcx/CGA_BW.PCX"), new Dimension(640, 200)), // RLE encoded indexed (CGA mode)
                new TestData(getClassLoaderResource("/pcx/CGA_FSD.PCX"), new Dimension(320, 200)), // RLE encoded indexed (CGA mode)
                new TestData(getClassLoaderResource("/pcx/CGA_RGBI.PCX"), new Dimension(320, 200)), // RLE encoded indexed (CGA mode)
                new TestData(getClassLoaderResource("/pcx/CGA_TST1.PCX"), new Dimension(320, 200)) // RLE encoded indexed (CGA mode)
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("PCX", "pcx");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("pcx");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(
                "image/pcx", "image/x-pcx"
        );
    }

    @Test
    public void testReadGray() throws IOException {
        // Seems like the last scan lines have been overwritten by an unnecessary 768 byte palette + 1 byte magic...
        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/pcx/GMARBLES.PCX"))) {
            PCXImageReader reader = createReader();
            reader.setInput(input);

            assertEquals(1, reader.getNumImages(true));
            assertEquals(1419, reader.getWidth(0));
            assertEquals(1001, reader.getHeight(0));

            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(1419, 1000)); // Ignore the last garbled line

            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(BufferedImage.TYPE_BYTE_INDEXED, image.getType());
            assertEquals(1419, image.getWidth());
            assertEquals(1000, image.getHeight());
        }
    }

    @Test
    public void testReadMonochromeNoPalette() throws IOException {
        // Monochrome image V3 (no palette), palette is all 0's
        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/pcx/no-palette-monochrome.pcx"))) {
            PCXImageReader reader = createReader();
            reader.setInput(input);

            assertEquals(1, reader.getNumImages(true));
            assertEquals(128, reader.getWidth(0));
            assertEquals(152, reader.getHeight(0));

            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(BufferedImage.TYPE_BYTE_BINARY, image.getType());
            assertEquals(128, image.getWidth());
            assertEquals(152, image.getHeight());

            assertRGBEquals("Should have white background", 0xffffffff, image.getRGB(0, 0), 0);
            assertRGBEquals("Should have black skull", 0xff000000, image.getRGB(64, 10), 0);
        }
    }

    @Override
    @Test
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        TestData data = getTestData().get(0);
        assertReadWithSourceRegionParamEqualImage(new Rectangle(66, 0, 4, 4), data, 0);
        assertReadWithSourceRegionParamEqualImage(new Rectangle(32, 20, 4, 4), data, 0);
        assertReadWithSourceRegionParamEqualImage(new Rectangle(0, 42, 4, 4), data, 0);
    }
}
