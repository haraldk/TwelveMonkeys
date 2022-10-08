package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;
import java.nio.ByteOrder;

final class XWDImageMetadata extends StandardImageMetadataSupport {
    XWDImageMetadata(ImageTypeSpecifier type, XWDX11Header header) {
        super(builder(type)
                      .withSampleMSB(header.bitsPerRGB < 8 && header.bitFillOrder == ByteOrder.LITTLE_ENDIAN ? 0 : 7) // TODO: This is unlikely to be correct...
                      .withFormatVersion("7.0") // The only format we support is the X11 format, and it's version is 7
                      .withTextEntry("DocumentName", header.windowName) // For TIFF interop :-)
        );
    }
}
