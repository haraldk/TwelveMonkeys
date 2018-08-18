/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.spi;

import org.hamcrest.Description;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * ReaderWriterProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ReaderWriterProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public abstract class ReaderWriterProviderInfoTest {

    private final ReaderWriterProviderInfo providerInfo = createProviderInfo();

    protected abstract ReaderWriterProviderInfo createProviderInfo();

    protected final ReaderWriterProviderInfo getProviderInfo() {
        return providerInfo;
    }

    @Test
    public void readerClassName() throws Exception {
        assertClassExists(providerInfo.readerClassName(), ImageReader.class);
    }

    @Test
    public void readerSpiClassNames() throws Exception {
        assertClassesExist(providerInfo.readerSpiClassNames(), ImageReaderSpi.class);
    }

    @Test
    public void inputTypes() throws Exception {
        assertNotNull(providerInfo.inputTypes());
    }

    @Test
    public void writerClassName() throws Exception {
        assertClassExists(providerInfo.writerClassName(), ImageWriter.class);
    }

    @Test
    public void writerSpiClassNames() throws Exception {
        assertClassesExist(providerInfo.writerSpiClassNames(), ImageWriterSpi.class);
    }

    @Test
    public void outputTypes() throws Exception {
        assertNotNull(providerInfo.outputTypes());
    }

    @Test
    public void nativeStreamMetadataFormatClassName() throws Exception {
        assertClassExists(providerInfo.nativeStreamMetadataFormatClassName(), IIOMetadataFormat.class);
    }

    @Test
    public void extraStreamMetadataFormatClassNames() throws Exception {
        assertClassesExist(providerInfo.extraStreamMetadataFormatClassNames(), IIOMetadataFormat.class);
    }

    @Test
    public void nativeImageMetadataFormatClassName() throws Exception {
        assertClassExists(providerInfo.nativeImageMetadataFormatClassName(), IIOMetadataFormat.class);
    }

    @Test
    public void extraImageMetadataFormatClassNames() throws Exception {
        assertClassesExist(providerInfo.extraImageMetadataFormatClassNames(), IIOMetadataFormat.class);
    }

    @Test
    public void formatNames() {
        String[] names = providerInfo.formatNames();
        assertNotNull(names);
        assertFalse(names.length == 0);

        List<String> list = asList(names);

        for (String name : list) {
            assertNotNull(name);
            assertFalse(name.isEmpty());

            assertTrue(list.contains(name.toLowerCase()));
            assertTrue(list.contains(name.toUpperCase()));
        }
    }

    @Test
    public void suffixes() {
        String[] suffixes = providerInfo.suffixes();
        assertNotNull(suffixes);
        assertFalse(suffixes.length == 0);

        for (String suffix : suffixes) {
            assertNotNull(suffix);
            assertFalse(suffix.isEmpty());
        }
    }

    @Test
    public void mimeTypes() {
        String[] mimeTypes = providerInfo.mimeTypes();
        assertNotNull(mimeTypes);
        assertFalse(mimeTypes.length == 0);

        for (String mimeType : mimeTypes) {
            assertNotNull(mimeType);
            assertFalse(mimeType.isEmpty());

            assertTrue(mimeType.length() > 1);
            assertTrue(mimeType.indexOf('/') > 0);
            assertTrue(mimeType.indexOf('/') < mimeType.length() - 1);
        }
    }

    public static <T> void assertClassExists(final String className, final Class<T> type) {
        if (className != null) {
            try {
                final Class<?> cl = Class.forName(className);

                assertThat(cl, new TypeSafeMatcher<Class<?>>() {
                    @Override
                    public boolean matchesSafely(Class<?> item) {
                        return type.isAssignableFrom(cl);
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("is subclass of ").appendValue(type);
                    }
                });
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
                fail("Class not found: " + e.getMessage());
            }
        }
    }

    public static <T> void assertClassesExist(final String[] classNames, final Class<T> type) {
        if (classNames != null) {
            for (String className : classNames) {
                assertClassExists(className, type);
            }
        }
    }
}