/*
 * Copyright (c) 2022, Harald Kuhr
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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream that provides on-the-fly upsampling of TIFF subsampled YCbCr samples.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: YCbCrUpsamplerStream.java,v 1.0 31.01.13 09:25 haraldk Exp$
 */
final class YCbCrPlanarUpsamplerStream extends FilterInputStream {

    private final int horizChromaSub;
    private final int vertChromaSub;
    private final int yCbCrPos;
    private final int columns;

    private final int units;
    private final byte[] decodedRows;
    int decodedLength;
    int decodedPos;

    private final byte[] buffer;
    int bufferLength;
    int bufferPos;

    public YCbCrPlanarUpsamplerStream(final InputStream stream, final int[] chromaSub, final int yCbCrPos, final int columns) {
        super(Validate.notNull(stream, "stream"));

        Validate.notNull(chromaSub, "chromaSub");
        Validate.isTrue(chromaSub.length == 2, "chromaSub.length != 2");

        this.horizChromaSub = chromaSub[0];
        this.vertChromaSub = chromaSub[1];
        this.yCbCrPos = yCbCrPos;
        this.columns = columns;

        units = (columns + horizChromaSub - 1) / horizChromaSub;    // If columns % horizChromasSub != 0...
        // ...each coded row will be padded to fill unit
        decodedRows = new byte[columns * vertChromaSub];
        buffer = new byte[units];
    }

    private void fetch() throws IOException {
        if (bufferPos >= bufferLength) {
            int pos = 0;
            int read;

            // This *SHOULD* read an entire row of units into the buffer, otherwise decodeRows will throw EOFException
            while (pos < buffer.length && (read = in.read(buffer, pos, buffer.length - pos)) > 0) {
                pos += read;
            }

            bufferLength = pos;
            bufferPos = 0;
        }

        if (bufferLength > 0) {
            decodeRows();
        }
        else {
            decodedLength = -1;
        }
    }

    private void decodeRows() throws EOFException {
        decodedLength = decodedRows.length;

        for (int u = 0; u < units; u++) {
            if (u >= bufferLength) {
                throw new EOFException("Unexpected end of stream");
            }

            // Decode one unit
            byte c = buffer[u];

            for (int y = 0; y < vertChromaSub; y++) {
                for (int x = 0; x < horizChromaSub; x++) {
                    // Skip padding at end of row
                    int column = horizChromaSub * u + x;
                    if (column >= columns) {
                        break;
                    }

                    int pixelOff = column + columns * y;
                    decodedRows[pixelOff] = c;
                }
            }
        }

        bufferPos = bufferLength;
        decodedPos = 0;
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

        return decodedRows[decodedPos++] & 0xff;
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
        System.arraycopy(decodedRows, decodedPos, b, off, read);
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

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
