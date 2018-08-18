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

package com.twelvemonkeys.lang;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * PlatformTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PlatformTest.java,v 1.0 11.04.12 16:21 haraldk Exp$
 */
public class PlatformTest {
    // TODO: Make a decision: 32/64 bit part of architecture, or separate property?
    // TODO: Should all i386/386/x86/i686 be normalized to x86?
    // TODO: Create a version class, to allow testing for equal or greater version requirements etc
    //       Break down version strings to tokens, compare numbers to numbers, strings to strings, etc.
    //       - 10.0 > 2.1 (numeric, numeric)
    //       - beta > alpha (alpha)
    //       - 1A > 1
    //       - 10 > 1A (what about hex numbering, does anyone use that?)
    //       - 1.0B > 1.0A (numeric, numeric, alpha)
    //       - 10.0RC > 10.0a > 10.0 ?? (numeric, numeric, alpha)
    //       - Feisty Fawn > Dapper Drake
    //       - special recognition of 'alpha', 'beta', 'rc' etc, to represent negative values??
    //       Have a look at the Maven version scheme/algorithm, and the JAR file spec (Package#isCompatibleWith(String))

    @Test
    public void testGet() {
        assertNotNull(Platform.get());
    }

    @Test
    public void testOS() {
        assertNotNull(Platform.os());
        assertEquals(Platform.get().getOS(), Platform.os());
    }

    @Test
    public void testVersion() {
        assertNotNull(Platform.version());
        assertEquals(Platform.get().getVersion(), Platform.version());
        assertEquals(System.getProperty("os.version"), Platform.version());
    }

    @Test
    public void testArch() {
        assertNotNull(Platform.arch());
        assertEquals(Platform.get().getArchitecture(), Platform.arch());
    }

    private static Properties createProperties(final String osName, final String osVersion, final String osArch) {
        Properties properties = new Properties();
        properties.put("os.name", osName);
        properties.put("os.version", osVersion);
        properties.put("os.arch", osArch);

        return properties;
    }

    @Test
    public void testCreateOSXx86_64() {
        Platform platform = new Platform(createProperties("Mac OS X", "10.7.3", "x86_64"));
        assertEquals(Platform.OperatingSystem.MacOS, platform.getOS());
        assertEquals(Platform.Architecture.X86, platform.getArchitecture());
    }

    @Test
    public void testCreateOSXDarwinx86() {
        Platform platform = new Platform(createProperties("Darwin", "0.0.0", "x86"));
        assertEquals(Platform.OperatingSystem.MacOS, platform.getOS());
        assertEquals(Platform.Architecture.X86, platform.getArchitecture());
    }

    @Test
    public void testCreateOSXPPC() {
        Platform platform = new Platform(createProperties("Mac OS X", "10.5.4", "PPC"));
        assertEquals(Platform.OperatingSystem.MacOS, platform.getOS());
        assertEquals(Platform.Architecture.PPC, platform.getArchitecture());
    }

    @Test
    public void testCreateWindows386() {
        Platform platform = new Platform(createProperties("Windows", "7.0.1.1", "i386"));
        assertEquals(Platform.OperatingSystem.Windows, platform.getOS());
        assertEquals(Platform.Architecture.X86, platform.getArchitecture());
    }

    @Ignore("Known issue, needs resolve")
    @Test
    public void testCreateWindows686() {
        Platform platform = new Platform(createProperties("Windows", "5.1", "686"));
        assertEquals(Platform.OperatingSystem.Windows, platform.getOS());
        assertEquals(Platform.Architecture.X86, platform.getArchitecture());
    }

    @Ignore("Known issue, needs resolve")
    @Test
    public void testCreateLinuxX86() {
        Platform platform = new Platform(createProperties("Linux", "3.0.18", "x86"));
        assertEquals(Platform.OperatingSystem.Linux, platform.getOS());
        assertEquals(Platform.Architecture.X86, platform.getArchitecture());
    }

    @Test
    public void testCreateLinuxPPC() {
        Platform platform = new Platform(createProperties("Linux", "2.6.11", "PPC"));
        assertEquals(Platform.OperatingSystem.Linux, platform.getOS());
        assertEquals(Platform.Architecture.PPC, platform.getArchitecture());
    }

    @Test
    public void testCreateSolarisSparc() {
        Platform platform = new Platform(createProperties("SunOS", "6.0", "Sparc"));
        assertEquals(Platform.OperatingSystem.Solaris, platform.getOS());
        assertEquals(Platform.Architecture.SPARC, platform.getArchitecture());
    }

    @Test
    public void testCreateSolarisX86() {
        Platform platform = new Platform(createProperties("Solaris", "5.0", "x86"));
        assertEquals(Platform.OperatingSystem.Solaris, platform.getOS());
        assertEquals(Platform.Architecture.X86, platform.getArchitecture());
    }

    @Test
    public void testCreateUnknownUnknown() {
        Platform platform = new Platform(createProperties("Amiga OS", "5.0", "68k"));
        assertEquals(Platform.OperatingSystem.Unknown, platform.getOS());
        assertEquals(Platform.Architecture.Unknown, platform.getArchitecture());
    }
}
