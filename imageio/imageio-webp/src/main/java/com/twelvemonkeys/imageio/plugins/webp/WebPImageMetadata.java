/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * WebPMetadata
 */
final class WebPImageMetadata extends AbstractMetadata {
    private final VP8xChunk header;

    WebPImageMetadata(final VP8xChunk header) {
        this.header = notNull(header, "header");
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(csType);
        csType.setAttribute("name", "RGB");

        // NOTE: Channels in chroma node reflects channels in color model (see data node, for channels in data)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChannels);
        numChannels.setAttribute("value", Integer.toString(header.containsALPH ? 4 : 3));

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        chroma.appendChild(blackIsZero);
        blackIsZero.setAttribute("value", "TRUE");

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode node = new IIOMetadataNode("Compression");

        IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
        node.appendChild(compressionTypeName);

        String value = header.isLossless ? "VP8L" : "VP8"; // TODO: Naming: VP8L and VP8 or WebP and WebP Lossless?
        compressionTypeName.setAttribute("value", value);

        // TODO: VP8 + lossless alpha!
        IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
        node.appendChild(lossless);
        lossless.setAttribute("value", header.isLossless ? "TRUE" : "FALSE");

        return node;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        // TODO: WebP seems to support planar as well?
        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        node.appendChild(planarConfiguration);
        planarConfiguration.setAttribute("value", "PixelInterleaved");

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        node.appendChild(sampleFormat);
        sampleFormat.setAttribute("value", "UnsignedIntegral");

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        node.appendChild(bitsPerSample);

        bitsPerSample.setAttribute("value", createListValue(header.containsALPH ? 4 : 3, Integer.toString(8)));

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
        imageOrientation.setAttribute("value", "Normal");

        IIOMetadataNode pixelAspectRatio = new IIOMetadataNode("PixelAspectRatio");
        dimension.appendChild(pixelAspectRatio);
        pixelAspectRatio.setAttribute("value", "1.0");

        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value", "1.0");

        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");

        // TODO: Get useful text nodes from EXIF or XMP
        // NOTE: Names corresponds to equivalent fields in TIFF

        return text.hasChildNodes() ? text : null;
    }

//    private void appendTextEntry(final IIOMetadataNode parent, final String keyword, final String value) {
//        if (value != null) {
//            IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
//            parent.appendChild(textEntry);
//            textEntry.setAttribute("keyword", keyword);
//            textEntry.setAttribute("value", value);
//        }
//    }

    // No tiling

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        if (header.containsALPH) {
            IIOMetadataNode transparency = new IIOMetadataNode("Transparency");
            IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
            transparency.appendChild(alpha);
            alpha.setAttribute("value", "nonpremultiplied");
            return transparency;
        }

        return null;
    }

    // TODO: Define native WebP metadata format (probably use RIFF structure)
}
