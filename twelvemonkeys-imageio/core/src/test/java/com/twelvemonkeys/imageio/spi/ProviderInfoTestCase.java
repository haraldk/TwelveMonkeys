package com.twelvemonkeys.imageio.spi;

import junit.framework.TestCase;

import java.net.URL;

/**
 * ProviderInfoTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ProviderInfoTestCase.java,v 1.0 Oct 31, 2009 3:51:22 PM haraldk Exp$
 */
public class ProviderInfoTestCase extends TestCase {
    public void testCreateNorma() {
        new ProviderInfo(Package.getPackage("java.util"));
    }

    public void testCreateNullPackage() {
        try {
            new ProviderInfo(null);
            fail("IllegalArgumentException expected for null package");
        }
        catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("package"));
        }
    }

    public void testGetVendorNonJARPackage() {
        ProviderInfo info = new ProviderInfo(mockNonJARPackage("org.foo"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("org.foo", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("DEV", version);
    }

    public void testGetVendorNonJARTMPackage() {
        ProviderInfo info = new ProviderInfo(mockNonJARPackage("com.twelvemonkeys"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("TwelveMonkeys", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("DEV", version);
    }

    public void testGetVendorKnownJARPackage() {
        ProviderInfo info = new ProviderInfo(mockJARPackage("com.acme", "1.7u4-BETA-b39", "Acme"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("Acme", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("1.7u4-BETA-b39", version);
    }

    private Package mockNonJARPackage(final String pName) {
        return new MockClassLoader().mockPackage(
                pName,
                null, null, null,
                null, null, null,
                null
        );
    }

    private Package mockJARPackage(final String pName, final String pImplVersion, final String pImplVendor) {
        return new MockClassLoader().mockPackage(
                pName,
                "The almighty specification", "1.0", "Acme Inc",
                "The buggy implementation", pImplVersion, pImplVendor,
                null
        );
    }

    private static class MockClassLoader extends ClassLoader {
        protected MockClassLoader() {
            super(null);
        }

        public Package mockPackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
            return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        }

        @Override
        protected Package getPackage(String name) {
            return null; // Allow re-createing packages
        }
    }
}
