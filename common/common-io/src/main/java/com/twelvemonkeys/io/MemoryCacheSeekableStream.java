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

        private final List<byte[]> cache = new ArrayList<>();
        private long length;
        private long position;
        private long start;

        private byte[] getBlock() throws IOException {
            final long currPos = position - start;
            if (currPos < 0) {
                throw new IOException("StreamCache flushed before read position");
            }

            long index = currPos / BLOCK_SIZE;

            if (index >= Integer.MAX_VALUE) {
                throw new IOException("Memory cache max size exceeded");
            }

            if (index >= cache.size()) {
                try {
                    cache.add(new byte[BLOCK_SIZE]);
//                    System.out.println("Allocating new block, size: " + BLOCK_SIZE);
//                    System.out.println("New total size: " + cache.size() * BLOCK_SIZE + " (" + cache.size() + " blocks)");
                }
                catch (OutOfMemoryError e) {
                    throw new IOException("No more memory for cache: " + cache.size() * BLOCK_SIZE);
                }
            }

            //System.out.println("index: " + index);

            return cache.get((int) index);
        }

        public void write(final int pByte) throws IOException {
            byte[] buffer = getBlock();

            int idx = (int) (position % BLOCK_SIZE);
            buffer[idx] = (byte) pByte;
            position++;

            if (position > length) {
                length = position;
            }
        }

        // TODO: OptimizeMe!!!
        @Override
        public void write(final byte[] pBuffer, final int pOffset, final int pLength) throws IOException {
            byte[] buffer = getBlock();
            for (int i = 0; i < pLength; i++) {
                int index = (int) position % BLOCK_SIZE;
                if (index == 0) {
                    buffer = getBlock();
                }
                buffer[index] = pBuffer[pOffset + i];

                position++;
            }
            if (position > length) {
                length = position;
            }
        }

        public int read() throws IOException {
            if (position >= length) {
                return -1;
            }

            byte[] buffer = getBlock();

            int idx = (int) (position % BLOCK_SIZE);
            position++;

            return buffer[idx] & 0xff;
        }

        // TODO: OptimizeMe!!!
        @Override
        public int read(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
            if (position >= length) {
                return -1;
            }

            byte[] buffer = getBlock();

            int bufferPos = (int) (position % BLOCK_SIZE);

            // Find maxIdx and simplify test in for-loop
            int maxLen = (int) Math.min(Math.min(pLength, buffer.length - bufferPos), length - position);

            int i;
            //for (i = 0; i < pLength && i < buffer.length - idx && i < length - position; i++) {
            for (i = 0; i < maxLen; i++) {
                pBytes[pOffset + i] = buffer[bufferPos + i];
            }

            position += i;

            return i;
        }

        public void seek(final long pPosition) throws IOException {
            if (pPosition < start) {
                throw new IOException("Seek before flush position");
            }
            position = pPosition;
        }

        @Override
        public void flush(final long pPosition) {
            int firstPos = (int) (pPosition / BLOCK_SIZE) - 1;

            for (int i = 0; i < firstPos; i++) {
                cache.remove(0);
            }

            start = pPosition;
        }

        @Override
        void close() throws IOException {
            cache.clear();
        }

        public long getPosition() {
            return position;
        }
    }
}
