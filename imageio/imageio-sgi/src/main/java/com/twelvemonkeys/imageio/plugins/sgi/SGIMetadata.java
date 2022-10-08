package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;

final class SGIMetadata extends StandardImageMetadataSupport {
    public SGIMetadata(ImageTypeSpecifier type, SGIHeader header) {
        super(builder(type)
                      .withSignificantBitsPerSample(computeSignificantBits(header))
                      .withCompressionName(compressionName(header))
                      .withOrientation(ImageOrientation.FlipV)
                      .withTextEntry("DocumentName", header.getName())
        );
    }

    private static int computeSignificantBits(SGIHeader header) {
        int maxSample = header.getMaxValue();

        int significantBits = 1;

        while ((maxSample >>>= 1) != 0) {
            significantBits++;
        }

        return significantBits;
    }

    private static String compressionName(SGIHeader header) {
        switch (header.getCompression()) {
            case SGI.COMPRESSION_NONE:
                return "None";
            case SGI.COMPRESSION_RLE:
                return "RLE";
        }

        return "Uknown";
    }
}
