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

package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.imageio.stream.BufferedImageInputStream;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.io.ole2.Entry;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * ICOImageReaderTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTestCase.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class ThumbsDBImageReaderTestCase extends ImageReaderAbstractTestCase<ThumbsDBImageReader> {
    private ThumbsDBImageReaderSpi mProvider = new ThumbsDBImageReaderSpi();

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
        return mProvider;
    }

    @Override
    protected ThumbsDBImageReader createReader() {
        return new ThumbsDBImageReader(mProvider);
    }

    protected Class<ThumbsDBImageReader> getReaderClass() {
        return ThumbsDBImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Arrays.asList("thumbs");
    }

    protected List<String> getSuffixes() {
        return Arrays.asList("db");
    }

    protected List<String> getMIMETypes() {
        return Arrays.asList("image/x-thumbs-db");
    }

    public void testArrayIndexOutOfBoundsBufferedReadBug() throws IOException {
        ImageInputStream input = new BufferedImageInputStream(new MemoryCacheImageInputStream(getClass().getResourceAsStream("/thumbsdb/Thumbs-camera.db")));
        input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        Entry root = new CompoundDocument(input).getRootEntry();
        
        Entry child = root.getChildEntry("Catalog");
        child.getInputStream();
    }
}