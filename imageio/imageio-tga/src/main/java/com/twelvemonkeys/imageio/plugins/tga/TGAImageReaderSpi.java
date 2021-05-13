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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

public final class TGAImageReaderSpi extends ImageReaderSpiBase {

    /**
     * Creates a {@code TGAImageReaderSpi}.
     */
    public TGAImageReaderSpi() {
        super(new TGAProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;

        stream.mark();
        ByteOrder originalByteOrder = stream.getByteOrder();

        try {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // NOTE: The original TGA format does not have a magic identifier, so this is guesswork...
            // We'll try to match sane values, and hope no other files contains the same sequence.

            stream.readUnsignedByte();

            int colorMapType = stream.readUnsignedByte();
            switch (colorMapType) {
                case TGA.COLORMAP_NONE:
                case TGA.COLORMAP_PALETTE:
                    break;
                default:
                    return false;
            }

            int imageType = stream.readUnsignedByte();
            switch (imageType) {
                case TGA.IMAGETYPE_NONE:
                case TGA.IMAGETYPE_COLORMAPPED:
                case TGA.IMAGETYPE_TRUECOLOR:
                case TGA.IMAGETYPE_MONOCHROME:
                case TGA.IMAGETYPE_COLORMAPPED_RLE:
                case TGA.IMAGETYPE_TRUECOLOR_RLE:
                case TGA.IMAGETYPE_MONOCHROME_RLE:
                    break;
                default:
                    return false;
            }

            int colorMapStart = stream.readUnsignedShort();
            int colorMapSize = stream.readUnsignedShort();
            int colorMapDepth = stream.readUnsignedByte();

            if (colorMapSize == 0) {
                // No color map, all 3 fields should be 0
                if (colorMapStart != 0 || colorMapDepth != 0) {
                    return false;
                }
            }
            else {
                if (colorMapType == TGA.COLORMAP_NONE) {
                    return false;
                }
                if (colorMapSize < 2) {
                    return false;
                }
                if (colorMapStart >= colorMapSize) {
                    return false;
                }
                if (colorMapDepth != 15 && colorMapDepth != 16 && colorMapDepth != 24 && colorMapDepth != 32) {
                    return false;
                }
            }

            // Skip x, y, w, h as these can be anything
            stream.readShort();
            stream.readShort();
            stream.readShort();
            stream.readShort();

            // Verify sane pixel depth
            int depth = stream.readUnsignedByte();
            switch (depth) {
                case 1:
                case 2:
                case 4:
                case 8:
                case 16:
                case 24:
                case 32:
                    break;
                default:
                    return false;
            }

            // We're pretty sure by now, but there can still be false positives...
            // For 2.0 format, we could skip to end, and read "TRUEVISION-XFILE.\0" but it would be too slow
            // unless we are working with a local file (and the file may still be a valid original TGA without it).
            return true;
        }
        finally {
            stream.reset();
            stream.setByteOrder(originalByteOrder);
        }
    }

    @Override public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new TGAImageReader(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "TrueVision TGA image reader";
    }
}

