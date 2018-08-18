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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * IIOInputStreamAdapter
 * <p/>
 * Note: You should always wrap this stream in a {@code BufferedInputStream}.
 * If not, performance may degrade significantly.
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: IIOInputStreamAdapter.java,v 1.0 Sep 26, 2007 11:35:59 AM haraldk Exp$
*/
class IIOInputStreamAdapter extends InputStream {
    private ImageInputStream input;
    private final boolean hasLength;
    private long left;
    private long markPosition;

    // TODO: Enforce stream boundaries!
    // TODO: Stream start position....

    /**
     * Creates an {@code InputStream} that reads from the given {@code ImageInputStream}.
     * The input stream will read from the current stream position, until the end of the
     * underlying stream.
     *
     * @param pInput the {@code ImageInputStream} to read from.
     */
    public IIOInputStreamAdapter(final ImageInputStream pInput) {
        this(pInput, -1, false);
    }

    /**
     * Creates an {@code InputStream} that reads from the given {@code ImageInputStream}.
     * The input stream will read from the current stream position, until at most
     * {@code pLength} bytes has been read.
     *
     * @param pInput the {@code ImageInputStream} to read from.
     * @param pLength the length of the stream
     */
    public IIOInputStreamAdapter(final ImageInputStream pInput, final long pLength) {
        this(pInput, pLength, true);
    }

    private IIOInputStreamAdapter(ImageInputStream pInput, long pLength, boolean pHasLength) {
        Validate.notNull(pInput, "stream");
        Validate.isTrue(!pHasLength || pLength >= 0, pLength, "length < 0: %f");

        input = pInput;
        left = pLength;
        hasLength = pHasLength;
    }


    /**
     * Marks this stream as closed.
     * This implementation does <em>not</em> close the underlying stream.
     */
    public void close() throws IOException {
        if (hasLength) {
            input.seek(input.getStreamPosition() + left);
        }

        left = 0;
        input = null;
    }

    public int available() throws IOException {
        if (hasLength) {
            return left > 0 ? (int) Math.min(Integer.MAX_VALUE, left) : 0;
        }

        return 0; // We don't really know, so we say 0 to be safe.
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    public void mark(int pReadLimit) {
        try {
            markPosition = input.getStreamPosition();
        }
        catch (IOException e) {
            // Let's hope this never happens, because it's not possible to reset then...
            throw new IllegalStateException("Could not read stream position: " + e.getMessage(), e);
        }
    }

    public void reset() throws IOException {
        long diff = input.getStreamPosition() - markPosition;
        input.seek(markPosition);
        left += diff;
    }

    public int read() throws IOException {
        if (hasLength && left-- <= 0) {
            left = 0;
            return -1;
        }
        return input.read();
    }

    public final int read(byte[] pBytes) throws IOException {
        return read(pBytes, 0, pBytes.length);
    }

    public int read(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        if (hasLength && left <= 0) {
            return -1;
        }

        int read = input.read(pBytes, pOffset, (int) findMaxLen(pLength));
        if (hasLength) {
            left = read < 0 ? 0 : left - read;
        }
        return read;
    }

    /**
     * Finds the maximum number of bytes we can read or skip, from this stream.
     * The number will be in the range {@code [0 ... bytes left]}.
     *
     * @param pLength the requested length
     * @return the maximum number of bytes to read
     */
    private long findMaxLen(long pLength) {
        if (hasLength && left < pLength) {
            return Math.max(left, 0);
        }
        else {
            return Math.max(pLength, 0);
        }
    }

    public long skip(long pLength) throws IOException {
        long skipped = input.skipBytes(findMaxLen(pLength)); // Skips 0 or more, never -1
        left -= skipped;
        return skipped;
    }
}
