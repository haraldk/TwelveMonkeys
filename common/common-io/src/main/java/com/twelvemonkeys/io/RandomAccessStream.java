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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.EOFException;

/**
 * A data stream that is both readable and writable, much like a
 * {@code RandomAccessFile}, except it may be backed by something other than a file.
 * <p/>
 *
 * @see java.io.RandomAccessFile
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/RandomAccessStream.java#3 $
 */
public abstract class RandomAccessStream implements Seekable, DataInput, DataOutput {
    // TODO: Use a RandomAcceessFile as backing in impl, probably
    // TODO: Create an in-memory implementation too?
    // TODO: Package private SeekableDelegate?

    // TODO: Both read and write must update stream position
    //private int position = -1;

    /** This random access stream, wrapped in an {@code InputStream} */
    SeekableInputStream inputView = null;
    /** This random access stream, wrapped in an {@code OutputStream} */
    SeekableOutputStream outputView = null;

    // TODO: Create an Input and an Output interface matching InputStream and OutputStream?
    public int read() throws IOException {
        try {
            return readByte() & 0xff;
        }
        catch (EOFException e) {
            return -1;
        }
    }

    public int read(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        if (pBytes == null) {
            throw new NullPointerException("bytes == null");
        }
        else if ((pOffset < 0) || (pOffset > pBytes.length) || (pLength < 0) ||
                ((pOffset + pLength) > pBytes.length) || ((pOffset + pLength) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        else if (pLength == 0) {
            return 0;
        }

        // Special case, allready at EOF
        int c = read();
        if (c == -1) {
            return -1;
        }

        // Otherwise, read as many as bytes as possible
        pBytes[pOffset] = (byte) c;

        int i = 1;
        try {
            for (; i < pLength; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                pBytes[pOffset + i] = (byte) c;
            }
        }
        catch (IOException ignore) {
            // Ignore exception, just return length
        }

        return i;
    }

    public final int read(byte[] pBytes) throws IOException {
        return read(pBytes, 0, pBytes != null ? pBytes.length : 1);
    }

    /**
     * Returns an input view of this {@code RandomAccessStream}.
     * Invoking this method several times, will return the same object.
     * <p/>
     * <em>Note that read access is NOT synchronized.</em>
     *
     * @return a {@code SeekableInputStream} reading from this stream
     */
    public final SeekableInputStream asInputStream() {
        if (inputView == null) {
             inputView = new InputStreamView(this);
        }
        return inputView;
    }

    /**
     * Returns an output view of this {@code RandomAccessStream}.
     * Invoking this method several times, will return the same object.
     * <p/>
     * <em>Note that write access is NOT synchronized.</em>
     *
     * @return a {@code SeekableOutputStream} writing to this stream
     */
    public final SeekableOutputStream asOutputStream() {
        if (outputView == null) {
            outputView = new OutputStreamView(this);
        }
        return outputView;
    }

    static final class InputStreamView extends SeekableInputStream {
        // TODO: Consider adding synchonization (on stream) for all operations
        // TODO: Is is a good thing that close/flush etc works on stream?
        //  - Or should it rather just work on the views?
        //  - Allow multiple views?

        final private RandomAccessStream mStream;

        public InputStreamView(RandomAccessStream pStream) {
            if (pStream == null) {
                throw new IllegalArgumentException("stream == null");
            }
            mStream = pStream;
        }

        public boolean isCached() {
            return mStream.isCached();
        }

        public boolean isCachedFile() {
            return mStream.isCachedFile();
        }

        public boolean isCachedMemory() {
            return mStream.isCachedMemory();
        }

        protected void closeImpl() throws IOException {
            mStream.close();
        }

        protected void flushBeforeImpl(long pPosition) throws IOException {
            mStream.flushBefore(pPosition);
        }

        protected void seekImpl(long pPosition) throws IOException {
            mStream.seek(pPosition);
        }

        public int read() throws IOException {
            return mStream.read();
        }

        @Override
        public int read(byte pBytes[], int pOffset, int pLength) throws IOException {
            return mStream.read(pBytes, pOffset, pLength);
        }
    }

    static final class OutputStreamView extends SeekableOutputStream {
        // TODO: Consider adding synchonization (on stream) for all operations
        // TODO: Is is a good thing that close/flush etc works on stream?
        //  - Or should it rather just work on the views?
        //  - Allow multiple views?

        final private RandomAccessStream mStream;

        public OutputStreamView(RandomAccessStream pStream) {
            if (pStream == null) {
                throw new IllegalArgumentException("stream == null");
            }
            mStream = pStream;
        }

        public boolean isCached() {
            return mStream.isCached();
        }

        public boolean isCachedFile() {
            return mStream.isCachedFile();
        }

        public boolean isCachedMemory() {
            return mStream.isCachedMemory();
        }

        protected void closeImpl() throws IOException {
            mStream.close();
        }

        protected void flushBeforeImpl(long pPosition) throws IOException {
            mStream.flushBefore(pPosition);
        }

        protected void seekImpl(long pPosition) throws IOException {
            mStream.seek(pPosition);
        }

        public void write(int pByte) throws IOException {
            mStream.write(pByte);
        }

        @Override
        public void write(byte pBytes[], int pOffset, int pLength) throws IOException {
            mStream.write(pBytes, pOffset, pLength);
        }
    }
}
