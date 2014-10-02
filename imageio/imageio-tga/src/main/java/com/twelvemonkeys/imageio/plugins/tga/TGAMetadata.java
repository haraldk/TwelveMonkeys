/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tga;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.IndexColorModel;

final class TGAMetadata extends IIOMetadata {
    // TODO: Clean up & extend AbstractMetadata (after moving from PSD -> Core)

    private final TGAHeader header;

    TGAMetadata(final TGAHeader header) {
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

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        switch (header.getImageType()) {
            case TGA.IMAGETYPE_MONOCHROME:
            case TGA.IMAGETYPE_MONOCHROME_RLE:
                csType.setAttribute("name", "GRAY");
                break;

            case TGA.IMAGETYPE_TRUECOLOR:
            case TGA.IMAGETYPE_TRUECOLOR_RLE:
            case TGA.IMAGETYPE_COLORMAPPED:
            case TGA.IMAGETYPE_COLORMAPPED_RLE:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
                csType.setAttribute("name", "RGB");
                break;
            default:
                csType.setAttribute("name", "Unknown");
        }
        chroma.appendChild(csType);

        // TODO: Channels in chroma node reflects channels in color model (see data node, for channels in data)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        switch (header.getPixelDepth()) {
            case 8:
            case 16:
                numChannels.setAttribute("value", Integer.toString(1));
                break;
            case 24:
                numChannels.setAttribute("value", Integer.toString(3));
                break;
            case 32:
                numChannels.setAttribute("value", Integer.toString(4));
                break;
        }
        chroma.appendChild(numChannels);

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        blackIsZero.setAttribute("value", "TRUE");
        chroma.appendChild(blackIsZero);

        // NOTE: TGA files may contain a color map, even if true color...
        // Not sure if this is a good idea to expose to the meta data,
        // as it might be unexpected... Then again...
        IndexColorModel colorMap = header.getColorMap();
        if (colorMap != null) {
            IIOMetadataNode palette = new IIOMetadataNode("Palette");
            chroma.appendChild(palette);

            for (int i = 0; i < colorMap.getMapSize(); i++) {
                IIOMetadataNode paletteEntry = new IIOMetadataNode("PaletteEntry");
                paletteEntry.setAttribute("index", Integer.toString(i));

                paletteEntry.setAttribute("red", Integer.toString(colorMap.getRed(i)));
                paletteEntry.setAttribute("green", Integer.toString(colorMap.getGreen(i)));
                paletteEntry.setAttribute("blue", Integer.toString(colorMap.getBlue(i)));

                palette.appendChild(paletteEntry);
            }
        }

        return chroma;
    }

    @Override protected IIOMetadataNode getStandardCompressionNode() {
        switch (header.getImageType()) {
            case TGA.IMAGETYPE_COLORMAPPED_RLE:
            case TGA.IMAGETYPE_TRUECOLOR_RLE:
            case TGA.IMAGETYPE_MONOCHROME_RLE:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
                IIOMetadataNode node = new IIOMetadataNode("Compression");
                IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
                String value = header.getImageType() == TGA.IMAGETYPE_COLORMAPPED_HUFFMAN || header.getImageType() == TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE
                                ? "Uknown" : "RLE";
                compressionTypeName.setAttribute("value", value);
                node.appendChild(compressionTypeName);

                IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
                lossless.setAttribute("value", "TRUE");
                node.appendChild(lossless);

                return node;
            default:
                // No compreesion
                return null;
        }
    }

    @Override protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        planarConfiguration.setAttribute("value", "PixelInterleaved");
        node.appendChild(planarConfiguration);

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        switch (header.getImageType()) {
            case TGA.IMAGETYPE_COLORMAPPED:
            case TGA.IMAGETYPE_COLORMAPPED_RLE:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
                sampleFormat.setAttribute("value", "Index");
                break;
            default:
                sampleFormat.setAttribute("value", "UnsignedIntegral");
                break;
        }

        node.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        switch (header.getPixelDepth()) {
            case 8:
            case 16:
                bitsPerSample.setAttribute("value", createListValue(1, Integer.toString(header.getPixelDepth())));
                break;
            case 24:
                bitsPerSample.setAttribute("value", createListValue(3, Integer.toString(8)));
                break;
            case 32:
                bitsPerSample.setAttribute("value", createListValue(4, Integer.toString(8)));
                break;
        }

        node.appendChild(bitsPerSample);

        // TODO: Do we need MSB?
//        IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
//        sampleMSB.setAttribute("value", createListValue(header.getChannels(), "0"));

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

        switch (header.getOrigin()) {
            case TGA.ORIGIN_LOWER_LEFT:
                imageOrientation.setAttribute("value", "FlipH");
                break;
            case TGA.ORIGIN_LOWER_RIGHT:
                imageOrientation.setAttribute("value", "Rotate180");
                break;
            case TGA.ORIGIN_UPPER_LEFT:
                imageOrientation.setAttribute("value", "Normal");
                break;
            case TGA.ORIGIN_UPPER_RIGHT:
                imageOrientation.setAttribute("value", "FlipV");
                break;
        }

        dimension.appendChild(imageOrientation);

        return dimension;
    }

    // No document node

    @Override protected IIOMetadataNode getStandardTextNode() {
        // TODO: Extra "developer area" and other stuff might go here...
        if (header.getIdentification() != null && !header.getIdentification().isEmpty()) {
            IIOMetadataNode text = new IIOMetadataNode("Text");

            IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
            textEntry.setAttribute("keyword", "identification");
            textEntry.setAttribute("value", header.getIdentification());
            text.appendChild(textEntry);

            return text;
        }

        return null;
    }

    // No tiling

    @Override protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
        alpha.setAttribute("value", header.getPixelDepth() == 32 ? "nonpremultiplied" : "none");
        transparency.appendChild(alpha);

        return transparency;
    }
}
