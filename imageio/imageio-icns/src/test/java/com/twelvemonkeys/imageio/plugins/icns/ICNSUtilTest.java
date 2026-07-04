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

import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ICNSUtilTest.
 */
public class ICNSUtilTest {

    private static DataInputStream stream(int... bytes) {
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return new DataInputStream(new ByteArrayInputStream(data));
    }

    @Test
    public void testDecompressUncompressedRun() throws Exception {
        // 0x03 -> uncompressed run of 4 bytes
        DataInputStream input = stream(0x03, 0x01, 0x02, 0x03, 0x04);
        byte[] result = new byte[4];

        ICNSUtil.decompress(input, result, 0, result.length);

        assertArrayEquals(new byte[] {0x01, 0x02, 0x03, 0x04}, result);
    }

    @Test
    public void testDecompressCompressedRunOverflowThrows() {
        // 0x85 -> compressed run of (0x85 - 256) + 131 = 8 bytes, but only 4 expected
        DataInputStream input = stream(0x85, 0x42);
        byte[] result = new byte[4];

        assertThrows(IIOException.class, () -> ICNSUtil.decompress(input, result, 0, result.length));
    }

    @Test
    public void testDecompressUncompressedRunOverflowThrows() {
        // 0x7f -> uncompressed run of 128 bytes, but only 4 expected
        int[] bytes = new int[129];
        bytes[0] = 0x7f;
        DataInputStream input = stream(bytes);
        byte[] result = new byte[4];

        assertThrows(IIOException.class, () -> ICNSUtil.decompress(input, result, 0, result.length));
    }
}
