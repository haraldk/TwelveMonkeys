package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

public final class PNMImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates a {@code PNMImageReaderSpi}.
     */
    public PNMImageReaderSpi() {
        this(IIOUtil.getProviderInfo(PNMImageReaderSpi.class));
    }

    private PNMImageReaderSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{
                        "pnm", "pbm", "pgm", "ppm", "pam",
                        "PNM", "PBM", "PGM", "PPM", "PAM"
                },
                new String[]{"pbm", "pgm", "ppm", "pam"},
                new String[]{
                        // No official IANA record exists, these are conventional
                        "image/x-portable-pixmap",
                        "image/x-portable-anymap",
                        "image/x-portable-arbitrarymap" // PAM
                },
                "com.twelvemkonkeys.imageio.plugins.pnm.PNMImageReader",
                new Class[] {ImageInputStream.class},
                new String[]{
                        "com.twelvemkonkeys.imageio.plugins.pnm.PNMImageWriterSpi",
                        "com.twelvemkonkeys.imageio.plugins.pnm.PAMImageWriterSpi"
                },
                true, // supports standard stream metadata
                null, null, // native stream format name and class
                null, null, // extra stream formats
                true, // supports standard image metadata
                null, null,
                null, null // extra image metadata formats
        );
    }

    @Override public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;

        stream.mark();

        try {
            short magic = stream.readShort();

            switch (magic) {
                case PNM.PBM_PLAIN:
                case PNM.PBM:
                case PNM.PGM_PLAIN:
                case PNM.PGM:
                case PNM.PPM_PLAIN:
                case PNM.PPM:
                case PNM.PFM_GRAY:
                case PNM.PFM_RGB:
                    return true;
                case PNM.PAM:
                    return stream.readInt() != PNM.XV_THUMBNAIL_MAGIC;
                default:
                    return false;
            }
        }
        finally {
            stream.reset();
        }
    }

    @Override public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new PNMImageReader(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "NetPBM Portable Any Map (PNM and PAM) image reader";
    }
}
