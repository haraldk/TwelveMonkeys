/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import java.util.Locale;

import static com.twelvemonkeys.imageio.plugins.psd.PSDImageWriter.getBitsPerSample;
import static com.twelvemonkeys.imageio.plugins.psd.PSDImageWriter.getColorMode;

/**
 * PSDImageWriterSpi
 */
public final class PSDImageWriterSpi extends ImageWriterSpiBase {

    public PSDImageWriterSpi() {
        super(new PSDProviderInfo());
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        // PSD supports:
        //  - 1, 8, 16 or 32 bit/sample
        //  - Number of samples <= 56
        //  - RGB, CMYK, Gray, Indexed color
        try {
            getBitsPerSample(type.getSampleModel());
            getColorMode(type.getColorModel());
        }
        catch (IllegalArgumentException ignore) {
            // We can't write this type
            return false;
        }

        return type.getNumBands() <= 56; // Can't be negative
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new PSDImageWriter(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Adobe Photoshop Document (PSD) image writer";
    }
}
