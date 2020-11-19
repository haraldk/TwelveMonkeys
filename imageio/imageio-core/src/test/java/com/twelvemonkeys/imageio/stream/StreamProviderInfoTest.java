package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StreamProviderInfoTest {
    private final ProviderInfo providerInfo = new StreamProviderInfo();

    @Test
    public void testVendorName() {
        assertNotNull(providerInfo.getVendorName());
        assertEquals("TwelveMonkeys", providerInfo.getVendorName());
    }

    @Test
    public void testVersion() {
        assertNotNull(providerInfo.getVersion());
    }
}