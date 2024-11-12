package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


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