/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata;

import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ReaderAbstractTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ReaderAbstractTest.java,v 1.0 04.01.12 09:40 haraldk Exp$
 */
public abstract class MetadataWriterAbstractTest {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    protected final URL getResource(final String name) throws IOException {
        return getClass().getResource(name);
    }

    protected final ImageInputStream getDataAsIIS() throws IOException {
        return ImageIO.createImageInputStream(getData());
    }

    protected abstract InputStream getData() throws IOException;

    protected abstract MetadataWriter createWriter();

    @Test
    public void testWriteNullDirectory() throws IOException {

        assertThrows(IllegalArgumentException.class, () ->  createWriter().write(null, new MemoryCacheImageOutputStream(new ByteArrayOutputStream())));
    }

    @Test
    public void testWriteNullStream() throws IOException {
        assertThrows(IllegalArgumentException.class, () ->
                createWriter().write(new AbstractDirectory(new ArrayList<Entry>()) {
                }, null)
        );
    }
}
