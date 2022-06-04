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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * An {@code OutputStream} that provides on-the-fly encoding to an underlying stream.
 *
 * @see DecoderStream
 * @see Encoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/EncoderStream.java#2 $
 */
public final class EncoderStream extends FilterOutputStream {

    private final Encoder encoder;
    private final boolean flushOnWrite;

    private final ByteBuffer buffer;

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param stream the underlying output stream
     * @param encoder the encoder to use
     */
    public EncoderStream(final OutputStream stream, final Encoder encoder) {
        this(stream, encoder, false);
    }

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param stream the underlying output stream
     * @param encoder the encoder to use
     * @param flushOnWrite if {@code true}, calls to the byte-array
     * {@code write} methods will automatically flush the buffer.
     */
    public EncoderStream(final OutputStream stream, final Encoder encoder, final boolean flushOnWrite) {
        super(stream);

        this.encoder = encoder;
        this.flushOnWrite = flushOnWrite;

        buffer = ByteBuffer.allocate(1024);
    }

    public void close() throws IOException {
        flush();
        super.close();
    }

    public void flush() throws IOException {
        encodeBuffer();
        super.flush();
    }

    private void encodeBuffer() throws IOException {
        if (buffer.position() != 0) {
            buffer.flip();

            // Make sure all remaining data in buffer is written to the stream
            encoder.encode(out, buffer);

            // Reset buffer
            buffer.clear();
        }
    }

    public void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    // TODO: Verify that this works for the general case (it probably won't)...
    // TODO: We might need a way to explicitly flush the encoder, or specify
    // that the encoder can't buffer. In that case, the encoder should probably
    // tell the EncoderStream how large buffer it prefers...
    public void write(final byte[] values, final int offset, final int length) throws IOException {
        if (!flushOnWrite && length < buffer.remaining()) {
            // Buffer data
            buffer.put(values, offset, length);
        }
        else {
            // Encode data already in the buffer
            encodeBuffer();

            // Encode rest without buffering
            encoder.encode(out, ByteBuffer.wrap(values, offset, length));
        }
    }

    public void write(final int value) throws IOException {
        if (!buffer.hasRemaining()) {
            encodeBuffer(); // Resets bufferPos to 0
        }

        buffer.put((byte) value);
    }
}
