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
 * Decoder implementation for Apple PackBits run-length encoding.
 * <p/>
 * <small>From Wikipedia, the free encyclopedia</small><br/>
 * PackBits is a fast, simple compression scheme for run-length encoding of
 * data.
 * <p/>
 * Apple introduced the PackBits format with the release of MacPaint on the
 * Macintosh computer. This compression scheme is one of the types of
 * compression that can be used in TIFF-files.
 * <p/>
 * A PackBits data stream consists of packets of one byte of header followed by
 * data. The header is a signed byte; the data can be signed, unsigned, or
 * packed (such as MacPaint pixels).
 * <p/>
 * <table><tr><th>Header byte</th><th>Data</th></tr>
 * <tr><td>0 to 127</td>    <td>1 + <i>n</i> literal bytes of data</td></tr>
 * <tr><td>0 to -127</td>   <td>One byte of data, repeated 1 - <i>n</i> times in
 *                           the decompressed output</td></tr>
 * <tr><td>-128</td>        <td>No operation</td></tr></table>
 * <p/>
 * Note that interpreting 0 as positive or negative makes no difference in the
 * output. Runs of two bytes adjacent to non-runs are typically written as
 * literal data.
 * <p/>
 * See <a href="http://developer.apple.com/technotes/tn/tn1023.html">Understanding PackBits</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/PackBitsDecoder.java#1 $
 */
public final class PackBitsDecoder implements Decoder {
    // TODO: Look at ICNSImageReader#unpackbits... What is this weirdness?

    private final boolean disableNoOp;
    private final byte[] sample;

    private int leftOfRun;
    private boolean splitRun;
    private boolean reachedEOF;

    /** Creates a {@code PackBitsDecoder}. */
    public PackBitsDecoder() {
        this(1, false);
    }

    /**
     * Creates a {@code PackBitsDecoder}, with optional compatibility mode.
     * <p/>
     * As some implementations of PackBits-like encoders treat {@code -128} as length of
     * a compressed run, instead of a no-op, it's possible to disable no-ops for compatibility.
     * Should be used with caution, even though, most known encoders never write no-ops in the compressed streams.
     *
     * @param disableNoOp {@code true} if {@code -128} should be treated as a compressed run, and not a no-op
     */
    public PackBitsDecoder(final boolean disableNoOp) {
        this(1, disableNoOp);
    }

    /**
     * Creates a {@code PackBitsDecoder}, with optional compatibility mode.
     * <p/>
     * As some implementations of PackBits-like encoders treat {@code -128} as length of
     * a compressed run, instead of a no-op, it's possible to disable no-ops for compatibility.
     * Should be used with caution, even though, most known encoders never write no-ops in the compressed streams.
     *
     * @param disableNoOp {@code true} if {@code -128} should be treated as a compressed run, and not a no-op
     */
    public PackBitsDecoder(int sampleSize, final boolean disableNoOp) {
        this.sample = new byte[sampleSize];
        this.disableNoOp = disableNoOp;
    }

    /**
     * Decodes bytes from the given input stream, to the given buffer.
     *
     * @param stream the stream to decode from
     * @param buffer a byte array, minimum 128 (or 129 if no-op is disabled) bytes long
     * @return The number of bytes decoded
     *
     * @throws java.io.IOException
     */
    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        if (reachedEOF) {
            return -1;
        }

        // TODO: Don't decode more than single runs, because some writers add pad bytes inside the stream...
        while (buffer.hasRemaining()) {
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
            if (n >= 0 && n + 1 > buffer.remaining()) {
                leftOfRun = n;
                splitRun = true;
                break;
            }
            else if (n < 0 && -n + 1 > buffer.remaining()) {
                leftOfRun = n;
                splitRun = true;
                break;
            }

            try {
                if (n >= 0) {
                    // Copy next n + 1 bytes literally
                    readFully(stream, buffer, sample.length * (n + 1));
                }
                // Allow -128 for compatibility, see above
                else if (disableNoOp || n != -128) {
                    // Replicate the next byte -n + 1 times
                    for (int s = 0; s < sample.length; s++) {
                        sample[s] = readByte(stream);
                    }

                    for (int i = -n + 1; i > 0; i--) {
                        buffer.put(sample);
                    }
                }
                // else NOOP (-128)
            }
            catch (IndexOutOfBoundsException e) {
                throw new DecodeException("Error in PackBits decompression, data seems corrupt", e);
            }
        }

        return buffer.position();
    }

    static byte readByte(final InputStream pStream) throws IOException {
        int read = pStream.read();

        if (read < 0) {
            throw new EOFException("Unexpected end of PackBits stream");
        }

        return (byte) read;
    }

    static void readFully(final InputStream pStream, final ByteBuffer pBuffer, final int pLength) throws IOException {
        if (pLength < 0) {
            throw new IndexOutOfBoundsException(String.format("Negative length: %d", pLength));
        }

        int total = 0;

        while (total < pLength) {
            int count = pStream.read(pBuffer.array(), pBuffer.arrayOffset() + pBuffer.position() + total, pLength - total);

            if (count < 0) {
                throw new EOFException("Unexpected end of PackBits stream");
            }

            total += count;
        }

        pBuffer.position(pBuffer.position() + total);
    }
}
