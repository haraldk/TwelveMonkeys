/*
 * Copyright (c) 2013, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.lang.Validate;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * A decoder for data converted using "horizontal differencing predictor".
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HorizontalDeDifferencingStream.java,v 1.0 11.03.13 14:20 haraldk Exp$
 */
final class HorizontalDeDifferencingStream extends FilterInputStream {
    // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.

    private final int columns;
    // NOTE: PlanarConfiguration == 2 may be treated as samplesPerPixel == 1
    private final int samplesPerPixel;
    private final int bitsPerSample;
    private final ByteOrder byteOrder;

    int decodedLength;
    int decodedPos;

    private final byte[] buffer;

    public HorizontalDeDifferencingStream(final InputStream stream, final int columns, final int samplesPerPixel, final int bitsPerSample, final ByteOrder byteOrder) {
        super(Validate.notNull(stream, "stream"));

        this.columns = Validate.isTrue(columns > 0, columns, "width must be greater than 0");
        this.samplesPerPixel = Validate.isTrue(bitsPerSample >= 8 || samplesPerPixel == 1, samplesPerPixel, "Unsupported samples per pixel for < 8 bit samples: %s");
        this.bitsPerSample = Validate.isTrue(isValidBPS(bitsPerSample), bitsPerSample, "Unsupported bits per sample value: %s");
        this.byteOrder = byteOrder;

        buffer = new byte[(columns * samplesPerPixel * bitsPerSample + 7) / 8];
    }

    private boolean isValidBPS(final int bitsPerSample) {
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

    private void fetch() throws IOException {
        int pos = 0;
        int read;

        // This *SHOULD* read an entire row of pixels (or nothing at all) into the buffer, otherwise we will throw EOFException below
        while (pos < buffer.length && (read = in.read(buffer, pos, buffer.length - pos)) > 0) {
            pos += read;
        }

        if (pos > 0) {
            if (buffer.length > pos) {
                throw new EOFException("Unexpected end of stream");
            }

            decodeRow();

            decodedLength = buffer.length;
            decodedPos = 0;
        }
        else {
            decodedLength = -1;
        }
    }

    private void decodeRow() throws EOFException {
        // Un-apply horizontal predictor
        int sample = 0;
        byte temp;

        switch (bitsPerSample) {
            case 1:
                for (int b = 0; b < (columns + 7) / 8; b++) {
                    sample += (buffer[b] >> 7) & 0x1;
                    temp = (byte) ((sample << 7) & 0x80);
                    sample += (buffer[b] >> 6) & 0x1;
                    temp |= (byte) ((sample << 6) & 0x40);
                    sample += (buffer[b] >> 5) & 0x1;
                    temp |= (byte) ((sample << 5) & 0x20);
                    sample += (buffer[b] >> 4) & 0x1;
                    temp |= (byte) ((sample << 4) & 0x10);
                    sample += (buffer[b] >> 3) & 0x1;
                    temp |= (byte) ((sample << 3) & 0x08);
                    sample += (buffer[b] >> 2) & 0x1;
                    temp |= (byte) ((sample << 2) & 0x04);
                    sample += (buffer[b] >> 1) & 0x1;
                    temp |= (byte) ((sample << 1) & 0x02);
                    sample += buffer[b] & 0x1;
                    buffer[b] = (byte) (temp | sample & 0x1);
                }
                break;
            case 2:
                for (int b = 0; b < (columns + 3) / 4; b++) {
                    sample += (buffer[b] >> 6) & 0x3;
                    temp = (byte) ((sample << 6) & 0xc0);
                    sample += (buffer[b] >> 4) & 0x3;
                    temp |= (byte) ((sample << 4) & 0x30);
                    sample += (buffer[b] >> 2) & 0x3;
                    temp |= (byte) ((sample << 2) & 0x0c);
                    sample += buffer[b] & 0x3;
                    buffer[b] = (byte) (temp | sample & 0x3);
                }
                break;

            case 4:
                for (int b = 0; b < (columns + 1) / 2; b++) {
                    sample += (buffer[b] >> 4) & 0xf;
                    temp = (byte) ((sample << 4) & 0xf0);
                    sample += buffer[b] & 0x0f;
                    buffer[b] = (byte) (temp | sample & 0xf);
                }
                break;

            case 8:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        buffer[off] = (byte) (buffer[off - samplesPerPixel] + buffer[off]);
                    }
                }
                break;

            case 16:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        putShort(off, asShort(off - samplesPerPixel) + asShort(off));
                    }
                }
                break;

            case 32:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        putInt(off, asInt(off - samplesPerPixel) + asInt(off));
                    }
                }
                break;

            case 64:
                for (int x = 1; x < columns; x++) {
                    for (int b = 0; b < samplesPerPixel; b++) {
                        int off = x * samplesPerPixel + b;
                        putLong(off, asLong(off - samplesPerPixel) + asLong(off));
                    }
                }
                break;

            default:
                throw new AssertionError(String.format("Unsupported bits per sample value: %d", bitsPerSample));
        }
    }

    private void putLong(final int index, final long value) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer[index * 8    ] = (byte) ((value >> 56) & 0xff);
            buffer[index * 8 + 1] = (byte) ((value >> 48) & 0xff);
            buffer[index * 8 + 2] = (byte) ((value >> 40) & 0xff);
            buffer[index * 8 + 3] = (byte) ((value >> 32) & 0xff);
            buffer[index * 8 + 4] = (byte) ((value >> 24) & 0xff);
            buffer[index * 8 + 5] = (byte) ((value >> 16) & 0xff);
            buffer[index * 8 + 6] = (byte) ((value >>  8) & 0xff);
            buffer[index * 8 + 7] = (byte) ((value) & 0xff);
        }
        else {
            buffer[index * 8 + 7] = (byte) ((value >> 56) & 0xff);
            buffer[index * 8 + 6] = (byte) ((value >> 48) & 0xff);
            buffer[index * 8 + 5] = (byte) ((value >> 40) & 0xff);
            buffer[index * 8 + 4] = (byte) ((value >> 32) & 0xff);
            buffer[index * 8 + 3] = (byte) ((value >> 24) & 0xff);
            buffer[index * 8 + 2] = (byte) ((value >> 16) & 0xff);
            buffer[index * 8 + 1] = (byte) ((value >>  8) & 0xff);
            buffer[index * 8    ] = (byte) ((value) & 0xff);
        }
    }

    private long asLong(final int index) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (buffer[index * 8  ] & 0xffl) << 56l | (buffer[index * 8 + 1] & 0xffl) << 48l |
                    (buffer[index * 8 + 2] & 0xffl) << 40l | (buffer[index * 8 + 3] & 0xffl) << 32l |
                    (buffer[index * 8 + 4] & 0xffl) << 24 | (buffer[index * 8 + 5] & 0xffl) << 16 |
                    (buffer[index * 8 + 6] & 0xffl) << 8 | buffer[index * 8 + 7] & 0xffl;
        }
        else {
            return (buffer[index * 8 + 7] & 0xffl) << 56l | (buffer[index * 8 + 6] & 0xffl) << 48l |
                    (buffer[index * 8 + 5] & 0xffl) << 40l | (buffer[index * 8 + 4] & 0xffl) << 32l |
                    (buffer[index * 8 + 3] & 0xffl) << 24 | (buffer[index * 8 + 2] & 0xffl) << 16 |
                    (buffer[index * 8 + 1] & 0xffl) << 8 | buffer[index * 8] & 0xffl;
        }
    }

    private void putInt(final int index, final int value) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer[index * 4    ] = (byte) ((value >> 24) & 0xff);
            buffer[index * 4 + 1] = (byte) ((value >> 16) & 0xff);
            buffer[index * 4 + 2] = (byte) ((value >> 8) & 0xff);
            buffer[index * 4 + 3] = (byte) ((value) & 0xff);
        }
        else {
            buffer[index * 4 + 3] = (byte) ((value >> 24) & 0xff);
            buffer[index * 4 + 2] = (byte) ((value >> 16) & 0xff);
            buffer[index * 4 + 1] = (byte) ((value >> 8) & 0xff);
            buffer[index * 4    ] = (byte) ((value) & 0xff);
        }
    }

    private int asInt(final int index) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (buffer[index * 4] & 0xff) << 24 | (buffer[index * 4 + 1] & 0xff) << 16 |
                    (buffer[index * 4 + 2] & 0xff) << 8 | buffer[index * 4 + 3] & 0xff;
        }
        else {
            return (buffer[index * 4 + 3] & 0xff) << 24 | (buffer[index * 4 + 2] & 0xff) << 16 |
                    (buffer[index * 4 + 1] & 0xff) << 8 | buffer[index * 4] & 0xff;
        }
    }

    private void putShort(final int index, final int value) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer[index * 2    ] = (byte) ((value >> 8) & 0xff);
            buffer[index * 2 + 1] = (byte) ((value) & 0xff);
        }
        else {
            buffer[index * 2 + 1] = (byte) ((value >> 8) & 0xff);
            buffer[index * 2    ] = (byte) ((value) & 0xff);
        }
    }

    private short asShort(final int index) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short) ((buffer[index * 2] & 0xff) << 8 | buffer[index * 2 + 1] & 0xff);
        }
        else {
            return (short) ((buffer[index * 2 + 1] & 0xff) << 8 | buffer[index * 2] & 0xff);
        }
    }

    @Override
    public int read() throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        return buffer[decodedPos++] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        int read = Math.min(decodedLength - decodedPos, len);
        System.arraycopy(buffer, decodedPos, b, off, read);
        decodedPos += read;

        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        int skipped = (int) Math.min(decodedLength - decodedPos, n);
        decodedPos += skipped;

        return skipped;
    }
}
