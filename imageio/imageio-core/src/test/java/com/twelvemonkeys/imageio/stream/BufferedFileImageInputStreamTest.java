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

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Random;

import static com.twelvemonkeys.imageio.stream.BufferedImageInputStreamTest.rangeEquals;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * BufferedFileImageInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedFileImageInputStreamTestCase.java,v 1.0 Apr 21, 2009 10:58:48 AM haraldk Exp$
 */
public class BufferedFileImageInputStreamTest {
    private final Random random = new Random(170984354357234566L);

    private File randomDataToFile(byte[] data) throws IOException {
        random.nextBytes(data);

        File file = File.createTempFile("read", ".tmp");
        Files.write(file.toPath(), data);
        return file;
    }

    @Test
    public void testCreate() throws IOException {
        BufferedFileImageInputStream stream = new BufferedFileImageInputStream(File.createTempFile("empty", ".tmp"));
        assertEquals("Data length should be same as stream length", 0, stream.length());
    }

    @Test
    public void testCreateNullFile() throws IOException {
        try {
            new BufferedFileImageInputStream((File) null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("file"));
            assertTrue("Exception message does not contain null", message.contains("null"));
        }
    }

    @Test
    public void testCreateNullRAF() {
        try {
            new BufferedFileImageInputStream((RandomAccessFile) null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("raf"));
            assertTrue("Exception message does not contain null", message.contains("null"));
        }
    }

    @Test
    public void testRead() throws IOException {
        byte[] data = new byte[1024 * 1024];
        File file = randomDataToFile(data);

        BufferedFileImageInputStream stream = new BufferedFileImageInputStream(file);

        assertEquals("File length should be same as stream length", file.length(), stream.length());

        for (byte value : data) {
            assertEquals("Wrong data read", value & 0xff, stream.read());
        }
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] data = new byte[1024 * 1024];
        File file = randomDataToFile(data);

        BufferedFileImageInputStream stream = new BufferedFileImageInputStream(file);

        assertEquals("File length should be same as stream length", file.length(), stream.length());

        byte[] result = new byte[1024];

        for (int i = 0; i < data.length / result.length; i++) {
            stream.readFully(result);
            assertTrue("Wrong data read: " + i, rangeEquals(data, i * result.length, result, 0, result.length));
        }
    }

    @Test
    public void testReadSkip() throws IOException {
        byte[] data = new byte[1024 * 14];
        File file = randomDataToFile(data);

        BufferedFileImageInputStream stream = new BufferedFileImageInputStream(file);

        assertEquals("File length should be same as stream length", file.length(), stream.length());

        byte[] result = new byte[7];

        for (int i = 0; i < data.length / result.length; i += 2) {
            stream.readFully(result);
            stream.skipBytes(result.length);
            assertTrue("Wrong data read: " + i, rangeEquals(data, i * result.length, result, 0, result.length));
        }
    }

    @Test
    public void testReadSeek() throws IOException {
        byte[] data = new byte[1024 * 18];
        File file = randomDataToFile(data);

        BufferedFileImageInputStream stream = new BufferedFileImageInputStream(file);

        assertEquals("File length should be same as stream length", file.length(), stream.length());

        byte[] result = new byte[9];

        for (int i = 0; i < data.length / result.length; i++) {
            // Read backwards
            long newPos = stream.length() - result.length - i * result.length;
            stream.seek(newPos);
            assertEquals("Wrong stream position", newPos, stream.getStreamPosition());
            stream.readFully(result);
            assertTrue("Wrong data read: " + i, rangeEquals(data, (int) newPos, result, 0, result.length));
        }
    }

    @Test
    public void testReadBitRandom() throws IOException {
        byte[] bytes = new byte[8];
        File file = randomDataToFile(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        ImageInputStream stream = new BufferedFileImageInputStream(file);

        for (int i = 1; i <= 64; i++) {
            assertEquals(String.format("bit %d differ", i), (value << (i - 1L)) >>> 63L, stream.readBit());
        }
    }

    @Test
    public void testReadBitsRandom() throws IOException {
        byte[] bytes = new byte[8];
        File file = randomDataToFile(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        ImageInputStream stream = new BufferedFileImageInputStream(file);

        for (int i = 1; i <= 64; i++) {
            stream.seek(0);
            assertEquals(String.format("bit %d differ", i), value >>> (64L - i), stream.readBits(i));
            assertEquals(i % 8, stream.getBitOffset());
        }
    }

    @Test
    public void testReadBitsRandomOffset() throws IOException {
        byte[] bytes = new byte[8];
        File file = randomDataToFile(bytes);
        long value = ByteBuffer.wrap(bytes).getLong();

        // Create stream
        ImageInputStream stream = new BufferedFileImageInputStream(file);

        for (int i = 1; i <= 60; i++) {
            stream.seek(0);
            stream.setBitOffset(i % 8);
            assertEquals(String.format("bit %d differ", i), (value << (i % 8)) >>> (64L - i), stream.readBits(i));
            assertEquals(i * 2 % 8, stream.getBitOffset());
        }
    }

    @Test
    public void testReadShort() throws IOException {
        byte[] bytes = new byte[8743]; // Slightly more than one buffer size
        File file = randomDataToFile(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        final ImageInputStream stream = new BufferedFileImageInputStream(file);
        stream.setByteOrder(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < bytes.length / 2; i++) {
            assertEquals(buffer.getShort(), stream.readShort());
        }

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readShort();
            }
        });

        stream.seek(0);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < bytes.length / 2; i++) {
            assertEquals(buffer.getShort(), stream.readShort());
        }

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readShort();
            }
        });
    }

    @Test
    public void testReadInt() throws IOException {
        byte[] bytes = new byte[8743]; // Slightly more than one buffer size
        File file = randomDataToFile(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        final ImageInputStream stream = new BufferedFileImageInputStream(file);
        stream.setByteOrder(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < bytes.length / 4; i++) {
            assertEquals(buffer.getInt(), stream.readInt());
        }

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readInt();
            }
        });

        stream.seek(0);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < bytes.length / 4; i++) {
            assertEquals(buffer.getInt(), stream.readInt());
        }

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readInt();
            }
        });
    }

    @Test
    public void testReadLong() throws IOException {
        byte[] bytes = new byte[8743]; // Slightly more than one buffer size
        File file = randomDataToFile(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        final ImageInputStream stream = new BufferedFileImageInputStream(file);
        stream.setByteOrder(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < bytes.length / 8; i++) {
            assertEquals(buffer.getLong(), stream.readLong());
        }

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readLong();
            }
        });

        stream.seek(0);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < bytes.length / 8; i++) {
            assertEquals(buffer.getLong(), stream.readLong());
        }

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readLong();
            }
        });
    }

    @Test
    public void testSeekPastEOF() throws IOException {
        byte[] bytes = new byte[9];
        File file = randomDataToFile(bytes);

        final ImageInputStream stream = new BufferedFileImageInputStream(file);
        stream.seek(1000);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[1], 0, 1));

        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readFully(new byte[1]);
            }
        });
        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readByte();
            }
        });
        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readShort();
            }
        });
        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readInt();
            }
        });
        assertThrows(EOFException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stream.readLong();
            }
        });

        stream.seek(0);
        for (byte value : bytes) {
            assertEquals(value, stream.readByte());
        }

        assertEquals(-1, stream.read());
    }

    @Test
    public void testClose() throws IOException {
        // Create wrapper stream
        RandomAccessFile mock = mock(RandomAccessFile.class);
        ImageInputStream stream = new BufferedFileImageInputStream(mock);

        stream.close();
        verify(mock, only()).close();
    }

    @Test
    public void testWorkaroundForWBMPImageReaderExpectsReadToBehaveAsReadFully() throws IOException {
        // See #606 for details.
        // Bug in JDK WBMPImageReader, uses read(byte[], int, int) instead of readFully(byte[], int, int).
        // Ie: Relies on read to return all bytes at once, without blocking
        int size = BufferedFileImageInputStream.DEFAULT_BUFFER_SIZE * 7;
        byte[] bytes = new byte[size];
        File file = randomDataToFile(bytes);

        try (BufferedFileImageInputStream stream = new BufferedFileImageInputStream(file)) {
            byte[] result = new byte[size];
            int head = stream.read(result, 0, 12);          // Provoke a buffered read
            int len = stream.read(result, 12, size - 12);   // Rest of buffer + direct read

            assertEquals(size, len + head);
            assertArrayEquals(bytes, result);
        }
   }
}
