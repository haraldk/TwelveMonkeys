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

/**
 * Abstract base class for native reource providers to iplement.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/NativeResourceSPI.java#1 $
 */
public abstract class NativeResourceSPI {

    private final String mResourceName;

    /**
     * Creates a {@code NativeResourceSPI} with the given name.
     *
     * The name will typically be a short string, with the common name of the
     * library that is provided, like "JMagick", "JOGL" or similar.
     *
     * @param pResourceName name of the resource (native library) provided by
     *        this SPI.
     *
     * @throws IllegalArgumentException if {@code pResourceName == null}
     */
    protected NativeResourceSPI(String pResourceName) {
        if (pResourceName == null) {
            throw new IllegalArgumentException("resourceName == null");
        }

        mResourceName = pResourceName;
    }

    /**
     * Returns the name of the resource (native library) provided by this SPI.
     *
     * The name will typically be a short string, with the common name of the
     * library that is provided, like "JMagick", "JOGL" or similar.
     * <p/>
     * NOTE: This method is intended for the SPI framework, and should not be
     * invoked by client code.
     *
     * @return the name of the resource provided by this SPI
     */
    public final String getResourceName() {
        return mResourceName;
    }

    /**
     * Returns the path to the classpath resource that is suited for the given
     * runtime configuration.
     * <p/>
     * In the common case, the {@code pPlatform} parameter is
     * normalized from the values found in
     * {@code System.getProperty("os.name")} and
     * {@code System.getProperty("os.arch")}.
     * For unknown operating systems and architectures, {@code toString()} on
     * the platforms's properties will return the the same value as these properties.
     * <p/>
     * NOTE: This method is intended for the SPI framework, and should not be
     * invoked by client code.
     *
     * @param pPlatform the current platform
     * @return a {@code String} containing the path to a classpath resource or
     * {@code null} if no resource is available.
     *
     * @see com.twelvemonkeys.lang.Platform.OperatingSystem
     * @see com.twelvemonkeys.lang.Platform.Architecture
     * @see System#getProperties()
     */
    public abstract String getClassPathResource(final Platform pPlatform);
}
