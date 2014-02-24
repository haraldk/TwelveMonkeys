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

/**
 * Decoder implementation for 16 bit-chunked Apple PackBits-like run-length
 * encoding.
 * <p/>
 * This version of the decoder decodes chunk of 16 bit, instead of 8 bit.
 * This format is used in certain PICT files. 
 *
 * @see PackBitsDecoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/PackBits16Decoder.java#2 $
 */
public final class PackBits16Decoder implements Decoder {
    // TODO: Refactor this into an option for the PackBitsDecoder?
    private final boolean disableNoop;

    private int leftOfRun;
    private boolean splitRun;
    private boolean reachedEOF;

    /**
     * Creates a {@code PackBitsDecoder}.
     */
    public PackBits16Decoder() {
        this(false);
    }

    /**
     * Creates a {@code PackBitsDecoder}.
     * <p/>
     * As some implementations of PackBits-like encoders treat {@code -128} as length of
     * a compressed run, instead of a no-op, it's possible to disable no-ops
     * for compatibility.
     * Should be used with caution, even though, most known encoders never write
     * no-ops in the compressed streams.
     *
     * @param pDisableNoop {@code true} if {@code -128} should be treated as a compressed run, and not a no-op
     */
    public PackBits16Decoder(final boolean pDisableNoop) {
        disableNoop = pDisableNoop;
    }

    /**
     * Decodes bytes from the given input stream, to the given buffer.
     *
     * @param stream the stream to decode from
     * @param buffer a byte array, minimum 128 (or 129 if no-op is disabled)
     * bytes long
     * @return The number of bytes decoded
     *
     * @throws java.io.IOException
     */
    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        if (reachedEOF) {
            return -1;
        }

        int read = 0;
        final int max = buffer.capacity();

        while (read < max) {
            int n;

            if (splitRun) {
                // Continue run
                n = leftOfRun;
                splitRun = false;
            }
            else {
                // Start new run
                int b = stream.read();
                if (b < 0) {
                    reachedEOF = true;
                    break;
                }
                n = (byte) b;
            }

            // Split run at or before max
            if (n >= 0 && 2 * (n + 1) + read > max) {
                leftOfRun = n;
                splitRun = true;
                break;
            }
            else if (n < 0 && 2 * (-n + 1) + read > max) {
                leftOfRun = n;
                splitRun = true;
                break;
            }

            try {
                if (n >= 0) {
                    // Copy next n + 1 shorts literally
                    int len = 2 * (n + 1);
                    readFully(stream, buffer, len);
                    read += len;
                }
                // Allow -128 for compatibility, see above
                else if (disableNoop || n != -128) {
                    // Replicate the next short -n + 1 times
                    byte value1 = readByte(stream);
                    byte value2 = readByte(stream);

                    for (int i = -n + 1; i > 0; i--) {
                        buffer.put(value1);
                        buffer.put(value2);
                    }
                }
                // else NOOP (-128)
            }
            catch (IndexOutOfBoundsException e) {
                throw new DecodeException("Error in PackBits decompression, data seems corrupt", e);
            }
        }

        return read;
    }

    private static byte readByte(final InputStream pStream) throws IOException {
        int read = pStream.read();

        if (read < 0) {
            throw new EOFException("Unexpected end of PackBits stream");
        }

        return (byte) read;
    }

    private static void readFully(final InputStream pStream, final ByteBuffer pBuffer, final int pLength) throws IOException {
        if (pLength < 0) {
            throw new IndexOutOfBoundsException();
        }

        int read = 0;

        while (read < pLength) {
            int count = pStream.read(pBuffer.array(), pBuffer.arrayOffset() + pBuffer.position() + read, pLength - read);

            if (count < 0) {
                throw new EOFException("Unexpected end of PackBits stream");
            }

            read += count;
        }
    }
}
