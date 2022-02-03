/*
 * Copyright (c) 2022, Harald Kuhr
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

import com.twelvemonkeys.io.enc.DecodeException;
import com.twelvemonkeys.io.enc.Decoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Decoder implementation for Impulse FORM RGB8 RLE compression (type 4).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RGB8RLEDecoder.java,v 1.0 28/01/2022 haraldk Exp$
 *
 * @see <a href="https://wiki.amigaos.net/wiki/RGBN_and_RGB8_IFF_Image_Data">RGBN and RGB8 IFF Image Data</a>
 */
final class RGB8RLEDecoder implements Decoder {
    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.remaining() >= 127 * 4) {
            int r = stream.read();
            int g = stream.read();
            int b = stream.read();
            int a = stream.read();

            if (a < 0) {
                // Normal EOF
                if (r == -1) {
                    break;
                }

                // Partial pixel read...
                throw new EOFException();
            }

            // Get "genlock" (transparency) bit + count
            boolean alpha = (a & 0x80) != 0;
            int count = a & 0x7f;
            a = alpha ? 0 : (byte) 0xff; // convert to full transparent/opaque;

            if (count == 0) {
                throw new DecodeException("Multi-byte counts not supported");
            }

            for (int i = 0; i < count; i++) {
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
        }

        return buffer.position();
    }
}
