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

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * A Reader implementation that can read from multiple sources.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/CompoundReader.java#2 $
 */
public class CompoundReader extends Reader {

    private Reader mCurrent;
    private List<Reader> mReaders;
    protected final Object mLock;

    protected final boolean mMarkSupported;

    private int mCurrentReader;
    private int mMarkedReader;
    private int mMark;
    private int mNext;

    /**
     * Create a new compound reader.
     *
     * @param pReaders {@code Iterator} containting {@code Reader}s,
     *        providing the character stream.
     *
     * @throws NullPointerException if {@code pReaders} is {@code null}, or
     *         any of the elements in the iterator is {@code null}.
     * @throws ClassCastException if any element of the iterator is not a
     *         {@code java.io.Reader}
     */
    public CompoundReader(final Iterator<Reader> pReaders) {
        super(Validate.notNull(pReaders, "readers"));

        mLock = pReaders; // NOTE: It's ok to sync on pReaders, as the
                          // reference can't change, only it's elements

        mReaders = new ArrayList<Reader>();

        boolean markSupported = true;
        while (pReaders.hasNext()) {
            Reader reader = pReaders.next();
            if (reader == null) {
                throw new NullPointerException("readers cannot contain null-elements");
            }
            mReaders.add(reader);
            markSupported = markSupported && reader.markSupported();
        }
        mMarkSupported = markSupported;

        mCurrent = nextReader();
    }

    protected final Reader nextReader() {
        if (mCurrentReader >= mReaders.size()) {
            mCurrent = new EmptyReader();
        }
        else {
            mCurrent = mReaders.get(mCurrentReader++);
        }
        
        // NOTE: Reset mNext for every reader, and record marked reader in mark/reset methods!
        mNext = 0;
        return mCurrent;
    }

    /**
     * Check to make sure that the stream has not been closed
     *
     * @throws IOException if the stream is closed
     */
    protected final void ensureOpen() throws IOException {
        if (mReaders == null) {
            throw new IOException("Stream closed");
        }
    }

    public void close() throws IOException {
        // Close all readers
        for (Reader reader : mReaders) {
            reader.close();
        }
        mReaders = null;
    }

    @Override
    public void mark(int pReadLimit) throws IOException {
        if (pReadLimit < 0) {
            throw new IllegalArgumentException("Read limit < 0");
        }

        // TODO: It would be nice if we could actually close some readers now

        synchronized (mLock) {
            ensureOpen();
            mMark = mNext;
            mMarkedReader = mCurrentReader;

            mCurrent.mark(pReadLimit);
        }
    }

    @Override
    public void reset() throws IOException {
        synchronized (mLock) {
            ensureOpen();

            if (mCurrentReader != mMarkedReader) {
                // Reset any reader before this
                for (int i = mCurrentReader; i >= mMarkedReader; i--) {
                    mReaders.get(i).reset();
                }

                mCurrentReader = mMarkedReader - 1;
                nextReader();
            }
            mCurrent.reset();

            mNext = mMark;
        }
    }

    @Override
    public boolean markSupported() {
        return mMarkSupported;
    }

    @Override
    public int read() throws IOException {
        synchronized (mLock) {
            int read = mCurrent.read();

            if (read < 0 && mCurrentReader < mReaders.size()) {
                nextReader();
                return read(); // In case of 0-length readers
            }

            mNext++;

            return read;
        }
    }

    public int read(char pBuffer[], int pOffset, int pLength) throws IOException {
        synchronized (mLock) {
            int read = mCurrent.read(pBuffer, pOffset, pLength);

            if (read < 0 && mCurrentReader < mReaders.size()) {
                nextReader();
                return read(pBuffer, pOffset, pLength); // In case of 0-length readers
            }

            mNext += read;

            return read;
        }
    }

    @Override
    public boolean ready() throws IOException {
        return mCurrent.ready();
    }

    @Override
    public long skip(long pChars) throws IOException {
        synchronized (mLock) {
            long skipped = mCurrent.skip(pChars);

            if (skipped == 0 && mCurrentReader < mReaders.size()) {
                nextReader();
                return skip(pChars); // In case of 0-length readers
            }

            mNext += skipped;

            return skipped;
        }
    }
}
