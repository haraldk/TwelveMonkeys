/*
 * Copyright (c) 2020 Harald Kuhr
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

import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.Path2D;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.twelvemonkeys.imageio.path.PathsTest.assertPathEquals;
import static com.twelvemonkeys.imageio.path.PathsTest.readExpectedPath;
import static org.junit.Assert.assertNotNull;

public class AdobePathReaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullBytes() {
        new AdobePathReader((byte[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        new AdobePathReader((DataInput) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateEmpty() {
        new AdobePathReader(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateShortPath() {
        new AdobePathReader(new byte[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateImpossiblePath() {
        new AdobePathReader(new byte[7]);
    }

    @Test
    public void testCreate() {
        new AdobePathReader(new byte[52]);
    }

    @Test
    public void testNoPath() throws IOException {
        Path2D path = new AdobePathReader(new byte[26]).readPath();
        assertNotNull(path);
    }

    @Test(expected = IIOException.class)
    public void testShortPath() throws IOException {
        byte[] data = new byte[26];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD);
        buffer.putShort((short) 1);

        Path2D path = new AdobePathReader(data).readPath();
        assertNotNull(path);
    }

    @Test(expected = IIOException.class)
    public void testShortPathToo() throws IOException {
        byte[] data = new byte[52];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD);
        buffer.putShort((short) 2);
        buffer.position(buffer.position() + 22);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);

        Path2D path = new AdobePathReader(data).readPath();
        assertNotNull(path);
    }

    @Test(expected = IIOException.class)
    public void testLongPath() throws IOException {
        byte[] data = new byte[78];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD);
        buffer.putShort((short) 1);
        buffer.position(buffer.position() + 22);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);
        buffer.position(buffer.position() + 24);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);

        Path2D path = new AdobePathReader(data).readPath();
        assertNotNull(path);
    }

    @Test(expected = IIOException.class)
    public void testPathMissingLength() throws IOException {
        byte[] data = new byte[26];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED);

        Path2D path = new AdobePathReader(data).readPath();
        assertNotNull(path);
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

        Path2D path = new AdobePathReader(data).readPath();

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

        Path2D path = new AdobePathReader(data).readPath();

        assertNotNull(path);
        assertPathEquals(path, readExpectedPath("/ser/multiple-clips.ser"));
    }
}