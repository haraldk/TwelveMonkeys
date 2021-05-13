/*
 * Copyright (c) 2014, Harald Kuhr
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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * A decoder for data converted using "horizontal differencing predictor".
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HorizontalDeDifferencingStream.java,v 1.0 11.03.13 14:20 haraldk Exp$
 */
final class HorizontalDifferencingStream extends OutputStream {
    // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.

    private final int columns;
    // NOTE: PlanarConfiguration == 2 may be treated as samplesPerPixel == 1
    private final int samplesPerPixel;
    private final int bitsPerSample;

    private final WritableByteChannel channel;
    private final ByteBuffer buffer;

    public HorizontalDifferencingStream(final OutputStream stream, final int columns, final int samplesPerPixel, final int bitsPerSample, final ByteOrder byteOrder) {
        this.columns = Validate.isTrue(columns > 0, columns, "width must be greater than 0");
        this.samplesPerPixel = Validate.isTrue(bitsPerSample >= 8 || samplesPerPixel == 1, samplesPerPixel, "Unsupported samples per pixel for < 8 bit samples: %s");
        this.bitsPerSample = Validate.isTrue(isValidBPS(bitsPerSample), bitsPerSample, "Unsupported bits per sample value: %s");

        channel = Channels.newChannel(Validate.notNull(stream, "stream"));

        buffer = ByteBuffer.allocate((columns * samplesPerPixel * bitsPerSample + 7) / 8).order(byteOrder);
    }

    static boolean isValidBPS(final int bitsPerSample) {
        switch (bitsPerSample) {
            case 1:
            case 2:
            case 4:
            case 8:
            case 16:
            case 32:
            case 64:
                return true;
            default:
                return false;
        }
    }

    private boolean flushBuffer() throws IOException {
        if (buffer.position() == 0) {
            return false;
        }

        encodeRow();

        buffer.flip();
        channel.write(buffer);
        buffer.clear();

        return true;
    }

    private void encodeRow() throws EOFException {
        // Apply horizontal predictor
        byte original;
        int sample = 0;
        int prev;
        byte temp;

        // Optimization:
        // Access array directly for <= 8 bits per sample, as buffer does extra index bounds check for every
        // put/get operation... (Measures to about 100 ms difference for 4000 x 3000 image)
        final byte[] array = buffer.array();

        switch (bitsPerSample) {
            case 1:
                for (int b = ((columns + 7) / 8) - 1; b > 0; b--) {
                    // Subtract previous sample from current sample
                    original = array[b];
                    prev = array[b - 1] & 0x1;
                    temp = (byte) ((((original & 0x80) >> 7) - prev) << 7);

                    sample = ((original & 0x40) >> 6) - ((original & 0x80) >> 7);
                    temp |= (sample << 6) & 0x40;

                    sample = ((original & 0x20) >> 5) - ((original & 0x40) >> 6);
                    temp |= (sample << 5) & 0x20;

                    sample = ((original & 0x10) >> 4) - ((original & 0x20) >> 5);
                    temp |= (sample << 4) & 0x10;

                    sample = ((original & 0x08) >> 3) - ((original & 0x10) >> 4);
                    temp |= (sample << 3) & 0x08;

                    sample = ((original & 0x04) >> 2) - ((original & 0x08) >> 3);
                    temp |= (sample << 2) & 0x04;

                    sample = ((original & 0x02) >> 1) - ((original & 0x04) >> 2);
                    temp |= (sample << 1) & 0x02;

                    sample = (original & 0x01) - ((original & 0x02) >> 1);

                    array[b] = (byte) (temp & 0xfe | sample & 0x01);
                }

                // First sample in row as is
                original = array[0];
                temp = (byte) (original & 0x80);

                sample = ((original & 0x40) >> 6) - ((original & 0x80) >> 7);
                temp |= (sample << 6) & 0x40;

                sample = ((original & 0x20) >> 5) - ((original & 0x40) >> 6);
                temp |= (sample << 5) & 0x20;

                sample = ((original & 0x10) >> 4) - ((original & 0x20) >> 5);
                temp |= (sample << 4) & 0x10;

                sample = ((original & 0x08) >> 3) - ((original & 0x10) >> 4);
                temp |= (sample << 3) & 0x08;

                sample = ((original & 0x04) >> 2) - ((original & 0x08) >> 3);
                temp |= (sample << 2) & 0x04;

                sample = ((original & 0x02) >> 1) - ((original & 0x04) >> 2);
                temp |= (sample << 1) & 0x02;

                sample = (original & 0x01) - ((original & 0x02) >> 1);

                array[0] = (byte) (temp & 0xfe | sample & 0x01);
                break;

            case 2:
                for (int b = ((columns + 3) / 4) - 1; b > 0; b--) {
                    // Subtract previous sample from current sample
                    original = array[b];
                    prev = array[b - 1] & 0x3;
                    temp = (byte) ((((original & 0xc0) >> 6) - prev) << 6);

                    sample = ((original & 0x30) >> 4) - ((original & 0xc0) >> 6);
                    temp |= (sample << 4) & 0x30;

                    sample = ((original & 0x0c) >> 2) - ((original & 0x30) >> 4);
                    temp |= (sample << 2) & 0x0c;

                    sample = (original & 0x03) - ((original & 0x0c) >> 2);

                    array[b] = (byte) (temp & 0xfc | sample & 0x03);
                }

                // First sample in row as is
                original = array[0];
                temp = (byte) (original & 0xc0);

                sample = ((original & 0x30) >> 4) - ((original & 0xc0) >> 6);
                temp |= (sample << 4) & 0x30;

                sample = ((original & 0x0c) >> 2) - ((original & 0x30) >> 4);
                temp |= (sample << 2) & 0x0c;

                sample = (original & 0x03) - ((original & 0x0c) >> 2);

                array[0] = (byte) (temp & 0xfc | sample & 0x03);
                break;

            case 4:
                for (int b = ((columns + 1) / 2) - 1; b > 0; b--) {
                    // Subtract previous sample from current sample
                    original = array[b];
                    prev = array[b - 1] & 0xf;
                    temp = (byte) ((((original & 0xf0) >> 4) - prev) << 4);
                    sample = (original & 0x0f) - ((original & 0xf0) >> 4);
                    array[b] = (byte) (temp & 0xf0 | sample & 0xf);
                }

                // First sample in row as is
                original = array[0];
                sample = (original & 0x0f) - ((original & 0xf0) >> 4);
                array[0] = (byte) (original & 0xf0 | sample & 0xf);

                break;

            case 8:
                for (int x = columns - 1; x > 0; x--) {
                    final int xOff = x * samplesPerPixel;

                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = xOff + b;
                        array[off] = (byte) (array[off] - array[off - samplesPerPixel]);
                    }
                }
                break;

            case 16:
                for (int x = columns - 1; x > 0; x--) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer.putShort(2 * off, (short) (buffer.getShort(2 * off) - buffer.getShort(2 * (off - samplesPerPixel))));
                    }
                }
                break;

            case 32:
                for (int x = columns - 1; x > 0; x--) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer.putInt(4 * off, buffer.getInt(4 * off) - buffer.getInt(4 * (off - samplesPerPixel)));
                    }
                }
                break;

            case 64:
                for (int x = columns - 1; x > 0; x--) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer.putLong(8 * off, buffer.getLong(8 * off) - buffer.getLong(8 * (off - samplesPerPixel)));
                    }
                }
                break;

            default:
                throw new AssertionError(String.format("Unsupported bits per sample value: %d", bitsPerSample));
        }
    }

    @Override
    public void write(int b) throws IOException {
        buffer.put((byte) b);

        if (!buffer.hasRemaining()) {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int maxLenForRow = Math.min(len, buffer.remaining());

            buffer.put(b, off, maxLenForRow);
            off += maxLenForRow;
            len -= maxLenForRow;

            if (!buffer.hasRemaining()) {
                flushBuffer();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
    }

    @Override
    public void close() throws IOException {
        try {
            flushBuffer();
            super.close();
        }
        finally {
            if (channel.isOpen()) {
                channel.close();
            }
        }
    }
}
