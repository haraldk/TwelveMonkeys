/*
 * Copyright (c) 2026, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jxl;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import static com.twelvemonkeys.imageio.util.IIOUtil.deregisterProvider;

/**
 * JXLImageReaderSpi.
 * <p>
 * The provider de-registers itself if the JXLatte decoder is not available
 * (missing from the class path, a JXLatte version without {@code JXLDecoder.readHeader()},
 * or running on a Java version older than 21).
 * </p>
 */
public final class JXLImageReaderSpi extends ImageReaderSpiBase {

    static final boolean JXLATTE_AVAILABLE = isJXLatteAvailable(JXL.JXLATTE_DECODER_CLASS, JXLImageReaderSpi.class.getClassLoader());

    static boolean isJXLatteAvailable(final String decoderClassName, final ClassLoader classLoader) {
        try {
            // Requires JXLDecoder.readHeader(), not available in JXLatte 0.1.0
            Class.forName(decoderClassName, true, classLoader).getMethod("readHeader");
            return true;
        }
        catch (ClassNotFoundException | NoSuchMethodException | LinkageError | SecurityException e) {
            // Not on class path, too old JXLatte, or too old Java version (JXLatte requires Java 21)
            return false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public JXLImageReaderSpi() {
        super(new JXLProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        return source instanceof ImageInputStream && canDecode((ImageInputStream) source);
    }

    private static boolean canDecode(final ImageInputStream stream) throws IOException {
        stream.mark();

        try {
            byte[] signature = new byte[JXL.CONTAINER_SIGNATURE.length];

            // A codestream signature is only 2 bytes, test this first, as a codestream may be very short
            stream.readFully(signature, 0, JXL.CODESTREAM_SIGNATURE.length);
            if (startsWith(signature, JXL.CODESTREAM_SIGNATURE)) {
                return true;
            }

            stream.readFully(signature, JXL.CODESTREAM_SIGNATURE.length, signature.length - JXL.CODESTREAM_SIGNATURE.length);
            return Arrays.equals(signature, JXL.CONTAINER_SIGNATURE);
        }
        catch (EOFException ignore) {
            return false;
        }
        finally {
            stream.reset();
        }
    }

    private static boolean startsWith(final byte[] bytes, final byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ImageReader createReaderInstance(final Object extension) {
        return new JXLImageReader(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "JPEG XL (JXL) image reader";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        if (!JXLATTE_AVAILABLE) {
            deregisterProvider(registry, this, category);
        }
    }
}
