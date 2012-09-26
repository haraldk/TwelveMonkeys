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

import java.io.*;

/**
 * A {@code SeekableInputStream} implementation that uses random access directly to a {@code File}.
 * <p/>
 * @see FileCacheSeekableStream
 * @see MemoryCacheSeekableStream
 * @see RandomAccessFile
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/FileSeekableStream.java#4 $
 */
public final class FileSeekableStream extends SeekableInputStream {

    // TODO: Figure out why this class is SLOWER than FileCacheSeekableStream in
    // my tests..?

    final RandomAccessFile mRandomAccess;

    /**
     * Creates a {@code FileSeekableStream} that reads from the given
     * {@code File}.
     *
     * @param pInput file to read from
     * @throws FileNotFoundException if {@code pInput} does not exist
     */
    public FileSeekableStream(final File pInput) throws FileNotFoundException {
        this(new RandomAccessFile(pInput, "r"));
    }

    /**
     * Creates a {@code FileSeekableStream} that reads from the given file.
     * The {@code RandomAccessFile} needs only to be open in read
     * ({@code "r"}) mode.
     *
     * @param pInput file to read from
     */
    public FileSeekableStream(final RandomAccessFile pInput) {
        mRandomAccess = pInput;
    }

    /// Seekable

    public boolean isCached() {
        return false;
    }

    public boolean isCachedFile() {
        return false;
    }

    public boolean isCachedMemory() {
        return false;
    }

    /// InputStream

    @Override
    public int available() throws IOException {
        long length = mRandomAccess.length() - position;
        return length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length;
    }

    public void closeImpl() throws IOException {
        mRandomAccess.close();
    }

    public int read() throws IOException {
        checkOpen();

        int read = mRandomAccess.read();
        if (read >= 0) {
            position++;
        }
        return read;
    }

    @Override
    public int read(byte pBytes[], int pOffset, int pLength) throws IOException {
        checkOpen();

        int read = mRandomAccess.read(pBytes, pOffset, pLength);
        if (read > 0) {
            position += read;
        }
        return read;
    }

    /**
     * Does nothing, as we don't really do any caching here.
     *
     * @param pPosition the position to flush to
     */
    protected void flushBeforeImpl(long pPosition) {
    }

    protected void seekImpl(long pPosition) throws IOException {
        mRandomAccess.seek(pPosition);
    }
}
