/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * ICNSImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICNSImageReaderTest.java,v 1.0 25.10.11 18:44 haraldk Exp$
 */
public class ICNSImageReaderTest extends ImageReaderAbstractTestCase {
    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(
                        getClassLoaderResource("/icns/GenericJavaApp.icns"),
                        new Dimension(16, 16),                          // 1 bit + 1 bit mask
                        new Dimension(16, 16), new Dimension(16, 16),   // 8 bit CMAP, 32 bit
                        new Dimension(32, 32),                          // 1 bit + 1 bit mask
                        new Dimension(32, 32), new Dimension(32, 32),   // 8 bit CMAP, 32 bit
                        new Dimension(128, 128)                         // 32 bit
                ),
                new TestData(
                        getClassLoaderResource("/icns/Apple Retro.icns"),
                        new Dimension(16, 16),                          // 24 bit + 8 bit mask
                        new Dimension(32, 32),                          // 24 bit + 8 bit mask
                        new Dimension(48, 48),                          // 24 bit + 8 bit mask
                        new Dimension(128, 128)                         // 24 bit + 8 bit mask
//,                       new Dimension(256, 256), // JPEG 2000, not readable without JAI or other JPEG 2000 support
//                        new Dimension(512, 512)  // JPEG 2000
                ),
                new TestData(
                        getClassLoaderResource("/icns/7zIcon.icns"), // Contains the icnV resource, that isn't an icon
                        new Dimension(16, 16),                          // 24 bit + 8 bit mask
                        new Dimension(32, 32),                          // 24 bit + 8 bit mask
                        new Dimension(128, 128)                         // 24 bit + 8 bit mask
//,                       new Dimension(256, 256), // JPEG 2000
//                        new Dimension(512, 512)  // JPEG 2000
                )
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new ICNSImageReaderSpi();
    }

    @Override
    protected ImageReader createReader() {
        return new ICNSImageReader();
    }

    @Override
    protected Class getReaderClass() {
        return ICNSImageReader.class;
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("icns");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("icns");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/x-apple-icons");
    }
}
