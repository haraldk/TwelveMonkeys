/*
 * Copyright (c) 2025, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.heic;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import openize.io.IOSeekMode;

import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageInputStreamIOStreamAdapterTest.
 */
public class ImageInputStreamIOStreamAdapterTest {

    private static final byte[] DATA = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    private static ImageInputStreamIOStreamAdapter createAdapter() {
        return new ImageInputStreamIOStreamAdapter(new ByteArrayImageInputStream(DATA));
    }

    @Test
    public void testRead() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        byte[] buffer = new byte[8];
        assertEquals(8, adapter.read(buffer));
        assertArrayEquals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7}, buffer);
        assertEquals(8, adapter.read(buffer));
        assertArrayEquals(new byte[] {8, 9, 10, 11, 12, 13, 14, 15}, buffer);
    }

    @Test
    public void testReadOffsetLength() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        byte[] buffer = new byte[8];
        assertEquals(4, adapter.read(buffer, 2, 4));
        assertArrayEquals(new byte[] {0, 0, 0, 1, 2, 3, 0, 0}, buffer);
    }

    @Test
    public void testReadAtEOF() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();
        adapter.setPosition(DATA.length);

        assertEquals(-1, adapter.read(new byte[4]));
    }

    @Test
    public void testGetLength() {
        assertEquals(DATA.length, createAdapter().getLength());
    }

    @Test
    public void testGetSetPosition() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        assertEquals(0, adapter.getPosition());
        assertEquals(0, adapter.setPosition(7)); // Returns the *old* position
        assertEquals(7, adapter.getPosition());
        assertEquals(7, adapter.setPosition(3));
        assertEquals(3, adapter.getPosition());
    }

    @Test
    public void testSeekBegin() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        adapter.seek(5, IOSeekMode.BEGIN);
        assertEquals(5, adapter.getPosition());
    }

    @Test
    public void testSeekCurrent() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        adapter.setPosition(5);
        adapter.seek(3, IOSeekMode.CURRENT);
        assertEquals(8, adapter.getPosition());
        adapter.seek(-4, IOSeekMode.CURRENT);
        assertEquals(4, adapter.getPosition());
    }

    @Test
    public void testSeekEnd() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        adapter.seek(-6, IOSeekMode.END);
        assertEquals(DATA.length - 6, adapter.getPosition());
    }

    @Test
    public void testWriteUnsupported() {
        ImageInputStreamIOStreamAdapter adapter = createAdapter();

        assertThrows(UnsupportedOperationException.class, () -> adapter.write(new byte[1]));
        assertThrows(UnsupportedOperationException.class, () -> adapter.write(new byte[4], 0, 4));
        assertThrows(UnsupportedOperationException.class, () -> adapter.setLength(42));
    }

    @Test
    public void testCloseDoesNotCloseUnderlyingStream() throws IOException {
        // Per ImageIO convention, the client owns the stream,
        // the reader (and its internals) should never close it
        ImageInputStream stream = new ByteArrayImageInputStream(DATA);
        ImageInputStreamIOStreamAdapter adapter = new ImageInputStreamIOStreamAdapter(stream);

        adapter.close();

        assertEquals(0, stream.read()); // Stream must still be usable
    }

    @Test
    public void testWrapsIOException() {
        ImageInputStream broken = new javax.imageio.stream.ImageInputStreamImpl() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("broken");
            }
        };

        ImageInputStreamIOStreamAdapter adapter = new ImageInputStreamIOStreamAdapter(broken);
        openize.io.IOException exception =
                assertThrows(openize.io.IOException.class, () -> adapter.read(new byte[4], 0, 4));
        assertEquals("broken", exception.getMessage());
    }
}
