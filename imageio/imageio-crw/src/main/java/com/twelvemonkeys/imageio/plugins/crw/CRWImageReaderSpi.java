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

package com.twelvemonkeys.imageio.plugins.crw;

import static java.util.Arrays.copyOfRange;

import com.twelvemonkeys.imageio.plugins.crw.ciff.CIFF;
import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;

/**
 * CRWImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CRWImageReaderSpi.java,v 1.0 07.04.14 21:26 haraldk Exp$
 */
public final class CRWImageReaderSpi extends ImageReaderSpiBase {
    @SuppressWarnings("WeakerAccess")
    public CRWImageReaderSpi() {
        super(new CRWProviderInfo());
    }

    @Override
    public void onRegistration(ServiceRegistry registry, Class<?> category) {
        // TODO: This code assumes that any TIFFImageReaderSpi is already installed... It may be installed at a later time. :-(
        // Make sure we're ordered before any TIFF reader
        Iterator<ImageReaderSpi> spis = registry.getServiceProviders(ImageReaderSpi.class, new ServiceRegistry.Filter() {
            @Override
            public boolean filter(Object provider) {
                return provider instanceof ImageReaderSpi && isTIFFReaderSpi((ImageReaderSpi) provider);
            }

            private boolean isTIFFReaderSpi(final ImageReaderSpi provider) {
                String[] formatNames = provider.getFormatNames();
                for (String formatName : formatNames) {
                    if (formatName.equalsIgnoreCase("TIFF")) {
                        return true;
                    }
                }

                return false;
            }
        }, true);

        while (spis.hasNext()) {
            ImageReaderSpi spi = spis.next();
            registry.setOrdering(ImageReaderSpi.class, this, spi);
        }
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
                // CIFF byte order mark II (Intel) or MM (Motorola), just like TIFF
                if (bom[0] == 'I' && bom[1] == 'I') {
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                }
                else if (bom[0] == 'M' && bom[1] == 'M') {
                    stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                }
                else  {
                    return false;
                }

                // CIFF header is always 26 bytes
                int size = stream.readInt();
                if (size != CIFF.HEADER_SIZE) {
                    return false;
                }

                // CRW uses type HEAP and subtype CCDR
                byte[] type = new byte[8];
                stream.readFully(type);

                if (!Arrays.equals(CIFF.TYPE_HEAP, copyOfRange(type, 0, 4))
                    || !Arrays.equals(CIFF.SUBTYPE_CCDR, copyOfRange(type, 4, 8))) {
                    return false;
                }

                // Version 1.2
                return stream.readUnsignedInt() == CIFF.VERSION_1_2;
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
    public CRWImageReader createReaderInstance(Object extension) {
        return new CRWImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Canon RAW (CRW) format Reader";
    }

}
