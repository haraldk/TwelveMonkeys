/*
 * Copyright (c) 2020, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;
import java.awt.image.*;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.twelvemonkeys.imageio.plugins.iff.IFF.*;
import static com.twelvemonkeys.imageio.plugins.iff.IFFUtil.toChunkStr;
import static com.twelvemonkeys.lang.Validate.notNull;
import static java.util.Collections.emptyList;

final class IFFImageMetadata extends StandardImageMetadataSupport {
    IFFImageMetadata(ImageTypeSpecifier type, Form header, IndexColorModel palette) {
        this(builder(type), notNull(header, "header"), palette);
    }

    private IFFImageMetadata(Builder builder, Form header, IndexColorModel palette) {
        super(builder.withPalette(palette)
                     .withCompressionName(compressionName(header))
                     .withBitsPerSample(bitsPerSample(header))
                     .withPlanarConfiguration(planarConfiguration(header))
                     .withPixelAspectRatio(header.aspect() != 0 ? header.aspect() : null)
                     .withFormatVersion("1.0")
                     .withTextEntries(textEntries(header)));
    }

    private static String compressionName(Form header) {
        switch (header.compressionType()) {
            case BMHDChunk.COMPRESSION_NONE:
                return "None";
            case BMHDChunk.COMPRESSION_BYTE_RUN:
                return "RLE";
            case 4:
                // Compression type 4 means different things for different FORM types, we support
                // Impulse RGB8 RLE compression: 24 bit RGB + 1 bit mask + 7 bit run count
                if (header.formType == TYPE_RGB8) {
                    return "RGB8";
                }
            default:
                return "Unknown";
        }
    }

    private static int[] bitsPerSample(Form header) {
        int bitplanes = header.bitplanes();

        switch (bitplanes) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return new int[] {bitplanes};
            case 24:
                return new int[] {8, 8, 8};
            case 25:
                if (header.formType != TYPE_RGB8) {
                    throw new IllegalArgumentException(String.format("25 bit depth only supported for FORM type RGB8: %s", IFFUtil.toChunkStr(header.formType)));
                }

                return new int[] {8, 8, 8, 1};
            case 32:
                return new int[] {8, 8, 8, 8};
            default:
                throw new IllegalArgumentException("Unknown bit count: " + bitplanes);
        }
    }

    private static PlanarConfiguration planarConfiguration(Form header) {
        switch (header.formType) {
            case TYPE_DEEP:
            case TYPE_TVPP:
            case TYPE_RGB8:
            case TYPE_PBM:
                return PlanarConfiguration.PixelInterleaved;
            case TYPE_ILBM:
                return PlanarConfiguration.PlaneInterleaved;
            default:
                return null;
        }
    }

    private static List<Map.Entry<String, String>> textEntries(Form header) {
        if (header.meta.isEmpty()) {
            return emptyList();
        }

        List<Map.Entry<String, String>> text = new ArrayList<>();
        for (GenericChunk chunk : header.meta) {
            text.add(new SimpleImmutableEntry<>(toChunkStr(chunk.chunkId),
                                                new String(chunk.data, chunk.chunkId == IFF.CHUNK_UTF8 ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII)));
        }

        return text;
    }
}
