/*
 * Copyright (c) 2017, Brooss, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.webp.vp8;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

final class BoolDecoder {
    private int bit_count; /* # of bits shifted out of value, at most 7 */
    ImageInputStream data;
    private long offset; /* pointer to next compressed data byte */
    private int range; /* always identical to encoder's range */
    private int value; /* contains at least 24 significant bits */

    BoolDecoder(ImageInputStream frame, long offset) throws IOException {
        this.data = frame;
        this.offset = offset;
        initBoolDecoder();
    }

    private void initBoolDecoder() throws IOException {
        value = 0; /* value = first 16 input bits */

        data.seek(offset);
        value = data.readUnsignedByte() << 8;
        // value = (data[offset]) << 8;
        offset++;

        range = 255; /* initial range is full */
        bit_count = 0; /* have not yet shifted out any bits */
    }

    public int readBit() throws IOException {
        return readBool(128);
    }

    public int readBool(int probability) throws IOException {
        int bit = 0;
        int split;
        int bigsplit;
        int range = this.range;
        int value = this.value;
        split = 1 + (((range - 1) * probability) >> 8);
        bigsplit = (split << 8);
        range = split;

        if (value >= bigsplit) {
            range = this.range - split;
            value = value - bigsplit;
            bit = 1;
        }

        {
            int count = this.bit_count;
            int shift = Globals.vp8dxBitreaderNorm[range];
            range <<= shift;
            value <<= shift;
            count -= shift;

            if (count <= 0) {
                // data.seek(offset);
                value |= data.readUnsignedByte() << (-count);
                // value |= data[offset] << (-count);
                offset++;
                count += 8;
            }

            this.bit_count = count;
        }
        this.value = value;
        this.range = range;
        return bit;
    }

    /*
     * Convenience function reads a "literal", that is, a "num_bits" wide
     * unsigned value whose bits come high- to low-order, with each bit encoded
     * at probability 128 (i.e., 1/2).
     */
    public int readLiteral(int num_bits) throws IOException {
        int v = 0;
        while (num_bits-- > 0) {
            v = (v << 1) + readBool(128);
        }
        return v;
    }

    //    int readTree(int t[], /* tree specification */ int p[] /* corresponding interior node probabilities */) throws IOException {
//        int i = 0; /* begin at root */
//
//		/* Descend tree until leaf is reached */
//        while ((i = t[i + readBool(p[i >> 1])]) > 0) {
//        }
//        return -i; /* return value is negation of nonpositive index */
//
//    }
//
//    int readTree(int t[], /* tree specification */ int p[], /* corresponding interior node probabilities */ int skip_branches) throws IOException {
    int readTree(int[] t, /* tree specification */ int[] p, /* corresponding interior node probabilities */ int skip_branches) throws IOException {
        int i = skip_branches * 2; /* begin at root */

        /* Descend tree until leaf is reached */
        while ((i = t[i + readBool(p[i >> 1])]) > 0) {
        }
        return -i; /* return value is negation of nonpositive index */
    }

    public void seek() throws IOException {
        data.seek(offset);
    }

    public String toString() {
        return "bc: " + value;
    }
}
