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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.io.InputStreamAbstractTest;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * IIOInputStreamAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IIOInputStreamAdapter.java,v 1.0 Apr 11, 2008 1:04:42 PM haraldk Exp$
 */
public class IIOInputStreamAdapterTest extends InputStreamAbstractTest {

    protected InputStream makeInputStream(byte[] pBytes) {
        return new IIOInputStreamAdapter(new MemoryCacheImageInputStream(new ByteArrayInputStream(pBytes)), pBytes.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        new IIOInputStreamAdapter(null);
    }

    @Test
    public void testReadSubstreamOpenEnd() throws IOException {
        byte[] bytes = new byte[20];

        MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));

        input.seek(10);
        assertEquals(10, input.getStreamPosition());

        IIOInputStreamAdapter stream = new IIOInputStreamAdapter(input);
        for (int i = 0; i < 10; i++) {
            assertTrue("Unexpected end of stream", -1 != stream.read());
        }

        assertEquals("Read value after end of stream", -1, stream.read());
        assertEquals("Read value after end of stream", -1, stream.read());

        // Make sure underlying stream is positioned at end of substream after close
        stream.close();
        assertEquals(20, input.getStreamPosition());

        input.close();
    }

    @Test
    public void testReadSubstream() throws IOException {
        byte[] bytes = new byte[20];

        MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));
        IIOInputStreamAdapter stream = new IIOInputStreamAdapter(input, 9);
        for (int i = 0; i < 9; i++) {
            assertTrue("Unexpected end of stream", -1 != stream.read());
        }

        assertEquals("Read value after end of stream", -1, stream.read());
        assertEquals("Read value after end of stream", -1, stream.read());

        // Make sure we don't read outside stream boundaries
        assertTrue(input.getStreamPosition() <= 9);

        input.close();
    }
    
    @Test
    public void testReadSubstreamRepositionOnClose() throws IOException {
        byte[] bytes = new byte[20];

        MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));
        IIOInputStreamAdapter stream = new IIOInputStreamAdapter(input, 10);
        for (int i = 0; i < 7; i++) {
            assertTrue("Unexpected end of stream", -1 != stream.read());
        }

        // Make sure we don't read outside stream boundaries
        assertTrue(input.getStreamPosition() <= 7);

        // Make sure underlying stream is positioned at end of substream after close
        stream.close();
        assertEquals(10, input.getStreamPosition());

        input.close();
    }

    @Test
    public void testSeekBeforeStreamNoEnd() throws IOException {
        byte[] bytes = new byte[20];

        MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));

        input.seek(10);
        assertEquals(10, input.getStreamPosition());

        IIOInputStreamAdapter stream = new IIOInputStreamAdapter(input);
        assertEquals("Should not skip backwards", 0, stream.skip(-5));
        assertEquals(10, input.getStreamPosition());
    }

    @Test
    public void testSeekBeforeStream() throws IOException {
        byte[] bytes = new byte[20];

        MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes));

        input.seek(10);
        assertEquals(10, input.getStreamPosition());

        IIOInputStreamAdapter stream = new IIOInputStreamAdapter(input, 9);
        assertEquals("Should not skip backwards", 0, stream.skip(-5));
        assertEquals(10, input.getStreamPosition());

    }
}
