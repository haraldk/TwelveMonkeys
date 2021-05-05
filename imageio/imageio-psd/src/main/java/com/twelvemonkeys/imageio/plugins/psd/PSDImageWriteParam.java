package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.ImageWriteParam;
import java.util.Locale;

/**
 * PSDImageWriteParam
 */
public final class PSDImageWriteParam extends ImageWriteParam {

    PSDImageWriteParam() {
        this(Locale.getDefault());
    }

    PSDImageWriteParam(final Locale locale) {
        super(locale);

        compressionTypes = new String[] {
                "None",
                "PackBits",
                // Two ZIP compression types are defined in spec, never seen in the wild...
                // "ZIP",
                // "ZIP+Predictor",
        };
        compressionType = compressionTypes[1];
        canWriteCompressed = true;
    }

    static int getCompressionType(final ImageWriteParam param) {
        if (param == null || param.getCompressionMode() != MODE_EXPLICIT || param.getCompressionType() == null || param.getCompressionType().equals("None")) {
            return PSD.COMPRESSION_NONE;
        }
        else if (param.getCompressionType().equals("PackBits")) {
            return PSD.COMPRESSION_RLE;
        }

        throw new IllegalArgumentException(String.format("Unsupported compression type: %s", param.getCompressionType()));
    }
}
