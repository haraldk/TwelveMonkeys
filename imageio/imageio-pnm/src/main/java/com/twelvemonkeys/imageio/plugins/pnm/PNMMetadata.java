/*
 * Copyright (c) 2022, Harald Kuhr
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

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.*;
import java.nio.ByteOrder;

/**
 * PNMMetadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
final class PNMMetadata extends StandardImageMetadataSupport {
    private final PNMHeader header;

    PNMMetadata(ImageTypeSpecifier type, PNMHeader header) {
        super(builder(type)
                      .withColorSpaceType(colorSpace(header))
                      // TODO: Might make sense to set gamma?
                      .withBlackIsZero(header.getTupleType() != TupleType.BLACKANDWHITE_WHITE_IS_ZERO)
                      .withSignificantBitsPerSample(significantBits(header))
                      .withSampleMSB(header.getByteOrder() == ByteOrder.BIG_ENDIAN ? 0 : header.getBitsPerSample() - 1)
                      .withOrientation(orientation(header))
        );

        this.header = header;
    }

    private static int significantBits(PNMHeader header) {
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

    private static ColorSpaceType colorSpace(PNMHeader header) {
        switch (header.getTupleType()) {
            case BLACKANDWHITE:
            case BLACKANDWHITE_ALPHA:
            case BLACKANDWHITE_WHITE_IS_ZERO:
            case GRAYSCALE:
            case GRAYSCALE_ALPHA:
                return ColorSpaceType.GRAY;
            default:
                return null; // Fall back to color model's type
        }
    }

    private static ImageOrientation orientation(PNMHeader header) {
        // For some reason, the float values are stored bottom-up
        return header.getFileType() == PNM.PFM_GRAY || header.getFileType() == PNM.PFM_RGB
               ? ImageOrientation.FlipH
               : ImageOrientation.Normal;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        // TODO: Could avoid this override, by changing the StandardImageMetadataSupport to
        //  use List<Entry<String, String>> instead of Map<String, String> (we use duplicate "comment"s).
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
}
