/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io.enc;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * {@code Encoder} implementation for standard base64 encoding.
 * <p/>
 * @see <a href="http://tools.ietf.org/html/rfc1421">RFC 1421</a>
 * @see <a href="http://tools.ietf.org/html/rfc2045"RFC 2045</a>
 *
 * @see Base64Decoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/Base64Encoder.java#2 $
 */
public class Base64Encoder implements Encoder {

    public void encode(final OutputStream stream, final ByteBuffer buffer)
            throws IOException
    {

        // TODO: Implement
        // NOTE: This is impossible, given the current spec, as we need to either:
        //  - buffer all data in the EncoderStream
        //  - or have flush/end method(s) in the Encoder
        // to ensure proper end of stream handling

        int length;

        // TODO: Temp impl, will only work for single writes
        while (buffer.hasRemaining()) {
            byte a, b, c;

//            if ((buffer.remaining()) > 2) {
//                length = 3;
//            }
//            else {
//                length = buffer.remaining();
//            }
            length = Math.min(3, buffer.remaining());

            switch (length) {
                case 1:
                    a = buffer.get();
                    b = 0;
                    stream.write(Base64Decoder.PEM_ARRAY[(a >>> 2) & 0x3F]);
                    stream.write(Base64Decoder.PEM_ARRAY[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
                    stream.write('=');
                    stream.write('=');
                    break;

                case 2:
                    a = buffer.get();
                    b = buffer.get();
                    c = 0;
                    stream.write(Base64Decoder.PEM_ARRAY[(a >>> 2) & 0x3F]);
                    stream.write(Base64Decoder.PEM_ARRAY[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
                    stream.write(Base64Decoder.PEM_ARRAY[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
                    stream.write('=');
                    break;

                default:
                    a = buffer.get();
                    b = buffer.get();
                    c = buffer.get();
                    stream.write(Base64Decoder.PEM_ARRAY[(a >>> 2) & 0x3F]);
                    stream.write(Base64Decoder.PEM_ARRAY[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
                    stream.write(Base64Decoder.PEM_ARRAY[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
                    stream.write(Base64Decoder.PEM_ARRAY[c & 0x3F]);
                    break;
            }
        }
    }
}
