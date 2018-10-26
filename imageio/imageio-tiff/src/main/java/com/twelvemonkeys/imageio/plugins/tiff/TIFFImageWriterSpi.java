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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import java.util.Locale;

import static com.twelvemonkeys.imageio.util.IIOUtil.lookupProviderByName;

/**
 * TIFFImageWriterSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageWriterSpi.java,v 1.0 18.09.13 12:46 haraldk Exp$
 */
public final class TIFFImageWriterSpi extends ImageWriterSpiBase {
    // TODO: Implement canEncodeImage better

    public TIFFImageWriterSpi() {
        super(new TIFFProviderInfo());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        // Make sure we're ordered before the new JEP 262 JRE bundled TIFF plugin
        ImageWriterSpi sunSpi = lookupProviderByName(registry, "com.sun.imageio.plugins.tiff.TIFFImageWriterSpi", ImageWriterSpi.class);

        if (sunSpi != null && sunSpi.getVendorName() != null && sunSpi.getVendorName().startsWith("Oracle")) {
            registry.setOrdering((Class<ImageWriterSpi>) category, this, sunSpi);
        }
    }

    @Override
    public boolean canEncodeImage(final ImageTypeSpecifier type) {
        // TODO: Test bit depths compatibility
        return true;
    }

    @Override
    public TIFFImageWriter createWriterInstance(final Object extension) {
        return new TIFFImageWriter(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "Aldus/Adobe Tagged Image File Format (TIFF) image writer";
    }
}
