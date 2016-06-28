/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;
import org.junit.Test;

import java.io.*;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

/**
 * HorizontalDeDifferencingStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HorizontalDeDifferencingStreamTest.java,v 1.0 13.03.13 12:46 haraldk Exp$
 */
public class HorizontalDeDifferencingStreamTest {
    @Test
    public void testRead1SPP1BPS() throws IOException {
        // 1 sample per pixel, 1 bits per sample (mono/indexed)
        byte[] data = {
                (byte) 0x80, 0x00, 0x00,
                0x71, 0x11, 0x44,
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 24, 1, 1, ByteOrder.BIG_ENDIAN);

        // Row 1
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());

        // Row 2
        assertEquals(0x5e, stream.read());
        assertEquals(0x1e, stream.read());
        assertEquals(0x78, stream.read());

        // EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead1SPP2BPS() throws IOException {
        // 1 sample per pixel, 2 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xc0, 0x00, 0x00, 0x00,
                0x71, 0x11, 0x44, (byte) 0xcc,
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 16, 1, 2, ByteOrder.BIG_ENDIAN);

        // Row 1
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());

        // Row 2
        assertEquals(0x41, stream.read());
        assertEquals(0x6b, stream.read());
        assertEquals(0x05, stream.read());
        assertEquals(0x0f, stream.read());

        // EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead1SPP4BPS() throws IOException {
        // 1 sample per pixel, 4 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xf0, 0x00, 0x00, 0x00,
                0x70, 0x11, 0x44, (byte) 0xcc,
                0x00, 0x01, 0x10, (byte) 0xe0
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 8, 1, 4, ByteOrder.BIG_ENDIAN);

        // Row 1
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());

        // Row 2
        assertEquals(0x77, stream.read());
        assertEquals(0x89, stream.read());
        assertEquals(0xd1, stream.read());
        assertEquals(0xd9, stream.read());

        // Row 3
        assertEquals(0x00, stream.read());
        assertEquals(0x01, stream.read());
        assertEquals(0x22, stream.read());
        assertEquals(0x00, stream.read());

        // EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead1SPP8BPS() throws IOException {
        // 1 sample per pixel, 8 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xff, 0, 0, 0,
                0x7f, 1, 4, -4,
                0x00, 127, 127, -127
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 4, 1, 8, ByteOrder.BIG_ENDIAN);

        // Row 1
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0xff, stream.read());

        // Row 2
        assertEquals(0x7f, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x84, stream.read());
        assertEquals(0x80, stream.read());

        // Row 3
        assertEquals(0x00, stream.read());
        assertEquals(0x7f, stream.read());
        assertEquals(0xfe, stream.read());
        assertEquals(0x7f, stream.read());

        // EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadArray1SPP8BPS() throws IOException {
        // 1 sample per pixel, 8 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xff, 0, 0, 0,
                0x7f, 1, 4, -4,
                0x00, 127, 127, -127
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 4, 1, 8, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[data.length];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(
                new byte[] {
                        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                        0x7f, (byte) 0x80, (byte) 0x84, (byte) 0x80,
                        0x00, 0x7f, (byte) 0xfe, 0x7f,
                },
                result
        );

        // EOF
        assertEquals(-1, stream.read(new byte[16]));
        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead1SPP32BPS() throws IOException {
        // 1 sample per pixel, 32 bits per sample (gray)
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(16);
        DataOutput dataOut = new DataOutputStream(out);
        dataOut.writeInt(0x00000000);
        dataOut.writeInt(305419896);
        dataOut.writeInt(305419896);
        dataOut.writeInt(-610839792);

        InputStream in = new HorizontalDeDifferencingStream(out.createInputStream(), 4, 1, 32, ByteOrder.BIG_ENDIAN);
        DataInput dataIn = new DataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readInt());
        assertEquals(305419896, dataIn.readInt());
        assertEquals(610839792, dataIn.readInt());
        assertEquals(0, dataIn.readInt());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead1SPP32BPSLittleEndian() throws IOException {
        // 1 sample per pixel, 32 bits per sample (gray)
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(16);
        DataOutput dataOut = new LittleEndianDataOutputStream(out);
        dataOut.writeInt(0x00000000);
        dataOut.writeInt(305419896);
        dataOut.writeInt(305419896);
        dataOut.writeInt(-610839792);

        InputStream in = new HorizontalDeDifferencingStream(out.createInputStream(), 4, 1, 32, ByteOrder.LITTLE_ENDIAN);
        DataInput dataIn = new LittleEndianDataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readInt());
        assertEquals(305419896, dataIn.readInt());
        assertEquals(610839792, dataIn.readInt());
        assertEquals(0, dataIn.readInt());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead1SPP64BPS() throws IOException {
        // 1 sample per pixel, 64 bits per sample (gray)
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(32);
        DataOutput dataOut = new DataOutputStream(out);
        dataOut.writeLong(0x00000000);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(-163971058432973790L);

        InputStream in = new HorizontalDeDifferencingStream(out.createInputStream(), 4, 1, 64, ByteOrder.BIG_ENDIAN);
        DataInput dataIn = new DataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readLong());
        assertEquals(81985529216486895L, dataIn.readLong());
        assertEquals(163971058432973790L, dataIn.readLong());
        assertEquals(0, dataIn.readLong());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead1SPP64BPSLittleEndian() throws IOException {
        // 1 sample per pixel, 64 bits per sample (gray)
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(32);
        DataOutput dataOut = new LittleEndianDataOutputStream(out);
        dataOut.writeLong(0x00000000);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(-163971058432973790L);

        InputStream in = new HorizontalDeDifferencingStream(out.createInputStream(), 4, 1, 64, ByteOrder.LITTLE_ENDIAN);
        DataInput dataIn = new LittleEndianDataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readLong());
        assertEquals(81985529216486895L, dataIn.readLong());
        assertEquals(163971058432973790L, dataIn.readLong());
        assertEquals(0, dataIn.readLong());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead3SPP8BPS() throws IOException {
        // 3 samples per pixel, 8 bits per sample (RGB)
        byte[] data = {
                (byte) 0xff, (byte) 0x00, (byte) 0x7f, -1, -1, -1, -4, -4, -4, 4, 4, 4,
                0x7f, 0x7f, 0x7f, 1, 1, 1, 4, 4, 4, -4, -4, -4,
                0x00, 0x00, 0x00, 127, -127, 0, -127, 127, 0, 0, 0, 127,
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 4, 3, 8, ByteOrder.BIG_ENDIAN);

        // Row 1
        assertEquals(0xff, stream.read());
        assertEquals(0x00, stream.read());
        assertEquals(0x7f, stream.read());

        assertEquals(0xfe, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0x7e, stream.read());

        assertEquals(0xfa, stream.read());
        assertEquals(0xfb, stream.read());
        assertEquals(0x7a, stream.read());

        assertEquals(0xfe, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0x7e, stream.read());

        // Row 2
        assertEquals(0x7f, stream.read());
        assertEquals(0x7f, stream.read());
        assertEquals(0x7f, stream.read());

        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());

        assertEquals(0x84, stream.read());
        assertEquals(0x84, stream.read());
        assertEquals(0x84, stream.read());

        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());

        // Row 3
        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x7f, stream.read());
        assertEquals(0x81, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());
        assertEquals(0x7f, stream.read());

        // EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead3SPP16BPS() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(24);
        DataOutput dataOut = new DataOutputStream(out);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(4660);
        dataOut.writeShort(30292);
        dataOut.writeShort(4660);
        dataOut.writeShort(4660);
        dataOut.writeShort(30292);
        dataOut.writeShort(4660);
        dataOut.writeShort(-9320);
        dataOut.writeShort(-60584);
        dataOut.writeShort(-9320);

        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(-60584);
        dataOut.writeShort(-60584);
        dataOut.writeShort(-60584);

        InputStream in = new HorizontalDeDifferencingStream(out.createInputStream(), 4, 3, 16, ByteOrder.BIG_ENDIAN);
        DataInput dataIn = new DataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(9320, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(9320, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());

        // Row 2
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead3SPP16BPSLittleEndian() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(24);
        DataOutput dataOut = new LittleEndianDataOutputStream(out);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(4660);
        dataOut.writeShort(30292);
        dataOut.writeShort(4660);
        dataOut.writeShort(4660);
        dataOut.writeShort(30292);
        dataOut.writeShort(4660);
        dataOut.writeShort(-9320);
        dataOut.writeShort(-60584);
        dataOut.writeShort(-9320);

        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(0x0000);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(30292);
        dataOut.writeShort(-60584);
        dataOut.writeShort(-60584);
        dataOut.writeShort(-60584);

        InputStream in = new HorizontalDeDifferencingStream(out.createInputStream(), 4, 3, 16, ByteOrder.LITTLE_ENDIAN);
        DataInput dataIn = new LittleEndianDataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(9320, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(9320, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());

        // Row 2
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(60584, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead4SPP8BPS() throws IOException {
        // 4 samples per pixel, 8 bits per sample (RGBA)
        byte[] data = {
                (byte) 0xff, (byte) 0x00, (byte) 0x7f, 0x00, -1, -1, -1, -1, -4, -4, -4, -4, 4, 4, 4, 4,
                0x7f, 0x7f, 0x7f, 0x7f, 1, 1, 1, 1, 4, 4, 4, 4, -4, -4, -4, -4,
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 4, 4, 8, ByteOrder.BIG_ENDIAN);

        // Row 1
        assertEquals(0xff, stream.read());
        assertEquals(0x00, stream.read());
        assertEquals(0x7f, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0xfe, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0x7e, stream.read());
        assertEquals(0xff, stream.read());

        assertEquals(0xfa, stream.read());
        assertEquals(0xfb, stream.read());
        assertEquals(0x7a, stream.read());
        assertEquals(0xfb, stream.read());

        assertEquals(0xfe, stream.read());
        assertEquals(0xff, stream.read());
        assertEquals(0x7e, stream.read());
        assertEquals(0xff, stream.read());

        // Row 2
        assertEquals(0x7f, stream.read());
        assertEquals(0x7f, stream.read());
        assertEquals(0x7f, stream.read());
        assertEquals(0x7f, stream.read());

        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());

        assertEquals(0x84, stream.read());
        assertEquals(0x84, stream.read());
        assertEquals(0x84, stream.read());
        assertEquals(0x84, stream.read());

        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());
        assertEquals(0x80, stream.read());

        // EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadArray4SPP8BPS() throws IOException {
        // 4 samples per pixel, 8 bits per sample (RGBA)
        byte[] data = {
                (byte) 0xff, (byte) 0x00, (byte) 0x7f, 0x00, -1, -1, -1, -1, -4, -4, -4, -4, 4, 4, 4, 4,
                0x7f, 0x7f, 0x7f, 0x7f, 1, 1, 1, 1, 4, 4, 4, 4, -4, -4, -4, -4,
        };

        InputStream stream = new HorizontalDeDifferencingStream(new ByteArrayInputStream(data), 4, 4, 8, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[data.length];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(
                new byte[] {
                        (byte) 0xff, 0x00, 0x7f, 0x00,
                        (byte) 0xfe, (byte) 0xff, 0x7e, (byte) 0xff,
                        (byte) 0xfa, (byte) 0xfb, 0x7a, (byte) 0xfb,
                        (byte) 0xfe, (byte) 0xff, 0x7e, (byte) 0xff,

                        0x7f, 0x7f, 0x7f, 0x7f,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                        (byte) 0x84, (byte) 0x84, (byte) 0x84, (byte) 0x84,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                },
                result
        );

        // EOF
        assertEquals(-1, stream.read(new byte[16]));
        assertEquals(-1, stream.read());
    }
}
