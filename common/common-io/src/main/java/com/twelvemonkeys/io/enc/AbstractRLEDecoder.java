/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io.enc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Abstract base class for RLE decoding as specified by in the Windows BMP (aka DIB) file format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/AbstractRLEDecoder.java#1 $
 */
// TODO: Move to other package or make public
abstract class AbstractRLEDecoder implements Decoder {
    protected final byte[] row;
    protected final int width;
    protected int srcX;
    protected int srcY;
    protected int dstX;
    protected int dstY;

    /**
     * Creates an RLEDecoder. As RLE encoded BMPs may contain x and y deltas,
     * etc, we need to know height and width of the image.
     *
     * @param pWidth width of the image
     * @param pHeight height of the image
     */
    AbstractRLEDecoder(final int pWidth, final int pHeight) {
        width = pWidth;
        int bytesPerRow = width;
        int mod = bytesPerRow % 4;
        
        if (mod != 0) {
            bytesPerRow += 4 - mod;
        }

        row = new byte[bytesPerRow];

        srcX = 0;
        srcY = pHeight - 1;

        dstX = srcX;
        dstY = srcY;
    }

    /**
     * Decodes one full row of image data.
     *
     * @param pStream the input stream containing RLE data
     *
     * @throws IOException if an I/O related exception occurs while reading
     */
    protected abstract void decodeRow(final InputStream pStream) throws IOException;

    /**
     * Decodes as much data as possible, from the stream into the buffer.
     *
     * @param stream the input stream containing RLE data
     * @param buffer the buffer to decode the data to
     *
     * @return the number of bytes decoded from the stream, to the buffer
     *
     * @throws IOException if an I/O related exception ocurs while reading
     */
    public final int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining() && dstY >= 0) {
            // NOTE: Decode only full rows, don't decode if y delta
            if (dstX == 0 && srcY == dstY) {
                decodeRow(stream);
            }

            int length = Math.min(row.length - dstX, buffer.remaining());
//            System.arraycopy(row, dstX, buffer, decoded, length);
            buffer.put(row, 0, length);
            dstX += length;
//            decoded += length;

            if (dstX == row.length) {
                dstX = 0;
                dstY--;

                // NOTE: If src Y is < dst Y, we have a delta, and have to fill the
                // gap with zero-bytes
                if (dstY > srcY) {
                    for (int i = 0; i < row.length; i++) {
                        row[i] = 0x00;
                    }
                }
            }
        }

        return buffer.position();
    }

    /**
     * Checks a read byte for EOF marker.
     *
     * @param pByte the byte to check
     * @return the value of {@code pByte} if positive.
     *
     * @throws EOFException if {@code pByte} is negative
     */
    protected static int checkEOF(final int pByte) throws EOFException {
        if (pByte < 0) {
            throw new EOFException("Premature end of file");
        }

        return pByte;
    }
}
