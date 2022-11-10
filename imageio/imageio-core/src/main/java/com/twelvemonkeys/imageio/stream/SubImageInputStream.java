/*
 * Copyright (c) 2009, Harald Kuhr
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

import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

/**
 * A wrapper for {@link ImageInputStream} to limit the number of bytes that can be read.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SubImageInputStream.java,v 1.0 Nov 8, 2009 2:50:58 PM haraldk Exp$
 */
public final class SubImageInputStream extends ImageInputStreamImpl {
    // NOTE: This class is based on com.sun.imageio.plugins.common.SubImageInputStream, but fixes some of its bugs.

    private final ImageInputStream stream;
    private final long startPos;
    private final long length;

    /**
     * Creates a {@link ImageInputStream}, reading up to a maximum number of bytes from the underlying stream.
     *
     * @param stream the underlying stream
     * @param length the maximum length to read from the stream.
     * Note that {@code stream} may contain less than this maximum number of bytes.
     *
     * @throws IOException if {@code stream}'s position can't be determined.
     * @throws IllegalArgumentException if {@code stream == null} or {@code length < 0}
     */
    public SubImageInputStream(final ImageInputStream stream, final long length) throws IOException {
        Validate.notNull(stream, "stream");
        Validate.isTrue(length >= 0, length, "length < 0: %d");

        this.stream = stream;
        this.startPos = stream.getStreamPosition();
        this.length = length;
    }

    public int read() throws IOException {
        if (streamPos >= length) { // Local EOF
            return -1;
        }
        else {
            int read = stream.read();

            if (read >= 0) {
                streamPos++;
            }
            
            return read;
        }
    }

    public int read(final byte[] bytes, final int off, final int len) throws IOException {
        if (streamPos >= length) { // Local EOF
            return -1;
        }

        // Safe cast, as len can never cause int overflow
        int length = (int) Math.min(len, this.length - streamPos);
        int count = stream.read(bytes, off, length);

        if (count >= 0) {
            streamPos += count;
        }

        return count;
    }

    @Override
    public long length() {
        try {
            long length = stream.length();
            return length < 0 ? -1 : Math.min(length - startPos, this.length);
        }
        catch (IOException ignore) {
        }

        return -1;
    }

    @Override
    public void seek(final long position) throws IOException {
        if (position < getFlushedPosition()) {
            throw new IndexOutOfBoundsException("pos < flushedPosition");
        }

        stream.seek(startPos + position);
        streamPos = position;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected void finalize() {
        // Empty finalizer (for improved performance; no need to call super.finalize() in this case)
    }
}
