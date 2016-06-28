/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * PCXImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PCXImageReaderTest.java,v 1.0 03.07.14 22:28 haraldk Exp$
 */
public class PCXImageReaderTest extends ImageReaderAbstractTest<PCXImageReader> {
    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/pcx/MARBLES.PCX"), new Dimension(1419, 1001)), // RLE encoded RGB
//                new TestData(getClassLoaderResource("/pcx/GMARBLES.PCX"), new Dimension(1419, 1001)) // RLE encoded gray (seems to be damanged, missing the last few scan lines)
                new TestData(getClassLoaderResource("/pcx/lena.pcx"), new Dimension(512, 512)), // RLE encoded RGB
                new TestData(getClassLoaderResource("/pcx/lena2.pcx"), new Dimension(512, 512)), // RLE encoded, 256 color indexed (8 bps/1 channel)
                new TestData(getClassLoaderResource("/pcx/lena3.pcx"), new Dimension(512, 512)), // RLE encoded, 16 color indexed (4 bps/1 channel)
                new TestData(getClassLoaderResource("/pcx/lena4.pcx"), new Dimension(512, 512)), // RLE encoded, 16 color indexed (1 bps/4 channels)
                new TestData(getClassLoaderResource("/pcx/lena5.pcx"), new Dimension(512, 512)), // RLE encoded, 256 color indexed (8 bps/1 channel)
                new TestData(getClassLoaderResource("/pcx/lena6.pcx"), new Dimension(512, 512)), // RLE encoded, 8 colorindexed (1 bps/3 channels)
                new TestData(getClassLoaderResource("/pcx/lena7.pcx"), new Dimension(512, 512)), // RLE encoded, 4 color indexed (1 bps/2 channels)
                new TestData(getClassLoaderResource("/pcx/lena8.pcx"), new Dimension(512, 512)), // RLE encoded, 4 color indexed (2 bps/1 channel)
                new TestData(getClassLoaderResource("/pcx/lena9.pcx"), new Dimension(512, 512)), // RLE encoded, 2 color indexed (1 bps/1 channel)
                new TestData(getClassLoaderResource("/pcx/lena10.pcx"), new Dimension(512, 512)), // RLE encoded, 16 color indexed (4 bps/1 channel) (uses only 8 colors)
                new TestData(getClassLoaderResource("/pcx/DARKSTAR.PCX"), new Dimension(88, 52)), // RLE encoded monochrome (1 bps/1 channel)
                // TODO: Get correct colors for CGA mode, see cga-pcx.txt (however, the text seems to be in error, the bits are not as described)
                new TestData(getClassLoaderResource("/pcx/CGA_BW.PCX"), new Dimension(640, 200)), // RLE encoded indexed (CGA mode)
                new TestData(getClassLoaderResource("/pcx/CGA_FSD.PCX"), new Dimension(320, 200)), // RLE encoded indexed (CGA mode)
                new TestData(getClassLoaderResource("/pcx/CGA_RGBI.PCX"), new Dimension(320, 200)), // RLE encoded indexed (CGA mode)
                new TestData(getClassLoaderResource("/pcx/CGA_TST1.PCX"), new Dimension(320, 200)) // RLE encoded indexed (CGA mode)
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new PCXImageReaderSpi();
    }

    @Override
    protected Class<PCXImageReader> getReaderClass() {
        return PCXImageReader.class;
    }

    @Override
    protected PCXImageReader createReader() {
        return new PCXImageReader(createProvider());
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
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        TestData data = getTestData().get(1);
        assertReadWithSourceRegionParamEqualImage(new Rectangle(200, 0, 4, 4), data, 0);
        assertReadWithSourceRegionParamEqualImage(new Rectangle(100, 100, 4, 4), data, 0);
        assertReadWithSourceRegionParamEqualImage(new Rectangle(0, 200, 4, 4), data, 0);
    }
}
