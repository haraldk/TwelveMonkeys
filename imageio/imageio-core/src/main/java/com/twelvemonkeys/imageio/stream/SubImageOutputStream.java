/*
 * Copyright (c) 2015, Harald Kuhr
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

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;
import java.io.IOException;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ImageInputStream that writes through a delegate, but keeps local position and bit offset.
 * Note: Flushing or closing this stream will *not* have an effect on the delegate.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: SubImageOutputStream.java,v 1.0 30/03/15 harald.kuhr Exp$
 */
public class SubImageOutputStream extends ImageOutputStreamImpl {
    private final ImageOutputStream stream;
    private final long startPos;

    public SubImageOutputStream(final ImageOutputStream stream) throws IOException {
        this.stream = notNull(stream, "stream");
        startPos = stream.getStreamPosition();
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
        stream.seek(startPos + pos);
    }

    @Override
    public void write(int b) throws IOException {
        flushBits();

        stream.write(b);
        streamPos++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        stream.write(b, off, len);
        streamPos += len;
    }

    @Override
    public int read() throws IOException {
        bitOffset = 0;
        streamPos++;
        return stream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        bitOffset = 0;
        int count = stream.read(b, off, len);
        streamPos += count;
        return count;
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
}
