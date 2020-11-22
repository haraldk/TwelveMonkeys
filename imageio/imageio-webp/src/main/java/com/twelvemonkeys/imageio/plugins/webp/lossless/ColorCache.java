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

package com.twelvemonkeys.imageio.plugins.webp.lossless;

import com.twelvemonkeys.lang.Validate;

/**
 * ColorCache.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
final class ColorCache {
    private final int[] colors;   // Color entries
    private final int hashShift;  // Hash shift: 32 - hashBits.

    private static final long K_HASH_MUL = 0x1e35a7bdL;

    private static int hashPix(final int argb, final int shift) {
        return (int) (((argb * K_HASH_MUL) & 0xffffffffL) >> shift);
    }

    ColorCache(final int hashBits) {
        Validate.isTrue(hashBits > 0, "hasBits must > 0");

        int hashSize = 1 << hashBits;

        colors = new int[hashSize];
        hashShift = 32 - hashBits;
    }

    int lookup(final int key) {
        return colors[key];
    }

    void set(final int key, final int argb) {
        colors[key] = argb;
    }

    void insert(final int argb) {
        colors[index(argb)] = argb;
    }

    int index(final int argb) {
        return hashPix(argb, hashShift);
    }

    // TODO: Use boolean?
    int contains(final int argb) {
        int key = index(argb);
        return colors[key] == argb ? key : -1;
    }
}
