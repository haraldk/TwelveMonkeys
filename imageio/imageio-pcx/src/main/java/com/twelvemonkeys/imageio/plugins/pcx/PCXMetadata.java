package com.twelvemonkeys.imageio.plugins.pcx;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;

import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import org.w3c.dom.Node;

final class PCXMetadata extends IIOMetadata {
    // TODO: Clean up & extend AbstractMetadata (after moving from PSD -> Core)

    private final PCXHeader header;
    private final IndexColorModel vgaPalette;

    PCXMetadata(final PCXHeader header, final IndexColorModel vgaPalette) {
        this.header = header;
        this.vgaPalette = vgaPalette;

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

        IndexColorModel palette = null;

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        switch (header.getBitsPerPixel()) {
            case 1:
            case 2:
            case 4:
                palette = header.getEGAPalette();
                csType.setAttribute("name", "RGB");
                break;
            case 8:
                // We may have IndexColorModel here for 1 channel images
                if (header.getChannels() == 1 && header.getPaletteInfo() != PCX.PALETTEINFO_GRAY) {
                    palette = vgaPalette;
                    csType.setAttribute("name", "RGB");
                    break;
                }
                if (header.getChannels() == 1) {
                    csType.setAttribute("name", "GRAY");
                    break;
                }
                csType.setAttribute("name", "RGB");
                break;

            case 24:
                // Some sources says this is possible... Untested.
                csType.setAttribute("name", "RGB");
                break;

            default:
                csType.setAttribute("name", "Unknown");
        }

        chroma.appendChild(csType);

        if (palette != null) {
            IIOMetadataNode paletteNode = new IIOMetadataNode("Palette");
            chroma.appendChild(paletteNode);

            for (int i = 0; i < palette.getMapSize(); i++) {
                IIOMetadataNode paletteEntry = new IIOMetadataNode("PaletteEntry");
                paletteEntry.setAttribute("index", Integer.toString(i));

                paletteEntry.setAttribute("red", Integer.toString(palette.getRed(i)));
                paletteEntry.setAttribute("green", Integer.toString(palette.getGreen(i)));
                paletteEntry.setAttribute("blue", Integer.toString(palette.getBlue(i)));

                paletteNode.appendChild(paletteEntry);
            }
        }

        // TODO: Channels in chroma node should reflect channels in color model, not data! (see data node)
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
        if (header.getCompression() != PCX.COMPRESSION_NONE) {
            IIOMetadataNode node = new IIOMetadataNode("Compression");

            IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
            compressionTypeName.setAttribute("value", header.getCompression() == PCX.COMPRESSION_RLE ? "RLE" : "Uknown");
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

        // Planar configuration only makes sense for multi-channel images
        if (header.getChannels() > 1) {
            IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
            planarConfiguration.setAttribute("value", "LineInterleaved");
            node.appendChild(planarConfiguration);
        }

        // TODO: SampleFormat value = Index if colormapped/palette data
        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", "UnsignedIntegral");
        node.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        bitsPerSample.setAttribute("value", createListValue(header.getChannels(), Integer.toString(header.getBitsPerPixel())));
        node.appendChild(bitsPerSample);

        IIOMetadataNode significantBitsPerSample = new IIOMetadataNode("SignificantBitsPerSample");
        significantBitsPerSample.setAttribute("value", createListValue(header.getChannels(), Integer.toString(header.getBitsPerPixel())));
        node.appendChild(significantBitsPerSample);

        IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
        sampleMSB.setAttribute("value", createListValue(header.getChannels(), "0"));

        return node;
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
        imageOrientation.setAttribute("value", "Normal");
        dimension.appendChild(imageOrientation);

        return dimension;
    }

    // TODO: document node with version

    // No text node

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
