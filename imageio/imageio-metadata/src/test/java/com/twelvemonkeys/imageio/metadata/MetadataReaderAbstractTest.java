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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * ReaderAbstractTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ReaderAbstractTest.java,v 1.0 04.01.12 09:40 haraldk Exp$
 */
public abstract class MetadataReaderAbstractTest {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    protected final URL getResource(final String name) {
        return getClass().getResource(name);
    }

    protected final ImageInputStream getDataAsIIS() throws IOException {
        return ImageIO.createImageInputStream(getData());
    }

    protected abstract InputStream getData() throws IOException;

    protected abstract MetadataReader createReader();

    @Test(expected = IllegalArgumentException.class)
    public void testReadNull() throws IOException {
        createReader().read(null);
    }

    @Test
    public void testRead() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());
        assertNotNull(directory);
    }

    protected static Matcher<Entry> hasValue(final Object value) {
        return new EntryHasValue(value);
    }

    private static class EntryHasValue extends TypeSafeMatcher<Entry> {
        private final Object value;

        public EntryHasValue(final Object value) {
            this.value = value;
        }

        @Override
        public boolean matchesSafely(final Entry entry) {
            return entry != null && (value == null ? entry.getValue() == null : valueEquals(value, entry.getValue()));
        }

        private static boolean valueEquals(final Object expected, final Object actual) {
            return expected.getClass().isArray() ? AbstractEntry.arrayEquals(expected, actual) : expected.equals(actual);
        }

        public void describeTo(final Description description) {
            description.appendText("has value ");
            description.appendValue(value);
        }
    }
}
