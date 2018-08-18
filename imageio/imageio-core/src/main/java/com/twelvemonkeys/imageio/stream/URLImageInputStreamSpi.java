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

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

/**
 * URLImageInputStreamSpi
 * Experimental
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: URLImageInputStreamSpi.java,v 1.0 May 15, 2008 2:14:59 PM haraldk Exp$
 */
 // TODO: URI instead of URL?
public class URLImageInputStreamSpi extends ImageInputStreamSpi {
    public URLImageInputStreamSpi() {
        super("TwelveMonkeys", "1.0 BETA", URL.class);
    }

    // TODO: Create a URI or URLImageInputStream class, with a getUR[I|L] method, to allow for multiple file formats
    // The good thing with that is that it does not clash with the built-in Sun-stuff or other people's hacks
    // The bad thing is that most people don't expect there to be an UR[I|L]ImageInputStreamSpi..
    public ImageInputStream createInputStreamInstance(final Object pInput, final boolean pUseCache, final File pCacheDir) throws IOException {
        if (pInput instanceof URL) {
            URL url = (URL) pInput;

            // Special case for file protocol, a lot faster than FileCacheImageInputStream
            if ("file".equals(url.getProtocol())) {
                try {
                    return new BufferedImageInputStream(new FileImageInputStream(new File(url.toURI())));
                }
                catch (URISyntaxException ignore) {
                    // This should never happen, but if it does, we'll fall back to using the stream  
                    ignore.printStackTrace();
                }
            }

            // Otherwise revert to cached
            final InputStream stream = url.openStream();
            if (pUseCache) {
                return new BufferedImageInputStream(new FileCacheImageInputStream(stream, pCacheDir) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        }
                        finally {
                            stream.close(); // NOTE: If this line throws IOE, it will shadow the original..
                        }
                    }
                });
            }
            else {
                return new MemoryCacheImageInputStream(stream) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        }
                        finally {
                            stream.close(); // NOTE: If this line throws IOE, it will shadow the original..
                        }
                    }
                };
            }
        }
        else {
            throw new IllegalArgumentException("Expected input of type URL: " + pInput);
        }
    }

    public String getDescription(final Locale pLocale) {
        return "Service provider that instantiates an ImageInputStream from a URL";
    }
}
