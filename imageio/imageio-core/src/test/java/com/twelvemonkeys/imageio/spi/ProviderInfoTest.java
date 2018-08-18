/*
 * Copyright (c) 2009, Harald Kuhr
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

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * ProviderInfoTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ProviderInfoTest.java,v 1.0 Oct 31, 2009 3:51:22 PM haraldk Exp$
 */
public class ProviderInfoTest {
    @Test
    public void testCreateNorma() {
        new ProviderInfo(Package.getPackage("java.util"));
    }

    @Test
    public void testCreateNullPackage() {
        try {
            new ProviderInfo(null);
            fail("IllegalArgumentException expected for null package");
        }
        catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("package"));
        }
    }

    @Test
    public void testGetVendorUnknownNonJARPackage() {
        ProviderInfo info = new ProviderInfo(mockNonJARPackage("org.foo"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("org.foo", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("Unspecified", version);
    }

    @Test
    public void testGetVendorNonJARTMPackage() {
        ProviderInfo info = new ProviderInfo(mockNonJARPackage("com.twelvemonkeys"));

        String vendor = info.getVendorName();
        assertNotNull(vendor);
        assertEquals("TwelveMonkeys", vendor);

        String version = info.getVersion();
        assertNotNull(version);
        assertEquals("DEV", version);
    }

    @Test
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
