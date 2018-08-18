/*
 * Copyright (c) 2008, Harald Kuhr
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

import java.util.Properties;

/**
 * Platform
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/Platform.java#1 $
 */
public final class Platform {
    /**
     * Normalized operating system constant
     */
    final OperatingSystem os;

    /**
     * Unnormalized operating system version constant (for completeness)
     */
    final String version;

    /**
     * Normalized system architecture constant
     */
    final Architecture architecture;

    static final private Platform INSTANCE = new Platform();

    private Platform() {
        this(System.getProperties());
    }

    Platform(final Properties properties) {
        os = normalizeOperatingSystem(properties.getProperty("os.name"));
        version = properties.getProperty("os.version");
        architecture = normalizeArchitecture(os, properties.getProperty("os.arch"));
    }

    static OperatingSystem normalizeOperatingSystem(final String osName) {
        String os = osName;

        if (os == null) {
            throw new IllegalStateException("System property \"os.name\" == null");
        }

        os = os.toLowerCase();

        if (os.startsWith("windows")) {
            return OperatingSystem.Windows;
        }
        else if (os.startsWith("linux")) {
            return OperatingSystem.Linux;
        }
        else if (os.startsWith("mac os") || os.startsWith("darwin")) {
            return OperatingSystem.MacOS;
        }
        else if (os.startsWith("solaris") || os.startsWith("sunos")) {
            return OperatingSystem.Solaris;
        }

        return OperatingSystem.Unknown;
    }

    static Architecture normalizeArchitecture(final OperatingSystem pOsName, final String osArch) {
        String arch = osArch;

        if (arch == null) {
            throw new IllegalStateException("System property \"os.arch\" == null");
        }

        arch = arch.toLowerCase();

        if (pOsName == OperatingSystem.Windows && (arch.startsWith("x86") || arch.startsWith("i386"))) {
            return Architecture.X86;
            // TODO: 64 bit
        }
        else if (pOsName == OperatingSystem.Linux) {
            if (arch.startsWith("x86") || arch.startsWith("i386")) {
                return Architecture.I386;
            }
            else if (arch.startsWith("i686")) {
                return Architecture.I686;
            }
            else if (arch.startsWith("power") || arch.startsWith("ppc")) {
                return Architecture.PPC;
            }
            // TODO: More Linux options?
            // TODO: 64 bit
        }
        else if (pOsName == OperatingSystem.MacOS) {
            if (arch.startsWith("power") || arch.startsWith("ppc")) {
                return Architecture.PPC;
            }
            else if (arch.startsWith("x86")) {
                return Architecture.X86;
            }
            else if (arch.startsWith("i386")) {
                return Architecture.X86;
            }
            // TODO: 64 bit
        }
        else if (pOsName == OperatingSystem.Solaris) {
            if (arch.startsWith("sparc")) {
                return Architecture.SPARC;
            }
            if (arch.startsWith("x86")) {
                // TODO: Should we use i386 as Linux and Mac does?
                return Architecture.X86;
            }
            // TODO: 64 bit
        }

        return Architecture.Unknown;
    }

    /**
     * Returns the current {@code Platform}.
     * @return the current {@code Platform}.
     */
    public static Platform get() {
        return INSTANCE;
    }

    /**
     * @return this platform's OS.
     */
    public OperatingSystem getOS() {
        return os;
    }

    /**
     * @return this platform's OS version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return this platform's architecture.
     */
    public Architecture getArchitecture() {
        return architecture;
    }

    /**
     * Shorthand for {@code Platform.get().getOS()}.
     * @return the current {@code OperatingSystem}.
     */
    public static OperatingSystem os() {
        return INSTANCE.os;
    }

    /**
     * Shorthand for {@code Platform.get().getVersion()}.
     * @return the current OS version.
     */
    public static String version() {
        return INSTANCE.version;
    }

    /**
     * Shorthand for {@code Platform.get().getArchitecture()}.
     * @return the current {@code Architecture}.
     */
    public static Architecture arch() {
        return INSTANCE.architecture;
    }

    /**
     * Enumeration of common System {@code Architecture}s.
     * <p/>
     * For {@link #Unknown unknown architectures}, {@code toString()} will return
     * the the same value as {@code System.getProperty("os.arch")}.
     *
     * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
     * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/Platform.java#1 $
     */
    public enum Architecture {
        X86("x86"),
        I386("i386"),
        I686("i686"),
        PPC("ppc"),
        SPARC("sparc"),

        Unknown(System.getProperty("os.arch"));

        final String name;// for debug only

        Architecture(String pName) {
            name = pName;
        }

        public String toString() {
            return name;
        }
    }

    /**
     * Enumeration of common {@code OperatingSystem}s.
     * <p/>
     * For {@link #Unknown unknown operating systems}, {@code getName()} will return
     * the the same value as {@code System.getProperty("os.name")}.
     *
     * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
     * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/Platform.java#1 $
     */
    public enum OperatingSystem {
        Windows("Windows", "win"),
        Linux("Linux", "lnx"),
        Solaris("Solaris", "sun"),
        MacOS("Mac OS", "osx"),

        Unknown(System.getProperty("os.name"), null);

        final String id;
        final String name;// for debug only

        OperatingSystem(String pName, String pId) {
            name = pName;
            id = pId != null ? pId : pName.toLowerCase();
        }

        public String getName() {
            return name;
        }

        public String id() {
            return id;
        }

        public String toString() {
            return String.format("%s (%s)", id, name);
        }
    }
}
