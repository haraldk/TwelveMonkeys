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

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * Abstract base class for {@code InputStream}s implementing the {@code Seekable} interface.
 * <p/>
 * @see SeekableOutputStream
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/SeekableInputStream.java#4 $
 */
public abstract class SeekableInputStream extends InputStream implements Seekable {

    // TODO: It's at the moment not possible to create subclasses outside this
    // package, as there's no access to mPosition. mPosition needs to be
    // updated from the read/read/read methods...

    /** The stream position in this stream */
    long mPosition;
    long mFlushedPosition;
    boolean mClosed;

    protected Stack<Long> mMarkedPositions = new Stack<Long>();

    /// InputStream overrides
    @Override
    public final int read(byte[] pBytes) throws IOException {
        return read(pBytes, 0, pBytes != null ? pBytes.length : 1);
    }

    /**
     * Implemented using {@code seek(currentPos + pLength)}.
     *
     * @param pLength the number of bytes to skip
     * @return the actual number of bytes skipped (may be equal to or less
     *         than {@code pLength})
     *
     * @throws IOException if an I/O exception occurs during skip
     */
    @Override
    public final long skip(long pLength) throws IOException {
        long pos = mPosition;
        if (pos + pLength < mFlushedPosition) {
            throw new IOException("position < flushedPosition");
        }

        // Stop at stream length for compatibility, even though it's allowed
        // to seek past end of stream
        seek(Math.min(pos + pLength, pos + available()));

        return mPosition - pos;
    }

    @Override
    public final void mark(int pLimit) {
        mark();

        // TODO: We don't really need to do this.. Is it a good idea?
        try {
            flushBefore(Math.max(mPosition - pLimit, mFlushedPosition));
        }
        catch (IOException ignore) {
            // Ignore, as it's not really critical
        }
    }

    /**
     * Returns {@code true}, as marking is always supported.
     *
     * @return {@code true}.
     */
    @Override
    public final boolean markSupported() {
        return true;
    }

    /// Seekable implementation
    public final void seek(long pPosition) throws IOException {
        checkOpen();

        // NOTE: This is correct according to javax.imageio (IndexOutOfBoundsException),
        // but it's kind of inconsistent with reset that throws IOException...
        if (pPosition < mFlushedPosition) {
           throw new IndexOutOfBoundsException("position < flushedPosition");
        }

        seekImpl(pPosition);
        mPosition = pPosition;
    }

    protected abstract void seekImpl(long pPosition) throws IOException;

    public final void mark() {
        mMarkedPositions.push(mPosition);
    }

    @Override
    public final void reset() throws IOException {
        checkOpen();
        if (!mMarkedPositions.isEmpty()) {
            long newPos = mMarkedPositions.pop();

            // NOTE: This is correct according to javax.imageio (IOException),
            // but it's kind of inconsistent with seek that throws IndexOutOfBoundsException...
            if (newPos < mFlushedPosition) {
                throw new IOException("Previous marked position has been discarded");
            }

            seek(newPos);
        }
        else {
            // TODO: To iron out some wrinkles due to conflicting contracts
            // (InputStream and Seekable both declare reset),
            // we might need to reset to the last marked position instead..
            // However, that becomes REALLY confusing if that position is after
            // the current position...
            seek(0);
        }
    }

    public final void flushBefore(long pPosition) throws IOException {
        if (pPosition < mFlushedPosition) {
            throw new IndexOutOfBoundsException("position < flushedPosition");
        }
        if (pPosition > getStreamPosition()) {
            throw new IndexOutOfBoundsException("position > stream position");
        }
        checkOpen();
        flushBeforeImpl(pPosition);
        mFlushedPosition = pPosition;
    }

    /**
     * Discards the initial portion of the stream prior to the indicated postion.
     *
     * @param pPosition the position to flush to
     * @throws IOException if an I/O exception occurs during the flush operation
     *
     * @see #flushBefore(long)
     */
    protected abstract void flushBeforeImpl(long pPosition) throws IOException;

    public final void flush() throws IOException {
        flushBefore(mFlushedPosition);
    }

    public final long getFlushedPosition() throws IOException {
        checkOpen();
        return mFlushedPosition;
    }

    public final long getStreamPosition() throws IOException {
        checkOpen();
        return mPosition;
    }

    protected final void checkOpen() throws IOException {
        if (mClosed) {
            throw new IOException("closed");
        }
    }

    @Override
    public final void close() throws IOException {
        checkOpen();
        mClosed = true;
        closeImpl();
    }

    protected abstract void closeImpl() throws IOException;

    /**
     * Finalizes this object prior to garbage collection.  The
     * {@code close} method is called to close any open input
     * source.  This method should not be called from application
     * code.
     *
     * @exception Throwable if an error occurs during superclass
     * finalization.
     */
    @Override
    protected void finalize() throws Throwable {
        if (!mClosed) {
            try {
                close();
            }
            catch (IOException ignore) {
                // Ignroe
            }
        }
        super.finalize();
    }
}
