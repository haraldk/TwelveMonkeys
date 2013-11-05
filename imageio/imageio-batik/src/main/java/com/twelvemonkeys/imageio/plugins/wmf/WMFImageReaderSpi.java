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

package com.twelvemonkeys.imageio.plugins.wmf;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.lang.SystemUtil;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * WMFImageReaderSpi
 * <p/>
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: WMFImageReaderSpi.java,v 1.1 2003/12/02 16:45:00 wmhakur Exp $
 */
public class WMFImageReaderSpi extends ImageReaderSpi {

    // This is correct, as we rely on the SVG reader
    private final static boolean WMF_READER_AVAILABLE = SystemUtil.isClassAvailable("com.twelvemonkeys.imageio.plugins.svg.SVGImageReader");

    /**
     * Creates a {@code WMFImageReaderSpi}.
     */
    public WMFImageReaderSpi() {
        this(IIOUtil.getProviderInfo(WMFImageReaderSpi.class));
    }

    private WMFImageReaderSpi(final ProviderInfo pProviderInfo) {
        super(
                pProviderInfo.getVendorName(), // Vendor name
                pProviderInfo.getVersion(), // Version
                WMF_READER_AVAILABLE ? new String[]{"wmf", "WMF"} : new String[]{""}, // Names
                WMF_READER_AVAILABLE ? new String[]{"wmf", "emf"} : null, // Suffixes
                WMF_READER_AVAILABLE ? new String[]{"application/x-msmetafile", "image/x-wmf"} : null, // Mime-types
                "com.twelvemonkeys.imageio.plugins.wmf.WMFImageReader", // Reader class name..?
                new Class[] {ImageInputStream.class}, // Input types
                null, // Writer SPI names
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
        return source instanceof ImageInputStream && WMF_READER_AVAILABLE && canDecode((ImageInputStream) source);
    }

    public static boolean canDecode(ImageInputStream pInput) throws IOException {
        if (pInput == null) {
            throw new IllegalArgumentException("input == null");
        }

        try {
            pInput.mark();

            for (byte header : WMF.HEADER) {
                int read = (byte) pInput.read();
                if (header != read) {
                    // System.out.println("--> " + i + ": " + read + " (expected " + header + ")");
                    return false;
                }
            }
            return true;

        }
        finally {
            pInput.reset();
        }
    }


    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new WMFImageReader(this);
    }

    public String getDescription(Locale locale) {
        return "Windows Meta File (WMF) image reader";
    }

    @SuppressWarnings({"deprecation"})
    @Override
    public void onRegistration(ServiceRegistry registry, Class<?> category) {
        if (!WMF_READER_AVAILABLE) {
            IIOUtil.deregisterProvider(registry, this, category);
        }
    }
}

