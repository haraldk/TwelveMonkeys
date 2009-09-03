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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import java.io.IOException;
import java.util.Locale;

/**
 * TIFFmageWriterSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: TIFFImageWriterSpi.java,v 1.2 2004/01/14 15:21:44 wmhakur Exp $
 */
public class TIFFImageWriterSpi extends ImageWriterSpi {

    /**
     * Creates a TIFFImageWriterSpi.
     */
    public TIFFImageWriterSpi() {
        super(
                "TwelveMonkeys", // Vendor name
                "2.0", // Version
                new String[]{"tiff", "TIFF"}, // Names
                new String[]{"tif", "tiff"}, // Suffixes
                new String[]{"image/tiff", "image/x-tiff"}, // Mime-types
                "com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter", // Writer class name..?
                STANDARD_OUTPUT_TYPE, // Output types
                new String[]{"com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi"}, // Reader SPI names
                true, // Supports standard stream metadata format
                null, // Native stream metadata format name
                null, // Native stream metadata format class name
                null, // Extra stream metadata format names
                null, // Extra stream metadata format class names
                true, // Supports standard image metadata format
                null, // Native image metadata format name
                null, // Native image metadata format class name
                null, // Extra image metadata format names
                null  // Extra image metadata format class names
        );
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return true;
    }

    public ImageWriter createWriterInstance(Object extension) throws IOException {
        try {
            return new TIFFImageWriter(this);
        }
        catch (Throwable t) {
            // Wrap in IOException if the writer can't be instantiated.
            // This makes the IIORegistry deregister this service provider
            IOException exception = new IOException(t.getMessage());
            exception.initCause(t);
            throw exception;
        }
    }

    public String getDescription(Locale locale) {
        return "Tagged Image File Format (TIFF) image writer";
    }

    public void onRegistration(ServiceRegistry registry, Class<?> category) {
        if (!TIFFImageReaderSpi.TIFF_CLASSES_AVAILABLE) {
            IIOUtil.deregisterProvider(registry, this, category);
        }
    }
}
