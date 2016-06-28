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

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * IFFImageWriterSpi
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: IFFImageWriterSpi.java,v 1.0 02.mar.2006 19:21:05 haku Exp$
 */
public class IFFImageWriterSpi extends ImageWriterSpiBase {

    /**
     * Creates an {@code IFFImageWriterSpi}.
     */
    public IFFImageWriterSpi() {
        super(new IFFProviderInfo());
    }

    public boolean canEncodeImage(final ImageTypeSpecifier pType) {
        // TODO: Probably can't store 16 bit types etc...
        // TODO: Can't store CMYK (well.. it does, but they can't be read back) 
        return true;
    }

    public ImageWriter createWriterInstance(Object pExtension) throws IOException {
        return new IFFImageWriter(this);
    }

    public String getDescription(Locale pLocale) {
        return "Commodore Amiga/Electronic Arts Image Interchange Format (IFF) image writer";
    }
}
