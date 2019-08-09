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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An unsynchronized {@code ByteArrayOutputStream} implementation. This version
 * also has a constructor that lets you create a stream with initial content.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: FastByteArrayOutputStream.java#2 $
 */
// TODO: Performance test of a stream impl that uses list of fixed size blocks, rather than contiguous block 
public final class FastByteArrayOutputStream extends ByteArrayOutputStream {
    /** Max grow size (unless if writing more than this amount of bytes) */
    protected int maxGrowSize = 1024 * 1024; // 1 MB

    /**
     * Creates a {@code ByteArrayOutputStream} with the given initial buffer
     * size.
     *
     * @param pSize initial buffer size
     */
    public FastByteArrayOutputStream(int pSize) {
        super(pSize);
    }

    /**
     * Creates a {@code ByteArrayOutputStream} with the given initial content.
     * <p>
     * Note that the buffer is not cloned, for maximum performance.
     * </p>
     *
     * @param pBuffer initial buffer
     */
    public FastByteArrayOutputStream(byte[] pBuffer) {
        super(0); // Don't allocate array
        buf = pBuffer;
        count = pBuffer.length;
    }

    @Override
    public void write(byte pBytes[], int pOffset, int pLength) {
        if ((pOffset < 0) || (pOffset > pBytes.length) || (pLength < 0) ||
                ((pOffset + pLength) > pBytes.length) || ((pOffset + pLength) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        else if (pLength == 0) {
            return;
        }

        int newCount = count + pLength;
        growIfNeeded(newCount);
        System.arraycopy(pBytes, pOffset, buf, count, pLength);
        count = newCount;
    }

    @Override
    public void write(int pByte) {
        int newCount = count + 1;
        growIfNeeded(newCount);
        buf[count] = (byte) pByte;
        count = newCount;
    }

    private void growIfNeeded(int pNewCount) {
        if (pNewCount > buf.length) {
            int newSize = Math.max(Math.min(buf.length << 1, buf.length + maxGrowSize), pNewCount);
            byte newBuf[] = new byte[newSize];
            System.arraycopy(buf, 0, newBuf, 0, count);
            buf = newBuf;
        }
    }

    // Non-synchronized version of writeTo
    @Override
    public void writeTo(OutputStream pOut) throws IOException {
        pOut.write(buf, 0, count);
    }

    // Non-synchronized version of toByteArray
    @Override
    public byte[] toByteArray() {
        byte newBuf[] = new byte[count];
        System.arraycopy(buf, 0, newBuf, 0, count);

        return newBuf;
    }

    /**
     * Creates a {@code ByteArrayInputStream} that reads directly from this
     * {@code FastByteArrayOutputStream}'s byte buffer.
     * The buffer is not cloned, for maximum performance.
     * <p>
     * Note that care needs to be taken to avoid writes to
     * this output stream after the input stream is created.
     * Failing to do so, may result in unpredictable behaviour.
     * </p>
     *
     * @return a new {@code ByteArrayInputStream}, reading from this stream's buffer.
     */
    public ByteArrayInputStream createInputStream() {
        return new ByteArrayInputStream(buf, 0, count);
    }
}