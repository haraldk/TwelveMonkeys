package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;

final class PCXMetadata extends StandardImageMetadataSupport {
    public PCXMetadata(ImageTypeSpecifier type, PCXHeader header) {
        super(builder(type)
                      .withPlanarConfiguration(planarConfiguration(header))
                      .withCompressionTypeName(compressionName(header))
                      .withFormatVersion(String.valueOf(header.getVersion())));
    }

    private static PlanarConfiguration planarConfiguration(PCXHeader header) {
        System.out.println("header = " + header);
        return header.getChannels() > 1 ? PlanarConfiguration.LineInterleaved : null;
    }

    private static String compressionName(PCXHeader header) {
        switch (header.getCompression()) {
            case PCX.COMPRESSION_NONE:
                return "None";
            case PCX.COMPRESSION_RLE:
                return "RLE";
        }

        return "Unknown";
    }
}
