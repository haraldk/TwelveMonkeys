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
import java.io.OutputStream;
import java.util.Stack;

/**
 * Abstract base class for {@code OutputStream}s implementing the
 * {@code Seekable} interface.
 * <p/>
 * @see SeekableInputStream
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/SeekableOutputStream.java#2 $
 */
public abstract class SeekableOutputStream extends OutputStream implements Seekable {
    // TODO: Implement
    long position;
    long flushedPosition;
    boolean closed;

    protected Stack<Long> markedPositions = new Stack<Long>();

    /// Outputstream overrides
    @Override
    public final void write(byte pBytes[]) throws IOException {
        write(pBytes, 0, pBytes != null ? pBytes.length : 1);
    }

    /// Seekable implementation
    // TODO: This is common behaviour/implementation with SeekableInputStream,
    // probably a good idea to extract a delegate..?
    public final void seek(long pPosition) throws IOException {
        checkOpen();

        // TODO: This is correct according to javax.imageio (IndexOutOfBoundsException),
        // but it's inconsistent with reset that throws IOException...
        if (pPosition < flushedPosition) {
            throw new IndexOutOfBoundsException("position < flushedPosition!");
        }

        seekImpl(pPosition);
        position = pPosition;
    }

    protected abstract void seekImpl(long pPosition) throws IOException;

    public final void mark() {
        markedPositions.push(position);
    }

    public final void reset() throws IOException {
        checkOpen();
        if (!markedPositions.isEmpty()) {
            long newPos = markedPositions.pop();

            // TODO: This is correct according to javax.imageio (IOException),
            // but it's inconsistent with seek that throws IndexOutOfBoundsException...
            if (newPos < flushedPosition) {
                throw new IOException("Previous marked position has been discarded!");
            }

            seek(newPos);
        }
    }

    public final void flushBefore(long pPosition) throws IOException {
        if (pPosition < flushedPosition) {
            throw new IndexOutOfBoundsException("position < flushedPosition!");
        }
        if (pPosition > getStreamPosition()) {
            throw new IndexOutOfBoundsException("position > getStreamPosition()!");
        }
        checkOpen();
        flushBeforeImpl(pPosition);
        flushedPosition = pPosition;
    }

    protected abstract void flushBeforeImpl(long pPosition) throws IOException;

    @Override
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
}
