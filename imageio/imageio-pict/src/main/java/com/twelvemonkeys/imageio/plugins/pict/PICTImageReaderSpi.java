/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.Locale;

/**
 * PICTImageReaderSpi
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: PICTImageReaderSpi.java,v 1.0 28.feb.2006 19:21:05 haku Exp$
 */
public class PICTImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates a {@code PICTImageReaderSpi}.
     */
    public PICTImageReaderSpi() {
        this(IIOUtil.getProviderInfo(PICTImageReaderSpi.class));
    }

    private PICTImageReaderSpi(final ProviderInfo pProviderInfo) {
        super(
                pProviderInfo.getVendorName(),
                pProviderInfo.getVersion(),
                new String[]{"pct", "PCT", "pict", "PICT"},
                new String[]{"pct", "pict"},
                new String[]{"image/pict", "image/x-pict"},
                "com.twelvemkonkeys.imageio.plugins.pict.PICTImageReader",
                new Class[] {ImageInputStream.class},
                new String[]{"com.twelvemkonkeys.imageio.plugins.pict.PICTImageWriterSpi"},
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;
        stream.mark();

        try {
            if (isPICT(stream)) {
                // If PICT Clipping format, return true immediately
                return true;
            }
            else {
                // Skip header 512 bytes for file-based streams
                stream.reset();
                PICTImageReader.skipNullHeader(stream);
            }

            return isPICT(stream);
        }
        catch (EOFException ignore) {
            return false;
        }
        finally {
            stream.reset();
        }
    }

    private boolean isPICT(final ImageInputStream pStream) throws IOException {
        // Size may be 0, so we can't use this for validation...
        pStream.readUnsignedShort();

        // Sanity check bounding box
        int y1 = pStream.readUnsignedShort();
        int x1 = pStream.readUnsignedShort();
        // TODO: Figure out if frame can ever start at negative bounds...
        // if (x1 != 0 || y1 != 0) {
        //     return false;
        // }

        int y2 = pStream.readUnsignedShort();
        int x2 = pStream.readUnsignedShort();
        if (x2 - x1 < 0 || y2 - y1 < 0) {
            return false;
        }

        int magic = pStream.readInt();
        
        return (magic & 0xffff0000) == PICT.MAGIC_V1 || magic == PICT.MAGIC_V2;
    }

    public ImageReader createReaderInstance(final Object pExtension) throws IOException {
        return new PICTImageReader(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Apple Mac Paint Picture (PICT) image reader";
    }
}
