package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.spi.ImageWriterSpiBase;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import java.util.Locale;

import static com.twelvemonkeys.imageio.plugins.psd.PSDImageWriter.getBitsPerSample;
import static com.twelvemonkeys.imageio.plugins.psd.PSDImageWriter.getColorMode;

/**
 * PSDImageWriterSpi
 */
public final class PSDImageWriterSpi extends ImageWriterSpiBase {

    public PSDImageWriterSpi() {
        super(new PSDProviderInfo());
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        // PSD supports:
        //  - 1, 8, 16 or 32 bit/sample
        //  - Number of samples <= 56
        //  - RGB, CMYK, Gray, Indexed color
        try {
            getBitsPerSample(type.getSampleModel());
            getColorMode(type.getColorModel());
        }
        catch (IllegalArgumentException ignore) {
            // We can't write this type
            return false;
        }

        return type.getNumBands() <= 56; // Can't be negative
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new PSDImageWriter(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Adobe Photoshop Document (PSD) image writer";
    }
}
