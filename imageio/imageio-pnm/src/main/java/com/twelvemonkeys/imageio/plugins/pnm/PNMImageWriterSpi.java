package com.twelvemonkeys.imageio.plugins.pnm;

import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

public final class PNMImageWriterSpi extends ImageWriterSpi {

    // TODO: Consider one Spi for each sub-format, as it makes no sense for the writer to write PPM if client code requested PBM or PGM format.
    // ...Then again, what if user asks for PNM? :-P
    /**
     * Creates a {@code PNMImageWriterSpi}.
     */
    public PNMImageWriterSpi() {
        this(IIOUtil.getProviderInfo(PNMImageWriterSpi.class));
    }

    private PNMImageWriterSpi(final ProviderInfo pProviderInfo) {
        super(
                pProviderInfo.getVendorName(),
                pProviderInfo.getVersion(),
                new String[]{
                        "pnm", "pbm", "pgm", "ppm",
                        "PNM", "PBM", "PGM", "PPM"
                },
                new String[]{"pbm", "pgm", "ppm"},
                new String[]{
                        // No official IANA record exists, these are conventional
                        "image/x-portable-pixmap",
                        "image/x-portable-anymap"
                },
                "com.twelvemonkeys.imageio.plugins.pnm.PNMImageWriter",
                new Class[] {ImageOutputStream.class},
                new String[] {"com.twelvemonkeys.imageio.plugins.pnm.PNMImageReaderSpi"},
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    public boolean canEncodeImage(final ImageTypeSpecifier pType) {
        // TODO: FixMe: Support only 1 bit b/w, 8-16 bit gray and 8-16 bit/sample RGB
        return true;
    }

    public ImageWriter createWriterInstance(final Object pExtension) {
        return new PNMImageWriter(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "NetPBM Portable Any Map (PNM) image writer";
    }
}
