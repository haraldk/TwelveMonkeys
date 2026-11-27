/*
 * Copyright (c) 2026, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jxl;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;

import com.traneptora.jxlatte.bundle.BitDepthHeader;
import com.traneptora.jxlatte.bundle.ImageHeader;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.Node;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * JPEG XL image metadata, in the standard metadata format only.
 * <p>
 * Text entries and image creation time come from the Exif box (if any), mapped like the TIFF plugin does,
 * XMP (if any) is added as a text entry with the keyword {@code XML:com.adobe.xmp}, like the JDK PNG reader does.
 * </p>
 */
final class JXLImageMetadata extends StandardImageMetadataSupport {

    static final String XMP_KEYWORD = "XML:com.adobe.xmp";

    // Same as the standard metadata of the TIFF plugin
    private static final int[] EXIF_TEXT_TAGS = {
            TIFF.TAG_DOCUMENT_NAME, TIFF.TAG_IMAGE_DESCRIPTION, TIFF.TAG_MAKE, TIFF.TAG_MODEL, TIFF.TAG_PAGE_NAME,
            TIFF.TAG_SOFTWARE, TIFF.TAG_ARTIST, TIFF.TAG_HOST_COMPUTER, TIFF.TAG_COPYRIGHT
    };

    private final boolean lossy;

    JXLImageMetadata(final ImageTypeSpecifier type, final ImageHeader header, final Directory exif, final String xmp) {
        super(withSignificantBits(builder(type), type, header)
                      .withCompressionTypeName("JPEG XL")
                      .withCompressionLossless(!header.isXYBEncoded())
                      .withPixelAspectRatio(1.0)
                      .withDocumentCreationTime(creationTime(exif))
                      .withTextEntries(textEntries(exif, xmp)));

        // XYB is always lossy, otherwise we can't tell without parsing the frame headers (lossy modular, VarDCT)
        lossy = header.isXYBEncoded();
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression = super.getStandardCompressionNode();

        if (compression != null && !lossy) {
            // Unknown, leave out the Lossless node (its default, "TRUE", could be wrong)
            Node lossless = compression.getElementsByTagName("Lossless").item(0);

            if (lossless != null) {
                compression.removeChild(lossless);
            }
        }

        return compression;
    }

    private static Builder withSignificantBits(final Builder builder, final ImageTypeSpecifier type, final ImageHeader header) {
        // Integer samples with less bits than the decoded samples (ie. 12 bits, decoded to 16)
        int bits = type.getSampleModel().getSampleSize(0);
        int colorBits = significantBits(header.getBitDepthHeader(), bits);
        int alphaBits = header.hasAlpha()
                        ? significantBits(header.getExtraChannelInfo(header.getAlphaIndex(0)).bitDepth, bits)
                        : colorBits;

        if (colorBits == bits && alphaBits == bits) {
            return builder;
        }

        int[] significantBits = new int[type.getNumBands()];
        for (int b = 0; b < significantBits.length; b++) {
            significantBits[b] = header.hasAlpha() && b == significantBits.length - 1 ? alphaBits : colorBits;
        }

        return builder.withSignificantBitsPerSample(significantBits);
    }

    private static int significantBits(final BitDepthHeader bitDepth, final int bits) {
        return bitDepth.usesFloatSamples ? bits : Math.min(bitDepth.bitsPerSample, bits);
    }

    private static Calendar creationTime(final Directory exif) {
        Entry dateTime = exif != null ? exif.getEntryById(TIFF.TAG_DATE_TIME) : null;

        if (dateTime != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            format.setLenient(false);

            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(format.parse(dateTime.getValueAsString()));

                return calendar;
            }
            catch (ParseException ignore) {
                // Bad format, ignore
            }
        }

        return null;
    }

    private static List<TextEntry> textEntries(final Directory exif, final String xmp) {
        List<TextEntry> entries = new ArrayList<>();

        if (exif != null) {
            for (int tag : EXIF_TEXT_TAGS) {
                Entry entry = exif.getEntryById(tag);

                if (entry != null) {
                    entries.add(new TextEntry(entry.getFieldName(), entry.getValueAsString()));
                }
            }
        }

        if (xmp != null) {
            entries.add(new TextEntry(XMP_KEYWORD, xmp));
        }

        return entries;
    }
}
