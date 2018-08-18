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
    // package, as there's no access to position. position needs to be
    // updated from the read/read/read methods...

    /** The stream position in this stream */
    long position;
    long flushedPosition;
    boolean closed;

    protected Stack<Long> markedPositions = new Stack<Long>();

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
    public final long skip(final long pLength) throws IOException {
        long pos = position;
        long wantedPosition = pos + pLength;
        if (wantedPosition < flushedPosition) {
            throw new IOException("position < flushedPosition");
        }

        // Stop at stream length for compatibility, even though it might be allowed
        // to seek past end of stream
        int available = available();
        if (available > 0) {
            seek(Math.min(wantedPosition, pos + available));
        }
        // TODO: Add optimization for streams with known length!
        else {
            // Slow mode...
            int toSkip = (int) Math.max(Math.min(pLength, 512), -512);
            while (toSkip > 0 && read() >= 0) {
                toSkip--;
            }
        }

        return position - pos;
    }

    @Override
    public final void mark(int pLimit) {
        mark();

        // TODO: We don't really need to do this.. Is it a good idea?
        try {
            flushBefore(Math.max(position - pLimit, flushedPosition));
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
        if (pPosition < flushedPosition) {
           throw new IndexOutOfBoundsException("position < flushedPosition");
        }

        seekImpl(pPosition);
        position = pPosition;
    }

    protected abstract void seekImpl(long pPosition) throws IOException;

    public final void mark() {
        markedPositions.push(position);
    }

    @Override
    public final void reset() throws IOException {
        checkOpen();
        if (!markedPositions.isEmpty()) {
            long newPos = markedPositions.pop();

            // NOTE: This is correct according to javax.imageio (IOException),
            // but it's kind of inconsistent with seek that throws IndexOutOfBoundsException...
            if (newPos < flushedPosition) {
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
        if (pPosition < flushedPosition) {
            throw new IndexOutOfBoundsException("position < flushedPosition");
        }
        if (pPosition > getStreamPosition()) {
            throw new IndexOutOfBoundsException("position > stream position");
        }
        checkOpen();
        flushBeforeImpl(pPosition);
        flushedPosition = pPosition;
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
        flushBefore(flushedPosition);
    }

    public final long getFlushedPosition() throws IOException {
        checkOpen();
        return flushedPosition;
    }

    public final long getStreamPosition() throws IOException {
        checkOpen();
        return position;
    }

    protected final void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("closed");
        }
    }

    @Override
    public final void close() throws IOException {
        checkOpen();
        closed = true;
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
        if (!closed) {
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
