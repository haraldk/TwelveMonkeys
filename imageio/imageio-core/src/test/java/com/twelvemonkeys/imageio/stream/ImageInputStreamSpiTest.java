package com.twelvemonkeys.imageio.stream;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageInputStreamSpi;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

abstract class ImageInputStreamSpiTest<T> {
    protected final ImageInputStreamSpi provider = createProvider();

    @SuppressWarnings("unchecked")
    protected final Class<T> inputClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    protected abstract ImageInputStreamSpi createProvider();

    protected abstract T createInput() throws IOException;

    @Test
    public void testInputClass() {
        assertEquals(inputClass, provider.getInputClass());
    }

    @Test
    public void testVendorName() {
        assertNotNull(provider.getVendorName());
        assertEquals("TwelveMonkeys", provider.getVendorName());
    }

    @Test
    public void testVersion() {
        assertNotNull(provider.getVersion());
    }

    @Test
    public void testDescription() {
        assertNotNull(provider.getDescription(null));
        assertNotNull(provider.getDescription(Locale.ENGLISH));
    }

    @Test
    public void createNull() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> provider.createInputStreamInstance(null));
    }

    @Test
    public void createNullCached() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> provider.createInputStreamInstance(null, true, ImageIO.getCacheDirectory()));
    }

    @Test
    public void createCachedNullCache() throws IOException {
        try {
            provider.createInputStreamInstance(createInput(), true, null);
        }
        catch (IllegalArgumentException expected) {
            // All good
            assertFalse(provider.needsCacheFile());
        }
    }

    @Test
    public void create() throws IOException {
        assertNotNull(provider.createInputStreamInstance(createInput()));
    }

    @Test
    public void createCached() throws IOException {
        if (provider.canUseCacheFile()) {
            assertNotNull(provider.createInputStreamInstance(createInput(), true, ImageIO.getCacheDirectory()));
        }
    }

    @Test
    public void createNonCached() throws IOException {
        if (!provider.needsCacheFile()) {
            assertNotNull(provider.createInputStreamInstance(createInput(), false, ImageIO.getCacheDirectory()));
        }
    }
}
