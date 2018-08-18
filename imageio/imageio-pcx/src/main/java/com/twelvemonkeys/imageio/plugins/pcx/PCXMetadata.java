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

package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.IndexColorModel;

final class PCXMetadata extends AbstractMetadata {
    private final PCXHeader header;
    private final IndexColorModel vgaPalette;

    PCXMetadata(final PCXHeader header, final IndexColorModel vgaPalette) {
        this.header = header;
        this.vgaPalette = vgaPalette;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IndexColorModel palette = null;
        boolean gray = false;

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
                if (header.getChannels() == 1 && vgaPalette != null) {
                    palette = vgaPalette;
                    csType.setAttribute("name", "RGB");
                    break;
                }
                if (header.getChannels() == 1) {
                    csType.setAttribute("name", "GRAY");
                    gray = true;
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

        // NOTE: Channels in chroma node reflects channels in color model, not data! (see data node)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        numChannels.setAttribute("value", gray ? "1" : "3");
        chroma.appendChild(numChannels);

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        blackIsZero.setAttribute("value", "TRUE");
        chroma.appendChild(blackIsZero);

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

        return chroma;
    }

    // No compression

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
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

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        // Planar configuration only makes sense for multi-channel images
        if (header.getChannels() > 1) {
            IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
            planarConfiguration.setAttribute("value", "LineInterleaved");
            node.appendChild(planarConfiguration);
        }

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");

        switch (header.getBitsPerPixel()) {
            case 1:
            case 2:
            case 4:
                sampleFormat.setAttribute("value", "Index");
                break;
            case 8:
                if (header.getChannels() == 1 && vgaPalette != null) {
                    sampleFormat.setAttribute("value", "Index");
                    break;
                }
                // Else fall through for GRAY
            default:
                sampleFormat.setAttribute("value", "UnsignedIntegral");
                break;
        }

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

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
        imageOrientation.setAttribute("value", "Normal");
        dimension.appendChild(imageOrientation);

        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Document");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("FormatVersion");
        imageOrientation.setAttribute("value", String.valueOf(header.getVersion()));
        dimension.appendChild(imageOrientation);

        return dimension;
    }

    // No text node

    // No tiling

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        // NOTE: There doesn't seem to be any god way to determine transparency, other than by convention
        // 1 channel: Gray, 2 channel: Gray + Alpha, 3 channel: RGB, 4 channel: RGBA (hopefully never CMYK...)

        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
        alpha.setAttribute("value", header.getChannels() == 1 || header.getChannels() == 3 ? "none" : "nonpremultiplied");
        transparency.appendChild(alpha);

        return transparency;
    }
}
