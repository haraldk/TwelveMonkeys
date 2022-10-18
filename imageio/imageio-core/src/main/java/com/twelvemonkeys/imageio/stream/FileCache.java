package com.twelvemonkeys.imageio.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;
import static java.lang.Math.max;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

// Note: We could consider creating a memory-mapped version...
// But, from java.nio.channels.FileChannel.map:
//      For most operating systems, mapping a file into memory is more
//      expensive than reading or writing a few tens of kilobytes of data via
//      the usual {@link #read read} and {@link #write write} methods.  From the
//      standpoint of performance it is generally only worth mapping relatively
//      large files into memory.
final class FileCache implements Cache {
    final static int BLOCK_SIZE = 1 << 13;

    private final FileChannel cache;
    private final ReadableByteChannel channel;

    // TODO: Perhaps skip this constructor?
    FileCache(InputStream stream, File cacheDir) throws IOException {
        // Stream will be closed with channel, documented behavior
        this(Channels.newChannel(notNull(stream, "stream")), cacheDir);
    }

    public FileCache(ReadableByteChannel channel, File cacheDir) throws IOException {
        this.channel = notNull(channel, "channel");
        isTrue(cacheDir == null || cacheDir.isDirectory(), cacheDir, "%s is not a directory");

        // Create a temp file to hold our cache,
        // will be deleted when this channel is closed, as we close the cache
        Path cacheFile = cacheDir == null
                         ? Files.createTempFile("imageio", ".tmp")
                         : Files.createTempFile(cacheDir.toPath(), "imageio", ".tmp");

        cache = FileChannel.open(cacheFile, DELETE_ON_CLOSE, READ, WRITE);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    void fetch() throws IOException {
        while (cache.position() >= cache.size() && cache.transferFrom(channel, cache.size(), max(cache.position() - cache.size(), BLOCK_SIZE)) > 0) {
            // Continue transfer...
        }
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        cache.close();
    }

    @Override
    public int read(ByteBuffer dest) throws IOException {
        fetch();

        if (cache.position() >= cache.size()) {
            return -1;
        }

        return cache.read(dest);
    }

    @Override
    public long position() throws IOException {
        return cache.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        cache.position(newPosition);
        return this;
    }

    @Override
    public long size() {
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

    @Override public void flushBefore(long pos) {
    }
}

