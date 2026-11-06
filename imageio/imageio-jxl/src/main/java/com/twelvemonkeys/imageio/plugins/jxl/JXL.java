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

/**
 * JPEG XL format constants.
 *
 * @see <a href="https://www.iso.org/standard/85066.html">ISO/IEC 18181-1 (Codestream)</a>
 * @see <a href="https://www.iso.org/standard/85253.html">ISO/IEC 18181-2 (File format)</a>
 */
final class JXL {
    /** Signature of a "naked" JPEG XL codestream. */
    static final byte[] CODESTREAM_SIGNATURE = {(byte) 0xFF, 0x0A};

    /** Signature of the ISO BMFF based JPEG XL container ({@code JXL } signature box). */
    static final byte[] CONTAINER_SIGNATURE = {
            0x00, 0x00, 0x00, 0x0C, 'J', 'X', 'L', ' ', 0x0D, 0x0A, (byte) 0x87, 0x0A
    };

    // ISO BMFF box types used in the container
    static final int BOX_EXIF = ('E' << 24) | ('x' << 16) | ('i' << 8) | 'f';
    static final int BOX_XML = ('x' << 24) | ('m' << 16) | ('l' << 8) | ' ';
    static final int BOX_BROB = ('b' << 24) | ('r' << 16) | ('o' << 8) | 'b';

    static String boxTypeName(final int type) {
        char[] chars = new char[4];
        for (int i = 0; i < 4; i++) {
            int c = (type >>> (24 - i * 8)) & 0xFF;
            chars[i] = c >= 0x20 && c < 0x7F ? (char) c : '?';
        }

        return new String(chars);
    }

    /** Fully qualified name of the JXLatte decoder class, used to test for availability. */
    static final String JXLATTE_DECODER_CLASS = "com.traneptora.jxlatte.JXLDecoder";

    private JXL() {}
}
