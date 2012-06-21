/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.lang;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.util.FilterIterator;
import com.twelvemonkeys.util.service.ServiceRegistry;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;

/**
 * NativeLoader
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/NativeLoader.java#2 $
 */
final class NativeLoader {
    // TODO: Considerations:
    // - Rename all libs like the current code, to <library>.(so|dll|dylib)?
    // - Keep library filename from jar, and rather store a separate
    //   properties-file with the library->library-file mappings?
    // - As all invocations are with library file name, we could probably skip
    //   both renaming and properties-file altogether...

    // TODO: The real trick here, is how to load the correct library for the
    // current platform...
    // - Change String pResource to String[] pResources?
    // - NativeResource class, that has a list of multiple resources?
    //   NativeResource(Map<String, String>) os->native lib mapping

    // TODO: Consider exposing the method from SystemUtil

    // TODO: How about a SPI based solution?!
    // public interface com.twelvemonkeys.lang.NativeResourceProvider
    //
    // imlementations return a pointer to the correct resource for a given (by
    // this class) OS.
    //
    // String getResourceName(...)
    //
    // See http://tolstoy.com/samizdat/sysprops.html
    // System properties:
    // "os.name"
    //      Windows, Linux, Solaris/SunOS,
    //      Mac OS/Mac OS X/Rhapsody (aka Mac OS X Server)
    //      General Unix (AIX, Digital Unix, FreeBSD, HP-UX, Irix)
    //      OS/2
    // "os.arch"
    //      Windows: x86
    //      Linux: x86, i386, i686, x86_64, ia64,
    //      Solaris: sparc, sparcv9, x86
    //      Mac OS: PowerPC, ppc, i386
    //      AIX: x86, ppc
    //      Digital Unix: alpha
    //      FreeBSD: x86, sparc
    //      HP-UX: PA-RISC
    //      Irix: mips
    //      OS/2: x86
    // "os.version"
    //      Windows: 4.0 -> NT/95, 5.0 -> 2000, 5.1 -> XP (don't care about old versions, CE etc)
    //      Mac OS: 8.0, 8.1, 10.0 -> OS X, 10.x.x -> OS X, 5.6 -> Rhapsody (!)
    //
    // Normalize os.name, os.arch and os.version?!


    ///** Normalized operating system constant */
    //static final OperatingSystem OS_NAME = normalizeOperatingSystem();
    //
    ///** Normalized system architecture constant */
    //static final Architecture OS_ARCHITECTURE = normalizeArchitecture();
    //
    ///** Unormalized operating system version constant (for completeness) */
    //static final String OS_VERSION = System.getProperty("os.version");

    static final NativeResourceRegistry sRegistry = new NativeResourceRegistry();

    private NativeLoader() {
    }

/*
    private static Architecture normalizeArchitecture() {
        String arch = System.getProperty("os.arch");
        if (arch == null) {
            throw new IllegalStateException("System property \"os.arch\" == null");
        }

        arch = arch.toLowerCase();
        if (OS_NAME == OperatingSystem.Windows
                && (arch.startsWith("x86") || arch.startsWith("i386"))) {
            return Architecture.X86;
            // TODO: 64 bit
        }
        else if (OS_NAME == OperatingSystem.Linux) {
            if  (arch.startsWith("x86") || arch.startsWith("i386")) {
               return Architecture.I386;
            }
            else if (arch.startsWith("i686")) {
               return Architecture.I686;
            }
            // TODO: More Linux options?
            // TODO: 64 bit
        }
        else if (OS_NAME == OperatingSystem.MacOS) {
            if (arch.startsWith("power") || arch.startsWith("ppc")) {
                return Architecture.PPC;
            }
            else if (arch.startsWith("i386")) {
                return Architecture.I386;
            }
        }
        else if (OS_NAME == OperatingSystem.Solaris) {
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
*/

/*
    private static OperatingSystem normalizeOperatingSystem() {
        String os = System.getProperty("os.name");
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
        else if (os.startsWith("mac os")) {
            return OperatingSystem.MacOS;
        }
        else if (os.startsWith("solaris") || os.startsWith("sunos")) {
            return OperatingSystem.Solaris;
        }

        return OperatingSystem.Unknown;
    }
*/

    // TODO: We could actually have more than one resource for each lib...
    private static String getResourceFor(String pLibrary) {
        Iterator<NativeResourceSPI> providers = sRegistry.providers(pLibrary);
        while (providers.hasNext()) {
            NativeResourceSPI resourceSPI = providers.next();

            try {
                return resourceSPI.getClassPathResource(Platform.get());
            }
            catch (Throwable t) {
                // Dergister and try next
                sRegistry.deregister(resourceSPI);
            }
        }

        return null;
    }

    /**
     * Loads a native library.
     *
     * @param pLibrary name of the library
     *
     * @throws UnsatisfiedLinkError
     */
    public static void loadLibrary(String pLibrary) {
        loadLibrary0(pLibrary, null, null);
    }

    /**
     * Loads a native library.
     *
     * @param pLibrary name of the library
     * @param pLoader the class loader to use
     *
     * @throws UnsatisfiedLinkError
     */
    public static void loadLibrary(String pLibrary, ClassLoader pLoader) {
        loadLibrary0(pLibrary, null, pLoader);
    }

    /**
     * Loads a native library.
     *
     * @param pLibrary name of the library
     * @param pResource name of the resource
     * @param pLoader the class loader to use
     *
     * @throws UnsatisfiedLinkError
     */
    static void loadLibrary0(String pLibrary, String pResource, ClassLoader pLoader) {
        if (pLibrary == null) {
            throw new IllegalArgumentException("library == null");
        }

        // Try loading normal way
        UnsatisfiedLinkError unsatisfied;
        try {
            System.loadLibrary(pLibrary);
            return;
        }
        catch (UnsatisfiedLinkError err) {
            // Ignore
            unsatisfied = err;
        }

        final ClassLoader loader = pLoader != null ? pLoader : Thread.currentThread().getContextClassLoader();
        final String resource = pResource != null ? pResource : getResourceFor(pLibrary);

        // TODO: resource may be null, and that MIGHT be okay, IFF the resource
        // is allready unpacked to the user dir... However, we then need another
        // way to resolve the library extension...
        // Right now we just fail in a predictable way (no NPE)!
        if (resource == null) {
            throw unsatisfied;
        }

        // Default to load/store from user.home
        File dir = new File(System.getProperty("user.home") + "/.twelvemonkeys/lib");
        dir.mkdirs();
        //File libraryFile = new File(dir.getAbsolutePath(), pLibrary + LIBRARY_EXTENSION);
        File libraryFile = new File(dir.getAbsolutePath(), pLibrary + "." + FileUtil.getExtension(resource));

        if (!libraryFile.exists()) {
            try {
                extractToUserDir(resource, libraryFile, loader);
            }
            catch (IOException ioe) {
                UnsatisfiedLinkError err = new UnsatisfiedLinkError("Unable to extract resource to dir: " + libraryFile.getAbsolutePath());
                err.initCause(ioe);
                throw err;
            }
        }

        // Try to load the library from the file we just wrote
        System.load(libraryFile.getAbsolutePath());
    }

    private static void extractToUserDir(String pResource, File pLibraryFile, ClassLoader pLoader) throws IOException {
        // Get resource from classpath
        InputStream in = pLoader.getResourceAsStream(pResource);
        if (in == null) {
            throw new FileNotFoundException("Unable to locate classpath resource: " + pResource);
        }

        // Write to file in user dir
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(pLibraryFile);

            byte[] tmp = new byte[1024];
            // copy the contents of our resource out to the destination
            // dir 1K at a time.  1K may seem arbitrary at first, but today
            // is a Tuesday, so it makes perfect sense.
            int bytesRead = in.read(tmp);
            while (bytesRead != -1) {
                fileOut.write(tmp, 0, bytesRead);
                bytesRead = in.read(tmp);
            }
        }
        finally {
            FileUtil.close(fileOut);
            FileUtil.close(in);
        }
    }

    // TODO: Validate OS names?
    // Windows
    // Linux
    // Solaris
    // Mac OS (OSX+)
    // Generic Unix?
    // Others?

    // TODO: OSes that support different architectures might require different
    // resources for each architecture.. Need a namespace/flavour system
    // TODO: 32 bit/64 bit issues?
    // Eg: Windows, Windows/32, Windows/64, Windows/Intel/64?
    //     Solaris/Sparc, Solaris/Intel/64
    //     MacOS/PowerPC, MacOS/Intel
    /*
    public static class NativeResource {
        private Map mMap;

        public NativeResource(String[] pOSNames, String[] pReourceNames) {
            if (pOSNames == null) {
                throw new IllegalArgumentException("osNames == null");
            }
            if (pReourceNames == null) {
                throw new IllegalArgumentException("resourceNames == null");
            }
            if (pOSNames.length != pReourceNames.length) {
                throw new IllegalArgumentException("osNames.length != resourceNames.length");
            }

            Map map = new HashMap();
            for (int i = 0; i < pOSNames.length; i++) {
                map.put(pOSNames[i], pReourceNames[i]);
            }

            mMap = Collections.unmodifiableMap(map);
        }

        public NativeResource(Map pMap) {
            if (pMap == null) {
                throw new IllegalArgumentException("map == null");
            }

            Map map = new HashMap(pMap);

            Iterator it = map.keySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                if (!(entry.getKey() instanceof String && entry.getValue() instanceof String)) {
                    throw new IllegalArgumentException("map contains non-string entries: " + entry);
                }
            }

            mMap = Collections.unmodifiableMap(map);
        }

        protected NativeResource() {
        }

        public final String resourceForCurrentOS() {
            throw new UnsupportedOperationException();
        }

        protected String getResourceName(String pOSName) {
            return (String) mMap.get(pOSName);
        }
    }
    */

    private static class NativeResourceRegistry extends ServiceRegistry {
        public NativeResourceRegistry() {
            super(Collections.singletonList(NativeResourceSPI.class).iterator());
            registerApplicationClasspathSPIs();
        }

        Iterator<NativeResourceSPI> providers(final String nativeResource) {
            return new FilterIterator<NativeResourceSPI>(
                    providers(NativeResourceSPI.class),
                    new NameFilter(nativeResource)
            );
        }
    }

    private static class NameFilter implements FilterIterator.Filter<NativeResourceSPI> {
        private final String name;

        NameFilter(String pName) {
            if (pName == null) {
                throw new IllegalArgumentException("name == null");
            }
            name = pName;
        }
        public boolean accept(NativeResourceSPI pElement) {
            return name.equals(pElement.getResourceName());
        }
    }
}