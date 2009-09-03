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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code SeekableInputStream} implementation that caches data in memory.
 * <p/>
 *
 * @see FileCacheSeekableStream
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/MemoryCacheSeekableStream.java#3 $
 */
public final class MemoryCacheSeekableStream extends AbstractCachedSeekableStream {

    /**
     * Creates a {@code MemoryCacheSeekableStream}, reading from the given
     * {@code InputStream}. Data will be cached in memory.
     *
     * @param pStream the {@code InputStream} to read from.
     */
    public MemoryCacheSeekableStream(final InputStream pStream) {
        super(pStream, new MemoryCache());
    }

    public final boolean isCachedMemory() {
        return true;
    }

    public final boolean isCachedFile() {
        return false;
    }

    final static class MemoryCache extends StreamCache {
        final static int BLOCK_SIZE = 1 << 13;

        private final List<byte[]> mCache = new ArrayList<byte[]>();
        private long mLength;
        private long mPosition;
        private long mStart;

        private byte[] getBlock() throws IOException {
            final long currPos = mPosition - mStart;
            if (currPos < 0) {
                throw new IOException("StreamCache flushed before read position");
            }

            long index = currPos / BLOCK_SIZE;

            if (index >= Integer.MAX_VALUE) {
                throw new IOException("Memory cache max size exceeded");
            }

            if (index >= mCache.size()) {
                try {
                    mCache.add(new byte[BLOCK_SIZE]);
//                    System.out.println("Allocating new block, size: " + BLOCK_SIZE);
//                    System.out.println("New total size: " + mCache.size() * BLOCK_SIZE + " (" + mCache.size() + " blocks)");
                }
                catch (OutOfMemoryError e) {
                    throw new IOException("No more memory for cache: " + mCache.size() * BLOCK_SIZE);
                }
            }

            //System.out.println("index: " + index);

            return mCache.get((int) index);
        }

        public void write(final int pByte) throws IOException {
            byte[] buffer = getBlock();

            int idx = (int) (mPosition % BLOCK_SIZE);
            buffer[idx] = (byte) pByte;
            mPosition++;

            if (mPosition > mLength) {
                mLength = mPosition;
            }
        }

        // TODO: OptimizeMe!!!
        @Override
        public void write(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
            byte[] buffer = getBlock();
            for (int i = 0; i < pLength; i++) {
                int index = (int) mPosition % BLOCK_SIZE;
                if (index == 0) {
                    buffer = getBlock();
                }
                buffer[index] = pBuffer[pOffset + i];

                mPosition++;
            }
            if (mPosition > mLength) {
                mLength = mPosition;
            }
        }

        public int read() throws IOException {
            if (mPosition >= mLength) {
                return -1;
            }

            byte[] buffer = getBlock();

            int idx = (int) (mPosition % BLOCK_SIZE);
            mPosition++;

            return buffer[idx] & 0xff;
        }

        // TODO: OptimizeMe!!!
        @Override
        public int read(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
            if (mPosition >= mLength) {
                return -1;
            }

            byte[] buffer = getBlock();

            int bufferPos = (int) (mPosition % BLOCK_SIZE);

            // Find maxIdx and simplify test in for-loop
            int maxLen = (int) Math.min(Math.min(pLength, buffer.length - bufferPos), mLength - mPosition);

            int i;
            //for (i = 0; i < pLength && i < buffer.length - idx && i < mLength - mPosition; i++) {
            for (i = 0; i < maxLen; i++) {
                pBytes[pOffset + i] = buffer[bufferPos + i];
            }

            mPosition += i;

            return i;
        }

        public void seek(final long pPosition) throws IOException {
            if (pPosition < mStart) {
                throw new IOException("Seek before flush position");
            }
            mPosition = pPosition;
        }

        @Override
        public void flush(final long pPosition) {
            int firstPos = (int) (pPosition / BLOCK_SIZE) - 1;

            for (int i = 0; i < firstPos; i++) {
                mCache.remove(0);
            }

            mStart = pPosition;
        }

        public long getPosition() {
            return mPosition;
        }
    }
}
