/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * TIFFImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReaderSpi.java,v 1.0 08.05.12 15:14 haraldk Exp$
 */
public class TIFFImageReaderSpi extends ImageReaderSpi {
    // TODO: Should we make sure we register (order) before the com.sun.imageio thing (that isn't what is says) provided by Apple?
    /**
     * Creates a {@code TIFFImageReaderSpi}.
     */
    public TIFFImageReaderSpi() {
        this(IIOUtil.getProviderInfo(TIFFImageReaderSpi.class));
    }

    private TIFFImageReaderSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{"tiff", "TIFF"},
                new String[]{"tif", "tiff"},
                new String[]{
                        "image/tiff", "image/x-tiff"
                },
                "com.twelvemkonkeys.imageio.plugins.tiff.TIFFImageReader",
                STANDARD_INPUT_TYPE,
//                new String[]{"com.twelvemkonkeys.imageio.plugins.tif.TIFFImageWriterSpi"},
                null,
                true, // supports standard stream metadata
                null, null, // native stream format name and class
                null, null, // extra stream formats
                true, // supports standard image metadata
                null, null,
                null, null // extra image metadata formats
        );
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

                // TODO: BigTiff uses version 43 instead of TIFF's 42, and header is slightly different, see
                // http://www.awaresystems.be/imaging/tiff/bigtiff.html
                int magic = stream.readUnsignedShort();

                return magic == TIFF.TIFF_MAGIC;
            }
            finally {
                stream.setByteOrder(originalOrder);
            }
        }
        finally {
            stream.reset();
        }
    }

    public TIFFImageReader createReaderInstance(final Object pExtension) {
        return new TIFFImageReader(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Aldus/Adobe Tagged Image File Format (TIFF) image reader";
    }
}
