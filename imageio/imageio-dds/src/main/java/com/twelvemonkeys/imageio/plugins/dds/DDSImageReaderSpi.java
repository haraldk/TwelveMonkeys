package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public final class DDSImageReaderSpi extends ImageReaderSpiBase {

    public DDSImageReaderSpi() {
        super(new DDSProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;

        stream.mark();

        try {
            byte[] magic = new byte[DDS.MAGIC.length];
            stream.readFully(magic);

            return Arrays.equals(DDS.MAGIC, magic);
        } finally {
            stream.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new DDSImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Direct DrawSurface (DDS) Image Reader";
    }
}
