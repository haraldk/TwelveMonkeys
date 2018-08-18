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

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * IIOOutputStreamAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IIOOutputStreamAdapter.java,v 1.0 Sep 26, 2007 11:50:38 AM haraldk Exp$
 */
class IIOOutputStreamAdapter extends OutputStream {
    private ImageOutputStream output;

    public IIOOutputStreamAdapter(final ImageOutputStream pOutput) {
        Validate.notNull(pOutput, "stream == null");

        output = pOutput;
    }

    @Override
    public void write(final byte[] pBytes) throws IOException {
        assertOpen();
        output.write(pBytes);
    }

    @Override
    public void write(final byte[] pBytes, final int pOffset, final int pLength) throws IOException {
        assertOpen();
        output.write(pBytes, pOffset, pLength);
    }

    @Override
    public void write(final int pByte) throws IOException {
        assertOpen();
        output.write(pByte);
    }

    @Override
    public void flush() throws IOException {
        // NOTE: The contract of OutputStream.flush is very different from ImageOutputStream.flush. We can't delegate.
        // TODO: Fulfill the contract of OutputStream.flush? This seems to be good enough for now.
        assertOpen();
    }

    private void assertOpen() throws IOException {
        if (output == null) {
            throw new IOException("stream already closed");
        }
    }

    @Override
    public void close() throws IOException {
        output = null;
    }
}
