/*
 * Copyright (c) 2013, Harald Kuhr
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

import com.twelvemonkeys.lang.Validate;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static com.twelvemonkeys.imageio.plugins.tiff.HorizontalDifferencingStream.isValidBPS;

/**
 * A decoder for data converted using "horizontal differencing predictor".
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HorizontalDeDifferencingStream.java,v 1.0 11.03.13 14:20 haraldk Exp$
 */
final class HorizontalDeDifferencingStream extends InputStream {
    // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.

    private final int columns;
    // NOTE: PlanarConfiguration == 2 may be treated as samplesPerPixel == 1
    private final int samplesPerPixel;
    private final int bitsPerSample;

    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;

    public HorizontalDeDifferencingStream(final InputStream stream, final int columns, final int samplesPerPixel, final int bitsPerSample, final ByteOrder byteOrder) {
        this.columns = Validate.isTrue(columns > 0, columns, "width must be greater than 0");
        this.samplesPerPixel = Validate.isTrue(bitsPerSample >= 8 || samplesPerPixel == 1, samplesPerPixel, "Unsupported samples per pixel for < 8 bit samples: %s");
        this.bitsPerSample = Validate.isTrue(isValidBPS(bitsPerSample), bitsPerSample, "Unsupported bits per sample value: %s");

        channel = Channels.newChannel(Validate.notNull(stream, "stream"));

        buffer = ByteBuffer.allocate((columns * samplesPerPixel * bitsPerSample + 7) / 8).order(byteOrder);
        buffer.flip();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private boolean fetch() throws IOException {
        buffer.clear();

        // This *SHOULD* read an entire row of pixels (or nothing at all) into the buffer,
        // otherwise we will throw EOFException below
        while (channel.read(buffer) > 0);

        if (buffer.position() > 0) {
            if (buffer.hasRemaining()) {
                throw new EOFException("Unexpected end of stream");
            }

            decodeRow();
            buffer.flip();

            return true;
        }
        else {
            buffer.position(buffer.capacity());

            return false;
        }
    }

    private void decodeRow() throws EOFException {
        // Un-apply horizontal predictor
        byte original;
        int sample = 0;
        byte temp;

        // Optimization:
        // Access array directly for <= 8 bits per sample, as buffer does extra index bounds check for every
        // put/get operation... (Measures to about 100 ms difference for 4000 x 3000 image)
        final byte[] array = buffer.array();

        switch (bitsPerSample) {
            case 1:
                for (int b = 0; b < (columns + 7) / 8; b++) {
                    original = array[b];
                    sample += (original >> 7) & 0x1;
                    temp = (byte) ((sample << 7) & 0x80);
                    sample += (original >> 6) & 0x1;
                    temp |= (byte) ((sample << 6) & 0x40);
                    sample += (original >> 5) & 0x1;
                    temp |= (byte) ((sample << 5) & 0x20);
                    sample += (original >> 4) & 0x1;
                    temp |= (byte) ((sample << 4) & 0x10);
                    sample += (original >> 3) & 0x1;
                    temp |= (byte) ((sample << 3) & 0x08);
                    sample += (original >> 2) & 0x1;
                    temp |= (byte) ((sample << 2) & 0x04);
                    sample += (original >> 1) & 0x1;
                    temp |= (byte) ((sample << 1) & 0x02);
                    sample += original & 0x1;
                    array[b] = (byte) (temp | sample & 0x1);
                }
                break;

            case 2:
                for (int b = 0; b < (columns + 3) / 4; b++) {
                    original = array[b];
                    sample += (original >> 6) & 0x3;
                    temp = (byte) ((sample << 6) & 0xc0);
                    sample += (original >> 4) & 0x3;
                    temp |= (byte) ((sample << 4) & 0x30);
                    sample += (original >> 2) & 0x3;
                    temp |= (byte) ((sample << 2) & 0x0c);
                    sample += original & 0x3;
                    array[b] = (byte) (temp | sample & 0x3);
                }
                break;

            case 4:
                for (int b = 0; b < (columns + 1) / 2; b++) {
                    original = array[b];
                    sample += (original >> 4) & 0xf;
                    temp = (byte) ((sample << 4) & 0xf0);
                    sample += original & 0x0f;
                    array[b] = (byte) (temp | sample & 0xf);
                }
                break;

            case 8:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        array[off] = (byte) (array[off - samplesPerPixel] + array[off]);
                    }
                }
                break;

            case 16:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer.putShort(2 * off, (short) (buffer.getShort(2 * (off - samplesPerPixel)) + buffer.getShort(2 * off)));
                    }
                }
                break;

            case 32:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer.putInt(4 * off, buffer.getInt(4 * (off - samplesPerPixel)) + buffer.getInt(4 * off));
                    }
                }
                break;

            case 64:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer.putLong(8 * off, buffer.getLong(8 * (off - samplesPerPixel)) + buffer.getLong(8 * off));
                    }
                }
                break;

            default:
                throw new AssertionError(String.format("Unsupported bits per sample value: %d", bitsPerSample));
        }
    }

    @Override
    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            if (!fetch()) {
                return -1;
            }
        }

        return buffer.get() & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!buffer.hasRemaining()) {
            if (!fetch()) {
                return -1;
            }
        }

        int read = Math.min(buffer.remaining(), len);
        buffer.get(b, off, read);

        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) {
            return 0;
        }

        if (!buffer.hasRemaining()) {
            if (!fetch()) {
                return 0; // SIC
            }
        }

        int skipped = (int) Math.min(buffer.remaining(), n);
        buffer.position(buffer.position() + skipped);

        return skipped;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        }
        finally {
            if (channel.isOpen()) {
                channel.close();
            }
        }
    }
}
