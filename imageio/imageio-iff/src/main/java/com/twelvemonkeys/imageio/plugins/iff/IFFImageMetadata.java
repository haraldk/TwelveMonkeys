package com.twelvemonkeys.imageio.plugins.iff;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.metadata.IIOMetadataNode;

import com.twelvemonkeys.imageio.AbstractMetadata;

final class IFFImageMetadata extends AbstractMetadata {
    private final int formType;
    private final BMHDChunk header;
    private final IndexColorModel colorMap;
    private final CAMGChunk viewPort;
    private final List<GenericChunk> meta;

    IFFImageMetadata(int formType, BMHDChunk header, IndexColorModel colorMap, CAMGChunk viewPort, List<GenericChunk> meta) {
        this.formType = formType;
        this.header = header;
        this.colorMap = colorMap;
        this.viewPort = viewPort;
        this.meta = meta;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(csType);

        switch (header.bitplanes) {
            case 8:
                if (colorMap == null) {
                    csType.setAttribute("name", "GRAY");
                    break;
                }
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 24:
            case 32:
                csType.setAttribute("name", "RGB");
                break;
            default:
                csType.setAttribute("name", "Unknown");
        }

        // NOTE: Channels in chroma node reflects channels in color model (see data node, for channels in data)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChannels);
        if (colorMap == null && header.bitplanes == 8) {
            numChannels.setAttribute("value", Integer.toString(1));
        }
        else if (header.bitplanes == 32) {
            numChannels.setAttribute("value", Integer.toString(4));
        }
        else {
            numChannels.setAttribute("value", Integer.toString(3));
        }

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        chroma.appendChild(blackIsZero);
        blackIsZero.setAttribute("value", "TRUE");

        // NOTE: TGA files may contain a color map, even if true color...
        // Not sure if this is a good idea to expose to the meta data,
        // as it might be unexpected... Then again...
        if (colorMap != null) {
            IIOMetadataNode palette = new IIOMetadataNode("Palette");
            chroma.appendChild(palette);

            for (int i = 0; i < colorMap.getMapSize(); i++) {
                IIOMetadataNode paletteEntry = new IIOMetadataNode("PaletteEntry");
                palette.appendChild(paletteEntry);
                paletteEntry.setAttribute("index", Integer.toString(i));

                paletteEntry.setAttribute("red", Integer.toString(colorMap.getRed(i)));
                paletteEntry.setAttribute("green", Integer.toString(colorMap.getGreen(i)));
                paletteEntry.setAttribute("blue", Integer.toString(colorMap.getBlue(i)));
            }
        }

        // TODO: Background color is the color of the transparent index in the color model?
//        if (extensions != null && extensions.getBackgroundColor() != 0) {
//            Color background = new Color(extensions.getBackgroundColor(), true);
//
//            IIOMetadataNode backgroundColor = new IIOMetadataNode("BackgroundColor");
//            chroma.appendChild(backgroundColor);
//
//            backgroundColor.setAttribute("red", Integer.toString(background.getRed()));
//            backgroundColor.setAttribute("green", Integer.toString(background.getGreen()));
//            backgroundColor.setAttribute("blue", Integer.toString(background.getBlue()));
//        }

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        if (header.compressionType == BMHDChunk.COMPRESSION_NONE) {
            return null; // All defaults
        }

        IIOMetadataNode node = new IIOMetadataNode("Compression");

        IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
        compressionTypeName.setAttribute("value", "RLE");
        node.appendChild(compressionTypeName);

        IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
        lossless.setAttribute("value", "TRUE");
        node.appendChild(lossless);

        return node;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data = new IIOMetadataNode("Data");

        // PlanarConfiguration
        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        switch (formType) {
            case IFF.TYPE_PBM:
                planarConfiguration.setAttribute("value", "PixelInterleaved");
                break;
            case IFF.TYPE_ILBM:
                planarConfiguration.setAttribute("value", "PlaneInterleaved");
                break;
            default:
                planarConfiguration.setAttribute("value", "Unknown " + IFFUtil.toChunkStr(formType));
                break;
        }
        data.appendChild(planarConfiguration);

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", colorMap != null ? "Index" : "UnsignedIntegral");
        data.appendChild(sampleFormat);

        // BitsPerSample
        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        String value = bitsPerSampleValue(header.bitplanes);
        bitsPerSample.setAttribute("value", value);
        data.appendChild(bitsPerSample);

        // SignificantBitsPerSample not in format
        // SampleMSB not in format

        return data;

    }

    private String bitsPerSampleValue(int bitplanes) {
        switch (bitplanes) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return Integer.toString(bitplanes);
            case 24:
                return "8 8 8";
            case 32:
                return "8 8 8 8";
            default:
                throw new IllegalArgumentException("Ubknown bit count: " + bitplanes);
        }
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        if (viewPort == null) {
            return null;
        }

        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        // PixelAspectRatio
        IIOMetadataNode pixelAspectRatio = new IIOMetadataNode("PixelAspectRatio");
        pixelAspectRatio.setAttribute("value", String.valueOf((viewPort.isHires() ? 2f : 1f) / (viewPort.isLaced() ? 2f : 1f)));
        dimension.appendChild(pixelAspectRatio);

        // TODO: HorizontalScreenSize?
        // TODO: VerticalScreenSize?

        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value",  "1.0");

        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        if (meta.isEmpty()) {
            return null;
        }

        IIOMetadataNode text = new IIOMetadataNode("Text");

        // /Text/TextEntry@keyword = field name, /Text/TextEntry@value = field value.
        for (GenericChunk chunk : meta) {
            IIOMetadataNode node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", IFFUtil.toChunkStr(chunk.chunkId));
            node.setAttribute("value", new String(chunk.data, chunk.chunkId == IFF.CHUNK_UTF8 ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII));
            text.appendChild(node);
        }

        return text;

    }

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        if ((colorMap == null || !colorMap.hasAlpha()) && header.bitplanes != 32) {
            return null;
        }

        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

        if (header.bitplanes == 32) {
            IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
            alpha.setAttribute("value", "nonpremultiplied");
            transparency.appendChild(alpha);
        }

        if  (colorMap != null && colorMap.getTransparency() == Transparency.BITMASK) {
            IIOMetadataNode transparentIndex = new IIOMetadataNode("TransparentIndex");
            transparentIndex.setAttribute("value", Integer.toString(colorMap.getTransparentPixel()));
            transparency.appendChild(transparentIndex);
        }

        return transparency;
    }
}
