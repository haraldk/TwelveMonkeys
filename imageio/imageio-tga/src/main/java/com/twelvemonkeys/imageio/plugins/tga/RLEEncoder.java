/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.io.enc.Encoder;
import com.twelvemonkeys.lang.Validate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

final class RLEEncoder implements Encoder {

    private final int pixelSize;

    RLEEncoder(final int pixelDepth) {
        Validate.isTrue(pixelDepth % Byte.SIZE == 0, "Depth must be a multiple of bytes (8 bits)");
        pixelSize = pixelDepth / Byte.SIZE;
    }

    public void encode(final OutputStream stream, final ByteBuffer buffer) throws IOException {
        encode(stream, buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        buffer.position(buffer.remaining());
    }

    private void encode(final OutputStream stream, final byte[] buffer, final int pOffset, final int length) throws IOException {
        // NOTE: It's best to encode a 2 byte repeat
        // run as a replicate run except when preceded and followed by a
        // literal run, in which case it's best to merge the three into one
        // literal run. Always encode 3 byte repeats as replicate runs.
        // Worst case: output = input + (input + 127) / 128

        int offset = pOffset;
        final int max = pOffset + length - pixelSize;
        final int maxMinus1 = max - pixelSize;

        while (offset <= max) {
            // Compressed run
            int run = 1;
            while (run < 127 && offset < max && equalPixel(buffer, offset, offset + pixelSize)) {
                offset += pixelSize;
                run++;
            }

            if (run > 1) {
                stream.write(0x80 | (run - 1));
                stream.write(buffer, offset, pixelSize);
                offset += pixelSize;
            }

            // Literal run
            int runStart = offset;
            run = 0;
            while ((run < 127 && ((offset < max && !(equalPixel(buffer, offset, offset + pixelSize)))
                    || (offset < maxMinus1 && !(equalPixel(buffer, offset, offset + 2 * pixelSize)))))) {
                offset += pixelSize;
                run++;
            }

            // If last pixel, include it in literal run, if space
            if (offset == max && run > 0 && run < 127) {
                offset += pixelSize;
                run++;
            }

            if (run > 0) {
                stream.write(run - 1);
                stream.write(buffer, runStart, run * pixelSize);
            }

            // If last pixel, and not space, start new literal run
            if (offset == max && (run <= 0 || run >= 127)) {
                stream.write(0);
                stream.write(buffer, offset, pixelSize);
                offset += pixelSize;
            }
        }
    }

    private boolean equalPixel(final byte[] buffer, final int offset, int compareOffset) {
        for (int i = 0; i < pixelSize; i++) {
            if (buffer[offset + i] != buffer[compareOffset + i]) {
                return false;
            }
        }

        return true;
    }
}
