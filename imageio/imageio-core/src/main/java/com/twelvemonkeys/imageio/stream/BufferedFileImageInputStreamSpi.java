/*
 * Copyright (c) 2021, Harald Kuhr
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
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Locale;

/**
 * BufferedFileImageInputStreamSpi
 * Experimental
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedFileImageInputStreamSpi.java,v 1.0 May 15, 2008 2:14:59 PM haraldk Exp$
 */
public class BufferedFileImageInputStreamSpi extends ImageInputStreamSpi {
    public BufferedFileImageInputStreamSpi() {
        this(new StreamProviderInfo());
    }

    private BufferedFileImageInputStreamSpi(ProviderInfo providerInfo) {
        super(providerInfo.getVendorName(), providerInfo.getVersion(), File.class);
    }

    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        Iterator<ImageInputStreamSpi> providers = registry.getServiceProviders(ImageInputStreamSpi.class, new FileInputFilter(), true);

        while (providers.hasNext()) {
            ImageInputStreamSpi provider = providers.next();
            if (provider != this) {
                registry.setOrdering(ImageInputStreamSpi.class, this, provider);
            }
        }
    }

    public ImageInputStream createInputStreamInstance(final Object input, final boolean pUseCache, final File pCacheDir) {
        if (input instanceof File) {
            try {
                return new BufferedFileImageInputStream((File) input);
            }
            catch (FileNotFoundException e) {
                // For consistency with the JRE bundled SPIs, we'll return null here,
                // even though the spec does not say that's allowed.
                // The problem is that the SPIs can only declare that they support an input type like a File,
                // instead they should be allowed to inspect the instance, to see that the file does exist...
                return null;
            }
        }

        throw new IllegalArgumentException("Expected input of type File: " + input);
    }

    @Override
    public boolean canUseCacheFile() {
        return false;
    }

    public String getDescription(final Locale pLocale) {
        return "Service provider that instantiates an ImageInputStream from a File";
    }

    private static class FileInputFilter implements ServiceRegistry.Filter {
        @Override
        public boolean filter(final Object provider) {
            return ((ImageInputStreamSpi) provider).getInputClass() == File.class;
        }
    }
}
