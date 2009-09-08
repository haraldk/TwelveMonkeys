package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

/**
 * BufferedFileImageInputStream
 * Experimental - seems to be effective for FileImageInputStream and FileCacheImageInputStream.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedFileImageInputStream.java,v 1.0 May 15, 2008 4:36:49 PM haraldk Exp$
 */
// TODO: Create a provider for this (wrapping the FileIIS and FileCacheIIS classes), and disable the Sun built-in spis?
public final class BufferedImageInputStream extends ImageInputStreamImpl implements ImageInputStream {

   static final int DEFAULT_BUFFER_SIZE = 8192;

    private ImageInputStream mStream;

    private byte[] mBuffer;
    private long mBufferStart = 0;
    private int mBufferPos = 0;
    private int mBufferLength = 0;

    public BufferedImageInputStream(final ImageInputStream pStream) throws IOException {
        this(pStream, DEFAULT_BUFFER_SIZE);
    }

    private BufferedImageInputStream(final ImageInputStream pStream, final int pBufferSize) throws IOException {
        Validate.notNull(pStream, "stream");

        mStream = pStream;
        streamPos = pStream.getStreamPosition();
        mBuffer = new byte[pBufferSize];
    }

    private void fillBuffer() throws IOException {
        mBufferStart = streamPos;
        mBufferLength = mStream.read(mBuffer, 0, mBuffer.length);
        mBufferPos = 0;
    }

    private boolean isBufferValid() throws IOException {
        return mBufferPos < mBufferLength && mBufferStart == mStream.getStreamPosition() - mBufferLength;
    }

    @Override
    public int read() throws IOException {
        if (!isBufferValid()) {
            fillBuffer();
        }
        if (mBufferLength <= 0) {
            return -1;
        }

        bitOffset = 0;
        streamPos++;

        return mBuffer[mBufferPos++] & 0xff;
    }

    @Override
    public int read(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
        bitOffset = 0;

        // TODO: Consider fixing the bypass buffer code...
        if (!isBufferValid()) {
            // Bypass cache if cache is empty for reads longer than buffer
            if (pLength >= mBuffer.length) {
                return readDirect(pBuffer, pOffset, pLength);
            }
            else {
                fillBuffer();
            }
        }

        return readBuffered(pBuffer, pOffset, pLength);
    }

    private int readDirect(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
//        System.err.println("BEFORE                   streamPos: " + streamPos);
//        System.err.println("BEFORE mStream.getStreamPosition(): " + mStream.getStreamPosition());

        int read = mStream.read(pBuffer, pOffset, Math.min(mBuffer.length, pLength));

        if (read > 0) {
            streamPos += read;
        }

//        System.err.println("AFTER                    streamPos: " + streamPos);
//        System.err.println("AFTER  mStream.getStreamPosition(): " + mStream.getStreamPosition());
//        System.err.println();
 

        mBufferStart = mStream.getStreamPosition();
        mBufferLength = 0;

        return read;
    }


    private int readBuffered(final byte[] pBuffer, final int pOffset, final int pLength) {
        if (mBufferLength <= 0) {
            return -1;
        }

        // Read as much as possible from buffer
        int length = Math.min(mBufferLength - mBufferPos, pLength);

        if (length > 0) {
            System.arraycopy(mBuffer, mBufferPos, pBuffer, pOffset, length);
            mBufferPos += length;
        }

        streamPos += length;

        return length;
    }

    @Override
    public void seek(long pPosition) throws IOException {
        // TODO: Could probably be optimized to not invalidate buffer if new pos is within current buffer
        mStream.seek(pPosition);
        mBufferLength = 0; // Will invalidate buffer
        streamPos = mStream.getStreamPosition();
    }

    @Override
    public void flushBefore(long pos) throws IOException {
        mStream.flushBefore(pos);
    }

    @Override
    public long getFlushedPosition() {
        return mStream.getFlushedPosition();
    }

    @Override
    public boolean isCached() {
        return mStream.isCached();
    }

    @Override
    public boolean isCachedMemory() {
        return mStream.isCachedMemory();
    }

    @Override
    public boolean isCachedFile() {
        return mStream.isCachedFile();
    }

    @Override
    public void close() throws IOException {
        if (mStream != null) {
            mStream.close();
            mStream = null;
            mBuffer = null;
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
            return mStream.length();
        }
        catch (IOException e) {
            throw unchecked(e, RuntimeException.class);
        }
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    private <T extends Throwable> T unchecked(IOException pExcption, Class<T> pClass) {
        // Ugly hack to fool the compiler..
        return (T) pExcption;
    }
}
