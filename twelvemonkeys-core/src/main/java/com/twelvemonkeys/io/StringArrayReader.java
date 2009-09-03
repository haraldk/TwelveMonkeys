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

import java.io.StringReader;
import java.io.IOException;
import java.io.Reader;

/**
 * StringArrayReader
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/StringArrayReader.java#2 $
 */
public class StringArrayReader extends StringReader {

    private StringReader mCurrent;
    private String[] mStrings;
    protected final Object mLock;
    private int mCurrentSting;
    private int mMarkedString;
    private int mMark;
    private int mNext;

    /**
     * Create a new string array reader.
     *
     * @param pStrings <tt>String</tt>s providing the character stream.
     */
    public StringArrayReader(final String[] pStrings) {
        super("");
        if (pStrings == null) {
            throw new NullPointerException("strings == null");
        }

        mLock = lock = pStrings; // NOTE: It's ok to sync on pStrings, as the
                                 // reference can't change, only it's elements

        mStrings = pStrings.clone(); // Defensive copy for content
        nextReader();
    }

    protected final Reader nextReader() {
        if (mCurrentSting >= mStrings.length) {
            mCurrent = new EmptyReader();
        }
        else {
            mCurrent = new StringReader(mStrings[mCurrentSting++]);
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
        if (mStrings == null) {
            throw new IOException("Stream closed");
        }
    }

    public void close() {
        super.close();
        mStrings = null;
        mCurrent.close();
    }

    public void mark(int pReadLimit) throws IOException {
        if (pReadLimit < 0){
            throw new IllegalArgumentException("Read limit < 0");
        }

        synchronized (mLock) {
            ensureOpen();
            mMark = mNext;
            mMarkedString = mCurrentSting;

            mCurrent.mark(pReadLimit);
        }
    }

    public void reset() throws IOException {
        synchronized (mLock) {
            ensureOpen();

            if (mCurrentSting != mMarkedString) {
                mCurrentSting = mMarkedString - 1;
                nextReader();
                mCurrent.skip(mMark);
            }
            else {
                mCurrent.reset();
            }

            mNext = mMark;
        }
    }

    public boolean markSupported() {
        return true;
    }

    public int read() throws IOException {
        synchronized (mLock) {
            int read = mCurrent.read();

            if (read < 0 && mCurrentSting < mStrings.length) {
                nextReader();
                return read(); // In case of empty strings
            }

            mNext++;

            return read;
        }
    }

    public int read(char pBuffer[], int pOffset, int pLength) throws IOException {
        synchronized (mLock) {
            int read = mCurrent.read(pBuffer, pOffset, pLength);

            if (read < 0 && mCurrentSting < mStrings.length) {
                nextReader();
                return read(pBuffer, pOffset, pLength); // In case of empty strings
            }

            mNext += read;

            return read;
        }
    }

    public boolean ready() throws IOException {
        return mCurrent.ready();
    }

    public long skip(long pChars) throws IOException {
        synchronized (mLock) {
            long skipped = mCurrent.skip(pChars);

            if (skipped == 0 && mCurrentSting < mStrings.length) {
                nextReader();
                return skip(pChars);
            }

            mNext += skipped;

            return skipped;
        }
    }

}
