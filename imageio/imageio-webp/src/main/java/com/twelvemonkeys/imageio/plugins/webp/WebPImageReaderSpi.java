/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * WebPImageReaderSpi
 */
public final class WebPImageReaderSpi extends ImageReaderSpiBase {
    @SuppressWarnings("WeakerAccess")
    public WebPImageReaderSpi() {
        super(new WebPProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        return source instanceof ImageInputStream && canDecode((ImageInputStream) source);
    }

    private static boolean canDecode(final ImageInputStream stream) throws IOException {
        ByteOrder originalOrder = stream.getByteOrder();
        stream.mark();

        try {
            // RIFF native order is Little Endian
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            if (stream.readInt() != WebP.RIFF_MAGIC) {
                return false;
            }

            stream.readInt(); // Skip file size

            if (stream.readInt() != WebP.WEBP_MAGIC) {
                return false;
            }

            int chunk = stream.readInt();

            switch (chunk) {
                // TODO. Support lossless
//                 case WebP.CHUNK_VP8L:
                case WebP.CHUNK_VP8X:
                    return containsSupportedChunk(stream, chunk);
                case WebP.CHUNK_VP8_:
                    return true;
                default:
                    return false;
            }
        }
        finally {
            stream.setByteOrder(originalOrder);
            stream.reset();
        }
    }

    private static boolean containsSupportedChunk(ImageInputStream stream, int chunk) throws IOException {
        // Temporary: Seek for VP8_, either first or second (after ICCP), or inside ANMF...
        try {
            while (chunk != WebP.CHUNK_VP8L && chunk != WebP.CHUNK_ALPH) {
                long length = stream.readUnsignedInt();
                stream.seek(stream.getStreamPosition() + length);
                chunk = stream.readInt();

                // Look inside ANMF chunks...
                if (chunk == WebP.CHUNK_ANMF) {
                    stream.seek(stream.getStreamPosition() + 4 + 16);
                    chunk = stream.readInt();
                }

                if (chunk == WebP.CHUNK_VP8_) {
                    return true;
                }
            }
        }
        catch (EOFException ignore) {}

        return false;
    }

    @Override
    public ImageReader createReaderInstance(final Object extension) {
        return new WebPImageReader(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "Google WebP File Format (WebP) Reader";
    }
}
