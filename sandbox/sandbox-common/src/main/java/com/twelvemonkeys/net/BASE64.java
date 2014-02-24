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

package com.twelvemonkeys.net;

import com.twelvemonkeys.io.*;
import com.twelvemonkeys.io.enc.Base64Decoder;
import com.twelvemonkeys.io.enc.DecoderStream;

import java.io.*;


/**
 * This class does BASE64 encoding (and decoding).
 *
 * @author unascribed
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/BASE64.java#1 $
 * @deprecated Use {@link com.twelvemonkeys.io.enc.Base64Encoder}/{@link Base64Decoder} instead
 */
class BASE64 {
    /**
     * This array maps the characters to their 6 bit values
     */
    private final static char[] PEM_ARRAY = {
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

    /**
     * Encodes the input data using the standard base64 encoding scheme.
     *
     * @param pData the bytes to encode to base64
     * @return a string with base64 encoded data
     */
    public static String encode(byte[] pData) {
        int offset = 0;
        int len;
        StringBuilder buf = new StringBuilder();

        while ((pData.length - offset) > 0) {
            byte a, b, c;
            if ((pData.length - offset) > 2) {
                len = 3;
            }
            else {
                len = pData.length - offset;
            }

            switch (len) {
                case 1:
                    a = pData[offset];
                    b = 0;
                    buf.append(PEM_ARRAY[(a >>> 2) & 0x3F]);
                    buf.append(PEM_ARRAY[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
                    buf.append('=');
                    buf.append('=');
                    offset++;
                    break;
                case 2:
                    a = pData[offset];
                    b = pData[offset + 1];
                    c = 0;
                    buf.append(PEM_ARRAY[(a >>> 2) & 0x3F]);
                    buf.append(PEM_ARRAY[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
                    buf.append(PEM_ARRAY[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
                    buf.append('=');
                    offset += offset + 2; // ???
                    break;
                default:
                    a = pData[offset];
                    b = pData[offset + 1];
                    c = pData[offset + 2];
                    buf.append(PEM_ARRAY[(a >>> 2) & 0x3F]);
                    buf.append(PEM_ARRAY[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
                    buf.append(PEM_ARRAY[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
                    buf.append(PEM_ARRAY[c & 0x3F]);
                    offset = offset + 3;
                    break;
            }

        }
        return buf.toString();
    }

    public static byte[] decode(String pData) throws IOException {
        InputStream in = new DecoderStream(new ByteArrayInputStream(pData.getBytes()), new Base64Decoder());
        ByteArrayOutputStream bytes = new FastByteArrayOutputStream(pData.length() * 3);
        FileUtil.copy(in, bytes);

        return bytes.toByteArray();
    }

    //private final static sun.misc.BASE64Decoder DECODER = new sun.misc.BASE64Decoder();

    public static void main(String[] pArgs) throws IOException {
        if (pArgs.length == 1) {
            System.out.println(encode(pArgs[0].getBytes()));
        }
        else
        if (pArgs.length == 2 && ("-d".equals(pArgs[0]) || "--decode".equals(pArgs[0])))
        {
            System.out.println(new String(decode(pArgs[1])));
        }
        else {
            System.err.println("BASE64 [ -d | --decode ] arg");
            System.err.println("Encodes or decodes a given string");
            System.exit(5);
        }
    }
}