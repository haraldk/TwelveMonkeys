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

package com.twelvemonkeys.imageio.reference;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import com.twelvemonkeys.lang.SystemUtil;

import com.sun.imageio.plugins.jpeg.JPEGImageReader;


import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.twelvemonkeys.imageio.util.IIOUtil.lookupProviderByName;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JPEGImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReaderTest.java,v 1.0 Oct 9, 2009 3:37:25 PM haraldk Exp$
 */
public class JPEGImageReaderTest extends ImageReaderAbstractTest<JPEGImageReader> {
    private static final boolean IS_JAVA_6_OR_LATER = SystemUtil.isClassAvailable("java.util.Deque");

    @Override
    protected ImageReaderSpi createProvider() {
        return lookupProviderByName(IIORegistry.getDefaultInstance(), "com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi", ImageReaderSpi.class);
    }

    @Override
    protected List<TestData> getTestData() {
        return Collections.singletonList(
                new TestData(getClassLoaderResource("/jpeg/R-7439-1151526181.jpeg"), new Dimension(386, 396))
        );
    }

    // These are NOT correct implementations, but I don't really care here
    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList(provider.getFormatNames());
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList(provider.getFileSuffixes());
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList(provider.getMIMETypes());
    }

    @Test
    @Override
    public void testSetDestination() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        assumeTrue(IS_JAVA_6_OR_LATER, "Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        super.testSetDestination();
    }

    @Test
    @Override
    public void testSetDestinationType() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        assumeTrue(IS_JAVA_6_OR_LATER, "Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        super.testSetDestinationType();
    }

    @Test
    @Disabled("Known issue")
    @Override
    public void testReadAsRenderedImageIndexOutOfBounds() throws IOException {
        super.testReadAsRenderedImageIndexOutOfBounds();
    }

    @Test
    @Disabled("JDK 25 now supports reading Exif thumbnails, but does not report thumbnail progress")
    public void testThumbnailProgress() throws IOException {
        super.testThumbnailProgress();
    }
    
    @Test
    @Disabled("No test data with JFIF thumbnail")
    @Override
    public void testNotBadCachingThumbnails() throws IOException {
        super.testNotBadCachingThumbnails();
    }
}
