package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

public final class SGIImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates a {@code SGIImageReaderSpi}.
     */
    public SGIImageReaderSpi() {
        this(IIOUtil.getProviderInfo(SGIImageReaderSpi.class));
    }

    private SGIImageReaderSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{
                        "sgi",
                        "SGI"
                },
                new String[]{"sgi"},
                new String[]{
                        // No official IANA record exists
                        "image/sgi",
                        "image/x-sgi",
                },
                "com.twelvemkonkeys.imageio.plugins.sgi.SGIImageReader",
                new Class[] {ImageInputStream.class},
                null,
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
                case SGI.MAGIC:
                    byte compression = stream.readByte();
                    byte bpp = stream.readByte();

                    return (compression == SGI.COMPRESSION_NONE || compression == SGI.COMPRESSION_RLE) && (bpp == 1 || bpp == 2);
                default:
                    return false;
            }
        }
        finally {
            stream.reset();
        }
    }

    @Override public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new SGIImageReader(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "Silicon Graphics (SGI) image reader";
    }
}

