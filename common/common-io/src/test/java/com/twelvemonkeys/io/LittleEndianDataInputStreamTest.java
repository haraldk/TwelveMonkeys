/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * LittleEndianDataInputStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LittleEndianDataInputStreamTest.java,v 1.0 15.02.13 11:04 haraldk Exp$
 */
public class LittleEndianDataInputStreamTest {
    @Test
    public void testReadBoolean() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[] {0, 1, 0x7f, (byte) 0xff}));
        assertFalse(data.readBoolean());
        assertTrue(data.readBoolean());
        assertTrue(data.readBoolean());
        assertTrue(data.readBoolean());
    }

    @Test
    public void testReadByte() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(
                new byte[] {
                        (byte) 0x00, (byte) 0x00,
                        (byte) 0x01, (byte) 0x00,
                        (byte) 0xff, (byte) 0xff,
                        (byte) 0x00, (byte) 0x80,
                        (byte) 0xff, (byte) 0x7f,
                        (byte) 0x00, (byte) 0x01,
                }

        ));

        assertEquals(0, data.readByte());
        assertEquals(0, data.readByte());
        assertEquals(1, data.readByte());
        assertEquals(0, data.readByte());
        assertEquals(-1, data.readByte());
        assertEquals(-1, data.readByte());
        assertEquals(0, data.readByte());
        assertEquals(Byte.MIN_VALUE, data.readByte());
        assertEquals(-1, data.readByte());
        assertEquals(Byte.MAX_VALUE, data.readByte());
        assertEquals(0, data.readByte());
        assertEquals(1, data.readByte());
    }

    @Test
    public void testReadUnsignedByte() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(
                new byte[] {
                        (byte) 0x00, (byte) 0x00,
                        (byte) 0x01, (byte) 0x00,
                        (byte) 0xff, (byte) 0xff,
                        (byte) 0x00, (byte) 0x80,
                        (byte) 0xff, (byte) 0x7f,
                        (byte) 0x00, (byte) 0x01,
                }

        ));

        assertEquals(0, data.readUnsignedByte());
        assertEquals(0, data.readUnsignedByte());
        assertEquals(1, data.readUnsignedByte());
        assertEquals(0, data.readUnsignedByte());
        assertEquals(255, data.readUnsignedByte());
        assertEquals(255, data.readUnsignedByte());
        assertEquals(0, data.readUnsignedByte());
        assertEquals(128, data.readUnsignedByte());
        assertEquals(255, data.readUnsignedByte());
        assertEquals(Byte.MAX_VALUE, data.readUnsignedByte());
        assertEquals(0, data.readUnsignedByte());
        assertEquals(1, data.readUnsignedByte());
    }

    @Test
    public void testReadShort() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(
                new byte[] {
                        (byte) 0x00, (byte) 0x00,
                        (byte) 0x01, (byte) 0x00,
                        (byte) 0xff, (byte) 0xff,
                        (byte) 0x00, (byte) 0x80,
                        (byte) 0xff, (byte) 0x7f,
                        (byte) 0x00, (byte) 0x01,
                }

        ));

        assertEquals(0, data.readShort());
        assertEquals(1, data.readShort());
        assertEquals(-1, data.readShort());
        assertEquals(Short.MIN_VALUE, data.readShort());
        assertEquals(Short.MAX_VALUE, data.readShort());
        assertEquals(256, data.readShort());
    }

    @Test
    public void testReadUnsignedShort() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(
                new byte[] {
                        (byte) 0x00, (byte) 0x00,
                        (byte) 0x01, (byte) 0x00,
                        (byte) 0xff, (byte) 0xff,
                        (byte) 0x00, (byte) 0x80,
                        (byte) 0xff, (byte) 0x7f,
                        (byte) 0x00, (byte) 0x01,
                }

        ));

        assertEquals(0, data.readUnsignedShort());
        assertEquals(1, data.readUnsignedShort());
        assertEquals(Short.MAX_VALUE * 2 + 1, data.readUnsignedShort());
        assertEquals(Short.MAX_VALUE + 1, data.readUnsignedShort());
        assertEquals(Short.MAX_VALUE, data.readUnsignedShort());
        assertEquals(256, data.readUnsignedShort());
    }

    @Test
    public void testReadInt() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(
                new byte[] {
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
                        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x7f,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                        (byte) 0xff, (byte) 0x00, (byte) 0xff, (byte) 0x00,
                        (byte) 0x00, (byte) 0xff, (byte) 0x00, (byte) 0xff,
                        (byte) 0xbe, (byte) 0xba, (byte) 0xfe, (byte) 0xca,
                        (byte) 0xca, (byte) 0xfe, (byte) 0xd0, (byte) 0x0d,
                }

        ));

        assertEquals(0, data.readInt());
        assertEquals(1, data.readInt());
        assertEquals(-1, data.readInt());
        assertEquals(Integer.MIN_VALUE, data.readInt());
        assertEquals(Integer.MAX_VALUE, data.readInt());
        assertEquals(16777216, data.readInt());
        assertEquals(0xff00ff, data.readInt());
        assertEquals(0xff00ff00, data.readInt());
        assertEquals(0xCafeBabe, data.readInt());
        assertEquals(0x0dd0feca, data.readInt());
    }

    @Test
    public void testReadLong() throws IOException {
        LittleEndianDataInputStream data = new LittleEndianDataInputStream(new ByteArrayInputStream(
                new byte[] {
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
                        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x7f,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                        (byte) 0x0d, (byte) 0xd0, (byte) 0xfe, (byte) 0xca, (byte) 0xbe, (byte) 0xba, (byte) 0xfe, (byte) 0xca,
                }

        ));

        assertEquals(0, data.readLong());
        assertEquals(1, data.readLong());
        assertEquals(-1, data.readLong());
        assertEquals(Long.MIN_VALUE, data.readLong());
        assertEquals(Long.MAX_VALUE, data.readLong());
        assertEquals(72057594037927936L, data.readLong());
        assertEquals(0xCafeBabeL << 32 | 0xCafeD00dL, data.readLong());
    }
}
