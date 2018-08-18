/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.util.Locale;

public final class PNMImageWriterSpi extends ImageWriterSpi {

    // TODO: Consider one Spi for each sub-format, as it makes no sense for the writer to write PPM if client code requested PBM or PGM format.
    // ...Then again, what if user asks for PNM? :-P

    /**
     * Creates a {@code PNMImageWriterSpi}.
     */
    public PNMImageWriterSpi() {
        this(new PNMProviderInfo());
    }

    private PNMImageWriterSpi(final ProviderInfo pProviderInfo) {
        super(
                pProviderInfo.getVendorName(),
                pProviderInfo.getVersion(),
                new String[] {
                        "pnm", "pbm", "pgm", "ppm",
                        "PNM", "PBM", "PGM", "PPM"
                },
                new String[] {"pbm", "pgm", "ppm"},
                new String[] {
                        // No official IANA record exists, these are conventional
                        "image/x-portable-pixmap",
                        "image/x-portable-anymap"
                },
                "com.twelvemonkeys.imageio.plugins.pnm.PNMImageWriter",
                new Class[] {ImageOutputStream.class},
                new String[] {"com.twelvemonkeys.imageio.plugins.pnm.PNMImageReaderSpi"},
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    public boolean canEncodeImage(final ImageTypeSpecifier pType) {
        // TODO: FixMe: Support only 1 bit b/w, 8-16 bit gray and 8-16 bit/sample RGB
        return true;
    }

    public ImageWriter createWriterInstance(final Object pExtension) {
        return new PNMImageWriter(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "NetPBM Portable Any Map (PNM) image writer";
    }
}
