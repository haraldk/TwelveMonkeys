/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * PSDImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageReaderSpi.java,v 1.0 Apr 29, 2008 4:49:03 PM haraldk Exp$
 */
final public class PSDImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates a {@code PSDImageReaderSpi}.
     */
    public PSDImageReaderSpi() {
        this(IIOUtil.getProviderInfo(PSDImageReaderSpi.class));
    }

    private PSDImageReaderSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[] {"psd", "PSD"},
                new String[] {"psd"},
                new String[] {
                        "image/vnd.adobe.photoshop",        // Official, IANA registered
                        "application/vnd.adobe.photoshop",  // Used in XMP
                        "image/x-psd",
                        "application/x-photoshop",
                        "image/x-photoshop"
                },
                "com.twelvemkonkeys.imageio.plugins.psd.PSDImageReader",
                new Class[] {ImageInputStream.class},
//                new String[] {"com.twelvemkonkeys.imageio.plugins.psd.PSDImageWriterSpi"},
                null,
                true, // supports standard stream metadata
                null, null, // native stream format name and class
                null, null, // extra stream formats
                true, // supports standard image metadata
                PSDMetadata.NATIVE_METADATA_FORMAT_NAME, PSDMetadata.NATIVE_METADATA_FORMAT_CLASS_NAME,
                null, null // extra image metadata formats
        );
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;

        stream.mark();

        try {
            if (stream.readInt() == PSD.SIGNATURE_8BPS) {
                int version = stream.readUnsignedShort();

                switch (version) {
                    case PSD.VERSION_PSD:
                    case PSD.VERSION_PSB:
                        break;
                    default:
                        return false;
                }

                return true;
            }

            return false;
        }
        finally {
            stream.reset();
        }
    }

    public ImageReader createReaderInstance(final Object pExtension) throws IOException {
        return new PSDImageReader(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Adobe Photoshop Document (PSD) image reader";
    }
}
