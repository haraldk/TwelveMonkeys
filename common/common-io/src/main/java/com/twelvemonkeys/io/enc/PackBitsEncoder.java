/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io.enc;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Encoder implementation for Apple PackBits run-length encoding.
 * <p/>
 * From Wikipedia, the free encyclopedia<br/>
 * PackBits is a fast, simple compression scheme for run-length encoding of
 * data.
 * <p/>
 * Apple introduced the PackBits format with the release of MacPaint on the
 * Macintosh computer. This compression scheme is one of the types of
 * compression that can be used in TIFF-files.
 * <p/>
 * A PackBits data stream consists of packets of one byte of header followed by
 * data. The header is a signed byte; the data can be signed, unsigned, or
 * packed (such as MacPaint pixels).
 * <p/>
 * <table><tr><th>Header byte</th><th>Data</th></tr>
 * <tr><td>0 to 127</td>    <td>1 + <i>n</i> literal bytes of data</td></tr>
 * <tr><td>0 to -127</td>   <td>One byte of data, repeated 1 - <i>n</i> times in
 *                           the decompressed output</td></tr>
 * <tr><td>-128</td>        <td>No operation</td></tr></table>
 * <p/>
 * Note that interpreting 0 as positive or negative makes no difference in the
 * output. Runs of two bytes adjacent to non-runs are typically written as
 * literal data.
 * <p/>
 * See <a href="http://developer.apple.com/technotes/tn/tn1023.html">Understanding PackBits</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/enc/PackBitsEncoder.java#1 $
 */
public final class PackBitsEncoder implements Encoder {

    final private byte[] buffer = new byte[128];

    /**
     * Creates a {@code PackBitsEncoder}.
     */
    public PackBitsEncoder() {
    }

    public void encode(final OutputStream stream, final ByteBuffer buffer) throws IOException {
        encode(stream, buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        buffer.position(buffer.remaining());
    }

    private void encode(OutputStream pStream, byte[] pBuffer, int pOffset, int pLength) throws IOException {
        // NOTE: It's best to encode a 2 byte repeat
        // run as a replicate run except when preceded and followed by a
        // literal run, in which case it's best to merge the three into one
        // literal run. Always encode 3 byte repeats as replicate runs.
        // NOTE: Worst case: output = input + (input + 127) / 128

        int offset = pOffset;
        final int max = pOffset + pLength - 1;
        final int maxMinus1 = max - 1;

        while (offset <= max) {
            // Compressed run
            int run = 1;
            byte replicate = pBuffer[offset];
            while (run < 127 && offset < max && pBuffer[offset] == pBuffer[offset + 1]) {
                offset++;
                run++;
            }

            if (run > 1) {
                offset++;
                pStream.write(-(run - 1));
                pStream.write(replicate);
            }

            // Literal run
            run = 0;
            while ((run < 128 && ((offset < max && pBuffer[offset] != pBuffer[offset + 1])
                    || (offset < maxMinus1 && pBuffer[offset] != pBuffer[offset + 2])))) {
                buffer[run++] = pBuffer[offset++];
            }

            // If last byte, include it in literal run, if space
            if (offset == max && run > 0 && run < 128) {
                buffer[run++] = pBuffer[offset++];
            }

            if (run > 0) {
                pStream.write(run - 1);
                pStream.write(buffer, 0, run);
            }

            // If last byte, and not space, start new literal run
            if (offset == max && (run <= 0 || run >= 128)) {
                pStream.write(0);
                pStream.write(pBuffer[offset++]);
            }
        }
    }
}
