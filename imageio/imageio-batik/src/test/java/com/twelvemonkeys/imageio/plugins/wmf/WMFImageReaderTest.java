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

package com.twelvemonkeys.imageio.plugins.wmf;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * WMFImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: WMFImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class WMFImageReaderTest extends ImageReaderAbstractTest<WMFImageReader> {
    private WMFImageReaderSpi provider = new WMFImageReaderSpi();

    protected List<TestData> getTestData() {
        return Collections.singletonList(
                // TODO: Dimensions does not look right...
                new TestData(getClassLoaderResource("/wmf/test.wmf"), new Dimension(841, 673))
        );
    }

    protected ImageReaderSpi createProvider() {
        return provider;
    }

    @Override
    protected WMFImageReader createReader() {
        return new WMFImageReader(createProvider());
    }

    protected Class<WMFImageReader> getReaderClass() {
        return WMFImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Collections.singletonList("wmf");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("wmf", "emf");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/x-wmf", "application/x-msmetafile");
    }

    @Test
    @Ignore("Known issue: Source region reading not supported")
    @Override
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        super.testReadWithSourceRegionParamEqualImage();
    }
}