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

package com.twelvemonkeys.imageio.plugins.dcx;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.imageio.spi.ImageReaderSpi;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;

/**
 * DCXImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DCXImageReaderTest.java,v 1.0 03.07.14 22:28 haraldk Exp$
 */
public class DCXImageReaderTest extends ImageReaderAbstractTestCase<DCXImageReader> {
    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/dcx/input.dcx"), new Dimension(70, 46)) // RLE encoded RGB (the only sample I've found)
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new DCXImageReaderSpi();
    }

    @Override
    protected Class<DCXImageReader> getReaderClass() {
        return DCXImageReader.class;
    }

    @Override
    protected DCXImageReader createReader() {
        return new DCXImageReader(createProvider());
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("DCX", "dcx");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("dcx");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(
                "image/dcx", "image/x-dcx"
        );
    }
}
