package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

public final class XWDImageReaderSpi extends ImageReaderSpiBase {
    public XWDImageReaderSpi() {
        super(new XWDProviderInfo());
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;

        stream.mark();

        try {
            return XWDX11Header.isX11(stream);
        } finally {
            stream.reset();
        }
    }

    @Override
    public XWDImageReader createReaderInstance(Object extension) {
        return new XWDImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "X11 Window Dump Format (XWD) reader";
    }
}
