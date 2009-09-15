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
