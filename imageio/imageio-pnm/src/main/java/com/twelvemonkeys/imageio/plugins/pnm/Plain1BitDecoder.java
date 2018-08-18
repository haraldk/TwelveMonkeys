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

package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;
import java.io.InputStream;

final class Plain1BitDecoder extends InputStream {
    private final InputStream stream;
    private final int samplesPerRow; // Padded to byte boundary
    private int pos = 0;

    public Plain1BitDecoder(final InputStream in, final int samplesPerRow) {
        this.stream = in;
        this.samplesPerRow = samplesPerRow;
    }

    @Override
    public int read() throws IOException {
        // Each 0 or 1 represents one bit, whitespace is ignored. Padded to byte boundary for each row.
        // NOTE: White is 0, black is 1!
        int result = 0;

        for (int bitPos = 7; bitPos >= 0; bitPos--) {

            int read;
            while ((read = stream.read()) != -1 && Character.isWhitespace(read)) {
                // Skip whitespace
            }

            if (read == -1) {
                if (bitPos == 7) {
                    return -1;
                }

                break;
            }

            int val = read - '0';

            result |= val << bitPos;

            if (++pos >= samplesPerRow) {
                pos = 0;
                break;
            }
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
