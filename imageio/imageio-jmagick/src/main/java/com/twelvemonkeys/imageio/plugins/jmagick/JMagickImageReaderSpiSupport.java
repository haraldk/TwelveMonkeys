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

import com.twelvemonkeys.lang.SystemUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * JMagickImageReaderSpiSupport
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: JimiImageReaderSpiSupport.java,v 1.1 2003/12/02 16:45:00 wmhakur Exp $
 */
abstract class JMagickImageReaderSpiSupport extends ImageReaderSpi {

    final static boolean AVAILABLE = SystemUtil.isClassAvailable("com.twelvemonkeys.imageio.plugins.jmagick.JMagick", JMagickImageReaderSpiSupport.class);

    /**
     * Creates a JMagickImageReaderSpiSupport
     *
     * @param pFormatNames     format names
     * @param pSuffixes        format suffixes
     * @param pMimeTypes       format MIME types
     * @param pReaderClassName format reader class name
     * @param pWriterSpiNames  format writer service provider namses
     */
    protected JMagickImageReaderSpiSupport(final String[] pFormatNames,
                                           final String[] pSuffixes,
                                           final String[] pMimeTypes,
                                           final String pReaderClassName,
                                           final String[] pWriterSpiNames) {
        super(
                "TwelveMonkeys", // Vendor name
                "2.0", // Version
                AVAILABLE ? pFormatNames : new String[]{""}, // Names
                AVAILABLE ? pSuffixes : null, // Suffixes
                AVAILABLE ? pMimeTypes : null, // Mime-types
                pReaderClassName, // Reader class name
                new Class[] {ImageInputStream.class}, // Input types
                pWriterSpiNames, // Writer SPI names
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

    public boolean canDecodeInput(Object pSource) throws IOException {
        return pSource instanceof ImageInputStream && AVAILABLE && canDecode0((ImageInputStream) pSource);
    }

    private boolean canDecode0(ImageInputStream pSource) throws IOException {
        pSource.mark();
        try {
            return canDecode(pSource);
        }
        finally {
            pSource.reset();
        }
    }

    /**
     * Specifies if this provider's reader may decode the provided input.
     * Stream mark/reset is handled, and need not be taken care of.
     *
     * @param pSource the source image input stream
     * @return {@code true} if the inout can be decoded
     * @throws IOException if an I/O exception occurs during the process
     */
    abstract boolean canDecode(ImageInputStream pSource) throws IOException;

    public final ImageReader createReaderInstance(Object pExtension) throws IOException {
        try {
            return createReaderImpl(pExtension);
        }
        catch (Throwable t) {
            // Wrap in IOException if the reader can't be instantiated.
            // This makes the IIORegistry deregister this service provider
            IOException exception = new IOException(t.getMessage());
            exception.initCause(t);
            throw exception;
        }
    }

    protected abstract JMagickReader createReaderImpl(Object pExtension) throws IOException;

    public String getDescription(Locale pLocale) {
        return "JMagick " + getFormatNames()[0].toUpperCase() + " image reader";
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void onRegistration(ServiceRegistry pRegistry, Class pCategory) {
        if (!AVAILABLE) {
            pRegistry.deregisterServiceProvider(this, pCategory);
        }
    }

    /**
     * Specifies if the reader created by this provider should use temporary
     * file, instead of passing the data in-memory to the native reader.
     *
     * @return {@code true} if the reader should use a temporary file
     */
    public boolean useTempFile() {
        return "TRUE".equalsIgnoreCase(JMagick.sProperties.getProperty(getFormatNames()[0] + ".useTempFile"));
    }
}

