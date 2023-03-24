/*
 * Copyright (c) 2023, Harald Kuhr
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

/**
 * A decoder for data converted using "floating point horizontal differencing predictor".
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HorizontalDeDifferencingStream.java,v 1.0 11.03.13 14:20 haraldk Exp$
 */
final class HorizontalDeDifferencingFloatingPointStream extends InputStream {
    // See TIFF 6.0 Specification, Section 14: "Differencing Predictor", page 64.
    // Adapted from the C code in Adobe Photoshop® TIFF Technical Note 3

    private final int columns;
    // NOTE: PlanarConfiguration == 2 may be treated as samplesPerPixel == 1
    private final int samplesPerPixel;
    private final int bytesPerSample;

    private final ReadableByteChannel channel;
    private final ByteBuffer buffer;
    private final byte[] fpRow;

    public HorizontalDeDifferencingFloatingPointStream(final InputStream stream, final int columns, final int samplesPerPixel, final int bitsPerSample, final ByteOrder byteOrder) {
        this.columns = Validate.isTrue(columns > 0, columns, "width must be greater than 0");
        this.samplesPerPixel = samplesPerPixel;
        Validate.isTrue(isValidBPS(bitsPerSample), bitsPerSample, "Unsupported bits per sample value: %s");
        bytesPerSample = (samplesPerPixel * bitsPerSample + 7) / 8;

        channel = Channels.newChannel(Validate.notNull(stream, "stream"));
        buffer = ByteBuffer.allocate(columns * bytesPerSample)
                           .order(byteOrder);
        fpRow = buffer.array().clone();

        buffer.flip();
    }

    private static boolean isValidBPS(final int bitsPerSample) {
        switch (bitsPerSample) {
            case 16:
            case 24:
            case 32:
            case 64:
                return true;
            default:
                return false;
        }
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

    private void decodeDeltaBytes(byte[] bytes, int columns, int samplesPerPixel) {
        for (int column = 1; column < columns; column++) {
            for (int channel = 0; channel < samplesPerPixel; channel++) {
                bytes[column * samplesPerPixel + channel] = (byte) (bytes[column * samplesPerPixel + channel] + bytes[(column - 1) * samplesPerPixel + channel]);
            }
        }
    }

    private void decodeFloatingPointDelta(byte[] input, byte[] output, int columns, int channels, int bytesPerSample, final ByteOrder order) {
        // undo byte difference on input
        decodeDeltaBytes(input, columns * bytesPerSample, channels);

        // reorder the bytes into the floating point buffer
        int rowIncrement = columns * channels;

        for (int column = 0; column < rowIncrement; column++) {
            for (int b = 0; b < bytesPerSample; b++) {
                output[bytesPerSample * column + b] = order == ByteOrder.BIG_ENDIAN
                        ? input[b * rowIncrement + column]
                        : input[(bytesPerSample - b - 1) * rowIncrement + column];
            }
        }
    }

    private void decodeRow() {
        // Un-apply horizontal predictor
        decodeFloatingPointDelta(buffer.array(), fpRow, columns, samplesPerPixel, bytesPerSample, buffer.order());
        System.arraycopy(fpRow, 0, buffer.array(), 0, fpRow.length);
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
