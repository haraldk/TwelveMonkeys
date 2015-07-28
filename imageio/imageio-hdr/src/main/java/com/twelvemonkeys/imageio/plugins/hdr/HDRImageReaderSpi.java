package com.twelvemonkeys.imageio.plugins.hdr;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * HDRImageReaderSpi.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRImageReaderSpi.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
public final class HDRImageReaderSpi extends ImageReaderSpiBase {
    public HDRImageReaderSpi() {
        super(new HDRProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;

        stream.mark();

        try {
            // NOTE: All images I have found starts with #?RADIANCE (or has no #? line at all),
            // although some sources claim that #?RGBE is also used.
            byte[] magic = new byte[HDR.RADIANCE_MAGIC.length];
            stream.readFully(magic);

            return Arrays.equals(HDR.RADIANCE_MAGIC, magic)
                    || Arrays.equals(HDR.RGBE_MAGIC, Arrays.copyOf(magic, 6));
        }
        finally {
            stream.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new HDRImageReader(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "Radiance RGBE High Dynaimc Range (HDR) image reader";
    }
}
