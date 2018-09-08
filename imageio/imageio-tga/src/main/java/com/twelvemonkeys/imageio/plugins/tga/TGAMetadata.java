/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.IndexColorModel;
import java.util.Calendar;

import static com.twelvemonkeys.lang.Validate.notNull;

final class TGAMetadata extends AbstractMetadata {
    private final TGAHeader header;
    private final TGAExtensions extensions;

    TGAMetadata(final TGAHeader header, final TGAExtensions extensions) {
        this.header = notNull(header, "header");
        this.extensions = extensions;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(csType);

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

        // NOTE: Channels in chroma node reflects channels in color model (see data node, for channels in data)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChannels);
        switch (header.getPixelDepth()) {
            case 8:
                numChannels.setAttribute("value", Integer.toString(1));
                break;
            case 16:
                if (header.getAttributeBits() > 0 && extensions != null && extensions.hasAlpha()) {
                    numChannels.setAttribute("value", Integer.toString(4));
                }
                else {
                    numChannels.setAttribute("value", Integer.toString(3));
                }
                break;
            case 24:
                numChannels.setAttribute("value", Integer.toString(3));
                break;
            case 32:
                numChannels.setAttribute("value", Integer.toString(4));
                break;
        }

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        chroma.appendChild(blackIsZero);
        blackIsZero.setAttribute("value", "TRUE");

        // NOTE: TGA files may contain a color map, even if true color...
        // Not sure if this is a good idea to expose to the meta data,
        // as it might be unexpected... Then again...
        IndexColorModel colorMap = header.getColorMap();
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

        if (extensions != null && extensions.getBackgroundColor() != 0) {
            Color background = new Color(extensions.getBackgroundColor(), true);

            IIOMetadataNode backgroundColor = new IIOMetadataNode("BackgroundColor");
            chroma.appendChild(backgroundColor);

            backgroundColor.setAttribute("red", Integer.toString(background.getRed()));
            backgroundColor.setAttribute("green", Integer.toString(background.getGreen()));
            backgroundColor.setAttribute("blue", Integer.toString(background.getBlue()));
        }

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        switch (header.getImageType()) {
            case TGA.IMAGETYPE_COLORMAPPED_RLE:
            case TGA.IMAGETYPE_TRUECOLOR_RLE:
            case TGA.IMAGETYPE_MONOCHROME_RLE:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
                IIOMetadataNode node = new IIOMetadataNode("Compression");

                IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
                node.appendChild(compressionTypeName);
                String value = header.getImageType() == TGA.IMAGETYPE_COLORMAPPED_HUFFMAN || header.getImageType() == TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE
                               ? "Uknown" : "RLE";
                compressionTypeName.setAttribute("value", value);

                IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
                node.appendChild(lossless);
                lossless.setAttribute("value", "TRUE");

                return node;
            default:
                // No compreesion
                return null;
        }
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        node.appendChild(planarConfiguration);
        planarConfiguration.setAttribute("value", "PixelInterleaved");

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        node.appendChild(sampleFormat);

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

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        node.appendChild(bitsPerSample);

        switch (header.getPixelDepth()) {
            case 8:
                bitsPerSample.setAttribute("value", createListValue(1, Integer.toString(header.getPixelDepth())));
            case 16:
                if (header.getAttributeBits() > 0 && extensions != null && extensions.hasAlpha()) {
                    bitsPerSample.setAttribute("value", "5, 5, 5, 1");
                }
                else {
                    bitsPerSample.setAttribute("value", createListValue(3, "5"));
                }
                break;
            case 24:
                bitsPerSample.setAttribute("value", createListValue(3, Integer.toString(8)));
                break;
            case 32:
                bitsPerSample.setAttribute("value", createListValue(4, Integer.toString(8)));
                break;
        }

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

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
        dimension.appendChild(imageOrientation);

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

        IIOMetadataNode pixelAspectRatio = new IIOMetadataNode("PixelAspectRatio");
        dimension.appendChild(pixelAspectRatio);
        pixelAspectRatio.setAttribute("value", extensions != null ? String.valueOf(extensions.getPixelAspectRatio()) : "1.0");

        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value", extensions == null ? "1.0" : "2.0");

        // ImageCreationTime from extensions date
        if (extensions != null && extensions.getCreationDate() != null) {
            IIOMetadataNode imageCreationTime = new IIOMetadataNode("ImageCreationTime");
            document.appendChild(imageCreationTime);

            Calendar date = extensions.getCreationDate();

            imageCreationTime.setAttribute("year", String.valueOf(date.get(Calendar.YEAR)));
            imageCreationTime.setAttribute("month", String.valueOf(date.get(Calendar.MONTH) + 1));
            imageCreationTime.setAttribute("day", String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
            imageCreationTime.setAttribute("hour", String.valueOf(date.get(Calendar.HOUR_OF_DAY)));
            imageCreationTime.setAttribute("minute", String.valueOf(date.get(Calendar.MINUTE)));
            imageCreationTime.setAttribute("second", String.valueOf(date.get(Calendar.SECOND)));
        }

        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");

        // NOTE: Names corresponds to equivalent fields in TIFF
        if (header.getIdentification() != null && !header.getIdentification().isEmpty()) {
            appendTextEntry(text, "DocumentName", header.getIdentification());
        }

        if (extensions != null) {
            appendTextEntry(text, "Software", extensions.getSoftwareVersion() == null ? extensions.getSoftware() : extensions.getSoftware() + " " + extensions.getSoftwareVersion());
            appendTextEntry(text, "Artist", extensions.getAuthorName());
            appendTextEntry(text, "UserComment", extensions.getAuthorComments());
        }

        return text.hasChildNodes() ? text : null;
    }

    private void appendTextEntry(final IIOMetadataNode parent, final String keyword, final String value) {
        if (value != null) {
            IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
            parent.appendChild(textEntry);
            textEntry.setAttribute("keyword", keyword);
            textEntry.setAttribute("value", value);
        }
    }

    // No tiling

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
        transparency.appendChild(alpha);

        if (extensions != null) {
            if (extensions.hasAlpha()) {
                alpha.setAttribute("value", extensions.isAlphaPremultiplied() ? "premultiplied" : "nonpremultiplied");
            }
            else {
                alpha.setAttribute("value", "none");
            }
        }
        else if (header.getAttributeBits() == 8) {
            alpha.setAttribute("value", "nonpremultiplied");
        }
        else {
            alpha.setAttribute("value", "none");
        }

        return transparency;
    }
}
