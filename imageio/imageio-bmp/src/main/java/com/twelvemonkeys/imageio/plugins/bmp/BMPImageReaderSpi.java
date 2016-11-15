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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import static com.twelvemonkeys.imageio.util.IIOUtil.lookupProviderByName;

/**
 * BMPImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: BMPImageReaderSpi.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 */
public final class BMPImageReaderSpi extends ImageReaderSpiBase {
    public BMPImageReaderSpi() {
        super(new BMPProviderInfo());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        ImageReaderSpi defaultProvider = lookupProviderByName(registry, "com.sun.imageio.plugins.bmp.BMPImageReaderSpi");

        if (defaultProvider != null) {
            // Order before com.sun provider, to aid ImageIO in selecting our reader
            registry.setOrdering((Class<ImageReaderSpi>) category, this, defaultProvider);
        }
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        return pSource instanceof ImageInputStream && canDecode((ImageInputStream) pSource);
    }

    private static boolean canDecode(final ImageInputStream pInput) throws IOException {
        byte[] fileHeader = new byte[18]; // Strictly: file header (14 bytes) + BMP header size field (4 bytes)

        try {
            pInput.mark();
            pInput.readFully(fileHeader);

            // Magic: BM
            if (fileHeader[0] != 'B' || fileHeader[1] != 'M') {
                return false;
            }

            ByteBuffer header = ByteBuffer.wrap(fileHeader);
            header.order(ByteOrder.LITTLE_ENDIAN);

            int fileSize = header.getInt(2);
            if (fileSize <= 0) {
                return false;
            }

            // Ignore hot-spots etc..

            int offset = header.getInt(10);
            if (offset <= 0) {
                return false;
            }

            int headerSize = header.getInt(14);
            switch (headerSize) {
                case DIB.BITMAP_CORE_HEADER_SIZE:
                case DIB.OS2_V2_HEADER_16_SIZE:
                case DIB.OS2_V2_HEADER_SIZE:
                case DIB.BITMAP_INFO_HEADER_SIZE:
                case DIB.BITMAP_V2_INFO_HEADER_SIZE:
                case DIB.BITMAP_V3_INFO_HEADER_SIZE:
                case DIB.BITMAP_V4_INFO_HEADER_SIZE:
                case DIB.BITMAP_V5_INFO_HEADER_SIZE:
                    return true;
                default:
                    return false;
            }
        }
        finally {
            pInput.reset();
        }
    }

    public ImageReader createReaderInstance(final Object pExtension) throws IOException {
        return new BMPImageReader(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Windows Device Independent Bitmap Format (BMP) Reader";
    }
}
