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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class DecoderStreamTest {

    private final Random rng = new Random(5467809876546L);

    private byte[] createData(final int length) {
        byte[] data = new byte[length];
        rng.nextBytes(data);
        return data;
    }

    @Test
    public void testDecodeSingleBytes() throws IOException {
        byte[] data = createData(1327);

        InputStream source = new ByteArrayInputStream(data);
        try (InputStream stream = new DecoderStream(source, new NullDecoder())) {
            for (byte datum : data) {
                int read = stream.read();
                assertNotEquals(-1, read);
                assertEquals(datum, (byte) read);
            }

            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testDecodeArray() throws IOException {
        int length = 793;
        byte[] data = createData(length * 10);

        InputStream source = new ByteArrayInputStream(data);
        byte[] result = new byte[477];

        try (InputStream stream = new DecoderStream(source, new NullDecoder())) {
            int dataOffset = 0;
            while (dataOffset < data.length) {
                int count = stream.read(result);

                assertFalse(count <= 0);
                assertArrayEquals(Arrays.copyOfRange(data, dataOffset, dataOffset + count), Arrays.copyOfRange(result, 0, count));

                dataOffset += count;
            }

            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testDecodeArrayOffset() throws IOException {
        int length = 793;
        byte[] data = createData(length * 10);

        InputStream source = new ByteArrayInputStream(data);
        byte[] result = new byte[477];

        try (InputStream stream = new DecoderStream(source, new NullDecoder())) {
            int dataOffset = 0;
            while (dataOffset < data.length) {
                int resultOffset = dataOffset % result.length;
                int count = stream.read(result, resultOffset, result.length - resultOffset);

                assertFalse(count <= 0);
                assertArrayEquals(Arrays.copyOfRange(data, dataOffset + resultOffset, dataOffset + count), Arrays.copyOfRange(result, resultOffset, count));

                dataOffset += count;
            }

            assertEquals(-1, stream.read());
        }
    }

    private static final class NullDecoder implements Decoder {
        @Override
        public int decode(InputStream stream, ByteBuffer buffer) throws IOException {
            int read = stream.read(buffer.array(), buffer.arrayOffset(), buffer.remaining());

            if (read > 0) {
                // Set position, should be equivalent to using buffer.put(stream.read()) until EOF or buffer full
                buffer.position(read);
            }

            return read;
        }
    }
}