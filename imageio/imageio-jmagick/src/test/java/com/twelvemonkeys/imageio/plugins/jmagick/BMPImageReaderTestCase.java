/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jmagick;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class BMPImageReaderTestCase extends JMagickImageReaderAbstractTestCase<BMPImageReader> {
    private BMPImageReaderSpi mProvider = new BMPImageReaderSpi();

    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/bmp/Blue Lace 16.bmp"), new Dimension(48, 48)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask444.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask555.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_16_bitmask565.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_24.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32_bitmask888.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_32_bitmask888_reversed.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4-IM.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_4.rle"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8-IM.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_8.rle"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_gray.bmp"), new Dimension(301, 331)),
                new TestData(getClassLoaderResource("/bmp/blauesglas_mono.bmp"), new Dimension(301, 331))
        );
    }

    protected Class<BMPImageReader> getReaderClass() {
        return BMPImageReader.class;
    }

    protected BMPImageReader createReader() {
        return new BMPImageReader(mProvider);
    }

    protected ImageReaderSpi createProvider() {
        return new BMPImageReaderSpi();
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("bmp");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("bmp", "rle", "dib");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/bmp", "image/x-bmp", "image/x-windows-bmp", "image/x-ms-bmp");
    }
}
