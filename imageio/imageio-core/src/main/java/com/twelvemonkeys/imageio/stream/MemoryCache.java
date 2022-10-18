package com.twelvemonkeys.imageio.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static com.twelvemonkeys.lang.Validate.notNull;
import static java.lang.Math.min;

final class MemoryCache implements Cache {

    final static int BLOCK_SIZE = 1 << 13;

    private final List<byte[]> cache = new ArrayList<>();
    private final ReadableByteChannel channel;
    private long length;
    private long position;
    private long start;

    // TODO: Maybe get rid of this constructor, as we don't want to do this if we have a FileInputStream/FileChannel...
    MemoryCache(InputStream stream) {
        this(Channels.newChannel(notNull(stream, "stream")));
    }

    public MemoryCache(ReadableByteChannel channel) {
        this.channel = notNull(channel, "channel");
    }

    byte[] fetchBlock() throws IOException {
        long currPos = position;

        long index = currPos / BLOCK_SIZE;

        if (index >= Integer.MAX_VALUE) {
            throw new IOException("Memory cache max size exceeded");
        }

        while (index >= cache.size()) {
            byte[] block;
            try {
                block = new byte[BLOCK_SIZE];
            }
            catch (OutOfMemoryError e) {
                throw new IOException("No more memory for cache: " + cache.size() * BLOCK_SIZE);
            }

            cache.add(block);
            length += readBlock(block);
        }

        return cache.get((int) index);
    }

    private int readBlock(final byte[] block) throws IOException {
        ByteBuffer wrapped = ByteBuffer.wrap(block);

        while (wrapped.hasRemaining()) {
            int count = channel.read(wrapped);
            if (count == -1) {
                // Last block
                break;
            }
        }

        return wrapped.position();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        cache.clear();
    }

    @Override
    public int read(ByteBuffer dest) throws IOException {
        byte[] buffer = fetchBlock();
        int bufferPos = (int) (position % BLOCK_SIZE);

        if (position >= length) {
            return -1;
        }

        int len = min(dest.remaining(), (int) min(BLOCK_SIZE - bufferPos, length - position));
        dest.put(buffer, bufferPos, len);

        position += len;

        return len;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < start) {
            throw new IOException("Seek before flush position");
        }

        this.position = newPosition;

        return this;
    }

    @Override
    public long size() throws IOException {
        // We could allow the size to grow, but that means the stream cannot rely on this size, so we'll just pretend we don't know...
        return -1;
    }

    @Override
    public int write(ByteBuffer src) {
        throw new NonWritableChannelException();
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    @Override
    public void flushBefore(long pos) {
        if (pos < start) {
            throw new IndexOutOfBoundsException("pos < flushed position");
        }
        if (pos > position) {
            throw new IndexOutOfBoundsException("pos > current position");
        }

        int blocks = (int) (pos / BLOCK_SIZE); // Overflow guarded for in fetchBlock

        // Clear blocks no longer needed
        for (int i = 0; i < blocks; i++) {
            cache.set(i, null);
        }

        start = pos;
    }
}

