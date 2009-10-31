package com.twelvemonkeys.imageio.spi;

import junit.framework.TestCase;

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

    public void testGetVendorUnknownPackage() {
        // TODO: FixMe: This test will fail if for some reason JUnit adds manifest info to their JAR..
        ProviderInfo info = new ProviderInfo(Package.getPackage("junit.framework"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("junit.framework", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("DEV", version);
    }

    public void testGetVendorTMPackage() {
        // TODO: FixMe: This test will fail if for some reason the tests are run from within a JAR-file,
        // and depends on implementation details. 
        ProviderInfo info = new ProviderInfo(getClass().getPackage());

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("TwelveMonkeys", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("DEV", version);
    }

    public void testGetVendorKnownPackage() {
        // TODO: FixMe: This test depends on implementation details, and may fail on various JRE's...
        ProviderInfo info = new ProviderInfo(Package.getPackage("java.util"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertFalse("".equals(vendor));

        // NOTE: Does not work: assertEquals(System.getProperty("java.vendor"), vendor);
        assertFalse("TwelveMonkeys".equals(vendor));
        assertFalse("java.util".equals(vendor));

        String version = info.getVersion();
        assertNotNull(version);
        assertFalse("".equals(version));
        assertFalse("DEV".equals(version));
        
        // Is this safe to assume?
        String jreVersion = System.getProperty("java.version");
        assertTrue(jreVersion.equals(version) || jreVersion.startsWith(version) || version.startsWith(jreVersion));
    }
}
