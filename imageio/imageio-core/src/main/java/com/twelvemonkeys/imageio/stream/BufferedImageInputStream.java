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

package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * A buffered {@code ImageInputStream}.
 * Experimental - seems to be effective for {@link javax.imageio.stream.FileImageInputStream}
 * and {@link javax.imageio.stream.FileCacheImageInputStream} when doing a lot of single-byte reads
 * (or short byte-array reads) on OS X at least.
 * Code that uses the {@code readFully} methods are not affected by the issue.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedFileImageInputStream.java,v 1.0 May 15, 2008 4:36:49 PM haraldk Exp$
 */
// TODO: Create a provider for this (wrapping the FileIIS and FileCacheIIS classes), and disable the Sun built-in spis?
// TODO: Test on other platforms, might be just an OS X issue
public final class BufferedImageInputStream extends ImageInputStreamImpl implements ImageInputStream {
    static final int DEFAULT_BUFFER_SIZE = 8192;

    private ImageInputStream stream;

    private ByteBuffer buffer;
    private ByteBuffer integralCache = ByteBuffer.allocate(8);

    public BufferedImageInputStream(final ImageInputStream pStream) throws IOException {
        this(pStream, DEFAULT_BUFFER_SIZE);
    }

    private BufferedImageInputStream(final ImageInputStream pStream, final int pBufferSize) throws IOException {
        stream = notNull(pStream, "stream");
        streamPos = pStream.getStreamPosition();
        buffer = ByteBuffer.allocate(pBufferSize);
        buffer.limit(0);
    }

    private void fillBuffer() throws IOException {
        buffer.clear();

        int length = stream.read(buffer.array(), 0, buffer.capacity());

        if (length >= 0) {
            buffer.position(length);
            buffer.flip();
        }
        else {
            buffer.limit(0);
        }
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
        super.setByteOrder(byteOrder);
        integralCache.order(byteOrder);
    }

    @Override
    public int read() throws IOException {
        checkClosed();

        if (!buffer.hasRemaining()) {
            fillBuffer();
        }

        if (!buffer.hasRemaining()) {
            return -1;
        }

        bitOffset = 0;
        streamPos++;

        return buffer.get() & 0xff;
    }

    @Override
    public int read(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
        checkClosed();
        bitOffset = 0;

        if (!buffer.hasRemaining()) {
            // Bypass buffer if buffer is empty for reads longer than buffer
            if (pLength >= buffer.capacity()) {
                return readDirect(pBuffer, pOffset, pLength);
            }
            else {
                fillBuffer();
            }
        }

        return readBuffered(pBuffer, pOffset, pLength);
    }

    private int readDirect(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
        // Invalidate the buffer, as its contents is no longer in sync with the stream's position.
        buffer.limit(0);
        int read = stream.read(pBuffer, pOffset, pLength);

        if (read > 0) {
            streamPos += read;
        }

        return read;
    }

    private int readBuffered(final byte[] pBuffer, final int pOffset, final int pLength) {
        if (!buffer.hasRemaining()) {
            return -1;
        }

        // Read as much as possible from buffer
        int length = Math.min(buffer.remaining(), pLength);

        if (length > 0) {
            int position = buffer.position();
            System.arraycopy(buffer.array(), position, pBuffer, pOffset, length);
            buffer.position(position + length);
        }

        streamPos += length;

        return length;
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
        readFully(integralCache.array(), 0, 2);

        return integralCache.getShort(0);
    }

    @Override
    public int readInt() throws IOException {
        readFully(integralCache.array(), 0, 4);

        return integralCache.getInt(0);
    }

    @Override
    public long readLong() throws IOException {
        readFully(integralCache.array(), 0, 8);

        return integralCache.getLong(0);
    }

    @Override
    public int readBit() throws IOException {
        checkClosed();

        if (!buffer.hasRemaining()) {
            fillBuffer();

            if (!buffer.hasRemaining()) {
                throw new EOFException();
            }
        }

        // Compute final bit offset before we call read() and seek()
        int newBitOffset = (this.bitOffset + 1) & 0x7;

        int val = buffer.get() & 0xff;

        if (newBitOffset != 0) {
            // Move byte position back if in the middle of a byte
            buffer.position(buffer.position() - 1);

            // Shift the bit to be read to the rightmost position
            val >>= 8 - newBitOffset;
        }
        else {
            streamPos++;
        }

        this.bitOffset = newBitOffset;

        return val & 0x1;
    }

    @Override
    public long readBits(int numBits) throws IOException {
        checkClosed();

        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException();
        }
        if (numBits == 0) {
            return 0L;
        }

        // Have to read additional bits on the left equal to the bit offset
        int bitsToRead = numBits + bitOffset;

        // Compute final bit offset before we call read() and seek()
        int newBitOffset = (this.bitOffset + numBits) & 0x7;

        // Read a byte at a time, accumulate
        long accum = 0L;
        while (bitsToRead > 0) {
            if (!buffer.hasRemaining()) {
                fillBuffer();

                if (!buffer.hasRemaining()) {
                    throw new EOFException();
                }
            }

            int val = buffer.get() & 0xff;

            accum <<= 8;
            accum |= val;
            bitsToRead -= 8;
        }

        // Move byte position back if in the middle of a byte
        if (newBitOffset != 0) {
            buffer.position(buffer.position() - 1);
        }
        else {
            streamPos++;
        }

        this.bitOffset = newBitOffset;

        // Shift away unwanted bits on the right.
        accum >>>= (-bitsToRead); // Negative of bitsToRead == extra bits read

        // Mask out unwanted bits on the left
        accum &= (-1L >>> (64 - numBits));

        return accum;
    }

    @Override
    public void seek(long pPosition) throws IOException {
        checkClosed();
        bitOffset = 0;

        if (streamPos == pPosition) {
            return;
        }

        // Optimized to not invalidate buffer if new position is within current buffer
        long newBufferPos = buffer.position() + pPosition - streamPos;
        if (newBufferPos >= 0 && newBufferPos <= buffer.limit()) {
            buffer.position((int) newBufferPos);
        }
        else {
            // Will invalidate buffer
            buffer.limit(0);
            stream.seek(pPosition);
        }

        streamPos = pPosition;
    }

    @Override
    public void flushBefore(long pos) throws IOException {
        checkClosed();
        stream.flushBefore(pos);
    }

    @Override
    public long getFlushedPosition() {
        return stream.getFlushedPosition();
    }

    @Override
    public boolean isCached() {
        return stream.isCached();
    }

    @Override
    public boolean isCachedMemory() {
        return stream.isCachedMemory();
    }

    @Override
    public boolean isCachedFile() {
        return stream.isCachedFile();
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            //stream.close();
            stream = null;
            buffer = null;
        }

        super.close();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public long length() {
        // WTF?! This method is allowed to throw IOException in the interface...
        try {
            return stream.length();
        }
        catch (IOException ignore) {
        }

        return -1;
    }
}
