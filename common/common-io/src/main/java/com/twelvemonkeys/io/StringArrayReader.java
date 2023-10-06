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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * StringArrayReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/StringArrayReader.java#2 $
 */
public class StringArrayReader extends StringReader {

    private StringReader current;
    private String[] strings;
    protected final Object finalLock;
    private int currentSting;
    private int markedString;
    private long mark;
    private long next;

    /**
     * Create a new string array reader.
     *
     * @param pStrings {@code String}s providing the character stream.
     */
    public StringArrayReader(final String[] pStrings) {
        super("");

        Validate.notNull(pStrings, "strings");

        finalLock = lock = pStrings; // NOTE: It's ok to sync on pStrings, as the
                                 // reference can't change, only it's elements

        strings = pStrings.clone(); // Defensive copy for content
        nextReader();
    }

    protected final Reader nextReader() {
        if (currentSting >= strings.length) {
            current = new EmptyReader();
        }
        else {
            current = new StringReader(strings[currentSting++]);
        }

        // NOTE: Reset next for every reader, and record marked reader in mark/reset methods!
        next = 0;
        
        return current;
    }

    /**
     * Check to make sure that the stream has not been closed
     *
     * @throws IOException if the stream is closed
     */
    protected final void ensureOpen() throws IOException {
        if (strings == null) {
            throw new IOException("Stream closed");
        }
    }

    public void close() {
        super.close();
        strings = null;
        current.close();
    }

    public void mark(int pReadLimit) throws IOException {
        if (pReadLimit < 0){
            throw new IllegalArgumentException("Read limit < 0");
        }

        synchronized (finalLock) {
            ensureOpen();
            mark = next;
            markedString = currentSting;

            current.mark(pReadLimit);
        }
    }

    public void reset() throws IOException {
        synchronized (finalLock) {
            ensureOpen();

            if (currentSting != markedString) {
                currentSting = markedString - 1;
                nextReader();
                current.skip(mark);
            }
            else {
                current.reset();
            }

            next = mark;
        }
    }

    public boolean markSupported() {
        return true;
    }

    public int read() throws IOException {
        synchronized (finalLock) {
            int read = current.read();

            if (read < 0 && currentSting < strings.length) {
                nextReader();
                return read(); // In case of empty strings
            }

            next++;

            return read;
        }
    }

    public int read(char[] pBuffer, int pOffset, int pLength) throws IOException {
        synchronized (finalLock) {
            int read = current.read(pBuffer, pOffset, pLength);

            if (read < 0 && currentSting < strings.length) {
                nextReader();
                return read(pBuffer, pOffset, pLength); // In case of empty strings
            }

            next += read;

            return read;
        }
    }

    public boolean ready() throws IOException {
        return current.ready();
    }

    public long skip(long pChars) throws IOException {
        synchronized (finalLock) {
            long skipped = current.skip(pChars);

            if (skipped == 0 && currentSting < strings.length) {
                nextReader();
                return skip(pChars);
            }

            next += skipped;

            return skipped;
        }
    }

}
