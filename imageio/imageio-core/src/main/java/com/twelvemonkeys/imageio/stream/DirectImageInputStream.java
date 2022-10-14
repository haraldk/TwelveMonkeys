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

package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.io.InputStream;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * An {@code ImageInputStream} that adapts an {@code InputSteam},
 * by reading directly from the stream without and form of caching or buffering.
 * <p>
 * Note: This is <em>not</em> a general-purpose {@code ImageInputStream}, and is designed for reading large chunks,
 * typically of pixel data, from an {@code InputStream}.
 * It does <em>not</em> support backwards seeking, or reading bits.
 * </p>
 */
public final class DirectImageInputStream extends ImageInputStreamImpl {
    private final InputStream stream;
    private final long length;

    public DirectImageInputStream(final InputStream stream) {
        this(stream, -1L);
    }

    public DirectImageInputStream(final InputStream stream, long length) {
        this.stream = notNull(stream, "stream");
        this.length = isTrue(length >= 0L || length == -1L, length, "negative length: %d");
    }

    @Override
    public int read() throws IOException {
        bitOffset = 0;
        streamPos++;
        return stream.read();
    }

    @Override
    public int read(final byte[] bytes, int off, int len) throws IOException {
        bitOffset = 0;

        int read = stream.read(bytes, off, len);
        if (read > 0) {
            streamPos += read;
        }

        return read;
    }

    @Override
    public void seek(long pos) throws IOException {
        checkClosed();

        if (pos < streamPos) {
            // Handle as if flushedPos == streamPos at any time
            throw new IndexOutOfBoundsException("pos < flushedPos");
        }

        bitOffset = 0;

        while (streamPos < pos) {
            long skipped = stream.skip(pos - streamPos);

            if (skipped <= 0) {
                break;
            }

            streamPos += skipped;
        }
    }

    @Override
    public long getFlushedPosition() {
        // Handle as if flushedPos == streamPos at any time
        return streamPos;
    }

    @Override
    public long length() {
        return length;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public int readBit() throws IOException {
        throw new UnsupportedOperationException("Bit reading not supported");
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public long readBits(int numBits) throws IOException {
        throw new UnsupportedOperationException("Bit reading not supported");
    }

    @Override
    public void close() throws IOException {
        // We could seek to EOF here, but the usual case is we know where the next chunk of data is

        stream.close();
        super.close();
    }
}
