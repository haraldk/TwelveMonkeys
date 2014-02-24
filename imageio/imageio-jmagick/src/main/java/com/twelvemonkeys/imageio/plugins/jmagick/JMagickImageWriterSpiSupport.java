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

package com.twelvemonkeys.imageio.plugins.jmagick;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * JMagickImageWriterSpiSupport
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: JMagickImageWriterSpiSupport.java,v 1.0 17.jan.2006 00:04:32 haku Exp$
 */
abstract class JMagickImageWriterSpiSupport extends ImageWriterSpi {

    private final static boolean AVAILABLE = JMagickImageReaderSpiSupport.AVAILABLE;

    /**
     * Creates a JMagickImageReaderSpiSupport
     *
     * @param pFormatNames     format names
     * @param pSuffixes        format suffixes
     * @param pMimeTypes       format MIME types
     * @param pWriterClassName format writer class name
     * @param pReaderSpiNames  format reader service provider namses
     */
    protected JMagickImageWriterSpiSupport(final String[] pFormatNames,
                                           final String[] pSuffixes,
                                           final String[] pMimeTypes,
                                           final String pWriterClassName,
                                           final String[] pReaderSpiNames) {
        super(
                "TwelveMonkeys", // Vendor name
                "2.0", // Version
                AVAILABLE ? pFormatNames : new String[]{""}, // Names
                AVAILABLE ? pSuffixes : null, // Suffixes
                AVAILABLE ? pMimeTypes : null, // Mime-types
                pWriterClassName, // Writer class name
                new Class[] {ImageOutputStream.class}, // Output types
                pReaderSpiNames, // Reader SPI names
                true, // Supports standard stream metadata format
                null, // Native stream metadata format name
                null, // Native stream metadata format class name
                null, // Extra stream metadata format names
                null, // Extra stream metadata format class names
                true, // Supports standard image metadata format
                null, // Native image metadata format name
                null, // Native image metadata format class name
                null, // Extra image metadata format names
                null// Extra image metadata format class names
        );
    }

    /**
     * This implementations simply returns {@code true}.
     *
     * @param pType ignored
     * @return {@code true} unless overriden
     */
    public boolean canEncodeImage(ImageTypeSpecifier pType) {
        return true;
    }

    public final ImageWriter createWriterInstance(Object pExtension) throws IOException {
        try {
            return createWriterImpl(pExtension);
        }
        catch (Throwable t) {
            // Wrap in IOException if the writer can't be instantiated.
            // This makes the IIORegistry deregister this service provider
            IOException exception = new IOException(t.getMessage());
            exception.initCause(t);
            throw exception;
        }
    }

    protected abstract JMagickWriter createWriterImpl(final Object pExtension) throws IOException;

    public String getDescription(Locale pLocale) {
        return "JMagick " + getFormatNames()[0].toUpperCase() + " image writer";
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void onRegistration(ServiceRegistry pRegistry, Class pCategory) {
        if (!AVAILABLE) {
            pRegistry.deregisterServiceProvider(this, pCategory);
        }
    }
}
