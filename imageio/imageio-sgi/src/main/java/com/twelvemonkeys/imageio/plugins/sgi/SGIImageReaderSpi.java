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

package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

public final class SGIImageReaderSpi extends ImageReaderSpiBase {

    /**
     * Creates a {@code SGIImageReaderSpi}.
     */
    public SGIImageReaderSpi() {
        super(new SGIProviderInfo());
    }

    @Override public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;

        stream.mark();

        try {
            short magic = stream.readShort();

            switch (magic) {
                case SGI.MAGIC:
                    byte compression = stream.readByte();
                    byte bpp = stream.readByte();

                    return (compression == SGI.COMPRESSION_NONE || compression == SGI.COMPRESSION_RLE) && (bpp == 1 || bpp == 2);
                default:
                    return false;
            }
        }
        finally {
            stream.reset();
        }
    }

    @Override public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new SGIImageReader(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "Silicon Graphics (SGI) image reader";
    }
}

