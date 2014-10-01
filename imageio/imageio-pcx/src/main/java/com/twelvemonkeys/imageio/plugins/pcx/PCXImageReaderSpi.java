package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

public final class PCXImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates a {@code PCXImageReaderSpi}.
     */
    public PCXImageReaderSpi() {
        this(IIOUtil.getProviderInfo(PCXImageReaderSpi.class));
    }

    private PCXImageReaderSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{
                        "pcx",
                        "PCX"
                },
                new String[]{"pcx"},
                new String[]{
                        // No official IANA record exists
                        "image/pcx",
                        "image/x-pcx",
                },
                "com.twelvemkonkeys.imageio.plugins.pcx.PCXImageReader",
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
            byte magic = stream.readByte();

            switch (magic) {
                case PCX.MAGIC:
                    byte version = stream.readByte();

                    switch (version) {
                        case PCX.VERSION_2_5:
                        case PCX.VERSION_2_8_PALETTE:
                        case PCX.VERSION_2_8_NO_PALETTE:
                        case PCX.VERSION_2_X_WINDOWS:
                        case PCX.VERSION_3:
                            byte compression = stream.readByte();
                            byte bpp = stream.readByte();

                            return (compression == PCX.COMPRESSION_NONE || compression == PCX.COMPRESSION_RLE) && (bpp == 1 || bpp == 2 || bpp == 4 || bpp == 8);
                        default:
                            return false;
                    }
                default:
                    return false;
            }
        }
        finally {
            stream.reset();
        }
    }

    @Override public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new PCXImageReader(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "PC Paintbrush (PCX) image reader";
    }
}

