package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;

import java.awt.image.Raster;
import java.util.Locale;

public final class DDSImageWriterSpi extends ImageWriterSpiBase {
    public DDSImageWriterSpi() {
        super(new DDSProviderInfo());
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        int numBands = type.getNumBands();
        if (numBands < 3 || numBands > 4) {
            return false;
        }

        return type.getSampleModel().getSampleSize(0) == 8;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new DDSImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "DirectDraw Surface (DDS) Image Writer";
    }
}
