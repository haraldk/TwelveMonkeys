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

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

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
                new TestData(getClassLoaderResource("/tiff/quad-lzw.tif"), new Dimension(512, 384)), // RGB, OLD LZW compressed, tiled
                new TestData(getClassLoaderResource("/tiff/bali.tif"), new Dimension(725, 489)), // Palette-based, LZW compressed
                new TestData(getClassLoaderResource("/tiff/f14.tif"), new Dimension(640, 480)), // Gray, uncompressed
                new TestData(getClassLoaderResource("/tiff/marbles.tif"), new Dimension(1419, 1001)), // RGB, LZW compressed w/predictor
                new TestData(getClassLoaderResource("/tiff/chifley_logo.tif"), new Dimension(591, 177)) // CMYK, uncompressed
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
}
