package com.twelvemonkeys.imageio.plugins.sgi;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.Node;

final class SGIMetadata extends IIOMetadata {
    // TODO: Clean up & extend AbstractMetadata (after moving from PSD -> Core)

    private final SGIHeader header;

    SGIMetadata(final SGIHeader header) {
        this.header = header;
        standardFormatSupported = true;
    }

    @Override public boolean isReadOnly() {
        return true;
    }

    @Override public Node getAsTree(final String formatName) {
        if (formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        }
        else {
            throw new IllegalArgumentException("Unsupported metadata format: " + formatName);
        }
    }

    @Override public void mergeTree(final String formatName, final Node root) {
        if (isReadOnly()) {
            throw new IllegalStateException("Metadata is read-only");
        }
    }

    @Override public void reset() {
        if (isReadOnly()) {
            throw new IllegalStateException("Metadata is read-only");
        }
    }

    @Override protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        // NOTE: There doesn't seem to be any god way to determine color space, other than by convention
        // 1 channel: Gray, 2 channel: Gray + Alpha, 3 channel: RGB, 4 channel: RGBA (hopefully never CMYK...)
        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        switch (header.getColorMode()) {
            case SGI.COLORMODE_NORMAL:
                switch (header.getChannels()) {
                    case 1:
                    case 2:
                        csType.setAttribute("name", "GRAY");
                        break;
                    case 3:
                    case 4:
                        csType.setAttribute("name", "RGB");
                        break;
                    default:
                        csType.setAttribute("name", Integer.toHexString(header.getChannels()).toUpperCase() + "CLR");
                        break;
                }
                break;

            // SGIIMAGE.TXT describes these as RGB
            case SGI.COLORMODE_DITHERED:
            case SGI.COLORMODE_SCREEN:
            case SGI.COLORMODE_COLORMAP:
                csType.setAttribute("name", "RGB");
                break;
        }

        if (csType.getAttribute("name") != null) {
            chroma.appendChild(csType);
        }

        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        numChannels.setAttribute("value", Integer.toString(header.getChannels()));
        chroma.appendChild(numChannels);

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        blackIsZero.setAttribute("value", "TRUE");
        chroma.appendChild(blackIsZero);

        return chroma;
    }

    // No compression

    @Override protected IIOMetadataNode getStandardCompressionNode() {
        if (header.getCompression() != SGI.COMPRESSION_NONE) {
            IIOMetadataNode node = new IIOMetadataNode("Compression");

            IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
            compressionTypeName.setAttribute("value", header.getCompression() == SGI.COMPRESSION_RLE ? "RLE" : "Uknown");
            node.appendChild(compressionTypeName);

            IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
            lossless.setAttribute("value", "TRUE");
            node.appendChild(lossless);

            return node;
        }

        return null;
    }

    @Override protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", "UnsignedIntegral");
        node.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        bitsPerSample.setAttribute("value", createListValue(header.getChannels(), Integer.toString(header.getBytesPerPixel() * 8)));
        node.appendChild(bitsPerSample);

        IIOMetadataNode significantBitsPerSample = new IIOMetadataNode("SignificantBitsPerSample");
        significantBitsPerSample.setAttribute("value", createListValue(header.getChannels(), Integer.toString(computeSignificantBits())));
        node.appendChild(significantBitsPerSample);

        IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
        sampleMSB.setAttribute("value", createListValue(header.getChannels(), "0"));

        return node;
    }

    private int computeSignificantBits() {
        int significantBits = 0;

        int maxSample = header.getMaxValue();

        while (maxSample > 0) {
            maxSample >>>= 1;
            significantBits++;
        }

        return significantBits;
    }

    private String createListValue(final int itemCount, final String... values) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < itemCount; i++) {
            if (buffer.length() > 0) {
                buffer.append(' ');
            }

            buffer.append(values[i % values.length]);
        }

        return buffer.toString();
    }

    @Override protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
        imageOrientation.setAttribute("value", "FlipV");
        dimension.appendChild(imageOrientation);

        return dimension;
    }

    // No document node

    @Override protected IIOMetadataNode getStandardTextNode() {
        if (!header.getName().isEmpty()) {
            IIOMetadataNode text = new IIOMetadataNode("Text");

            IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
            textEntry.setAttribute("keyword", "name");
            textEntry.setAttribute("value", header.getName());
            text.appendChild(textEntry);

            return text;
        }

        return null;
    }

    // No tiling

    @Override protected IIOMetadataNode getStandardTransparencyNode() {
        // NOTE: There doesn't seem to be any god way to determine transparency, other than by convention
        // 1 channel: Gray, 2 channel: Gray + Alpha, 3 channel: RGB, 4 channel: RGBA (hopefully never CMYK...)

        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
        alpha.setAttribute("value", header.getChannels() == 1 || header.getChannels() == 3 ? "none" : "nonpremultiplied");
        transparency.appendChild(alpha);

        return transparency;
    }
}
