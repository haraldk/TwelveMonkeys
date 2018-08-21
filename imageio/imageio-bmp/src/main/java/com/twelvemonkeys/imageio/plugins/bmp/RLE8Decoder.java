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

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Implements 8 bit RLE decoding as specified by in the Windows BMP (aka DIB) file format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: RLE8Decoder.java#1 $
 */
final class RLE8Decoder extends AbstractRLEDecoder {
    public RLE8Decoder(final int width) {
        super(width, 8);
    }

    protected void decodeRow(final InputStream stream) throws IOException {
        int deltaX = 0;
        int deltaY = 0;

        while (srcY >= 0) {
            int byte1 = stream.read();
            int byte2 = checkEOF(stream.read());
            
            if (byte1 == 0x00) {
                switch (byte2) {
                    case 0x00:
                        // End of line
                        // NOTE: Some BMPs have double EOLs..
                        if (srcX != 0) {
                            Arrays.fill(row, srcX, row.length, (byte) 0);
                            srcX = row.length;
                        }

                        break;

                    case 0x01:
                        // End of bitmap
                        Arrays.fill(row, srcX, row.length, (byte) 0);
                        srcX = row.length;
                        srcY = -1; // TODO: Do we need to allow reading more (and thus re-introduce height parameter)..?

                        break;

                    case 0x02:
                        // Delta
                        deltaX = srcX + stream.read();
                        deltaY = srcY + checkEOF(stream.read());

                        Arrays.fill(row, srcX, deltaX, (byte) 0);

                        // TODO: Handle x delta inline!
//                        if (deltaY != srcY) {
                            srcX = row.length;
//                        }

                        break;

                    default:
                        // Absolute mode
                        // Copy the next byte2 (3..255) bytes from file to output
                        // If the number bytes is not a multiple of 2,
                        // an additional padding byte is in the stream and must be skipped
                        boolean paddingByte = (byte2 % 2) != 0;

                        while (byte2-- > 0) {
                            row[srcX++] = (byte) checkEOF(stream.read());
                        }

                        if (paddingByte) {
                            checkEOF(stream.read());
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
            if (srcX >= row.length) {
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
                else if (srcY == -1) {
                    break;
                }
                else {
                    srcX = 0;
                    srcY++;
                    break;
                }
            }
        }
    }
}