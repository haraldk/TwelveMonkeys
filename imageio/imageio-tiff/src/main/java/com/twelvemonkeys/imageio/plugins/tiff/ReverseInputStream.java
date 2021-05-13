/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ReverseInputStream.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ReverseInputStream.java,v 1.0 19/12/2017 harald.kuhr Exp$
 */
final class ReverseInputStream extends FilterInputStream {
    // http://graphics.stanford.edu/~seander/bithacks.html
    static final byte[] BIT_REVERSE_TABLE = {
            0x00, (byte) 0x80, 0x40, (byte) 0xC0, 0x20, (byte) 0xA0, 0x60, (byte) 0xE0, 0x10, (byte) 0x90, 0x50, (byte) 0xD0, 0x30, (byte) 0xB0, 0x70, (byte) 0xF0,
            0x08, (byte) 0x88, 0x48, (byte) 0xC8, 0x28, (byte) 0xA8, 0x68, (byte) 0xE8, 0x18, (byte) 0x98, 0x58, (byte) 0xD8, 0x38, (byte) 0xB8, 0x78, (byte) 0xF8,
            0x04, (byte) 0x84, 0x44, (byte) 0xC4, 0x24, (byte) 0xA4, 0x64, (byte) 0xE4, 0x14, (byte) 0x94, 0x54, (byte) 0xD4, 0x34, (byte) 0xB4, 0x74, (byte) 0xF4,
            0x0C, (byte) 0x8C, 0x4C, (byte) 0xCC, 0x2C, (byte) 0xAC, 0x6C, (byte) 0xEC, 0x1C, (byte) 0x9C, 0x5C, (byte) 0xDC, 0x3C, (byte) 0xBC, 0x7C, (byte) 0xFC,
            0x02, (byte) 0x82, 0x42, (byte) 0xC2, 0x22, (byte) 0xA2, 0x62, (byte) 0xE2, 0x12, (byte) 0x92, 0x52, (byte) 0xD2, 0x32, (byte) 0xB2, 0x72, (byte) 0xF2,
            0x0A, (byte) 0x8A, 0x4A, (byte) 0xCA, 0x2A, (byte) 0xAA, 0x6A, (byte) 0xEA, 0x1A, (byte) 0x9A, 0x5A, (byte) 0xDA, 0x3A, (byte) 0xBA, 0x7A, (byte) 0xFA,
            0x06, (byte) 0x86, 0x46, (byte) 0xC6, 0x26, (byte) 0xA6, 0x66, (byte) 0xE6, 0x16, (byte) 0x96, 0x56, (byte) 0xD6, 0x36, (byte) 0xB6, 0x76, (byte) 0xF6,
            0x0E, (byte) 0x8E, 0x4E, (byte) 0xCE, 0x2E, (byte) 0xAE, 0x6E, (byte) 0xEE, 0x1E, (byte) 0x9E, 0x5E, (byte) 0xDE, 0x3E, (byte) 0xBE, 0x7E, (byte) 0xFE,
            0x01, (byte) 0x81, 0x41, (byte) 0xC1, 0x21, (byte) 0xA1, 0x61, (byte) 0xE1, 0x11, (byte) 0x91, 0x51, (byte) 0xD1, 0x31, (byte) 0xB1, 0x71, (byte) 0xF1,
            0x09, (byte) 0x89, 0x49, (byte) 0xC9, 0x29, (byte) 0xA9, 0x69, (byte) 0xE9, 0x19, (byte) 0x99, 0x59, (byte) 0xD9, 0x39, (byte) 0xB9, 0x79, (byte) 0xF9,
            0x05, (byte) 0x85, 0x45, (byte) 0xC5, 0x25, (byte) 0xA5, 0x65, (byte) 0xE5, 0x15, (byte) 0x95, 0x55, (byte) 0xD5, 0x35, (byte) 0xB5, 0x75, (byte) 0xF5,
            0x0D, (byte) 0x8D, 0x4D, (byte) 0xCD, 0x2D, (byte) 0xAD, 0x6D, (byte) 0xED, 0x1D, (byte) 0x9D, 0x5D, (byte) 0xDD, 0x3D, (byte) 0xBD, 0x7D, (byte) 0xFD,
            0x03, (byte) 0x83, 0x43, (byte) 0xC3, 0x23, (byte) 0xA3, 0x63, (byte) 0xE3, 0x13, (byte) 0x93, 0x53, (byte) 0xD3, 0x33, (byte) 0xB3, 0x73, (byte) 0xF3,
            0x0B, (byte) 0x8B, 0x4B, (byte) 0xCB, 0x2B, (byte) 0xAB, 0x6B, (byte) 0xEB, 0x1B, (byte) 0x9B, 0x5B, (byte) 0xDB, 0x3B, (byte) 0xBB, 0x7B, (byte) 0xFB,
            0x07, (byte) 0x87, 0x47, (byte) 0xC7, 0x27, (byte) 0xA7, 0x67, (byte) 0xE7, 0x17, (byte) 0x97, 0x57, (byte) 0xD7, 0x37, (byte) 0xB7, 0x77, (byte) 0xF7,
            0x0F, (byte) 0x8F, 0x4F, (byte) 0xCF, 0x2F, (byte) 0xAF, 0x6F, (byte) 0xEF, 0x1F, (byte) 0x9F, 0x5F, (byte) 0xDF, 0x3F, (byte) 0xBF, 0x7F, (byte) 0xFF
    };

    ReverseInputStream(final InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        return value < 0 ? value : BIT_REVERSE_TABLE[value] & 0xff;
    }

    @Override
    public int read(final byte[] bytes, final int off, final int len) throws IOException {
        int count = super.read(bytes, off, len);

        if (count > 0) {
            for (int i = 0; i < count; i++) {
                bytes[off + i] = BIT_REVERSE_TABLE[bytes[off + i] & 0xff];
            }
        }

        return count;
    }
}
