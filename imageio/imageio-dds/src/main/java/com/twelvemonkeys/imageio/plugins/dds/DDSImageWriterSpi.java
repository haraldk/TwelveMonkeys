package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import java.util.Locale;

public final class DDSImageWriterSpi extends ImageWriterSpiBase {
    public DDSImageWriterSpi() {
        super(new DDSProviderInfo());
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new DDSImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Direct Draw Surface (DDS) Image Writer";
    }
}
