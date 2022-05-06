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
 * An {@code InputStream} that provides on-the-fly decoding from an underlying stream.
 *
 * @see EncoderStream
 * @see Decoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/DecoderStream.java#2 $
 */
public final class DecoderStream extends FilterInputStream {
    private final ByteBuffer buffer;
    private final Decoder decoder;

    /**
     * Creates a new decoder stream and chains it to the
     * input stream specified by the {@code stream} argument.
     * The stream will use a default decode buffer size.
     *
     * @param stream the underlying input stream.
     * @param decoder the decoder that will be used to decode the underlying stream
     *
     * @see java.io.FilterInputStream#in
     */
    public DecoderStream(final InputStream stream, final Decoder decoder) {
        // TODO: Let the decoder decide preferred buffer size 
        this(stream, decoder, 1024);
    }

    /**
     * Creates a new decoder stream and chains it to the
     * input stream specified by the {@code stream} argument.
     *
     * @param stream the underlying input stream.
     * @param decoder the decoder that will be used to decode the underlying stream
     * @param bufferSize the size of the decode buffer
     *
     * @see java.io.FilterInputStream#in
     */
    public DecoderStream(final InputStream stream, final Decoder decoder, final int bufferSize) {
        super(stream);

        this.decoder = decoder;
        buffer = ByteBuffer.allocate(bufferSize);
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

    public int read(final byte[] bytes, final int offset, final int length) throws IOException {
        if (bytes == null) {
            throw new NullPointerException();
        }
        else if ((offset < 0) || (offset > bytes.length) || (length < 0) ||
                ((offset + length) > bytes.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException("bytes.length=" + bytes.length + " offset=" + offset + " length=" + length);
        }
        else if (length == 0) {
            return 0;
        }

        // End of file?
        if (!buffer.hasRemaining()) {
            if (fill() < 0) {
                return -1;
            }
        }

        // Read until we have read length bytes, or have reached EOF
        int count = 0;
        int off = offset;

        while (length > count) {
            if (!buffer.hasRemaining()) {
                if (fill() < 0) {
                    break;
                }
            }

            // Copy as many bytes as possible
            int dstLen = Math.min(length - count, buffer.remaining());
            buffer.get(bytes, off, dstLen);

            // Update offset (rest)
            off += dstLen;

            // Increase count
            count += dstLen;
        }

        return count;
    }

    public long skip(final long length) throws IOException {
        // End of file?
        if (!buffer.hasRemaining()) {
            if (fill() < 0) {
                return 0; // Yes, 0, not -1
            }
        }

        // Skip until we have skipped length bytes, or have reached EOF
        long total = 0;

        while (total < length) {
            if (!buffer.hasRemaining()) {
                if (fill() < 0) {
                    break;
                }
            }

            // NOTE: Skipped can never be more than avail, which is an int, so the cast is safe
            int skipped = (int) Math.min(length - total, buffer.remaining());
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
    private int fill() throws IOException {
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
