package com.twelvemonkeys.imageio.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * ByteArrayImageInputStreamSpi
 * Experimental
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ByteArrayImageInputStreamSpi.java,v 1.0 May 15, 2008 2:12:12 PM haraldk Exp$
 */
public class ByteArrayImageInputStreamSpi extends ImageInputStreamSpi {

    public ByteArrayImageInputStreamSpi() {
        super("TwelveMonkeys", "1.0 BETA", byte[].class);
    }

    public ImageInputStream createInputStreamInstance(Object pInput, boolean pUseCache, File pCacheDir) throws IOException {
        if (pInput instanceof byte[]) {
            return new ByteArrayImageInputStream((byte[]) pInput);
        }
        else {
            throw new IllegalArgumentException("Expected input of type byte[]: " + pInput);
        }
    }

    public String getDescription(Locale pLocale) {
        return "Service provider that instantiates an ImageInputStream from a byte array";
    }

}
