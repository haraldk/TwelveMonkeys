/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.color;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Locale;

import static com.twelvemonkeys.imageio.util.IIOUtil.deregisterProvider;

/**
 * This class exists to force early invocation of {@code ProfileDeferralMgr.activateProfiles()},
 * in an attempt to avoid JDK-6986863 and related bugs in Java < 17.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @see <a href="https://bugs.openjdk.java.net/browse/JDK-6986863">JDK-6986863</a>
 */
final class ProfileDeferralActivator {

    static {
        activateProfilesInternal();
    }

    private static void activateProfilesInternal() {
        try {
            // Force invocation of ProfileDeferralMgr.activateProfiles() to avoid JDK-6986863 and friends.
            // Relies on static initializer in ColorConvertOp to actually invoke ProfileDeferralMgr.activateProfiles()
            Class.forName("java.awt.image.ColorConvertOp");
        }
        catch (Throwable disasters) {
            System.err.println("ProfileDeferralMgr.activateProfiles() failed. ICC Color Profiles may not work properly, see stack trace below.");
            System.err.println("For more information, see https://bugs.openjdk.java.net/browse/JDK-6986863");
            System.err.println("Please upgrade to Java 17 or later where this bug is fixed, or ask your JRE provider to backport the fix.");
            System.err.println();
            System.err.println("If you can't update to Java 17, a possible workaround is to add");
            System.err.println("\tClass.forName(\"java.awt.image.ColorConvertOp\");");
            System.err.println("*early* in your application startup code, to force profile activation before profiles are accessed.");
            System.err.println();

            disasters.printStackTrace();
        }
    }

    static void activateProfiles() {
        // This method exists for other classes in the package to
        // ensure this class' static initializer is run.
    }

    /**
     * This is not a service provider, but exploits the SPI mechanism as a hook to force early profile activation.
     */
    public static final class Spi extends ImageInputStreamSpi {
        @Override public void onRegistration(ServiceRegistry registry, Class<?> category) {
            activateProfiles();

            deregisterProvider(registry, this, category);
        }

        @Override public String getDescription(Locale locale) {
            return getClass().getName();
        }

        @Override public ImageInputStream createInputStreamInstance(Object input, boolean useCache, File cacheDir) {
            throw new UnsupportedOperationException();
        }
    }
}
