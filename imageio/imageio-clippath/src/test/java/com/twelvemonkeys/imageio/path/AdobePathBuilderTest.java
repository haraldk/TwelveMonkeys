/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.path;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.Path2D;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static com.twelvemonkeys.imageio.path.PathsTest.assertPathEquals;
import static com.twelvemonkeys.imageio.path.PathsTest.readExpectedPath;

@SuppressWarnings("deprecation")
public class AdobePathBuilderTest {

    @Test
    public void testCreateNullBytes() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathBuilder((byte[]) null));

    }

    @Test
    public void testCreateNull() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathBuilder((DataInput) null));
    }

    @Test
    public void testCreateEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathBuilder(new byte[0]));
    }

    @Test
    public void testCreateShortPath() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathBuilder(new byte[3]));
    }

    @Test
    public void testCreateImpossiblePath() {
        assertThrows(IllegalArgumentException.class, () -> new AdobePathBuilder(new byte[7]));
    }

    @Test
    public void testCreate() {
        new AdobePathBuilder(new byte[52]);
    }

    @Test
    public void testNoPath() throws IOException {
        Path2D path = new AdobePathBuilder(new byte[26]).path();
        assertNotNull(path);
    }

    @Test
    public void testShortPath() throws IOException {
        byte[] data = new byte[26];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD);
        buffer.putShort((short) 1);

        assertThrows(IIOException.class, () -> {
            Path2D path = new AdobePathBuilder(data).path();
            assertNotNull(path);
        });
    }

    @Test
    public void testShortPathToo() throws IOException {
        byte[] data = new byte[52];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD);
        buffer.putShort((short) 2);
        buffer.position(buffer.position() + 22);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);

        assertThrows(IIOException.class, () -> {
            Path2D path = new AdobePathBuilder(data).path();
            assertNotNull(path);
        });
    }

    @Test
    public void testLongPath() throws IOException {
        byte[] data = new byte[78];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD);
        buffer.putShort((short) 1);
        buffer.position(buffer.position() + 22);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);
        buffer.position(buffer.position() + 24);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);

        assertThrows(IIOException.class, () -> {
            Path2D path = new AdobePathBuilder(data).path();
            assertNotNull(path);
        });
    }

    @Test
    public void testPathMissingLength() throws IOException {
        byte[] data = new byte[26];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);

        assertThrows(IIOException.class, () -> {
            Path2D path = new AdobePathBuilder(data).path();
            assertNotNull(path);
        });
    }

    @Test
    public void testSimplePath() throws IOException {
        // We'll read this from a real file, with hardcoded offsets for simplicity
        // PSD IRB: offset: 34, length: 32598
        // Clipping path: offset: 31146, length: 1248
        ImageInputStream stream = PathsTest.resourceAsIIOStream("/psd/grape_with_path.psd");
        stream.seek(34 + 31146);
        byte[] data = new byte[1248];
        stream.readFully(data);

        Path2D path = new AdobePathBuilder(data).path();

        assertNotNull(path);
        assertPathEquals(path, readExpectedPath("/ser/grape-path.ser"));
    }

    @Test
    public void testComplexPath() throws IOException {
        // We'll read this from a real file, with hardcoded offsets for simplicity
        // PSD IRB: offset: 16970, length: 11152
        // Clipping path: offset: 9250, length: 1534
        ImageInputStream stream = PathsTest.resourceAsIIOStream("/tiff/big-endian-multiple-clips.tif");
        stream.seek(16970 + 9250);
        byte[] data = new byte[1534];
        stream.readFully(data);

        Path2D path = new AdobePathBuilder(data).path();

        assertNotNull(path);
        assertPathEquals(path, readExpectedPath("/ser/multiple-clips.ser"));
    }
}