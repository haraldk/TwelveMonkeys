/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * ICOImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ICOImageReaderSpi.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 */
public final class ICOImageReaderSpi extends ImageReaderSpiBase {

    public ICOImageReaderSpi() {
        super(new ICOProviderInfo());
    }

    public boolean canDecodeInput(final Object source) throws IOException {
        return source instanceof ImageInputStream && canDecode((ImageInputStream) source, DIB.TYPE_ICO);
    }

    static boolean canDecode(final ImageInputStream input, final int type) throws IOException {
        byte[] signature = new byte[4];

        try {
            input.mark();
            input.readFully(signature);

            int count = input.readByte() + (input.readByte() << 8);

            return (signature[0] == 0x0 && signature[1] == 0x0 && signature[2] == type
                    && signature[3] == 0x0 && count > 0);
        }
        finally {
            input.reset();
        }
    }

    public ICOImageReader createReaderInstance(final Object extension) {
        return new ICOImageReader(this);
    }

    public String getDescription(final Locale locale) {
        return "Windows Icon Format (ICO) Reader";
    }
}
