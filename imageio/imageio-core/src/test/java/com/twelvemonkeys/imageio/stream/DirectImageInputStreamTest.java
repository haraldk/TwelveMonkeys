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

package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static com.twelvemonkeys.imageio.stream.BufferedImageInputStreamTest.rangeEquals;

import static org.mockito.Mockito.*;

/**
 * NonSeekableImageInputStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: NonSeekableImageInputStreamTest.java,v 1.0 Apr 21, 2009 10:58:48 AM haraldk Exp$
 */
public class DirectImageInputStreamTest {
    private final Random random = new Random(170984354357234566L);

    private InputStream randomData(byte[] data) {
        random.nextBytes(data);

        return new ByteArrayInputStream(data);
    }

    @Test
    public void testCreate() throws IOException {
        try (DirectImageInputStream stream = new DirectImageInputStream(new ByteArrayInputStream(new byte[0]), 0)) {
            assertEquals(0, stream.length(), "Data length should be same as stream length");
        }
    }

    @Test
    public void testCreateNullFile() throws IOException {
        try (@SuppressWarnings("unused") DirectImageInputStream stream = new DirectImageInputStream(null)) {
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("stream"), "Exception message does not contain parameter name");
            assertTrue(message.contains("null"), "Exception message does not contain null");
        }
    }

    @Test
    public void testRead() throws IOException {
        byte[] data = new byte[1024 * 1024];
        InputStream input = randomData(data);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            for (byte value : data) {
                assertEquals(value & 0xff, stream.read(), "Wrong data read");
            }
        }
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] data = new byte[1024 * 10];
        InputStream input = randomData(data);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            byte[] result = new byte[1024];

            for (int i = 0; i < data.length / result.length; i++) {
                stream.readFully(result);
                assertTrue(rangeEquals(data, i * result.length, result, 0, result.length), "Wrong data read: " + i);
            }
        }
    }

    @Test
    public void testReadSkip() throws IOException {
        byte[] data = new byte[1024 * 14];
        InputStream input = randomData(data);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            byte[] result = new byte[7];

            for (int i = 0; i < data.length / result.length; i += 2) {
                stream.readFully(result);
                stream.skipBytes(result.length);
                assertTrue(rangeEquals(data, i * result.length, result, 0, result.length), "Wrong data read: " + i);
            }
        }
    }

    @Test
    public void testReadSeek() throws IOException {
        byte[] data = new byte[24 * 18];
        InputStream input = randomData(data);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            byte[] result = new byte[9];

            for (int i = 0; i < data.length / (2 * result.length); i++) {
                long newPos = i * 2 * result.length;
                stream.seek(newPos);
                assertEquals(newPos, stream.getStreamPosition(), "Wrong stream position");
                stream.readFully(result);
                assertTrue( rangeEquals(data, (int) newPos, result, 0, result.length), "Wrong data read: " + i);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Disabled("Bit reading requires backwards seek or buffer...")
    @Test
    public void testReadBitRandom() throws IOException {
        byte[] bytes = new byte[8];
        InputStream input = randomData(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            for (int i = 1; i <= 64; i++) {
                assertEquals((value << (i - 1L)) >>> 63L, stream.readBit(), String.format("bit %d differ", i));
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Disabled("Bit reading requires backwards seek or buffer...")
    @Test
    public void testReadBitsRandom() throws IOException {
        byte[] bytes = new byte[8];
        InputStream input = randomData(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            for (int i = 1; i <= 64; i++) {
                stream.seek(0);
                assertEquals(value >>> (64L - i), stream.readBits(i), String.format("bit %d differ", i));
                assertEquals(i % 8, stream.getBitOffset());
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Disabled("Bit reading requires backwards seek or buffer...")
    @Test
    public void testReadBitsRandomOffset() throws IOException {
        byte[] bytes = new byte[8];
        InputStream input = randomData(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            for (int i = 1; i <= 60; i++) {
                stream.seek(0);
                stream.setBitOffset(i % 8);
                assertEquals((value << (i % 8)) >>> (64L - i), stream.readBits(i), String.format("bit %d differ", i));
                assertEquals(i * 2L % 8, stream.getBitOffset());
            }
        }
    }

    @Test
    public void testReadShort() throws IOException {
        byte[] bytes = new byte[31];
        InputStream input = randomData(bytes);

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < bytes.length / 2; i++) {
                assertEquals(buffer.getShort(), stream.readShort());
            }

            assertThrows(EOFException.class, () -> stream.readShort());
        }

        try (DirectImageInputStream stream = new DirectImageInputStream(new ByteArrayInputStream(bytes))) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            buffer.position(0);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < bytes.length / 2; i++) {
                assertEquals(buffer.getShort(), stream.readShort());
            }

            assertThrows(EOFException.class, () -> stream.readShort());
        }
    }

    @Test
    public void testReadInt() throws IOException {
        byte[] bytes = new byte[31];
        InputStream input = randomData(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        // Create stream
        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < bytes.length / 4; i++) {
                assertEquals(buffer.getInt(), stream.readInt());
            }
            assertThrows(EOFException.class, () -> stream.readInt());
        }

        try (DirectImageInputStream stream = new DirectImageInputStream(new ByteArrayInputStream(bytes))) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            buffer.position(0);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < bytes.length / 4; i++) {
                assertEquals(buffer.getInt(), stream.readInt());
            }

            assertThrows(EOFException.class, () -> stream.readInt());
        }
    }

    @Test
    public void testReadLong() throws IOException {
        byte[] bytes = new byte[8743]; // Slightly more than one buffer size
        InputStream input = randomData(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < bytes.length / 8; i++) {
                assertEquals(buffer.getLong(), stream.readLong());
            }
            assertThrows(EOFException.class, () -> stream.readLong());
        }

        try (DirectImageInputStream stream = new DirectImageInputStream(new ByteArrayInputStream(bytes))) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            buffer.position(0);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < bytes.length / 8; i++) {
                assertEquals(buffer.getLong(), stream.readLong());
            }
            assertThrows(EOFException.class, () -> stream.readLong());
        }
    }

    @Test
    public void testSeekPastEOF() throws IOException {
        byte[] bytes = new byte[9];
        InputStream input = randomData(bytes);

        try (DirectImageInputStream stream = new DirectImageInputStream(input)) {
            stream.seek(1000);

            assertEquals(-1, stream.read());
            assertEquals(-1, stream.read(new byte[1], 0, 1));

            assertThrows(EOFException.class, () -> stream.readFully(new byte[1]));
            assertThrows(EOFException.class, () -> stream.readByte());
            assertThrows(EOFException.class, () -> stream.readShort());
            assertThrows(EOFException.class, () -> stream.readInt());
            assertThrows(EOFException.class, () -> stream.readLong());
        }

        try (DirectImageInputStream stream = new DirectImageInputStream(new ByteArrayInputStream(bytes))) {
            for (byte value : bytes) {
                assertEquals(value, stream.readByte());
            }

            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testClose() throws IOException {
        // Create wrapper stream
        InputStream input = mock(InputStream.class);
        ImageInputStream stream = new DirectImageInputStream(input);

        stream.close();
        verify(input, only()).close();
    }
}
