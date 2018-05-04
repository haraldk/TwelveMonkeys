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

package com.twelvemonkeys.imageio.plugins.dng;

import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * CR2ImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CR2ImageReaderSpi.java,v 1.0 07.04.14 21:26 haraldk Exp$
 */
public final class DNGImageReaderSpi extends ImageReaderSpiBase {
    public DNGImageReaderSpi() {
        super(new DNGProviderInfo());
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;

        stream.mark();
        try {
            byte[] bom = new byte[2];
            stream.readFully(bom);

            ByteOrder originalOrder = stream.getByteOrder();

            try {
                if (bom[0] == 'I' && bom[1] == 'I') {
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                }
                else if (bom[0] == 'M' && bom[1] == 'M') {
                    stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                }
                else  {
                    return false;
                }

                int tiffMagic = stream.readUnsignedShort();
                if (tiffMagic != TIFF.TIFF_MAGIC) {
                    return false;
                }

                // TODO: This is not different from a normal TIFF...

                return true;
            }
            finally {
                stream.setByteOrder(originalOrder);
            }
        }
        finally {
            stream.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new DNGImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Adobe Digital Negative (DNG) format Reader";
    }
}
