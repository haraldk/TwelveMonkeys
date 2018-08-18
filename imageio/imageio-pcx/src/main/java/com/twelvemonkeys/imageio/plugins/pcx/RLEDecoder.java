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

package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.io.enc.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class RLEDecoder implements Decoder {

    static final int COMPRESSED_RUN_MASK = 0xc0;

    // A rather strange and inefficient RLE encoding, but it probably made sense at the time...
    // Uses the upper two bits to flag if the next values are to be treated as a compressed run.
    // This means that any value above 0b11000000/0xc0/192 must be encoded as a compressed run,
    // even if this will make the output larger.
    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.remaining() >= 64) {
            int val = stream.read();
            if (val < 0) {
                break; // EOF
            }

            if ((val & COMPRESSED_RUN_MASK) == COMPRESSED_RUN_MASK) {
                int count = val & ~COMPRESSED_RUN_MASK;

                int pixel = stream.read();
                if (pixel < 0) {
                    break; // EOF
                }

                for (int i = 0; i < count; i++) {
                    buffer.put((byte) pixel);
                }
            }
            else {
                buffer.put((byte) val);
            }
        }

        return buffer.position();
    }
}
