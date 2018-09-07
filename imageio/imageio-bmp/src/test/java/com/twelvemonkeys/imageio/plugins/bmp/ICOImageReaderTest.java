/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

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
 * ICOImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class ICOImageReaderTest extends ImageReaderAbstractTest<ICOImageReader> {
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(
                        getClassLoaderResource("/ico/JavaCup.ico"),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16)
                ),
                new TestData(getClassLoaderResource("/ico/favicon.ico"), new Dimension(32, 32)),
                new TestData(
                        getClassLoaderResource("/ico/joypad.ico"),
                        new Dimension(16, 16), new Dimension(24, 24), new Dimension(32, 32), new Dimension(48, 48),
                        new Dimension(16, 16), new Dimension(24, 24), new Dimension(32, 32), new Dimension(48, 48)
                ),
                // Windows Vista icon, PNG encoded for 256x256 sizes
                new TestData(
                        getClassLoaderResource("/ico/down.ico"),
                        new Dimension(16, 16), new Dimension(16, 16), new Dimension(32, 32), new Dimension(32, 32),
                        new Dimension(48, 48), new Dimension(48, 48), new Dimension(256, 256), new Dimension(256, 256),
                        new Dimension(16, 16), new Dimension(32, 32), new Dimension(48, 48), new Dimension(256, 256)
                ),
                // Problematic icon that reports 24 bit in the descriptor, but has separate 1 bit ''mask (height 2 x icon height)!
                new TestData(getClassLoaderResource("/ico/rgb24bitmask.ico"), new Dimension(32, 32))
        );
    }

    protected ImageReaderSpi createProvider() {
        return new ICOImageReaderSpi();
    }

    @Override
    protected ICOImageReader createReader() {
        return new ICOImageReader();
    }

    protected Class<ICOImageReader> getReaderClass() {
        return ICOImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Collections.singletonList("ico");
    }

    protected List<String> getSuffixes() {
        return Collections.singletonList("ico");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/vnd.microsoft.icon", "image/ico", "image/x-icon");
    }

    @Test
    @Ignore("Known issue")
    @Override
    public void testNotBadCaching() throws IOException {
        super.testNotBadCaching();
    }

    @Test
    @Ignore("Known issue: Subsampled reading currently not supported")
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }
}