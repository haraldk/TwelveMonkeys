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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.ImagingOpException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * SVGImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SVGImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class SVGImageReaderTestCase extends ImageReaderAbstractTestCase<SVGImageReader> {
    private SVGImageReaderSpi provider = new SVGImageReaderSpi();

    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/svg/batikLogo.svg"), new Dimension(450, 500))
        );
    }

    protected ImageReaderSpi createProvider() {
        return provider;
    }

    @Override
    protected SVGImageReader createReader() {
        return new SVGImageReader(createProvider());
    }

    protected Class<SVGImageReader> getReaderClass() {
        return SVGImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("svg");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("svg");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/svg+xml");
    }

    @Test
    @Override
    public void testReadWithSizeParam() {
        try {
            super.testReadWithSizeParam();
        }
        catch (AssertionError failure) {
            Throwable cause = failure;

            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause instanceof ImagingOpException && cause.getMessage().equals("Unable to transform src image")) {
                // This is a very strange regression introduced by the later JDK/JRE (at least it's in 7u45)
                // Haven't found a workaround yet
                System.err.println("WARNING: Oracle JRE 7u45 broke my SVGImageReader (known issue): " + cause.getMessage());
            }
            else {
                throw failure;
            }
        }
    }

    @Test
    @Ignore("Known issue: Source region reading not supported")
    @Override
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        super.testReadWithSourceRegionParamEqualImage();
    }
}