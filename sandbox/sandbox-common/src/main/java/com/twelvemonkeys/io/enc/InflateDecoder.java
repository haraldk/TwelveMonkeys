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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * {@code Decoder} implementation for standard DEFLATE encoding.
 * <p/>
 *
 * @see <a href="http://tools.ietf.org/html/rfc1951">RFC 1951</a>
 *
 * @see Inflater
 * @see DeflateEncoder
 * @see java.util.zip.InflaterInputStream
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/InflateDecoder.java#2 $
 */
final class InflateDecoder implements Decoder {

    private final Inflater inflater;

    private final byte[] buffer;

    /**
     * Creates an {@code InflateDecoder}
     *
     */
    public InflateDecoder() {
        this(new Inflater(true));
    }

    /**
     * Creates an {@code InflateDecoder}
     *
     * @param pInflater the inflater instance to use
     */
    public InflateDecoder(final Inflater pInflater) {
        if (pInflater == null) {
            throw new IllegalArgumentException("inflater == null");
        }

        inflater = pInflater;
        buffer = new byte[1024];
    }

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        try {
            int decoded;

            while ((decoded = inflater.inflate(buffer.array(), buffer.arrayOffset(), buffer.capacity())) == 0) {
                if (inflater.finished() || inflater.needsDictionary()) {
                    return 0;
                }

                if (inflater.needsInput()) {
                    fill(stream);
                }
            }

            return decoded;
        }
        catch (DataFormatException e) {
            String message = e.getMessage();
            throw new DecodeException(message != null ? message : "Invalid ZLIB data format", e);
        }
    }

    private void fill(final InputStream pStream) throws IOException {
        int available = pStream.read(buffer, 0, buffer.length);

        if (available == -1) {
            throw new EOFException("Unexpected end of ZLIB stream");
        }

        inflater.setInput(buffer, 0, available);
    }
}