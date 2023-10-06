package com.twelvemonkeys.imageio.plugins.hdr;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;

final class HDRMetadata extends StandardImageMetadataSupport {
    public HDRMetadata(ImageTypeSpecifier type, HDRHeader header) {
        super(builder(type)
                      .withCompressionTypeName("RLE")
                      .withTextEntry("Software", header.getSoftware()));
    }

    // For HDR, the stored sample data is UnsignedIntegral and data is 4 channels (RGB+Exp),
    // but decoded to Real (float) 3 chanel RGB
    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", "UnsignedIntegral");
        node.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        bitsPerSample.setAttribute("value", "8 8 8 8");
        node.appendChild(bitsPerSample);

        return node;
    }
}
