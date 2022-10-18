/*
 * Copyright (c) 2022, Harald Kuhr
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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static com.twelvemonkeys.lang.Validate.notNull;
import static java.lang.Math.max;

/**
 * A buffered {@link javax.imageio.stream.ImageInputStream} that is backed by a {@link java.nio.channels.SeekableByteChannel}
 * and provides greatly improved performance
 * compared to {@link javax.imageio.stream.FileCacheImageInputStream} or {@link javax.imageio.stream.MemoryCacheImageInputStream}
 * for shorter reads, like single byte or bit reads.
 */
final class BufferedChannelImageInputStream extends ImageInputStreamImpl {
    private static final Closeable CLOSEABLE_STUB = new Closeable() {
        @Override public void close() {}
    };

    static final int DEFAULT_BUFFER_SIZE = 8192;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    private byte[] buffer = byteBuffer.array();
    private int bufferPos;
    private int bufferLimit;

    private final ByteBuffer integralCache = ByteBuffer.allocate(8);
    private final byte[] integralCacheArray = integralCache.array();

    private SeekableByteChannel channel;
    private Closeable closeable;

    /**
     * Constructs a {@code BufferedChannelImageInputStream} that will read from a given {@code File}.
     *
     * @param file a {@code File} to read from.
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws SecurityException        if a security manager is installed, and it denies read access to the file.
     * @throws IOException              if an I/O error occurs while opening the file.
     */
    public BufferedChannelImageInputStream(final File file) throws IOException {
        this(notNull(file, "file").toPath());
    }

    /**
     * Constructs a {@code BufferedChannelImageInputStream} that will read from a given {@code Path}.
     *
     * @param file a {@code Path} to read from.
     * @throws IllegalArgumentException      if {@code file} is {@code null}.
     * @throws UnsupportedOperationException if the {@code file} is associated with a provider that does not support creating file channels.
     * @throws IOException                   if an I/O error occurs while opening the file.
     * @throws SecurityException             if a security manager is installed, and it denies read access to the file.
     */
    public BufferedChannelImageInputStream(final Path file) throws IOException {
        this(FileChannel.open(notNull(file, "file"), StandardOpenOption.READ), true);
    }

    /**
     * Constructs a {@code BufferedChannelImageInputStream} that will read from a given {@code RandomAccessFile}.
     *
     * @param file a {@code RandomAccessFile} to read from.
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     */
    public BufferedChannelImageInputStream(final RandomAccessFile file) {
        // Closing the RAF is inconsistent, but emulates the behavior of javax.imageio.stream.FileImageInputStream
        this(notNull(file, "file").getChannel(), true);
    }

    /**
     * Constructs a {@code BufferedChannelImageInputStream} that will read from a given {@code FileInputStream}.
     * <p>
     * Closing this stream will <em>not</em> close the {@code FileInputStream}.
     * </p>
     *
     * @param inputStream a {@code FileInputStream} to read from.
     * @throws IllegalArgumentException if {@code inputStream} is {@code null}.
     */
    public BufferedChannelImageInputStream(final FileInputStream inputStream) {
        this(notNull(inputStream, "inputStream").getChannel(), false);
    }

    /**
     * Constructs a {@code BufferedChannelImageInputStream} that will read from a given {@code SeekableByteChannel}.
     * <p>
     * Closing this stream will <em>not</em> close the {@code SeekableByteChannel}.
     * </p>
     *
     * @param channel a {@code SeekableByteChannel} to read from.
     * @throws IllegalArgumentException if {@code channel} is {@code null}.
     */
    public BufferedChannelImageInputStream(final SeekableByteChannel channel) {
        this(notNull(channel, "channel"), false);
    }

    /**
     * Constructs a {@code BufferedChannelImageInputStream} that will read from a given {@code Cache}.
     * <p>
     * Closing this stream will close the {@code Cache}.
     * </p>
     *
     * @param cache a {@code SeekableByteChannel} to read from.
     * @throws IllegalArgumentException if {@code channel} is {@code null}.
     */
    BufferedChannelImageInputStream(final Cache cache) {
        this(notNull(cache, "cache"), true);
    }

    private BufferedChannelImageInputStream(final SeekableByteChannel channel, boolean closeChannelOnClose) {
        this.channel = notNull(channel, "channel");
        this.closeable = closeChannelOnClose ? this.channel : CLOSEABLE_STUB;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean fillBuffer() throws IOException {
        byteBuffer.rewind();
        int length = channel.read(byteBuffer);
        bufferPos = 0;
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
    public int read(final byte[] bytes, final int offset, final int length) throws IOException {
        checkClosed();
        bitOffset = 0;

        if (bufferEmpty()) {
            // Bypass buffer if buffer is empty for reads longer than buffer
            if (length >= buffer.length) {
                return readDirect(bytes, offset, length);
            }
            else if (!fillBuffer()) {
                return -1;
            }
        }

        int fromBuffer = readBuffered(bytes, offset, length);

        if (length > fromBuffer) {
            // Due to known bugs in certain JDK-bundled ImageIO plugins expecting read to behave as readFully,
            // we'll read as much as possible from the buffer, and the rest directly after
            return fromBuffer + max(0, readDirect(bytes, offset + fromBuffer, length - fromBuffer));
        }

        return fromBuffer;
    }

    private int readDirect(final byte[] bytes, final int offset, final int length) throws IOException {
        // Invalidate the buffer, as its contents is no longer in sync with the stream's position.
        bufferLimit = 0;

        ByteBuffer wrapped = ByteBuffer.wrap(bytes, offset, length);
        int read = 0;
        while (wrapped.hasRemaining()) {
            int count = channel.read(wrapped);
            if (count == -1) {
                if (read == 0) {
                    return -1;
                }

                break;
            }

            read += count;
        }

        streamPos += read;

        return read;
    }

    private int readBuffered(final byte[] bytes, final int offset, final int length) {
        // Read as much as possible from buffer
        int available = Math.min(bufferLimit - bufferPos, length);

        if (available > 0) {
            System.arraycopy(buffer, bufferPos, bytes, offset, available);
            bufferPos += available;
            streamPos += available;
        }

        return available;
    }

    public long length() {
        // WTF?! This method is allowed to throw IOException in the interface...
        try {
            checkClosed();
            return channel.size();
        }
        catch (IOException ignore) {
        }

        return -1;
    }

    public void close() throws IOException {
        super.close();

        buffer = null;
        byteBuffer = null;

        channel = null;

        try {
            closeable.close();
        }
        finally {
            closeable = null;
        }
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
        if (newBufferPos >= 0 && newBufferPos < bufferLimit) {
            bufferPos = (int) newBufferPos;
        }
        else {
            // Will invalidate buffer
            bufferLimit = 0;
            channel.position(position);
        }

        streamPos = position;
    }

    @Override
    public void flushBefore(final long pos) throws IOException {
        super.flushBefore(pos);

        if (channel instanceof Cache) {
            // In case of memory cache, free up memory
            ((Cache) channel).flushBefore(pos);
        }
    }
}
