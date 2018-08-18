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

import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * SubImageInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SubImageInputStreamTestCase.java,v 1.0 Nov 8, 2009 3:03:32 PM haraldk Exp$
 */
public class SubImageInputStreamTest {
    // TODO: Extract super test case for all stream tests
    private final Random random = new Random(837468L);

    private ImageInputStream createStream(final int pSize) {
        byte[] bytes = new byte[pSize];

        random.nextBytes(bytes);

        return new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            public long length() {
                return pSize;
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullStream() throws IOException {
        new SubImageInputStream(null, 1);
        fail("Expected IllegalArgumentException with null stream");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNegativeLength() throws IOException {
        new SubImageInputStream(createStream(0), -1);
        fail("Expected IllegalArgumentException with negative length");
    }

    @Test
    public void testCreate() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(11), 7);

        assertEquals(0, stream.getStreamPosition());
        assertEquals(7, stream.length());
    }

    @Test
    public void testWraphBeyondWrappedLength() throws IOException {
        SubImageInputStream stream = new SubImageInputStream(createStream(5), 6);
        assertEquals(5, stream.length());
    }

    @Test
    public void testWrapUnknownLength() throws IOException {
        SubImageInputStream stream = new SubImageInputStream(new ImageInputStreamImpl() {
            @Override
            public int read() throws IOException {
                throw new UnsupportedOperationException("Method read not implemented");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new UnsupportedOperationException("Method read not implemented");
            }

            @Override
            public long length() {
                return -1;
            }
        }, 6);

        assertEquals(-1, stream.length());
    }

    @Test
    public void testRead() throws IOException {
        ImageInputStream wrapped = createStream(42);

        wrapped.skipBytes(13);

        ImageInputStream stream = new SubImageInputStream(wrapped, 27);

        assertEquals(0, stream.getStreamPosition());
        assertEquals(27, stream.length());

        stream.read();
        assertEquals(1, stream.getStreamPosition());
        assertEquals(27, stream.length());

        stream.readFully(new byte[11]);
        assertEquals(12, stream.getStreamPosition());
        assertEquals(27, stream.length());

        assertEquals(25, wrapped.getStreamPosition());
    }

    @Test
    public void testReadResetRead() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(32), 16);
        stream.mark();

        byte[] first = new byte[16];
        stream.readFully(first);

        stream.reset();

        byte[] second = new byte[16];
        stream.readFully(second);

        assertTrue(Arrays.equals(first, second));
    }

    @Test
    public void testSeekNegative() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        try {
            stream.seek(-2);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException expected) {
            assertTrue(expected.getMessage().contains("pos"));
        }

        assertEquals(0, stream.getStreamPosition());
    }

    @Test
    public void testSeekBeforeFlushedPos() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        stream.seek(3);
        stream.flushBefore(3);

        assertEquals(3, stream.getStreamPosition());

        try {
            stream.seek(0);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException expected) {
            assertTrue(expected.getMessage().contains("pos"));
        }

        assertEquals(3, stream.getStreamPosition());
    }

    @Test
    public void testSeekAfterEOF() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        stream.seek(6);

        assertEquals(-1, stream.read());
    }

    @Test
    public void testSeek() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        stream.seek(5);
        assertEquals(5, stream.getStreamPosition());

        stream.seek(1);
        assertEquals(1, stream.getStreamPosition());
    }
}
