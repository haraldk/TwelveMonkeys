/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * TGAImageWriterSpi
 */
public final class TGAImageWriterSpi extends ImageWriterSpiBase {
    @SuppressWarnings("WeakerAccess")
    public TGAImageWriterSpi() {
        super(new TGAProviderInfo());
    }

    @Override
    public boolean canEncodeImage(final ImageTypeSpecifier type) {
        // Fast case, it's a known, supported type
        switch (type.getBufferedImageType()) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_INDEXED:
                return true;
            case BufferedImage.TYPE_BYTE_BINARY: // Could be supported? Uncertain if the format allows < 8 bit/sample for color map entries
            case BufferedImage.TYPE_INT_BGR: // TODO: Should be supported, just needs to invert band indices/offsets
            case BufferedImage.TYPE_USHORT_565_RGB:
            case BufferedImage.TYPE_USHORT_GRAY:
                return false;
            default:
                // Fall through
        }

        // Inspect color model etc.
        ColorSpace colorSpace = type.getColorModel().getColorSpace();
        if (!(colorSpace.getType() == ColorSpace.TYPE_RGB || colorSpace.getType() == ColorSpace.TYPE_GRAY)) {
            return false;
        }



        // TODO!

        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new TGAImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "TrueVision TGA image writer";
    }
}
