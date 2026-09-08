/*
 * Copyright (c) 2024, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;

final class DDSImageMetadata extends StandardImageMetadataSupport {

    DDSImageMetadata(ImageTypeSpecifier specifier, DDSType type) {
        super(builder(specifier)
            .withCompressionTypeName(compressionName(type))
            .withCompressionLossless(!type.isBlockCompression())
            .withBitsPerSample(bitsPerSample(type))
            .withFormatVersion("1.0")
        );
    }

    private static String compressionName(DDSType type) {
        if (type != null && type.isFourCC()) {
            return type.name();
        }

        return "None";
    }

    private static int[] bitsPerSample(DDSType type) {
        if (type.isBlockCompression()) {
            return null; // Use defaults
        }

        int[] bitsPerSample = new int[4];

        for (int i = 0; i < bitsPerSample.length; i++) {
            bitsPerSample[i] = countMaskBits(type.rgbaMasks[i]);
        }

        return bitsPerSample;
    }

    private static int countMaskBits(int mask) {
        // See https://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetKernighan
        int count;

        for (count = 0; mask != 0; count++) {
            mask &= mask - 1; // clear the least significant bit set
        }

        return count;
    }
}
