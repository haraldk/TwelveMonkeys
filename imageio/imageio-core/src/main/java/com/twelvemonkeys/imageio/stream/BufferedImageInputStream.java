package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.nio.ByteBuffer;

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
            try {
                buffer.position(length);
            }
            catch (IllegalArgumentException e) {
                System.err.println("length: " + length);
                throw e;
            }
            buffer.flip();
        }
        else {
            buffer.limit(0);
        }
    }


    @Override
    public int read() throws IOException {
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
        bitOffset = 0;

        if (!buffer.hasRemaining()) {
            // Bypass cache if cache is empty for reads longer than buffer
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
        // TODO: Figure out why reading more than the buffer length causes alignment issues...
        int read = stream.read(pBuffer, pOffset, Math.min(buffer.capacity(), pLength));

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

    @Override
    public void seek(long pPosition) throws IOException {
        // TODO: Could probably be optimized to not invalidate buffer if new position is within current buffer
        stream.seek(pPosition);
        buffer.limit(0); // Will invalidate buffer
        streamPos = stream.getStreamPosition();
    }

    @Override
    public void flushBefore(long pos) throws IOException {
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
