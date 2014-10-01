package com.twelvemonkeys.imageio.plugins.pnm;

import java.util.Locale;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

public final class PAMImageWriterSpi extends ImageWriterSpi {

    /**
     * Creates a {@code PAMImageWriterSpi}.
     */
    public PAMImageWriterSpi() {
        this(IIOUtil.getProviderInfo(PAMImageWriterSpi.class));
    }

    private PAMImageWriterSpi(final ProviderInfo pProviderInfo) {
        super(
                pProviderInfo.getVendorName(),
                pProviderInfo.getVersion(),
                new String[]{"pam", "PAM"},
                new String[]{"pam"},
                new String[]{
                        // No official IANA record exists, these are conventional
                        "image/x-portable-arbitrarymap" // PAM
                },
                "com.twelvemonkeys.imageio.plugins.pnm.PNMImageWriter",
                new Class[] {ImageOutputStream.class},
                new String[] {"com.twelvemonkeys.imageio.plugins.pnm.PNMImageReaderSpi"},
                true, null, null, null, null,
                true, null, null, null, null
        );
    }

    public boolean canEncodeImage(final ImageTypeSpecifier pType) {
        // TODO: FixMe
        return true;
    }

    public ImageWriter createWriterInstance(final Object pExtension) {
        return new PNMImageWriter(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "NetPBM Portable Arbitrary Map (PAM) image writer";
    }
}
