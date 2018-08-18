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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.lang.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class RLEDecoder implements Decoder {
    private final byte[] pixel;

    RLEDecoder(final int pixelDepth) {
        Validate.isTrue(pixelDepth % Byte.SIZE == 0, "Depth must be a multiple of bytes (8 bits)");
        pixel = new byte[pixelDepth / Byte.SIZE];
    }

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.remaining() >= 128 * pixel.length) {
            int val = stream.read();
            if (val < 0) {
                break; // EOF
            }

            int pixelCount = (val & 0x7f) + 1;

            if ((val & 0x80) == 0) {
                for (int i = 0; i < pixelCount * pixel.length; i++) {
                    int data = stream.read();
                    if (data < 0) {
                        break; // EOF
                    }

                    buffer.put((byte) data);
                }
            } else {
                for (int b = 0; b < pixel.length; b++) {
                    int data = stream.read();
                    if (data < 0) {
                        break; // EOF
                    }

                    pixel[b] = (byte) data;
                }

                for (int i = 0; i < pixelCount; i++) {
                    buffer.put(pixel);
                }
            }
        }

        return buffer.position();
    }
}
