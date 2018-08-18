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

package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.nio.ByteOrder;

final class PNMMetadata extends AbstractMetadata {
    private final PNMHeader header;

    PNMMetadata(final PNMHeader header) {
        this.header = header;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        switch (header.getTupleType()) {
            case BLACKANDWHITE:
            case BLACKANDWHITE_ALPHA:
            case BLACKANDWHITE_WHITE_IS_ZERO:
            case GRAYSCALE:
            case GRAYSCALE_ALPHA:
                csType.setAttribute("name", "GRAY");
                break;
            case RGB:
            case RGB_ALPHA:
                csType.setAttribute("name", "RGB");
                break;
            case CMYK:
            case CMYK_ALPHA:
                csType.setAttribute("name", "CMYK");
                break;
        }

        if (csType.getAttribute("name") != null) {
            chroma.appendChild(csType);
        }

        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        numChannels.setAttribute("value", Integer.toString(header.getSamplesPerPixel()));
        chroma.appendChild(numChannels);

        // TODO: Might make sense to set gamma?

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        blackIsZero.setAttribute("value", header.getTupleType() == TupleType.BLACKANDWHITE_WHITE_IS_ZERO
                                          ? "FALSE"
                                          : "TRUE");
        chroma.appendChild(blackIsZero);

        return chroma;
    }

    // No compression

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", header.getTransferType() == DataBuffer.TYPE_FLOAT
                                           ? "Real"
                                           : "UnsignedIntegral");
        node.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        bitsPerSample.setAttribute("value", createListValue(header.getSamplesPerPixel(), Integer.toString(header.getBitsPerSample())));
        node.appendChild(bitsPerSample);

        IIOMetadataNode significantBitsPerSample = new IIOMetadataNode("SignificantBitsPerSample");
        significantBitsPerSample.setAttribute("value", createListValue(header.getSamplesPerPixel(), Integer.toString(computeSignificantBits())));
        node.appendChild(significantBitsPerSample);

        String msb = header.getByteOrder() == ByteOrder.BIG_ENDIAN
                     ? "0"
                     : Integer.toString(header.getBitsPerSample() - 1);
        IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
        sampleMSB.setAttribute("value", createListValue(header.getSamplesPerPixel(), msb));

        return node;
    }

    private int computeSignificantBits() {
        if (header.getTransferType() == DataBuffer.TYPE_FLOAT) {
            return header.getBitsPerSample();
        }

        int significantBits = 0;

        int maxSample = header.getMaxSample();

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

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
        imageOrientation.setAttribute("value", "Normal");
        dimension.appendChild(imageOrientation);

        return dimension;
    }

    // No document node

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        if (!header.getComments().isEmpty()) {
            IIOMetadataNode text = new IIOMetadataNode("Text");

            for (String comment : header.getComments()) {
                IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
                textEntry.setAttribute("keyword", "comment");
                textEntry.setAttribute("value", comment);
                text.appendChild(textEntry);
            }

            return text;
        }

        return null;
    }

    // No tiling

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
        alpha.setAttribute("value", header.getTransparency() == Transparency.OPAQUE ? "none" : "nonpremultiplied");
        transparency.appendChild(alpha);

        return transparency;
    }
}
