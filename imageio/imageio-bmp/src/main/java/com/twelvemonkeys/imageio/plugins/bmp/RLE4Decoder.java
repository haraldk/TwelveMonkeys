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

package com.twelvemonkeys.imageio.plugins.bmp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Implements 4 bit RLE decoding as specified by in the Windows BMP (aka DIB) file format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: RLE4Decoder.java#1 $
 */
final class RLE4Decoder extends AbstractRLEDecoder {
    final static int BIT_MASKS[] = {0xf0, 0x0f};
    final static int BIT_SHIFTS[] = {4, 0};

    public RLE4Decoder(final int width) {
        super(width, 4);
    }

    protected void decodeRow(final InputStream stream) throws IOException {
        // Just clear row now, and be done with it...
        Arrays.fill(row, (byte) 0);

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
                            srcX = row.length * 2;
                        }

                        break;

                    case 0x01:
                        // End of bitmap
                        srcX = row.length * 2;
                        srcY = -1;
                        break;

                    case 0x02:
                        // Delta
                        deltaX = srcX + stream.read();
                        deltaY = srcY + checkEOF(stream.read());

                        srcX = row.length * 2;

                        break;

                    default:
                        // Absolute mode
                        // Copy the next byte2 (3..255) nibbles from file to output
                        // Two samples are packed into one byte
                        // If the *number of bytes* used to pack is not a multiple of 2,
                        // an additional padding byte is in the stream and must be skipped
                        boolean paddingByte = (((byte2 + 1) / 2) % 2) != 0;

                        int packed = 0;
                        for (int i = 0; i < byte2; i++) {
                            if (i % 2 == 0) {
                                packed = checkEOF(stream.read());
                            }

                            row[srcX / 2] |= (byte) (((packed & BIT_MASKS[i % 2]) >> BIT_SHIFTS[i % 2])<< BIT_SHIFTS[srcX % 2]);
                            srcX++;
                        }

                        if (paddingByte) {
                            checkEOF(stream.read());
                        }
                }
            }
            else {
                // Encoded mode
                // Replicate the two samples in byte2 as many times as byte1 says
                for (int i = 0; i < byte1; i++) {
                    row[srcX / 2] |= (byte) (((byte2 & BIT_MASKS[i % 2]) >> BIT_SHIFTS[i % 2]) << BIT_SHIFTS[srcX % 2]);
                    srcX++;
                }
            }

            // If we're done with a complete row, copy the data
            if (srcX >= row.length * 2) {
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
