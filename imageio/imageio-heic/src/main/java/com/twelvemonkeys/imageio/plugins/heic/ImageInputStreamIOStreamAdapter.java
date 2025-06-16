/*
 * Copyright (c) 2025, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.heic;

import openize.io.IOSeekMode;
import openize.io.IOStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * ImageInputStreamIOStreamAdapter.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageInputStreamIOStreamAdapter.java,v 1.0 04/06/2025 haraldk Exp$
 */
final class ImageInputStreamIOStreamAdapter implements IOStream {
    private final ImageInputStream input;

    public ImageInputStreamIOStreamAdapter(ImageInputStream input) {
        this.input = input;
    }

    @Override
    public int read(byte[] b) {
        try {
            return input.read(b);
        }
        catch (IOException e) {
            throw new openize.io.IOException(e.getMessage(), e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        try {
            return input.read(b, off, len);
        }
        catch (IOException e) {
            throw new openize.io.IOException(e.getMessage(), e);
        }
    }

    @Override
    public long setPosition(long newPos) {
        try {
            long oldPos = input.getStreamPosition();
            input.seek(newPos);
            return oldPos;
        }
        catch (IOException e) {
            throw new openize.io.IOException(e.getMessage(), e);
        }
    }

    @Override
    public long getPosition() {
        try {
            return input.getStreamPosition();
        }
        catch (IOException e) {
            throw new openize.io.IOException(e.getMessage(), e);
        }
    }

    @Override
    public void seek(long relativePos, IOSeekMode seekMode) {
        try {
            switch (seekMode) {
                case BEGIN:
                    input.seek(relativePos);
                    break;

                case CURRENT:
                    input.seek(input.getStreamPosition() + relativePos);
                    break;

                case END:
                    long pos = input.length() + relativePos;
                    input.seek(pos);
                    break;
            }
        }
        catch (IOException e) {
            throw new openize.io.IOException(e.getMessage(), e);
        }
    }

    @Override
    public long getLength() {
        try {
            return input.length();
        }
        catch (IOException e) {
            throw new openize.io.IOException(e.getMessage(), e);
        }
    }

    // TODO: Write operations could probably be allowed, using instanceof ImageOutputStream + cast...
    @Override
    public void write(byte[] bytes) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public void write(byte[] bytes, int off, int len) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public void setLength(long length) {
        throw new UnsupportedOperationException("setLength");
    }

    @Override
    public void close() throws IOException {
        // TODO: Maybe not?
        input.close();
    }
}
