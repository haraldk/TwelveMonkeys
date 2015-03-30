/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.io.enc.Decoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Abstract base class for RLE decoding as specified by in the Windows BMP (aka DIB) file format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: AbstractRLEDecoder.java#1 $
 */
abstract class AbstractRLEDecoder implements Decoder {
    protected final int width;
    protected final int bitsPerSample;

    protected final byte[] row;

    protected int srcX;
    protected int srcY;
    protected int dstX;
    protected int dstY;

    /**
     * Creates an RLEDecoder. As RLE encoded BMPs may contain x and y deltas,
     * etc, we need to know height and width of the image.
     *  @param width width of the image
     * @param bitsPerSample pits per sample
     */
    AbstractRLEDecoder(final int width, final int bitsPerSample) {
        this.width = width;
        this.bitsPerSample = bitsPerSample;

        // Pad row to multiple of 4
        int bytesPerRow = ((bitsPerSample * this.width + 31) / 32) * 4;
        row = new byte[bytesPerRow];
    }

    /**
     * Decodes one full row of image data.
     *
     * @param stream the input stream containing RLE data
     *
     * @throws IOException if an I/O related exception occurs while reading
     */
    protected abstract void decodeRow(final InputStream stream) throws IOException;

    /**
     * Decodes as much data as possible, from the stream into the buffer.
     *
     * @param stream the input stream containing RLE data
     * @param buffer the buffer to decode the data to
     *
     * @return the number of bytes decoded from the stream, to the buffer
     *
     * @throws IOException if an I/O related exception occurs while reading
     */
    public final int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        // TODO: Allow decoding < row.length at a time and get rid of this assertion...
        if (buffer.capacity() < row.length) {
            throw new AssertionError("This decoder needs a buffer.capacity() of at least one row");
        }

        while (buffer.remaining() >= row.length && srcY >= 0) {
            // NOTE: Decode only full rows, don't decode if y delta
            if (dstX == 0 && srcY == dstY) {
                decodeRow(stream);
            }

            int length = Math.min(row.length - (dstX * bitsPerSample) / 8, buffer.remaining());
            buffer.put(row, 0, length);
            dstX += (length * 8) / bitsPerSample;

            if (dstX == (row.length * 8) / bitsPerSample) {
                dstX = 0;
                dstY++;

                // NOTE: If src Y is > dst Y, we have a delta, and have to fill the
                // gap with zero-bytes
                if (srcX > dstX) {
                    Arrays.fill(row, 0, (srcX * bitsPerSample) / 8, (byte) 0);
                }
                if (srcY > dstY) {
                    Arrays.fill(row, (byte) 0);
                }
            }
        }

        return buffer.position();
    }

    /**
     * Checks a read byte for EOF marker.
     *
     * @param val the byte to check
     * @return the value of {@code val} if positive.
     *
     * @throws EOFException if {@code val} is negative
     */
    protected static int checkEOF(final int val) throws EOFException {
        if (val < 0) {
            throw new EOFException("Premature end of file");
        }

        return val;
    }
}
