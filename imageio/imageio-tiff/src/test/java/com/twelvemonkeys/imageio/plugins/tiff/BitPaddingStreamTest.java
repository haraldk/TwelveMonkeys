/*
 * Copyright (c) 2016, Harald Kuhr
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * BitPaddingStreamTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: BitPaddingStreamTest.java,v 1.0 16/11/2016 harald.kuhr Exp$
 */
public class BitPaddingStreamTest {

    @Test
    public void testCreateNullStream() {
        assertThrows(IllegalArgumentException.class, () -> new BitPaddingStream(null, 1, 12, 4, ByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testCreateBadBits() {
        assertThrows(IllegalArgumentException.class, () -> new BitPaddingStream(new ByteArrayInputStream(new byte[6]), 1, 7, 4, ByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testCreateBadBitsLarge() {
        assertThrows(IllegalArgumentException.class, () -> new BitPaddingStream(new ByteArrayInputStream(new byte[6]), 1, 37, 4, ByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testCreateNullByteOrder() {
        assertThrows(IllegalArgumentException.class, () -> new BitPaddingStream(new ByteArrayInputStream(new byte[6]), 1, 12, 4, null));
    }

    @Test
    public void testRead() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.BIG_ENDIAN);
        assertEquals(0x0f, stream.read());
        assertEquals(0xff, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x06, stream.read());
        assertEquals(0x66, stream.read());

        assertEquals(0x07, stream.read());
        assertEquals(0x89, stream.read());

        assertEquals(-1, stream.read());
    }

    // TODO: Test read 10, 14, etc bits....

    @Test
    public void testReadLittleEndian() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.LITTLE_ENDIAN);
        assertEquals(0xff, stream.read());
        assertEquals(0x0f, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x66, stream.read());
        assertEquals(0x06, stream.read());

        assertEquals(0x89, stream.read());
        assertEquals(0x07, stream.read());

        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead3Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x60};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 3, 12, 1, ByteOrder.BIG_ENDIAN);
        assertEquals(0x0f, stream.read());
        assertEquals(0xff, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x06, stream.read());
        assertEquals(0x66, stream.read());

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66, 0x07, (byte) 0x89}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArrayLittleEndian() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.LITTLE_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {(byte) 0xff, 0x0f, 0x00, 0x00, 0x66, 0x06, (byte) 0x89, 0x07}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArray2Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 2, 12, 2, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66, 0x07, (byte) 0x89}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArray3Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x6f};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 3, 12, 1, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[6];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArray4Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 4, 12, 1, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66, 0x07, (byte) 0x89}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testSkip() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.BIG_ENDIAN);

        assertEquals(4, stream.skip(4));   // Normal skip
        assertEquals(0x06, stream.read()); // Verify position after skip
        assertEquals(3, stream.skip(4));   // Partial skip
        assertEquals(-1, stream.read());   // Verify position (EOF)
    }
}