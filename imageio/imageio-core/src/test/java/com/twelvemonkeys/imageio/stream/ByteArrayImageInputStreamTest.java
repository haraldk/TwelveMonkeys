/*
 * Copyright (c) 2009, Harald Kuhr
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


import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static com.twelvemonkeys.imageio.stream.BufferedImageInputStreamTest.rangeEquals;

/**
 * ByteArrayImageInputStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ByteArrayImageInputStreamTest.java,v 1.0 Apr 21, 2009 10:58:48 AM haraldk Exp$
 */
public class ByteArrayImageInputStreamTest {
    private final Random random = new Random(1709843507234566L);

    @Test
    public void testCreate() {
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(new byte[0]);
        assertEquals(0, stream.length(), "Data length should be same as stream length");
    }

    @Test
    public void testCreateNull() {
        try {
            new ByteArrayImageInputStream(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("data"), "Exception message does not contain parameter name");
            assertTrue(message.contains("null"), "Exception message does not contain null");
        }
    }

    @Test
    public void testCreateNullOffLen() {
        try {
            new ByteArrayImageInputStream(null, 0, -1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("data"), "Exception message does not contain parameter name");
            assertTrue(message.contains("null"), "Exception message does not contain null");
        }
    }

    @Test
    public void testCreateNegativeOff() {
        try {
            new ByteArrayImageInputStream(new byte[0], -1, 1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("offset"), "Exception message does not contain parameter name");
            assertTrue(message.contains("-1"), "Exception message does not contain -1");
        }
    }

    @Test
    public void testCreateBadOff() {
        try {
            new ByteArrayImageInputStream(new byte[1], 2, 0);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("offset"), "Exception message does not contain parameter name");
            assertTrue(message.contains("2"), "Exception message does not contain 2");
        }
    }

    @Test
    public void testCreateNegativeLen() {
        try {
            new ByteArrayImageInputStream(new byte[0], 0, -1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("length"), "Exception message does not contain parameter name");
            assertTrue(message.contains("-1"), "Exception message does not contain -1");
        }
    }

    @Test
    public void testCreateBadLen() {
        try {
            new ByteArrayImageInputStream(new byte[1], 0, 2);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("length"), "Exception message does not contain parameter name");
            assertTrue(message.contains("2"), "Exception message does not contain ™");
        }
    }

    @Test
    public void testRead() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals(data.length, stream.length(), "Data length should be same as stream length");

        for (byte b : data) {
            assertEquals(b & 0xff, stream.read(), "Wrong data read");
        }
    }

    @Test
    public void testReadOffsetLen() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        int offset = random.nextInt(data.length / 10);
        int length = random.nextInt(data.length - offset);
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data, offset, length);

        assertEquals(length, stream.length(), "Data length should be same as stream length");

        for (int i = offset; i < offset + length; i++) {
            assertEquals(data[i] & 0xff, stream.read(), "Wrong data read");
        }
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals(data.length, stream.length(), "Data length should be same as stream length");

        byte[] result = new byte[1024];

        for (int i = 0; i < data.length / result.length; i++) {
            stream.readFully(result);
            assertTrue(rangeEquals(data, i * result.length, result, 0, result.length), "Wrong data read: " + i);
        }
    }

    @Test
    public void testReadArrayOffLen() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        int offset = random.nextInt(data.length - 10240);
        int length = 10240;
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data, offset, length);

        assertEquals(length, stream.length(), "Data length should be same as stream length");

        byte[] result = new byte[1024];

        for (int i = 0; i < length / result.length; i++) {
            stream.readFully(result);
            assertTrue(rangeEquals(data, offset + i * result.length, result, 0, result.length), "Wrong data read: " + i);
        }
    }

    @Test
    public void testReadSkip() throws IOException {
        byte[] data = new byte[1024 * 14];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals(data.length, stream.length(), "Data length should be same as stream length");

        byte[] result = new byte[7];

        for (int i = 0; i < data.length / result.length; i += 2) {
            stream.readFully(result);
            stream.skipBytes(result.length);
            assertTrue(rangeEquals(data, i * result.length, result, 0, result.length), "Wrong data read: " + i);
        }
    }

    @Test
    public void testReadSeek() throws IOException {
        byte[] data = new byte[1024 * 18];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals(data.length, stream.length(), "Data length should be same as stream length");

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
