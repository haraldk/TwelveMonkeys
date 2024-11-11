/*
 * Copyright (c) 2020, Harald Kuhr
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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static com.twelvemonkeys.imageio.stream.BufferedImageInputStreamTest.rangeEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * BufferedFileImageInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedFileImageInputStreamTestCase.java,v 1.0 Apr 21, 2009 10:58:48 AM haraldk Exp$
 */
public class BufferedChannelImageInputStreamTest {
    private final Random random = new Random(170984354357234566L);

    private File randomDataToFile(byte[] data) throws IOException {
        random.nextBytes(data);

        File file = File.createTempFile("read", ".tmp");
        Files.write(file.toPath(), data);
        return file;
    }

    @Test
    public void testCreate() throws IOException {
        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(File.createTempFile("empty", ".tmp")))) {
            assertEquals(0, stream.length(), "Data length should be same as stream length");
        }
    }

    @Test
    public void testCreateNullFileInputStream() {
        try {
            new BufferedChannelImageInputStream((FileInputStream) null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("inputstream"), "Exception message does not contain parameter name");
            assertTrue(message.contains("null"), "Exception message does not contain null");
        }
    }

    @Test
    public void testCreateNullByteChannel() {
        try {
            new BufferedChannelImageInputStream((SeekableByteChannel) null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("channel"), "Exception message does not contain parameter name");
            assertTrue(message.contains("null"), "Exception message does not contain null");
        }
    }

    @Test
    public void testRead() throws IOException {
        byte[] data = new byte[1024 * 1024];
        File file = randomDataToFile(data);

        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            assertEquals(file.length(), stream.length(), "File length should be same as stream length");

            for (byte value : data) {
                assertEquals(value & 0xff, stream.read(), "Wrong data read");
            }
        }
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] data = new byte[1024 * 1024];
        File file = randomDataToFile(data);

        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            assertEquals(file.length(), stream.length(), "File length should be same as stream length");

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
        File file = randomDataToFile(data);

        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            assertEquals(file.length(), stream.length(), "File length should be same as stream length");

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
        byte[] data = new byte[1024 * 18];
        File file = randomDataToFile(data);

        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            assertEquals(file.length(), stream.length(), "File length should be same as stream length");

            byte[] result = new byte[9];

            for (int i = 0; i < data.length / result.length; i++) {
                // Read backwards
                long newPos = stream.length() - result.length - i * result.length;
                stream.seek(newPos);
                assertEquals(newPos, stream.getStreamPosition(), "Wrong stream position");
                stream.readFully(result);
                assertTrue(rangeEquals(data, (int) newPos, result, 0, result.length), "Wrong data read: " + i);
            }
        }
    }

    @Test
    public void testReadOutsideDataSeek0Read() throws IOException {
        byte[] data = new byte[256];
        File file = randomDataToFile(data);

        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            assertEquals(file.length(), stream.length(), "File length should be same as stream length");

            byte[] buffer = new byte[data.length * 2];
            stream.read(buffer);
            stream.seek(0);
            assertNotEquals(-1, stream.read());
            assertNotEquals(-1, stream.read(buffer));
        }
    }

    @Test
    public void testReadBitRandom() throws IOException {
        byte[] bytes = new byte[8];
        File file = randomDataToFile(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        try (ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            for (int i = 1; i <= 64; i++) {
                assertEquals((value << (i - 1L)) >>> 63L, stream.readBit(), String.format("bit %d differ", i));
            }
        }
    }

    @Test
    public void testReadBitsRandom() throws IOException {
        byte[] bytes = new byte[8];
        File file = randomDataToFile(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        try (ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            for (int i = 1; i <= 64; i++) {
                stream.seek(0);
                assertEquals(value >>> (64L - i), stream.readBits(i), String.format("bit %d differ", i));
                assertEquals(i % 8, stream.getBitOffset());
            }
        }
    }

    @Test
    public void testReadBitsRandomOffset() throws IOException {
        byte[] bytes = new byte[8];
        File file = randomDataToFile(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        try (ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            for (int i = 1; i <= 60; i++) {
                stream.seek(0);
                stream.setBitOffset(i % 8);
                assertEquals((value << (i % 8)) >>> (64L - i), stream.readBits(i), String.format("bit %d differ", i));
                assertEquals(i * 2 % 8, stream.getBitOffset());
            }
        }
    }

    @Test
    public void testReadShort() throws IOException {
        byte[] bytes = new byte[8743]; // Slightly more than one buffer size
        File file = randomDataToFile(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        try (final ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < bytes.length / 2; i++) {
                assertEquals(buffer.getShort(), stream.readShort());
            }

            assertThrows(EOFException.class, () -> stream.readShort());

            stream.seek(0);
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
        byte[] bytes = new byte[8743]; // Slightly more than one buffer size
        File file = randomDataToFile(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        try (final ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < bytes.length / 4; i++) {
                assertEquals(buffer.getInt(), stream.readInt());
            }

            assertThrows(EOFException.class, () -> stream.readInt());

            stream.seek(0);
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
        File file = randomDataToFile(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        try (final ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < bytes.length / 8; i++) {
                assertEquals(buffer.getLong(), stream.readLong());
            }
            assertThrows(EOFException.class, () -> stream.readLong());

            stream.seek(0);
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
        File file = randomDataToFile(bytes);

        try (final ImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            stream.seek(1000);

            assertEquals(-1, stream.read());
            assertEquals(-1, stream.read(new byte[1], 0, 1));

            assertThrows(EOFException.class, () -> stream.readFully(new byte[1]));
            assertThrows(EOFException.class, () -> stream.readByte());
            assertThrows(EOFException.class, () -> stream.readShort());
            assertThrows(EOFException.class, () -> stream.readInt());
            assertThrows(EOFException.class, () -> stream.readLong());

            stream.seek(0);
            for (byte value : bytes) {
                assertEquals(value, stream.readByte());
            }

            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testCloseChannel() throws IOException {
        SeekableByteChannel channel = mock(SeekableByteChannel.class);
        ImageInputStream stream = new BufferedChannelImageInputStream(channel);

        stream.close();
        verify(channel, never()).close();
    }

    @Test
    public void testWorkaroundForWBMPImageReaderExpectsReadToBehaveAsReadFully() throws IOException {
        // See #606 for details.
        // Bug in JDK WBMPImageReader, uses read(byte[], int, int) instead of readFully(byte[], int, int).
        // Ie: Relies on read to return all bytes at once, without blocking
        int size = BufferedChannelImageInputStream.DEFAULT_BUFFER_SIZE * 7;
        byte[] bytes = new byte[size];
        File file = randomDataToFile(bytes);

        try (BufferedChannelImageInputStream stream = new BufferedChannelImageInputStream(new FileInputStream(file))) {
            byte[] result = new byte[size];
            int head = stream.read(result, 0, 12);          // Provoke a buffered read
            int len = stream.read(result, 12, size - 12);   // Rest of buffer + direct read

            assertEquals(size, len + head);
            assertArrayEquals(bytes, result);
        }
   }
}
