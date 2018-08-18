/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * ICNSUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICNSUtil.java,v 1.0 26.10.11 11:49 haraldk Exp$
 */
final class ICNSUtil {

    private ICNSUtil() {}

    // TODO: Duplicated code from IFF plugin, move to some common util?
    static String intToStr(int pChunkId) {
        return new String(
                new byte[]{
                        (byte) ((pChunkId & 0xff000000) >> 24),
                        (byte) ((pChunkId & 0x00ff0000) >> 16),
                        (byte) ((pChunkId & 0x0000ff00) >> 8),
                        (byte) ((pChunkId & 0x000000ff))
                }
        );
    }

    /*
    * http://www.macdisk.com/maciconen.php:
    * "For [...] (width * height of the icon), read a byte.
    * if bit 8 of the byte is set:
    *   This is a compressed run, for some value (next byte).
    *   The length is byte - 125. (*
    *   Put so many copies of the byte in the current color channel.
    * Else:
    *   This is an uncompressed run, whose values follow.
    *   The length is byte + 1.
    *   Read the bytes and put them in the current color channel."
    *
    *   *): With signed bytes, byte is always negative in this case, so it's actually -byte - 125,
    *       which is the same as byte + 131.
    */
    // NOTE: This is very close to PackBits (as described by the Wikipedia article), but it is not PackBits!
    static void decompress(final DataInputStream input, final byte[] result, int offset, int length) throws IOException {
        int resultPos = offset;
        int remaining = length;

        while (remaining > 0) {
            byte run = input.readByte();
            int runLength;

            if ((run & 0x80) != 0) {
                // Compressed run
                runLength = run + 131; // PackBits: -run + 1 and run == 0x80 is no-op... This allows 1 byte longer runs...

                byte runData = input.readByte();

                for (int i = 0; i < runLength; i++) {
                    result[resultPos++] = runData;
                }
            }
            else {
                // Uncompressed run
                runLength = run + 1;

                input.readFully(result, resultPos, runLength);
                resultPos += runLength;
            }

            remaining -= runLength;
        }
    }
}
