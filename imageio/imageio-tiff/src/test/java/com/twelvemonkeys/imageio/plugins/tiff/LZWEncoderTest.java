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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.enc.Decoder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * LZWEncoderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWEncoderTest.java,v 1.0 06.12.13 13:48 haraldk Exp$
 */
public class LZWEncoderTest {

    static final int SPEED_TEST_RUNS = 1024;
    static final int LENGTH = 1024;
    static final int ITERATIONS = 4;

    private final Random random = new Random(2451348571893475l);

    @Test
    public void testExample() throws IOException {
        byte[] bytes = new byte[] {7, 7, 7, 8, 8, 7, 7, 6, 6};
        LZWEncoder encoder = new LZWEncoder(bytes.length);

        OutputStream stream = new FastByteArrayOutputStream(10);
        encoder.encode(stream, ByteBuffer.wrap(bytes));
    }

    @Test
    public void testExampleEncodeDecode() throws IOException {
        byte[] bytes = new byte[] {7, 7, 7, 8, 8, 7, 7, 6, 6};
        LZWEncoder encoder = new LZWEncoder(bytes.length);

        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(10);
        encoder.encode(stream, ByteBuffer.wrap(bytes));

        ByteArrayInputStream inputStream = stream.createInputStream();
        Decoder decoder = LZWDecoder.create(false);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        int index = 0;

        while (decoder.decode(inputStream, buffer) > 0) {
            buffer.flip();

            while (buffer.hasRemaining()) {
                assertEquals(String.format("Diff at index %s", index), bytes[index], buffer.get());
                index++;
            }

            buffer.clear();
        }

        assertEquals(9, index);
        assertEquals(-1, inputStream.read());
    }

    @Test
    public void testEncodeDecode() throws IOException {
        byte[] bytes = new byte[LENGTH];
        LZWEncoder encoder = new LZWEncoder(bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        FastByteArrayOutputStream stream = new FastByteArrayOutputStream((LENGTH * 3) / 4);

        for (int i = 0; i < ITERATIONS; i++) {
            encoder.encode(stream, ByteBuffer.wrap(bytes, i * LENGTH / ITERATIONS, LENGTH / ITERATIONS));
        }

        ByteArrayInputStream inputStream = stream.createInputStream();
        LZWDecoder decoder = new LZWDecoder.LZWSpecDecoder(); // Strict mode
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH / ITERATIONS);

        int index = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            while (decoder.decode(inputStream, buffer) > 0) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte expected = bytes[index];
                    byte actual = buffer.get();
                    assertEquals(String.format("Diff at index %s: 0x%02x != 0x%02x", index, expected, actual), expected, actual);
                    index++;
                }

                buffer.clear();
            }
        }

        assertEquals(LENGTH, index);
        assertEquals(-1, inputStream.read());
    }

    @Test
    public void testEncodeDecodeRandom() throws IOException {
        byte[] bytes = new byte[LENGTH];
        LZWEncoder encoder = new LZWEncoder(bytes.length);

        random.nextBytes(bytes);

        FastByteArrayOutputStream stream = new FastByteArrayOutputStream((LENGTH * 3) / 4);

        for (int i = 0; i < ITERATIONS; i++) {
            encoder.encode(stream, ByteBuffer.wrap(bytes, i * LENGTH / ITERATIONS, LENGTH / ITERATIONS));
        }

        ByteArrayInputStream inputStream = stream.createInputStream();
        LZWDecoder decoder = new LZWDecoder.LZWSpecDecoder(); // Strict mode
        ByteBuffer buffer = ByteBuffer.allocate(LENGTH / ITERATIONS);

        int index = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            while (decoder.decode(inputStream, buffer) > 0) {
                buffer.flip();

                while (buffer.hasRemaining()) {
                    byte expected = bytes[index];
                    byte actual = buffer.get();
                    assertEquals(String.format("Diff at index %s: 0x%02x != 0x%02x", index, expected, actual), expected, actual);
//                    System.err.println(String.format("Equal at index %s: 0x%02x (%d)", index, expected & 0xff, expected));
                    index++;
                }

                buffer.clear();
            }
        }

        assertEquals(LENGTH, index);
        assertEquals(-1, inputStream.read());
    }

    @Ignore
    @Test(timeout = 10000)
    public void testSpeed() throws IOException {
        for (int run = 0; run < SPEED_TEST_RUNS; run++) {
            byte[] bytes = new byte[LENGTH];
            LZWEncoder encoder = new LZWEncoder(bytes.length);

            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) i;
            }

            FastByteArrayOutputStream stream = new FastByteArrayOutputStream((LENGTH * 3) / 4);

            for (int i = 0; i < ITERATIONS; i++) {
                encoder.encode(stream, ByteBuffer.wrap(bytes, i * LENGTH / ITERATIONS, LENGTH / ITERATIONS));
            }

            assertEquals(719, stream.size());
        }
    }
}
