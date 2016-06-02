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