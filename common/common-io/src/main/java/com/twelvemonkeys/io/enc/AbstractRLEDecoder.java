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

/**
 * Abstract base class for RLE decoding as specified by in the Windows BMP (aka DIB) file format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/AbstractRLEDecoder.java#1 $
 */
// TODO: Move to other package or make public
abstract class AbstractRLEDecoder implements Decoder {
    protected final byte[] mRow;
    protected final int mWidth;
    protected int mSrcX;
    protected int mSrcY;
    protected int mDstX;
    protected int mDstY;

    /**
     * Creates an RLEDecoder. As RLE encoded BMP's may contain x and y deltas,
     * etc, we need to know height and width of the image.
     *
     * @param pWidth width of the image
     * @param pHeight heigth of the image
     */
    AbstractRLEDecoder(int pWidth, int pHeight) {
        mWidth = pWidth;
        int bytesPerRow = mWidth;
        int mod = bytesPerRow % 4;
        
        if (mod != 0) {
            bytesPerRow += 4 - mod;
        }

        mRow = new byte[bytesPerRow];

        mSrcX = 0;
        mSrcY = pHeight - 1;

        mDstX = mSrcX;
        mDstY = mSrcY;
    }

    /**
     * Decodes one full row of image data.
     *
     * @param pStream the input stream containint RLE data
     *
     * @throws IOException if an I/O related exception ocurs while reading
     */
    protected abstract void decodeRow(InputStream pStream) throws IOException;

    /**
     * Decodes as much data as possible, from the stream into the buffer.
     *
     * @param pStream the input stream containing RLE data
     * @param pBuffer tge buffer to decode the data to
     *
     * @return the number of bytes decoded from the stream, to the buffer
     *
     * @throws IOException if an I/O related exception ocurs while reading
     */
    public final int decode(InputStream pStream, byte[] pBuffer) throws IOException {
        int decoded = 0;

        while (decoded < pBuffer.length && mDstY >= 0) {
            // NOTE: Decode only full rows, don't decode if y delta
            if (mDstX == 0 && mSrcY == mDstY) {
                decodeRow(pStream);
            }

            int length = Math.min(mRow.length - mDstX, pBuffer.length - decoded);
            System.arraycopy(mRow, mDstX, pBuffer, decoded, length);
            mDstX += length;
            decoded += length;

            if (mDstX == mRow.length) {
                mDstX = 0;
                mDstY--;

                // NOTE: If src Y is < dst Y, we have a delta, and have to fill the
                // gap with zero-bytes
                if (mDstY > mSrcY) {
                    for (int i = 0; i < mRow.length; i++) {
                        mRow[i] = 0x00;
                    }
                }
            }
        }

        return decoded;
    }

    /**
     * Checks a read byte for EOF marker.
     *
     * @param pByte the byte to check
     * @return the value of {@code pByte} if positive.
     *
     * @throws EOFException if {@code pByte} is negative
     */
    protected static int checkEOF(int pByte) throws EOFException {
        if (pByte < 0) {
            throw new EOFException("Premature end of file");
        }

        return pByte;
    }
}
