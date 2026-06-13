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

package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * SGIImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SGIImageReaderTest.java,v 1.0 03.07.14 22:28 haraldk Exp$
 */
public class SGIImageReaderTest extends ImageReaderAbstractTest<SGIImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new SGIImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/sgi/input.sgi"), new Dimension(70, 46)), // RLE encoded RGB
                new TestData(getClassLoaderResource("/sgi/MARBLES.SGI"), new Dimension(1419, 1001)) // RLE encoded RGB
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("SGI", "sgi");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("sgi");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(
                "image/sgi", "image/x-sgi"
        );
    }

    @Test
    public void testNormalize8Bit() throws IOException {
        // 3x1 8-bit grayscale SGI with PixMin=0, PixMax=200
        // Raw pixel values: [0, 100, 200]
        // After normalization: [0, 127, 255] — PixMax should map to full white
        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/sgi/normalize_8bit.sgi"))) {
            SGIImageReader reader = createReader();
            reader.setInput(input);

            BufferedImage image = reader.read(0);
            assertNotNull(image);

            // Pixel at x=2 has raw value 200 (= PixMax), should normalize to 255
            assertRGBEquals("8-bit PixMax value should normalize to white", 0xffffffff, image.getRGB(2, 0), 1);

            // Pixel at x=0 has raw value 0 (= PixMin), should remain black
            assertRGBEquals("8-bit PixMin value should be black", 0xff000000, image.getRGB(0, 0), 0);

            // Pixel at x=1 has raw value 100 (half of PixMax), should normalize to ~127
            assertRGBEquals("8-bit mid value should normalize correctly", 0xff7f7f7f, image.getRGB(1, 0), 1);

            reader.dispose();
        }
    }

    @Test
    public void testNormalize16Bit() throws IOException {
        // 3x1 16-bit grayscale SGI with PixMin=0, PixMax=1000
        // Raw pixel values: [0, 500, 1000]
        // After normalization: [0, 32767, 65535] — PixMax should map to full white
        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/sgi/normalize_16bit.sgi"))) {
            SGIImageReader reader = createReader();
            reader.setInput(input);

            BufferedImage image = reader.read(0);
            assertNotNull(image);

            // Pixel at x=2 has raw value 1000 (= PixMax), should normalize to 65535 (white)
            assertRGBEquals("16-bit PixMax value should normalize to white", 0xffffffff, image.getRGB(2, 0), 1);

            // Pixel at x=0 has raw value 0 (= PixMin), should remain black
            assertRGBEquals("16-bit PixMin value should be black", 0xff000000, image.getRGB(0, 0), 0);

            // Pixel at x=1 has raw value 500 (half of PixMax), should normalize to ~32767 (~mid-gray)
            assertRGBEquals("16-bit mid value should normalize correctly", 0xff7f7f7f, image.getRGB(1, 0), 2);

            reader.dispose();
        }
    }
}
