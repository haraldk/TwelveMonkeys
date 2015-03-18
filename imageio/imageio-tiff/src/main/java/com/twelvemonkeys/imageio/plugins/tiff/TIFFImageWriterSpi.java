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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * TIFFImageWriterSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriterSpi.java,v 1.0 18.09.13 12:46 haraldk Exp$
 */
public final class TIFFImageWriterSpi extends ImageWriterSpi {
    // TODO: Implement canEncodeImage better

    public TIFFImageWriterSpi() {
        this(IIOUtil.getProviderInfo(TIFFImageWriterSpi.class));
    }

    private TIFFImageWriterSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(), providerInfo.getVersion(),
                new String[] {"tiff", "TIFF", "tif", "TIFF"},
                new String[] {"tif", "tiff"},
                new String[] {"image/tiff", "image/x-tiff"},
                "com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter",
                new Class<?>[] {ImageOutputStream.class},
                new String[] {"com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi"},
                true, // supports standard stream metadata
                null, null, // native stream format name and class
                null, null, // extra stream formats
                true, // supports standard image metadata
                null, null,
                null, null // extra image metadata formats
        );
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        // TODO: Test bit depths compatibility

        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new TIFFImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Aldus/Adobe Tagged Image File Format (TIFF) image writer";
    }
}
