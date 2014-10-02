/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.io.enc.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class RLEDecoder implements Decoder {

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        // Adapted from c code sample in tgaffs.pdf
        while (buffer.remaining() >= 0x7f) {
            int val = stream.read();
            if (val < 0) {
                break; // EOF
            }

            int count = val & 0x7f;

            if (count == 0) {
                break; // No more data
            }

            if ((val & 0x80) != 0) {
                for (int i = 0; i < count; i++) {
                    int pixel = stream.read();
                    if (pixel < 0) {
                        break; // EOF
                    }

                    buffer.put((byte) pixel);
                }
            }
            else {
                int pixel = stream.read();
                if (pixel < 0) {
                    break; // EOF
                }

                for (int i = 0; i < count; i++) {
                    buffer.put((byte) pixel);
                }
            }
        }

        return buffer.position();
    }
}
