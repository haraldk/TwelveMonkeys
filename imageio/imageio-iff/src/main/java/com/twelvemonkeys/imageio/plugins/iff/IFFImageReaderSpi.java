/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * IFFImageReaderSpi
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: IFFImageWriterSpi.java,v 1.0 28.feb.2006 19:21:05 haku Exp$
 */
public final class IFFImageReaderSpi extends ImageReaderSpiBase {

    /**
     * Creates an {@code IFFImageReaderSpi}.
     */
    public IFFImageReaderSpi() {
        super(new IFFProviderInfo());
    }

    public boolean canDecodeInput(Object pSource) throws IOException {
        return pSource instanceof ImageInputStream && canDecode((ImageInputStream) pSource);
    }

    private static boolean canDecode(ImageInputStream pInput) throws IOException {
        pInput.mark();

        try {
            // Is it IFF
            if (pInput.readInt() == IFF.CHUNK_FORM) {
                pInput.readInt();// Skip length field

                int type = pInput.readInt();

                // Is it ILBM or PBM
                if (type == IFF.TYPE_ILBM || type == IFF.TYPE_PBM) {
                    return true;
                }

            }
        }
        finally {
            pInput.reset();
        }

        return false;
    }

    public ImageReader createReaderInstance(Object pExtension) throws IOException {
        return new IFFImageReader(this);
    }

    public String getDescription(Locale pLocale) {
        return "Commodore Amiga/Electronic Arts Image Interchange Format (IFF) image reader";
    }
}
