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

package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Locale;

/**
 * ByteArrayImageInputStreamSpi
 * Experimental
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ByteArrayImageInputStreamSpi.java,v 1.0 May 15, 2008 2:12:12 PM haraldk Exp$
 */
public final class ByteArrayImageInputStreamSpi extends ImageInputStreamSpi {

    public ByteArrayImageInputStreamSpi() {
        this(new StreamProviderInfo());
    }

    private ByteArrayImageInputStreamSpi(ProviderInfo providerInfo) {
        super(providerInfo.getVendorName(), providerInfo.getVersion(), byte[].class);
    }

    @Override
    public ImageInputStream createInputStreamInstance(Object input, boolean useCacheFile, File cacheDir) {
        if (input instanceof byte[]) {
            return new ByteArrayImageInputStream((byte[]) input);
        }

        throw new IllegalArgumentException("Expected input of type byte[]: " + input);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates an ImageInputStream from a byte array";
    }

}
