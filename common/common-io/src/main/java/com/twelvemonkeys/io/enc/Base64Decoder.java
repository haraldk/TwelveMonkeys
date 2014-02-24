/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io.enc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * {@code Decoder} implementation for standard base64 encoding.
 * <p/>
 * @see <a href="http://tools.ietf.org/html/rfc1421">RFC 1421</a>
 * @see <a href="http://tools.ietf.org/html/rfc2045"RFC 2045</a>
 *
 * @see Base64Encoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/Base64Decoder.java#2 $
 */
public final class Base64Decoder implements Decoder {
    /**
     * This array maps the characters to their 6 bit values
     */
    final static byte[] PEM_ARRAY = {
            //0   1    2    3    4    5    6    7
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', // 0
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', // 1
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', // 2
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', // 3
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', // 4
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', // 5
            'w', 'x', 'y', 'z', '0', '1', '2', '3', // 6
            '4', '5', '6', '7', '8', '9', '+', '/'  // 7
    };

    final static byte[] PEM_CONVERT_ARRAY;

    private byte[] decodeBuffer = new byte[4];

    static {
        PEM_CONVERT_ARRAY = new byte[256];

        for (int i = 0; i < 255; i++) {
            PEM_CONVERT_ARRAY[i] = -1;
        }

        for (int i = 0; i < PEM_ARRAY.length; i++) {
            PEM_CONVERT_ARRAY[PEM_ARRAY[i]] = (byte) i;
        }
    }

    protected static int readFully(final InputStream pStream, final byte pBytes[], final int pOffset, final int pLength)
            throws IOException
    {
        for (int i = 0; i < pLength; i++) {
            int read = pStream.read();

            if (read == -1) {
                return i != 0 ? i : -1;
            }

            pBytes[i + pOffset] = (byte) read;
        }

        return pLength;
    }

    protected boolean decodeAtom(final InputStream pInput, final ByteBuffer pOutput, final int pLength)
            throws IOException {

        byte byte0 = -1;
        byte byte1 = -1;
        byte byte2 = -1;
        byte byte3 = -1;

        if (pLength < 2) {
            throw new IOException("BASE64Decoder: Not enough bytes for an atom.");
        }

        int read;

        // Skip line feeds
        do {
            read = pInput.read();

            if (read == -1) {
                return false;
            }
        } while (read == 10 || read == 13);

        decodeBuffer[0] = (byte) read;
        read = readFully(pInput, decodeBuffer, 1, pLength - 1);

        if (read == -1) {
            return false;
        }

        int length = pLength;

        if (length > 3 && decodeBuffer[3] == 61) {
            length = 3;
        }

        if (length > 2 && decodeBuffer[2] == 61) {
            length = 2;
        }

        switch (length) {
            case 4:
                byte3 = PEM_CONVERT_ARRAY[decodeBuffer[3] & 255];
                // fall through
            case 3:
                byte2 = PEM_CONVERT_ARRAY[decodeBuffer[2] & 255];
                // fall through
            case 2:
                byte1 = PEM_CONVERT_ARRAY[decodeBuffer[1] & 255];
                byte0 = PEM_CONVERT_ARRAY[decodeBuffer[0] & 255];
                // fall through
            default:
                switch (length) {
                    case 2:
                        pOutput.put((byte) (byte0 << 2 & 252 | byte1 >>> 4 & 3));
                        break;
                    case 3:
                        pOutput.put((byte) (byte0 << 2 & 252 | byte1 >>> 4 & 3));
                        pOutput.put((byte) (byte1 << 4 & 240 | byte2 >>> 2 & 15));
                        break;
                    case 4:
                        pOutput.put((byte) (byte0 << 2 & 252 | byte1 >>> 4 & 3));
                        pOutput.put((byte) (byte1 << 4 & 240 | byte2 >>> 2 & 15));
                        pOutput.put((byte) (byte2 << 6 & 192 | byte3 & 63));
                        break;
                }

                break;
        }

        return true;
    }

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        do {
            int k = 72;
            int i;

            for (i = 0; i + 4 < k; i += 4) {
                if(!decodeAtom(stream, buffer, 4)) {
                    break;
                }
            }

            if (!decodeAtom(stream, buffer, k - i)) {
                break;
            }
        }
        while (buffer.remaining() > 54); // 72 char lines should produce no more than 54 bytes

        return buffer.position();
    }
}
