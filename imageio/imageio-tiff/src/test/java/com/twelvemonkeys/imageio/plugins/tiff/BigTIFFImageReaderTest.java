/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BigTIFFImageReaderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: BigTIFFImageReaderTest.java,v 1.0 26/04/2017 harald.kuhr Exp$
 */
public class BigTIFFImageReaderTest extends ImageReaderAbstractTest<TIFFImageReader> {

    @Override
    protected ImageReaderSpi createProvider() {
        return new BigTIFFImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/bigtiff/BigTIFF.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFMotorola.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFLong.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFLong8.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFMotorolaLongStrips.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFLong8Tiles.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFSubIFD4.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/BigTIFFSubIFD8.tif"), new Dimension(64, 64)),
                new TestData(getClassLoaderResource("/bigtiff/G32DS.tif"), new Dimension(2464, 3248))
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("bigtiff", "BigTIFF", "BIGTIFF");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("tif", "tiff", "btf", "tf8");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/tiff");
    }

    // TODO: Test that all BigTIFFs are decoded equal to the classic TIFF

    // TODO: Test metadata
}
