/*
 * Copyright (c) 2022, Harald Kuhr
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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class EncoderStreamTest {

    private final Random rng = new Random(5467809876546L);

    private byte[] createData(final int length) {
        byte[] data = new byte[length];
        rng.nextBytes(data);
        return data;
    }

    @Test
    public void testEncodeSingleBytes() throws IOException {
        byte[] data = createData(1327);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (OutputStream stream = new EncoderStream(result, new NullEncoder())) {
            for (byte datum : data) {
                stream.write(datum);
            }
        }

        assertArrayEquals(data, result.toByteArray());
    }

    @Test
    public void testEncodeArray() throws IOException {
        byte[] data = createData(1793);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (OutputStream stream = new EncoderStream(result, new NullEncoder())) {
            for (int i = 0; i < 10; i++) {
                stream.write(data);
            }
        }

        byte[] encoded = result.toByteArray();

        for (int i = 0; i < 10; i++) {
            assertArrayEquals(data, Arrays.copyOfRange(encoded, i * data.length, (i + 1) * data.length));
        }
    }

    @Test
    public void testEncodeArrayOffset() throws IOException {
        byte[] data = createData(87);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (OutputStream stream = new EncoderStream(result, new NullEncoder())) {
            for (int i = 0; i < 10; i++) {
                stream.write(data, 13, 59);
            }
        }

        byte[] original = Arrays.copyOfRange(data, 13, 13 + 59);
        byte[] encoded = result.toByteArray();

        for (int i = 0; i < 10; i++) {
            assertArrayEquals(original, Arrays.copyOfRange(encoded, i * original.length, (i + 1) * original.length));
        }
    }

    private static final class NullEncoder implements Encoder {
        @Override
        public void encode(OutputStream stream, ByteBuffer buffer) throws IOException {
            stream.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        }
    }
}