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

import com.twelvemonkeys.lang.SystemUtil;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * TIFFImageReaderSpi
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: TIFFImageReaderSpi.java,v 1.1 2003/12/02 16:45:00 wmhakur Exp $
 */
public class TIFFImageReaderSpi extends ImageReaderSpi {

    final static boolean TIFF_CLASSES_AVAILABLE = SystemUtil.isClassAvailable("com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReader");

    /**
     * Creates an SVGImageReaderSpi
     */
    public TIFFImageReaderSpi() {
        super(
                "TwelveMonkeys", // Vendor name
                "2.0", // Version
                TIFF_CLASSES_AVAILABLE ? new String[]{"tiff", "TIFF"} : new String[] {""}, // Names
                TIFF_CLASSES_AVAILABLE ? new String[]{"tiff", "tif"} : null, // Suffixes
                TIFF_CLASSES_AVAILABLE ? new String[]{"image/tiff", "image/x-tiff"} : null, // Mime-types
                "com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReader", // Writer class name..?
                ImageReaderSpi.STANDARD_INPUT_TYPE, // Output types
                new String[]{"com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriterSpi"}, // Writer SPI names
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

    public boolean canDecodeInput(Object source) throws IOException {
        return source instanceof ImageInputStream && TIFF_CLASSES_AVAILABLE && canDecode((ImageInputStream) source);
    }


    static boolean canDecode(ImageInputStream pInput) throws IOException {
        try {
            pInput.mark();
            int byte0 = pInput.read(); // Byte order 1 (M or I)
            int byte1 = pInput.read(); // Byte order 2 (always same as 1)
            int byte2 = pInput.read(); // Version number 1 (M: 0, I: 42)
            int byte3 = pInput.read(); // Version number 2  (M: 42, I: 0)

            // Test for Motorola or Intel byte order, and version number == 42
            if ((byte0 == 'M' && byte1 == 'M' && byte2 == 0 && byte3 == 42)
                    || (byte0 == 'I' && byte1 == 'I' && byte2 == 42 && byte3 == 0)) {
                return true;
            }

        }
        finally {
            pInput.reset();
        }

        return false;
    }

    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new TIFFImageReader(this);
    }

    public String getDescription(Locale locale) {
        return "Tagged Image File Format (TIFF) image reader";
    }

    @Override
    public void onRegistration(ServiceRegistry registry, Class<?> category) {
        if (!TIFF_CLASSES_AVAILABLE) {
            IIOUtil.deregisterProvider(registry, this, category);
        }
    }
}