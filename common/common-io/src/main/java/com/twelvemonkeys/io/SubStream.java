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

package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.Validate;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@code InputStream} reading up to a specified number of bytes from an
 * underlying stream.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public final class SubStream extends FilterInputStream {
    private long bytesLeft;
    private int markLimit;

    /**
     * Creates a {@code SubStream} of the given {@code stream}.
     *
     * @param stream the underlying input stream
     * @param length maximum number of bytes to read from this stream
     */
    public SubStream(final InputStream stream, final long length) {
        super(Validate.notNull(stream, "stream"));
        bytesLeft = length;
    }

    /**
     * Marks this stream as closed.
     * This implementation does <em>not</em> close the underlying stream.
     */
    @Override
    public void close() throws IOException {
        // NOTE: Do not close the underlying stream
        while (bytesLeft > 0) {
            //noinspection ResultOfMethodCallIgnored
            skip(bytesLeft);
        }
    }

    @Override
    public int available() throws IOException {
        return (int) findMaxLen(super.available());
    }

    @Override
    public void mark(int readLimit) {
        super.mark(readLimit);// This either succeeds or does nothing...
        markLimit = readLimit;
    }

    @Override
    public void reset() throws IOException {
        super.reset();// This either succeeds or throws IOException
        bytesLeft += markLimit;
    }

    @Override
    public int read() throws IOException {
        if (bytesLeft-- <= 0) {
            return -1;
        }

        return super.read();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    @Override
    public int read(final byte[] bytes, final int off, final int len) throws IOException {
        if (bytesLeft <= 0) {
            return -1;
        }

        int read = super.read(bytes, off, (int) findMaxLen(len));
        bytesLeft = read < 0 ? 0 : bytesLeft - read;

        return read;
    }

    @Override
    public long skip(long length) throws IOException {
        long skipped = super.skip(findMaxLen(length));// Skips 0 or more, never -1
        bytesLeft -= skipped;

        return skipped;
    }

    /**
     * Finds the maximum number of bytes we can read or skip, from this stream.
     *
     * @param length the requested length
     * @return the maximum number of bytes to read
     */
    private long findMaxLen(long length) {
        return bytesLeft < length ? Math.max(bytesLeft, 0) : length;
    }
}
