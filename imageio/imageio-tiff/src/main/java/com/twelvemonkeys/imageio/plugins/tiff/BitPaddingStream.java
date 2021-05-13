/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * BitPaddingStream.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: Foo.java,v 1.0 15/11/2016 harald.kuhr Exp$
 */
final class BitPaddingStream extends FilterInputStream {
    // Bit masks 0 - 32 bits
    private static final int[] MASK = {
            0x0,
            0x1, 0x3, 0x7, 0xf,
            0x1f, 0x3f, 0x7f, 0xff,
            0x1ff, 0x3ff, 0x7ff, 0xfff,
            0x1fff, 0x3fff, 0x7fff, 0xffff,
            0x1ffff, 0x3ffff, 0x7ffff, 0xfffff,
            0x1fffff, 0x3fffff, 0x7fffff, 0xffffff,
            0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff,
            0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff
    };

    private final int bitsPerSample;

    private final byte[] inputBuffer;
    private final ByteBuffer buffer;
    private int componentSize;

    BitPaddingStream(final InputStream stream, int samplesPerPixel, final int bitsPerSample, final int colsInTile, final ByteOrder byteOrder) {
        super(notNull(stream, "stream"));

        this.bitsPerSample = bitsPerSample;

        notNull(byteOrder, "byteOrder");

        switch (bitsPerSample) {
            case 2:
            case 4:
            case 6:
                // Byte
                componentSize = 1;
                break;
            case 10:
            case 12:
            case 14:
                // Short
                componentSize = 2;
                break;
            case 18:
            case 20:
            case 22:
            case 24:
            case 26:
            case 28:
            case 30:
                // Int
                componentSize = 4;
                break;
            default:
                throw new IllegalArgumentException("Unsupported BitsPerSample value: " + bitsPerSample);
        }

        int rowByteLength = (samplesPerPixel * bitsPerSample * colsInTile + 7) / 8;
        inputBuffer = new byte[rowByteLength];

        int rowLength = samplesPerPixel * colsInTile * componentSize;
        buffer = ByteBuffer.allocate(rowLength);
        buffer.order(byteOrder);
        buffer.position(buffer.limit()); // Make sure we start by filling the buffer
    }

    @Override
    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            if (!fillBuffer()) {
                return -1;
            }
        }

        return buffer.get() & 0xff;
    }

    private boolean readFully(final byte[] bytes) throws IOException {
        int rest = bytes.length;

        while (rest > 0) {
            int read = in.read(bytes, bytes.length - rest, rest);

            if (read == -1) {
                // NOTE: If we did a partial read here, we are in trouble...
                // Most likely an EOFException will happen up-stream
                return false;
            }

            rest -= read;
        }

        return true;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (!buffer.hasRemaining()) {
            if (!fillBuffer()) {
                return -1;
            }
        }

        int length = Math.min(len, buffer.remaining());
        buffer.get(b, off, length);

        return length;
    }

    @Override
    public long skip(final long n) throws IOException {
        if (n <= 0) {
            return 0;
        }

        if (!buffer.hasRemaining()) {
            if (!fillBuffer()) {
                return 0;
            }
        }

        int length = (int) Math.min(n, buffer.remaining());
        buffer.position(buffer.position() + length);

        return length;
    }

    private boolean fillBuffer() throws IOException {
        if (!readFully(inputBuffer)) {
            return false;
        }

        buffer.clear();
        padBits(buffer, componentSize, bitsPerSample, inputBuffer);
        buffer.rewind();

        return true;
    }

    private void padBits(final ByteBuffer buffer, final int componentSize, final int bitsPerSample, final byte[] samples) {
        int offset = 0;
        int remainingBits = 0;
        int temp = 0;

        while (true) {
            int value = temp & MASK[remainingBits];

            // Read smallest number of bytes > bits
            while (remainingBits < bitsPerSample) {
                if (offset >= samples.length) {
                    // End of data
                    return;
                }

                temp = samples[offset++] & 0xff;
                value = value << 8 | temp;
                remainingBits += 8;
            }

            remainingBits -= bitsPerSample;
            value = (value >> remainingBits) & MASK[bitsPerSample];

            switch (componentSize) {
                case 1:
                    buffer.put((byte) value);
                    break;
                case 2:
                    buffer.putShort((short) value);
                    break;
                case 4:
                    buffer.putInt(value);
                    break;
                default:
                    // Guarded in constructor
                    throw new AssertionError();
            }
        }
    }
}
