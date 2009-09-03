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

package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.Validate;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@code InputStream} reading up to a specified number of bytes from an
 * underlying stream.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/SubStream.java#2 $
 */
public final class SubStream extends FilterInputStream {
    private long mLeft;
    private int mMarkLimit;

    /**
     * Creates a {@code SubStream} of the given {@code pStream}.
     *
     * @param pStream the underlying input stream
     * @param pLength maximum number of bytes to read drom this stream
     */
    public SubStream(final InputStream pStream, final long pLength) {
        super(Validate.notNull(pStream, "stream"));
        mLeft = pLength;
    }

    /**
     * Marks this stream as closed.
     * This implementation does <em>not</em> close the underlying stream.
     */
    @Override
    public void close() throws IOException {
        // NOTE: Do not close the underlying stream
        while (mLeft > 0) {
            //noinspection ResultOfMethodCallIgnored
            skip(mLeft);
        }
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(super.available(), mLeft);
    }

    @Override
    public void mark(int pReadLimit) {
        super.mark(pReadLimit);// This either succeeds or does nothing...
        mMarkLimit = pReadLimit;
    }

    @Override
    public void reset() throws IOException {
        super.reset();// This either succeeds or throws IOException
        mLeft += mMarkLimit;
    }

    @Override
    public int read() throws IOException {
        if (mLeft-- <= 0) {
            return -1;
        }
        return super.read();
    }

    @Override
    public final int read(byte[] pBytes) throws IOException {
        return read(pBytes, 0, pBytes.length);
    }

    @Override
    public int read(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        if (mLeft <= 0) {
            return -1;
        }

        int read = super.read(pBytes, pOffset, (int) findMaxLen(pLength));
        mLeft = read < 0 ? 0 : mLeft - read;
        return read;
    }

    /**
     * Finds the maximum number of bytes we can read or skip, from this stream.
     *
     * @param pLength the requested length
     * @return the maximum number of bytes to read
     */
    private long findMaxLen(long pLength) {
        if (mLeft < pLength) {
            return (int) Math.max(mLeft, 0);
        }
        else {
            return pLength;
        }
    }

    @Override
    public long skip(long pLength) throws IOException {
        long skipped = super.skip(findMaxLen(pLength));// Skips 0 or more, never -1
        mLeft -= skipped;
        return skipped;
    }
}
