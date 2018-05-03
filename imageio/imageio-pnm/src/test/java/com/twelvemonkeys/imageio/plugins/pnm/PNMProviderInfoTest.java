package com.twelvemonkeys.imageio.plugins.pnm;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * PNMProviderInfoTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PNMProviderInfoTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class PNMProviderInfoTest {
    @Test
    public void vendorVersion() {
        PNMProviderInfo providerInfo = new PNMProviderInfo();
        assertNotNull(providerInfo.getVendorName());
        assertNotNull(providerInfo.getVersion());
    }
}