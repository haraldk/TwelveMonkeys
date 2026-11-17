/*
 * Copyright (c) 2026, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XS24ChunkTest {

    private static XS24Chunk parse(final byte[] body) throws IOException {
        XS24Chunk chunk = new XS24Chunk(body.length);
        chunk.readChunk(new ByteArrayImageInputStream(body));
        return chunk;
    }

    @Test
    void validThumbnailParses() throws IOException {
        // 2 x 1 thumbnail: 6 byte header + 6 bytes RGB
        byte[] body = {0x00, 0x02, 0x00, 0x01, 0x00, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66};

        XS24Chunk chunk = parse(body);

        assertEquals(2, chunk.thumbnail().getWidth());
        assertEquals(1, chunk.thumbnail().getHeight());
    }

    @Test
    void overflowingDimensionsThrows() {
        // 65535 * 65535 * 3 overflows int to a negative length, which passes the chunk length check
        byte[] body = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00};

        assertThrows(IIOException.class, () -> parse(body));
    }
}
