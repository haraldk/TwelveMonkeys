/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.imageio.stream.BufferedImageInputStream;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.io.ole2.Entry;
import com.twelvemonkeys.lang.SystemUtil;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * ICOImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class ThumbsDBImageReaderTest extends ImageReaderAbstractTest<ThumbsDBImageReader> {
    private static final boolean IS_JAVA_6 = SystemUtil.isClassAvailable("java.util.Deque");

    private ThumbsDBImageReaderSpi provider = new ThumbsDBImageReaderSpi();

    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(
                        getClassLoaderResource("/thumbsdb/Thumbs.db"),
                        new Dimension(96, 96), new Dimension(96, 96), new Dimension(16, 16), 
                        new Dimension(96, 45), new Dimension(63, 96), new Dimension(96, 96),
                        new Dimension(96, 64), new Dimension(96, 96), new Dimension(96, 77)
                ),
                new TestData(
                        getClassLoaderResource("/thumbsdb/Thumbs-camera.db"),
                        new Dimension(96, 96),
                        new Dimension(64, 96),
                        new Dimension(96, 96),
                        new Dimension(96, 64), new Dimension(64, 96),
                        new Dimension(96, 64), new Dimension(96, 64), new Dimension(96, 64),
                        new Dimension(64, 96), new Dimension(64, 96), new Dimension(64, 96),
                        new Dimension(64, 96), new Dimension(64, 96), new Dimension(64, 96),
                        new Dimension(96, 64),
                        new Dimension(64, 96),
                        new Dimension(96, 64), new Dimension(96, 64), new Dimension(96, 64),
                        new Dimension(64, 96), new Dimension(64, 96),
                        new Dimension(64, 96), new Dimension(64, 96), new Dimension(64, 96)
                )
        );
    }

    protected ImageReaderSpi createProvider() {
        return provider;
    }

    @Override
    protected ThumbsDBImageReader createReader() {
        return new ThumbsDBImageReader(provider);
    }

    protected Class<ThumbsDBImageReader> getReaderClass() {
        return ThumbsDBImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Collections.singletonList("thumbs");
    }

    protected List<String> getSuffixes() {
        return Collections.singletonList("db");
    }

    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/x-thumbs-db");
    }

    @Test
    public void testArrayIndexOutOfBoundsBufferedReadBug() throws IOException {
        ImageInputStream input = new BufferedImageInputStream(new MemoryCacheImageInputStream(getClass().getResourceAsStream("/thumbsdb/Thumbs-camera.db")));
        input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        Entry root = new CompoundDocument(input).getRootEntry();
        
        Entry child = root.getChildEntry("Catalog");

        assertNotNull(child);
        assertNotNull(child.getInputStream());
    }

    @Test
    @Override
    public void testSetDestination() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        if (IS_JAVA_6) {
            super.testSetDestination();
        }
        else {
            System.err.println("WARNING: Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        }
    }

    @Test
    @Override
    public void testSetDestinationType() throws IOException {
        // Known bug in Sun JPEGImageReader before Java 6
        if (IS_JAVA_6) {
            super.testSetDestinationType();
        }
        else {
            System.err.println("WARNING: Test skipped due to known bug in Java 1.5, please test again with Java 6 or later");
        }
    }

    @Test
    @Ignore("Known issue")
    @Override
    public void testNotBadCaching() throws IOException {
        super.testNotBadCaching();
    }
}