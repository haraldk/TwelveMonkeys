/*
 * Copyright (c) 2008, Harald Kuhr
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

import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.io.ole2.Entry;


import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static java.util.Arrays.fill;
import static org.mockito.Mockito.*;

/**
 * BufferedImageInputStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedImageInputStreamTest.java,v 1.0 Jun 30, 2008 3:07:42 PM haraldk Exp$
 */
@SuppressWarnings("deprecation")
public class BufferedImageInputStreamTest {
    private final Random random = new Random(3450972865211L);

    @Test
    public void testCreate() throws IOException {
        new BufferedImageInputStream(new ByteArrayImageInputStream(new byte[0]));
    }

    @Test
    public void testCreateNull() throws IOException {
        try {
            new BufferedImageInputStream(null);
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
    public void testReadBit() throws IOException {
        byte[] bytes = new byte[] {(byte) 0xF0, (byte) 0x0F};

        // Create wrapper stream
        BufferedImageInputStream stream = new BufferedImageInputStream(new ByteArrayImageInputStream(bytes));

        // Read all bits
        assertEquals(1, stream.readBit());
        assertEquals(1, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(2, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(3, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(4, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0, stream.readBit());
        assertEquals(5, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0, stream.readBit());
        assertEquals(6, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0, stream.readBit());
        assertEquals(7, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0, stream.readBit()); // last bit
        assertEquals(0, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        // Full reset, read same sequence again
        stream.seek(0);
        assertEquals(0, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());
        assertEquals(0, stream.readBit());
        assertEquals(0, stream.readBit());
        assertEquals(0, stream.readBit());
        assertEquals(0, stream.readBit());

        assertEquals(0, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        // Full reset, read partial
        stream.seek(0);

        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());

        // Byte reset, read same sequence again
        stream.setBitOffset(0);

        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());
        assertEquals(1, stream.readBit());
        assertEquals(0, stream.readBit());

        // Byte reset, read partial sequence again
        stream.setBitOffset(3);

        assertEquals(1, stream.readBit());
        assertEquals(0, stream.readBit());
        assertEquals(0, stream.getStreamPosition());

        // Byte reset, read partial sequence again
        stream.setBitOffset(6);

        assertEquals(0, stream.readBit());
        assertEquals(0, stream.readBit());
        assertEquals(1, stream.getStreamPosition());

        // Read all bits, second byte
        assertEquals(0, stream.readBit());
        assertEquals(1, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(0, stream.readBit());
        assertEquals(2, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(0, stream.readBit());
        assertEquals(3, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(0, stream.readBit());
        assertEquals(4, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(5, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(6, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(1, stream.readBit());
        assertEquals(7, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(1, stream.readBit()); // last bit
        assertEquals(0, stream.getBitOffset());
        assertEquals(2, stream.getStreamPosition());
    }

    @Test
    public void testReadBits() throws IOException {
        byte[] bytes = new byte[] {(byte) 0xF0, (byte) 0xCC, (byte) 0xAA};

        // Create wrapper stream
        BufferedImageInputStream stream = new BufferedImageInputStream(new ByteArrayImageInputStream(bytes));

        // Read all bits, first byte
        assertEquals(3, stream.readBits(2));
        assertEquals(2, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(3, stream.readBits(2));
        assertEquals(4, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0, stream.readBits(2));
        assertEquals(6, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0, stream.readBits(2));
        assertEquals(0, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        // Read all bits, second byte
        assertEquals(3, stream.readBits(2));
        assertEquals(2, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(0, stream.readBits(2));
        assertEquals(4, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(3, stream.readBits(2));
        assertEquals(6, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(0, stream.readBits(2));
        assertEquals(0, stream.getBitOffset());
        assertEquals(2, stream.getStreamPosition());

        // Read all bits, third byte
        assertEquals(2, stream.readBits(2));
        assertEquals(2, stream.getBitOffset());
        assertEquals(2, stream.getStreamPosition());

        assertEquals(2, stream.readBits(2));
        assertEquals(4, stream.getBitOffset());
        assertEquals(2, stream.getStreamPosition());

        assertEquals(2, stream.readBits(2));
        assertEquals(6, stream.getBitOffset());
        assertEquals(2, stream.getStreamPosition());

        assertEquals(2, stream.readBits(2));
        assertEquals(0, stream.getBitOffset());
        assertEquals(3, stream.getStreamPosition());

        // Full reset, read same sequence again
        stream.seek(0);
        assertEquals(0, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        // Read all bits, increasing size
        assertEquals(7, stream.readBits(3)); // 111
        assertEquals(3, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(8, stream.readBits(4)); // 1000
        assertEquals(7, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(12, stream.readBits(5)); // 01100
        assertEquals(4, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(50, stream.readBits(6)); // 110010
        assertEquals(2, stream.getBitOffset());
        assertEquals(2, stream.getStreamPosition());

        assertEquals(42, stream.readBits(6)); // 101010
        assertEquals(0, stream.getBitOffset());
        assertEquals(3, stream.getStreamPosition());

        // Full reset, read same sequence again
        stream.seek(0);
        assertEquals(0, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        // Read all bits multi-byte
        assertEquals(0xF0C, stream.readBits(12)); // 111100001100
        assertEquals(4, stream.getBitOffset());
        assertEquals(1, stream.getStreamPosition());

        assertEquals(0xCAA, stream.readBits(12)); // 110010101010
        assertEquals(0, stream.getBitOffset());
        assertEquals(3, stream.getStreamPosition());

        // Full reset, read same sequence again, all bits in one go
        stream.seek(0);
        assertEquals(0, stream.getBitOffset());
        assertEquals(0, stream.getStreamPosition());

        assertEquals(0xF0CCAA, stream.readBits(24));
    }

    @Test
    public void testReadBitsRandom() throws IOException {
        long value = random.nextLong();
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(value);

        // Create wrapper stream
        BufferedImageInputStream stream = new BufferedImageInputStream(new ByteArrayImageInputStream(bytes));

        for (int i = 1; i < 64; i++) {
            stream.seek(0);
            assertEquals(value >>> (64L - i), stream.readBits(i), i + " bits differ");
        }
    }

    @Test
    public void testClose() throws IOException {
        // Create wrapper stream
        ImageInputStream mock = mock(ImageInputStream.class);
        BufferedImageInputStream stream = new BufferedImageInputStream(mock);

        stream.close();
        verify(mock, never()).close();
    }

    // TODO: Write other tests

    // TODO: Create test that exposes read += -1 (eof) bug

    @Test
    public void testArrayIndexOutOfBoundsBufferedReadBug() throws IOException {
        // TODO: Create a more straight forward way to prove correctness, for now this is good enough to avoid regression
        ImageInputStream input = new BufferedImageInputStream(new MemoryCacheImageInputStream(getClass().getResourceAsStream("/Thumbs-camera.db")));
        input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        Entry root = new CompoundDocument(input).getRootEntry();

        Entry catalog = root.getChildEntry("Catalog");

        assertNotNull(catalog, "Catalog should not be null");
        assertNotNull(catalog.getInputStream(), "Input stream can never be null");
    }

    @Test
    public void testReadResetReadDirectBufferBug() throws IOException {
        // Make sure we use the exact size of the buffer
        int size = BufferedImageInputStream.DEFAULT_BUFFER_SIZE;

        // Fill bytes
        byte[] bytes = new byte[size * 2];
        random.nextBytes(bytes);

        // Create wrapper stream
        BufferedImageInputStream stream = new BufferedImageInputStream(new ByteArrayImageInputStream(bytes));

        // Read to fill the buffer, then reset
        stream.readLong();
        stream.seek(0);

        // Read fully and compare
        byte[] result = new byte[size];
        stream.readFully(result);
        assertTrue(rangeEquals(bytes, 0, result, 0, size));

        stream.readFully(result);
        assertTrue(rangeEquals(bytes, size, result, 0, size));
    }

    @Test
    public void testBufferPositionCorrect() throws IOException {
        // Fill bytes
        byte[] bytes = new byte[1024];
        random.nextBytes(bytes);

        ByteArrayImageInputStream input = new ByteArrayImageInputStream(bytes);

        input.readByte();
        input.readByte();
        input.skipBytes(124);
        input.readByte();
        input.readByte();

        // Sanity check
        assertEquals(128, input.getStreamPosition());

        BufferedImageInputStream stream = new BufferedImageInputStream(input);

        assertEquals(input.getStreamPosition(), stream.getStreamPosition());

        stream.skipBytes(128);

        //assertTrue(256 <= input.getStreamPosition());
        assertEquals(256, stream.getStreamPosition());
        
        stream.seek(1020);
        assertEquals(1020, stream.getStreamPosition());
    }

    @Test
    public void testReadIntegralOnBufferBoundary() throws IOException {
        // Make sure we use the exact size of the buffer
        int size = BufferedImageInputStream.DEFAULT_BUFFER_SIZE;

        // Fill bytes
        byte[] bytes = new byte[size * 2];
        fill(bytes, size - 4, size + 4, (byte) 0xff);

        // Create wrapper stream
        BufferedImageInputStream stream = new BufferedImageInputStream(new ByteArrayImageInputStream(bytes));

        // Read to fill the buffer, then seek to almost end of buffer
        assertEquals(0, stream.readInt());
        stream.seek(size - 3);
        assertEquals(0xffffffff, stream.readInt());
        assertEquals(size + 1, stream.getStreamPosition());
    }

    /**
     * Test two arrays for range equality. That is, they contain the same elements for some specified range.
     *
     * @param pFirst one array to test for equality
     * @param pFirstOffset the offset into the first array to start testing for equality
     * @param pSecond the other array to test for equality
     * @param pSecondOffset the offset into the second array to start testing for equality
     * @param pLength the length of the range to check for equality
     *
     * @return {@code true} if both arrays are non-{@code null}
     * and have at least {@code offset + pLength} elements
     * and all elements in the range from the first array is equal to the elements from the second array,
     * or if {@code pFirst == pSecond} (including both arrays being {@code null})
     * and {@code pFirstOffset == pSecondOffset}.
     * Otherwise {@code false}.
     */
    public static boolean rangeEquals(byte[] pFirst, int pFirstOffset, byte[] pSecond, int pSecondOffset, int pLength) {
        if (pFirst == pSecond && pFirstOffset == pSecondOffset) {
            return true;
        }

        if (pFirst == null || pSecond == null) {
            return false;
        }

        if (pFirst.length < pFirstOffset + pLength || pSecond.length < pSecondOffset + pLength) {
            return false;
        }

        for (int i = 0; i < pLength; i++) {
            if (pFirst[pFirstOffset + i] != pSecond[pSecondOffset + i]) {
                return false;
            }
        }

        return true;
    }
}
