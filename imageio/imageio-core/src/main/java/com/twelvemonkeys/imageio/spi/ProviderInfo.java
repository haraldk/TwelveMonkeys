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

import com.twelvemonkeys.lang.Validate;

/**
 * Provides provider info, like vendor name and version,
 * for {@link javax.imageio.spi.ImageReaderWriterSpi} subclasses based on information in the manifest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ProviderInfo.java,v 1.0 Oct 31, 2009 3:49:39 PM haraldk Exp$
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#JAR%20Manifest">JAR Manifest</a>
 */
public class ProviderInfo {
    // TODO: Consider reading the META-INF/MANIFEST.MF from the class path using java.util.jar.Manifest.
    // Use the manifest that is located in the same class path folder as the package.

    private final String title;
    private final String vendorName;
    private final String version;

    /**
     * Creates a provider information instance based on the given package.
     *
     * @param pPackage the package to get provider information from.
     * This should typically be the package containing the Spi class.
     *
     * @throws IllegalArgumentException if {@code pPackage == null}
     */
    public ProviderInfo(final Package pPackage) {
        Validate.notNull(pPackage, "package");

        String title = pPackage.getImplementationTitle();
        this.title = title != null ? title : pPackage.getName();

        String vendor = pPackage.getImplementationVendor();
        vendorName = vendor != null ? vendor : fakeVendor(pPackage);

        String version = pPackage.getImplementationVersion();
        this.version = version != null ? version : fakeVersion(pPackage);
    }

    private static String fakeVendor(final Package pPackage) {
        String name = pPackage.getName();
        return name.startsWith("com.twelvemonkeys") ? "TwelveMonkeys" : name;
    }

    private String fakeVersion(final Package pPackage) {
        String name = pPackage.getName();
        return name.startsWith("com.twelvemonkeys") ? "DEV" : "Unspecified";
    }

    /**
     * Returns the implementation title, as specified in the manifest entry
     * {@code Implementation-Title} for the package.
     * If the title is unavailable, the package name or some default name
     * for known packages are used.
     *
     * @return the implementation title
     */
    final String getImplementationTitle() {
        return title;
    }

    /**
     * Returns the vendor name, as specified in the manifest entry
     * {@code Implementation-Vendor} for the package.
     * If the vendor name is unavailable, the package name or some default name
     * for known packages are used.
     *
     * @return the vendor name.
     */
    public final String getVendorName() {
        return vendorName;
    }

    /**
     * Returns the version/build number string, as specified in the manifest entry
     * {@code Implementation-Version} for the package.
     * If the version is unavailable, some arbitrary (non-{@code null}) value is used.
     *
     * @return the vendor name.
     */
    public final String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return title + ", " + version + " by " + vendorName;
    }
}
