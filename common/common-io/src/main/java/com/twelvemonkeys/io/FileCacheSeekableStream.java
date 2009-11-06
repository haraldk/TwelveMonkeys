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

import com.twelvemonkeys.lang.Validate;

import java.io.*;

/**
 * A {@code SeekableInputStream} implementation that caches data in a temporary {@code File}.
 * <p/>
 * Temporary files are created as specified in {@link File#createTempFile(String, String, java.io.File)}.
 *
 * @see MemoryCacheSeekableStream
 * @see FileSeekableStream
 *
 * @see File#createTempFile(String, String)
 * @see RandomAccessFile
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/FileCacheSeekableStream.java#5 $
 */
public final class FileCacheSeekableStream extends AbstractCachedSeekableStream {

//    private final InputStream mStream;
//    private final RandomAccessFile mCache;
    private  byte[] mBuffer;

    /** The stream positon in the backing stream (mStream) */
//    private long mStreamPosition;

    // TODO: getStreamPosition() should always be the same as
    // mCache.getFilePointer()
    // otherwise there's some inconsistency here... Enforce this?

    /**
     * Creates a {@code FileCacheSeekableStream} reading from the given
     * {@code InputStream}. Data will be cached in a temporary file.
     *
     * @param pStream the {@code InputStream} to read from
     *
     * @throws IOException if the temporary file cannot be created,
     *          or cannot be opened for random access.
     */
    public FileCacheSeekableStream(final InputStream pStream) throws IOException {
        this(pStream, "iocache", null);
    }

    /**
     * Creates a {@code FileCacheSeekableStream} reading from the given
     * {@code InputStream}. Data will be cached in a temporary file, with
     * the given base name.
     *
     * @param pStream the {@code InputStream} to read from
     * @param pTempBaseName optional base name for the temporary file
     *
     * @throws IOException if the temporary file cannot be created,
     *          or cannot be opened for random access.
     */
    public FileCacheSeekableStream(final InputStream pStream, final String pTempBaseName) throws IOException {
        this(pStream, pTempBaseName, null);
    }

    /**
     * Creates a {@code FileCacheSeekableStream} reading from the given
     * {@code InputStream}. Data will be cached in a temporary file, with
     * the given base name, in the given directory
     *
     * @param pStream the {@code InputStream} to read from
     * @param pTempBaseName optional base name for the temporary file
     * @param pTempDir optional temp directory
     *
     * @throws IOException if the temporary file cannot be created,
     *          or cannot be opened for random access.
     */
    public FileCacheSeekableStream(final InputStream pStream, final String pTempBaseName, final File pTempDir) throws IOException {
        // NOTE: We do validation BEFORE we create temp file, to avoid orphan files
        this(Validate.notNull(pStream, "stream"), createTempFile(pTempBaseName, pTempDir));
    }

    /*protected*/ static File createTempFile(String pTempBaseName, File pTempDir) throws IOException {
        Validate.notNull(pTempBaseName, "tempBaseName");

        File file = File.createTempFile(pTempBaseName, null, pTempDir);
        file.deleteOnExit();

        return file;
    }

    // TODO: Consider exposing this for external use
    /*protected*/ FileCacheSeekableStream(final InputStream pStream, final File pFile) throws FileNotFoundException {
        super(pStream, new FileCache(pFile));

        // TODO: Allow for custom buffer sizes?
        mBuffer = new byte[1024];
    }

    public final boolean isCachedMemory() {
        return false;
    }

    public final boolean isCachedFile() {
        return true;
    }

    @Override
    protected void closeImpl() throws IOException {
        super.closeImpl();
        mBuffer = null;
    }
/*
    public final boolean isCached() {
        return true;
    }

    // InputStream overrides
    @Override
    public int available() throws IOException {
        long avail = mStreamPosition - mPosition + mStream.available();
        return avail > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) avail;
    }

    public void closeImpl() throws IOException {
        mStream.close();
        mCache.close();

        // TODO: Delete cache file here?
        // ThreadPool.invokeLater(new DeleteFileAction(mCacheFile));
    }
    */

    @Override
    public int read() throws IOException {
        checkOpen();

        int read;
        if (mPosition == mStreamPosition) {
            // Read ahead into buffer, for performance
            read = readAhead(mBuffer, 0, mBuffer.length);
            if (read >= 0) {
                read = mBuffer[0] & 0xff;
            }

            //System.out.println("Read 1 byte from stream: " + Integer.toHexString(read & 0xff));
        }
        else {
            // ..or read byte from the cache
            syncPosition();
            read = getCache().read();

            //System.out.println("Read 1 byte from cache: " + Integer.toHexString(read & 0xff));
        }

        // TODO: This field is not REALLY considered accessible.. :-P
        if (read != -1) {
            mPosition++;
        }
        return read;
    }

    @Override
    public int read(byte[] pBytes, int pOffset, int pLength) throws IOException {
        checkOpen();

        int length;
        if (mPosition == mStreamPosition) {
            // Read bytes from the stream
            length = readAhead(pBytes, pOffset, pLength);

            //System.out.println("Read " + length + " byte from stream");
        }
        else {
            // ...or read bytes from the cache
            syncPosition();
            length = getCache().read(pBytes, pOffset, (int) Math.min(pLength, mStreamPosition - mPosition));

            //System.out.println("Read " + length + " byte from cache");
        }

        // TODO: This field is not REALLY considered accessible.. :-P
        if (length > 0) {
            mPosition += length;
        }
        return length;
    }

    private int readAhead(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        int length;
        length = mStream.read(pBytes, pOffset, pLength);

        if (length > 0) {
            mStreamPosition += length;
            getCache().write(pBytes, pOffset, length);
        }
        return length;
    }

    /*
    private void syncPosition() throws IOException {
        if (mCache.getFilePointer() != mPosition) {
            mCache.seek(mPosition); // Assure EOF is correctly thrown
        }
    }

    // Seekable overrides

    protected void flushBeforeImpl(long pPosition) {
        // TODO: Implement
        // For now, it's probably okay to do nothing, this is just for
        // performance (as long as people follow spec, not behaviour)
    }

    protected void seekImpl(long pPosition) throws IOException {
        if (mStreamPosition < pPosition) {
            // Make sure we append at end of cache
            if (mCache.getFilePointer() != mStreamPosition) {
                mCache.seek(mStreamPosition);
            }

            // Read diff from stream into cache
            long left = pPosition - mStreamPosition;
            int bufferLen = left > 1024 ? 1024 : (int) left;
            byte[] buffer = new byte[bufferLen];

            while (left > 0) {
                int length = buffer.length < left ? buffer.length : (int) left;
                int read = mStream.read(buffer, 0, length);

                if (read > 0) {
                    mCache.write(buffer, 0, read);
                    mStreamPosition += read;
                    left -= read;
                }
                else if (read < 0) {
                    break;
                }
            }
        }
        else if (mStreamPosition >= pPosition) {
            // Seek backwards into the cache
            mCache.seek(pPosition);
        }

//        System.out.println("pPosition: " + pPosition);
//        System.out.println("mStreamPosition: " + mStreamPosition);
//        System.out.println("mCache.getFilePointer(): " + mCache.getFilePointer());

        // NOTE: If mPosition == pPosition then we're good to go
    }
    */

    final static class FileCache extends StreamCache {
        private RandomAccessFile mCacheFile;

        public FileCache(final File pFile) throws FileNotFoundException {
            Validate.notNull(pFile, "file");
            mCacheFile = new RandomAccessFile(pFile, "rw");
        }

        public void write(final int pByte) throws IOException {
            mCacheFile.write(pByte);
        }

        @Override
        public void write(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
            mCacheFile.write(pBuffer, pOffset, pLength);
        }

        public int read() throws IOException {
            return mCacheFile.read();
        }

        @Override
        public int read(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
            return mCacheFile.read(pBuffer, pOffset, pLength);
        }

        public void seek(final long pPosition) throws IOException {
            mCacheFile.seek(pPosition);
        }

        public long getPosition() throws IOException {
            return mCacheFile.getFilePointer();
        }
    }

}
