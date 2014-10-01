package com.twelvemonkeys.imageio.plugins.dcx;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;
import com.twelvemonkeys.imageio.util.IIOUtil;

public final class DCXImageReaderSpi extends ImageReaderSpi {

    /**
     * Creates a {@code DCXImageReaderSpi}.
     */
    public DCXImageReaderSpi() {
        this(IIOUtil.getProviderInfo(DCXImageReaderSpi.class));
    }

    private DCXImageReaderSpi(final ProviderInfo providerInfo) {
        super(
                providerInfo.getVendorName(),
                providerInfo.getVersion(),
                new String[]{
                        "dcx",
                        "DCX"
                },
                new String[]{"dcx"},
                new String[]{
                        // No official IANA record exists
                        "image/dcx",
                        "image/x-dcx",
                },
                "com.twelvemkonkeys.imageio.plugins.dcx.DCXImageReader",
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
            ByteOrder originalByteOrder = stream.getByteOrder();
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            try {
                return stream.readInt() == DCX.MAGIC;
            }
            finally {
                stream.setByteOrder(originalByteOrder);
            }
        }
        finally{
            stream.reset();
        }
    }

    @Override public ImageReader createReaderInstance(final Object extension) throws IOException {
        return new DCXImageReader(this);
    }

    @Override public String getDescription(final Locale locale) {
        return "Multi-page PCX fax document (DCX) image reader";
    }}
