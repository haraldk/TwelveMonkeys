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

import java.io.InputStream;
import java.io.IOException;

/**
 * Implements 8 bit RLE decoding as specified by in the Windows BMP (aka DIB) file format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/RLE8Decoder.java#1 $
 */
// TODO: Move to other package or make public
final class RLE8Decoder extends AbstractRLEDecoder {

    public RLE8Decoder(final int pWidth, final int pHeight) {
        super(pWidth, pHeight);
    }

    protected void decodeRow(final InputStream pInput) throws IOException {
        int deltaX = 0;
        int deltaY = 0;

        while (srcY >= 0) {
            int byte1 = pInput.read();
            int byte2 = checkEOF(pInput.read());
            
            if (byte1 == 0x00) {
                switch (byte2) {
                    case 0x00:
                        // End of line
                        // NOTE: Some BMPs have double EOLs..
                        if (srcX != 0) {
                            srcX = row.length;
                        }
                        break;
                    case 0x01:
                        // End of bitmap
                        srcX = row.length;
                        srcY = 0;
                        break;
                    case 0x02:
                        // Delta
                        deltaX = srcX + pInput.read();
                        deltaY = srcY - checkEOF(pInput.read());
                        srcX = row.length;
                        break;
                    default:
                        // Absolute mode
                        // Copy the next byte2 (3..255) bytes from file to output
                        boolean paddingByte = (byte2 % 2) != 0;
                        while (byte2-- > 0) {
                            row[srcX++] = (byte) checkEOF(pInput.read());
                        }
                        if (paddingByte) {
                            checkEOF(pInput.read());
                        }
                }
            }
            else {
                // Encoded mode
                // Replicate byte2 as many times as byte1 says
                byte value = (byte) byte2;
                while (byte1-- > 0) {
                    row[srcX++] = value;
                }
            }

            // If we're done with a complete row, copy the data
            if (srcX == row.length) {

                // Move to new position, either absolute (delta) or next line
                if (deltaX != 0 || deltaY != 0) {
                    srcX = deltaX;
                    if (deltaY != srcY) {
                        srcY = deltaY;
                        break;
                    }
                    deltaX = 0;
                    deltaY = 0;
                }
                else {
                    srcX = 0;
                    srcY--;
                    break;
                }
            }
        }
    }
}