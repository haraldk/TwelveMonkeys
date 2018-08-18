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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An {@code InputStream} that provides on-the-fly decoding from an underlying
 * stream.
 * <p/>
 * @see EncoderStream
 * @see Decoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/DecoderStream.java#2 $
 */
public final class DecoderStream extends FilterInputStream {
    protected final ByteBuffer buffer;
    protected final Decoder decoder;

    /**
     * Creates a new decoder stream and chains it to the
     * input stream specified by the {@code pStream} argument.
     * The stream will use a default decode buffer size.
     *
     * @param pStream the underlying input stream.
     * @param pDecoder the decoder that will be used to decode the underlying stream
     *
     * @see java.io.FilterInputStream#in
     */
    public DecoderStream(final InputStream pStream, final Decoder pDecoder) {
        // TODO: Let the decoder decide preferred buffer size 
        this(pStream, pDecoder, 1024);
    }

    /**
     * Creates a new decoder stream and chains it to the
     * input stream specified by the {@code pStream} argument.
     *
     * @param pStream the underlying input stream.
     * @param pDecoder the decoder that will be used to decode the underlying stream
     * @param pBufferSize the size of the decode buffer
     *
     * @see java.io.FilterInputStream#in
     */
    public DecoderStream(final InputStream pStream, final Decoder pDecoder, final int pBufferSize) {
        super(pStream);

        decoder = pDecoder;
        buffer = ByteBuffer.allocate(pBufferSize);
        buffer.flip();
    }

    public int available() throws IOException {
        return buffer.remaining();
    }

    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            if (fill() < 0) {
                return -1;
            }
        }

        return buffer.get() & 0xff;
    }

    public int read(final byte pBytes[], final int pOffset, final int pLength) throws IOException {
        if (pBytes == null) {
            throw new NullPointerException();
        }
        else if ((pOffset < 0) || (pOffset > pBytes.length) || (pLength < 0) ||
                ((pOffset + pLength) > pBytes.length) || ((pOffset + pLength) < 0)) {
            throw new IndexOutOfBoundsException("bytes.length=" + pBytes.length + " offset=" + pOffset + " length=" + pLength);
        }
        else if (pLength == 0) {
            return 0;
        }

        // End of file?
        if (!buffer.hasRemaining()) {
            if (fill() < 0) {
                return -1;
            }
        }

        // Read until we have read pLength bytes, or have reached EOF
        int count = 0;
        int off = pOffset;

        while (pLength > count) {
            if (!buffer.hasRemaining()) {
                if (fill() < 0) {
                    break;
                }
            }

            // Copy as many bytes as possible
            int dstLen = Math.min(pLength - count, buffer.remaining());
            buffer.get(pBytes, off, dstLen);

            // Update offset (rest)
            off += dstLen;

            // Increase count
            count += dstLen;
        }

        return count;
    }

    public long skip(final long pLength) throws IOException {
        // End of file?
        if (!buffer.hasRemaining()) {
            if (fill() < 0) {
                return 0; // Yes, 0, not -1
            }
        }

        // Skip until we have skipped pLength bytes, or have reached EOF
        long total = 0;

        while (total < pLength) {
            if (!buffer.hasRemaining()) {
                if (fill() < 0) {
                    break;
                }
            }

            // NOTE: Skipped can never be more than avail, which is an int, so the cast is safe
            int skipped = (int) Math.min(pLength - total, buffer.remaining());
            buffer.position(buffer.position() + skipped);
            total += skipped;
        }

        return total;
    }

    /**
     * Fills the buffer, by decoding data from the underlying input stream.
     *
     * @return the number of bytes decoded, or {@code -1} if the end of the
     * file is reached
     *
     * @throws IOException if an I/O error occurs
     */
    protected int fill() throws IOException {
        buffer.clear();
        int read = decoder.decode(in, buffer);

        // TODO: Enforce this in test case, leave here to aid debugging
        if (read > buffer.capacity()) {
            throw new AssertionError(
                    String.format(
                            "Decode beyond buffer (%d): %d (using %s decoder)",
                            buffer.capacity(), read, decoder.getClass().getName()
                    )
            );
        }

        buffer.flip();

        if (read == 0) {
            return -1;
        }

        return read;
    }
}
