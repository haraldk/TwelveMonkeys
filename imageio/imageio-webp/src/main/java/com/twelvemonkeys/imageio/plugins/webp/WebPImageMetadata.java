package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;

import static com.twelvemonkeys.lang.Validate.notNull;

final class WebPImageMetadata extends StandardImageMetadataSupport {
    WebPImageMetadata(ImageTypeSpecifier type, VP8xChunk header) {
        super(builder(type)
                      .withCompressionTypeName(notNull(header, "header").isLossless ? "VP8L" : "VP8")
                      .withCompressionLossless(header.isLossless)
                      .withPixelAspectRatio(1.0)
                      .withFormatVersion("1.0")
              // TODO: Get useful text nodes from EXIF or XMP
        );
    }
}
