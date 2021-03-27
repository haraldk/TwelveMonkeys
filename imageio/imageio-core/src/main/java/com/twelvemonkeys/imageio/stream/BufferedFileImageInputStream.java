/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.twelvemonkeys.lang.Validate.notNull;
import static java.lang.Math.max;

/**
 * A buffered replacement for {@link javax.imageio.stream.FileImageInputStream}
 * that provides greatly improved performance for shorter reads, like single
 * byte or bit reads.
 * As with {@code javax.imageio.stream.FileImageInputStream}, either
 * {@link File} or {@link RandomAccessFile} can be used as input.
 *
 * @see javax.imageio.stream.FileImageInputStream
 */
// TODO: Create a memory-mapped version?
//  Or not... From java.nio.channels.FileChannel.map:
//      For most operating systems, mapping a file into memory is more
//      expensive than reading or writing a few tens of kilobytes of data via
//      the usual {@link #read read} and {@link #write write} methods.  From the
//      standpoint of performance it is generally only worth mapping relatively
//      large files into memory.
public final class BufferedFileImageInputStream extends ImageInputStreamImpl {
    static final int DEFAULT_BUFFER_SIZE = 8192;

    private byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    private int bufferPos;
    private int bufferLimit;

    private final ByteBuffer integralCache = ByteBuffer.allocate(8);
    private final byte[] integralCacheArray = integralCache.array();

    private RandomAccessFile raf;

    /**
     * Constructs a <code>BufferedFileImageInputStream</code> that will read from a given <code>File</code>.
     *
     * @param file a <code>File</code> to read from.
     * @throws IllegalArgumentException if <code>file</code> is <code>null</code>.
     * @throws FileNotFoundException    if <code>file</code> is a directory or cannot be opened for reading
     *                                  for any reason.
     */
    public BufferedFileImageInputStream(final File file) throws FileNotFoundException {
        this(new RandomAccessFile(notNull(file, "file"), "r"));
    }

    /**
     * Constructs a <code>BufferedFileImageInputStream</code> that will read from a given <code>RandomAccessFile</code>.
     *
     * @param raf a <code>RandomAccessFile</code> to read from.
     * @throws IllegalArgumentException if <code>raf</code> is <code>null</code>.
     */
    public BufferedFileImageInputStream(final RandomAccessFile raf) {
        this.raf = notNull(raf, "raf");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean fillBuffer() throws IOException {
        bufferPos = 0;
        int length = raf.read(buffer, 0, buffer.length);
        bufferLimit = max(length, 0);

        return bufferLimit > 0;
    }

    private boolean bufferEmpty() {
        return bufferPos >= bufferLimit;
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
        super.setByteOrder(byteOrder);
        integralCache.order(byteOrder);
    }

    @Override
    public int read() throws IOException {
        checkClosed();

        if (bufferEmpty() && !fillBuffer()) {
            return -1;
        }

        bitOffset = 0;
        streamPos++;

        return buffer[bufferPos++] & 0xff;
    }

    @Override
    public int read(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
        checkClosed();
        bitOffset = 0;

        if (bufferEmpty()) {
            // Bypass buffer if buffer is empty for reads longer than buffer
            if (pLength >= buffer.length) {
                return readDirect(pBuffer, pOffset, pLength);
            }
            else if (!fillBuffer()) {
                return -1;
            }
        }

        return readBuffered(pBuffer, pOffset, pLength);
    }

    private int readDirect(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
        // Invalidate the buffer, as its contents is no longer in sync with the stream's position.
        bufferLimit = 0;
        int read = raf.read(pBuffer, pOffset, pLength);

        if (read > 0) {
            streamPos += read;
        }

        return read;
    }

    private int readBuffered(final byte[] pBuffer, final int pOffset, final int pLength) {
        // Read as much as possible from buffer
        int length = Math.min(bufferLimit - bufferPos, pLength);

        if (length > 0) {
            System.arraycopy(buffer, bufferPos, pBuffer, pOffset, length);
            bufferPos += length;
            streamPos += length;
        }

        return length;
    }

    public long length() {
        // WTF?! This method is allowed to throw IOException in the interface...
        try {
            checkClosed();
            return raf.length();
        }
        catch (IOException ignore) {
        }

        return -1;
    }

    public void close() throws IOException {
        super.close();

        raf.close();

        raf = null;
        buffer = null;
    }

    // Need to override the readShort(), readInt() and readLong() methods,
    // because the implementations in ImageInputStreamImpl expects the
    // read(byte[], int, int) to always read the expected number of bytes,
    // causing uninitialized values, alignment issues and EOFExceptions at
    // random places...
    // Notes:
    // * readUnsignedXx() is covered by their signed counterparts
    // * readChar() is covered by readShort()
    // * readFloat() and readDouble() is covered by readInt() and readLong()
    //   respectively.
    // * readLong() may be covered by two readInt()s, we'll override to be safe

    @Override
    public short readShort() throws IOException {
        readFully(integralCacheArray, 0, 2);

        return integralCache.getShort(0);
    }

    @Override
    public int readInt() throws IOException {
        readFully(integralCacheArray, 0, 4);

        return integralCache.getInt(0);
    }

    @Override
    public long readLong() throws IOException {
        readFully(integralCacheArray, 0, 8);

        return integralCache.getLong(0);
    }

    @Override
    public void seek(long position) throws IOException {
        checkClosed();

        if (position < flushedPos) {
            throw new IndexOutOfBoundsException("position < flushedPos!");
        }

        bitOffset = 0;

        if (streamPos == position) {
            return;
        }

        // Optimized to not invalidate buffer if new position is within current buffer
        long newBufferPos = bufferPos + position - streamPos;
        if (newBufferPos >= 0 && newBufferPos <= bufferLimit) {
            bufferPos = (int) newBufferPos;
        }
        else {
            // Will invalidate buffer
            bufferLimit = 0;
            raf.seek(position);
        }

        streamPos = position;
    }
}
