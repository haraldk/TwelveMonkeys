/*
 * Copyright (c) 2026, TwelveMonkeys contributors
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpegxl;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * JXLImageReaderSpi.
 */
public final class JXLImageReaderSpi extends ImageReaderSpiBase {

    /**
     * Creates a {@code JXLImageReaderSpi}.
     */
    public JXLImageReaderSpi() {
        super(new JXLProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;
        stream.mark();

        try {
            byte[] header = new byte[12];
            int read = stream.read(header);
            if (read >= 2 && header[0] == (byte) 0xFF && header[1] == 0x0A) {
                return true; // Codestream signature
            }

            return read == 12
                    && header[0] == 0 && header[1] == 0 && header[2] == 0 && header[3] == 0x0C
                    && header[4] == 'J' && header[5] == 'X' && header[6] == 'L' && header[7] == ' '
                    && header[8] == 0x0D && header[9] == 0x0A
                    && header[10] == (byte) 0x87 && header[11] == 0x0A; // Container signature
        }
        finally {
            stream.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(final Object extension) {
        return new JXLImageReader(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "JPEG XL image reader (via libjxl djxl)";
    }
}
