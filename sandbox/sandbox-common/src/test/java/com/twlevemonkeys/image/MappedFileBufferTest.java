/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twlevemonkeys.image;

import com.twelvemonkeys.image.MappedFileBuffer;
import org.junit.Test;

import java.awt.image.DataBuffer;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * MappedFileBufferTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedFileBufferTest.java,v 1.0 01.06.12 14:23 haraldk Exp$
 */
public class MappedFileBufferTest {
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalidType() throws IOException {
        MappedFileBuffer.create(-1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalidSize() throws IOException {
        MappedFileBuffer.create(DataBuffer.TYPE_USHORT, -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalidBands() throws IOException {
        MappedFileBuffer.create(DataBuffer.TYPE_BYTE, 1, -1);
    }

    @Test
    public void testCreateByte() throws IOException {
        DataBuffer buffer = MappedFileBuffer.create(DataBuffer.TYPE_BYTE, 256, 3);
        assertNotNull(buffer);

        assertEquals(DataBuffer.TYPE_BYTE, buffer.getDataType());
        assertEquals(256, buffer.getSize());
        assertEquals(3, buffer.getNumBanks());
    }

    @Test
    public void testSetGetElemByte() throws IOException {
        final int size = 256;
        DataBuffer buffer = MappedFileBuffer.create(DataBuffer.TYPE_BYTE, size, 3);
        assertNotNull(buffer);

        for (int b = 0; b < 3; b++) {
            for (int i = 0; i < size; i++) {
                buffer.setElem(b, i, i);

                assertEquals(i, buffer.getElem(b, i));
            }
        }
    }

    @Test
    public void testCreateUShort() throws IOException {
        DataBuffer buffer = MappedFileBuffer.create(DataBuffer.TYPE_USHORT, 256, 3);
        assertNotNull(buffer);

        assertEquals(DataBuffer.TYPE_USHORT, buffer.getDataType());
        assertEquals(256, buffer.getSize());
        assertEquals(3, buffer.getNumBanks());
    }

    @Test
    public void testSetGetElemUShort() throws IOException {
        final int size = (Short.MAX_VALUE + 1) * 2;
        DataBuffer buffer = MappedFileBuffer.create(DataBuffer.TYPE_USHORT, size, 3);
        assertNotNull(buffer);

        for (int b = 0; b < 3; b++) {
            for (int i = 0; i < size; i++) {
                buffer.setElem(b, i, i);

                assertEquals(i, buffer.getElem(b, i));
            }
        }
    }

    @Test
    public void testCreateInt() throws IOException {
        DataBuffer buffer = MappedFileBuffer.create(DataBuffer.TYPE_INT, 256, 3);
        assertNotNull(buffer);

        assertEquals(DataBuffer.TYPE_INT, buffer.getDataType());
        assertEquals(256, buffer.getSize());
        assertEquals(3, buffer.getNumBanks());
    }

    @Test
    public void testSetGetElemInt() throws IOException {
        final int size = (Short.MAX_VALUE + 1) * 2;
        DataBuffer buffer = MappedFileBuffer.create(DataBuffer.TYPE_INT, size, 3);
        assertNotNull(buffer);

        for (int b = 0; b < 3; b++) {
            for (int i = 0; i < size; i++) {
                buffer.setElem(b, i, i * i);

                assertEquals(i * i, buffer.getElem(b, i));
            }
        }
    }
}
