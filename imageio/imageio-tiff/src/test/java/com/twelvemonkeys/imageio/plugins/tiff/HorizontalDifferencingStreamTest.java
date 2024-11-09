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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;

import java.io.*;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * HorizontalDifferencingStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HorizontalDifferencingStreamTest.java,v 1.0 02.12.13 09:50 haraldk Exp$
 */
public class HorizontalDifferencingStreamTest {

    @Test
    public void testWrite1SPP1BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 24, 1, 1, ByteOrder.BIG_ENDIAN);

        // Row 1
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);

        // Row 2
        stream.write(0x5e);
        stream.write(0x1e);
        stream.write(0x78);


        // 1 sample per pixel, 1 bits per sample (mono/indexed)
        byte[] data = {
                (byte) 0x80, 0x00, 0x00,
                0x71, 0x11, 0x44,
        };

        assertArrayEquals(data, bytes.toByteArray());
    }

    @Test
    public void testWrite1SPP2BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 16, 1, 2, ByteOrder.BIG_ENDIAN);

        // Row 1
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);

        // Row 2
        stream.write(0x41);
        stream.write(0x6b);
        stream.write(0x05);
        stream.write(0x0f);

        // 1 sample per pixel, 2 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xc0, 0x00, 0x00, 0x00,
                0x71, 0x11, 0x44, (byte) 0xcc,
        };

        assertArrayEquals(data, bytes.toByteArray());
    }

    @Test
    public void testWrite1SPP4BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 8, 1, 4, ByteOrder.BIG_ENDIAN);

        // Row 1
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);

        // Row 2
        stream.write(0x77);
        stream.write(0x89);
        stream.write(0xd1);
        stream.write(0xd9);

        // Row 3
        stream.write(0x00);
        stream.write(0x01);
        stream.write(0x22);
        stream.write(0x00);

        // 1 sample per pixel, 4 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xf0, 0x00, 0x00, 0x00,
                0x70, 0x11, 0x44, (byte) 0xcc,
                0x00, 0x01, 0x10, (byte) 0xe0
        };

        assertArrayEquals(data, bytes.toByteArray());
    }

    @Test
    public void testWrite1SPP8BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 4, 1, 8, ByteOrder.BIG_ENDIAN);

        // Row 1
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);
        stream.write(0xff);

        // Row 2
        stream.write(0x7f);
        stream.write(0x80);
        stream.write(0x84);
        stream.write(0x80);

        // Row 3
        stream.write(0x00);
        stream.write(0x7f);
        stream.write(0xfe);
        stream.write(0x7f);

        // 1 sample per pixel, 8 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xff, 0, 0, 0,
                0x7f, 1, 4, -4,
                0x00, 127, 127, -127
        };

        assertArrayEquals(data, bytes.toByteArray());
    }

    @Test
    public void testWriteArray1SPP8BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        OutputStream stream = new HorizontalDifferencingStream(bytes, 4, 1, 8, ByteOrder.BIG_ENDIAN);

        stream.write(new byte[] {
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                0x7f, (byte) 0x80, (byte) 0x84, (byte) 0x80,
                0x00, 0x7f, (byte) 0xfe, 0x7f,
        });

        // 1 sample per pixel, 8 bits per sample (gray/indexed)
        byte[] data = {
                (byte) 0xff, 0, 0, 0,
                0x7f, 1, 4, -4,
                0x00, 127, 127, -127
        };

        assertArrayEquals(data, bytes.toByteArray());
    }

    @Test
    public void testWrite1SPP32BPS() throws IOException {
        // 1 sample per pixel, 32 bits per sample (gray)
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(16);
        OutputStream out = new HorizontalDifferencingStream(bytes, 4, 1, 32, ByteOrder.BIG_ENDIAN);
        DataOutput dataOut = new DataOutputStream(out);
        dataOut.writeInt(0x00000000);
        dataOut.writeInt(305419896);
        dataOut.writeInt(305419896);
        dataOut.writeInt(-610839792);

        InputStream in = bytes.createInputStream();
        DataInput dataIn = new DataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readInt());
        assertEquals(305419896, dataIn.readInt());
        assertEquals(0, dataIn.readInt());
        assertEquals(-916259688, dataIn.readInt());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testWrite1SPP32BPSLittleEndian() throws IOException {
        // 1 sample per pixel, 32 bits per sample (gray)
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(16);
        OutputStream out = new HorizontalDifferencingStream(bytes, 4, 1, 32, ByteOrder.LITTLE_ENDIAN);
        DataOutput dataOut = new LittleEndianDataOutputStream(out);
        dataOut.writeInt(0x00000000);
        dataOut.writeInt(305419896);
        dataOut.writeInt(305419896);
        dataOut.writeInt(-610839792);

        InputStream in = bytes.createInputStream();
        DataInput dataIn = new LittleEndianDataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readInt());
        assertEquals(305419896, dataIn.readInt());
        assertEquals(0, dataIn.readInt());
        assertEquals(-916259688, dataIn.readInt());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testWrite1SPP64BPS() throws IOException {
        // 1 sample per pixel, 64 bits per sample (gray)
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(32);

        OutputStream out = new HorizontalDifferencingStream(bytes, 4, 1, 64, ByteOrder.BIG_ENDIAN);
        DataOutput dataOut = new DataOutputStream(out);
        dataOut.writeLong(0x00000000);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(-163971058432973790L);

        InputStream in = bytes.createInputStream();
        DataInput dataIn = new DataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readLong());
        assertEquals(81985529216486895L, dataIn.readLong());
        assertEquals(0, dataIn.readLong());
        assertEquals(-245956587649460685L, dataIn.readLong());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testWrite1SPP64BPSLittleEndian() throws IOException {
        // 1 sample per pixel, 64 bits per sample (gray)
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(32);

        OutputStream out = new HorizontalDifferencingStream(bytes, 4, 1, 64, ByteOrder.LITTLE_ENDIAN);
        DataOutput dataOut = new LittleEndianDataOutputStream(out);
        dataOut.writeLong(0x00000000);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(81985529216486895L);
        dataOut.writeLong(-163971058432973790L);

        InputStream in = bytes.createInputStream();
        DataInput dataIn = new LittleEndianDataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readLong());
        assertEquals(81985529216486895L, dataIn.readLong());
        assertEquals(0, dataIn.readLong());
        assertEquals(-245956587649460685L, dataIn.readLong());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testWrite3SPP8BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 4, 3, 8, ByteOrder.BIG_ENDIAN);

        // Row 1
        stream.write(0xff);
        stream.write(0x00);
        stream.write(0x7f);

        stream.write(0xfe);
        stream.write(0xff);
        stream.write(0x7e);

        stream.write(0xfa);
        stream.write(0xfb);
        stream.write(0x7a);

        stream.write(0xfe);
        stream.write(0xff);
        stream.write(0x7e);

        // Row 2
        stream.write(0x7f);
        stream.write(0x7f);
        stream.write(0x7f);

        stream.write(0x80);
        stream.write(0x80);
        stream.write(0x80);

        stream.write(0x84);
        stream.write(0x84);
        stream.write(0x84);

        stream.write(0x80);
        stream.write(0x80);
        stream.write(0x80);

        // Row 3
        stream.write(0x00);
        stream.write(0x00);
        stream.write(0x00);

        stream.write(0x7f);
        stream.write(0x81);
        stream.write(0x00);

        stream.write(0x00);
        stream.write(0x00);
        stream.write(0x00);

        stream.write(0x00);
        stream.write(0x00);
        stream.write(0x7f);

        // 3 samples per pixel, 8 bits per sample (RGB)
        byte[] data = {
                (byte) 0xff, (byte) 0x00, (byte) 0x7f, -1, -1, -1, -4, -4, -4, 4, 4, 4,
                0x7f, 0x7f, 0x7f, 1, 1, 1, 4, 4, 4, -4, -4, -4,
                0x00, 0x00, 0x00, 127, -127, 0, -127, 127, 0, 0, 0, 127,
        };

        assertArrayEquals(data, bytes.toByteArray());

    }

    @Test
    public void testWrite3SPP16BPS() throws IOException {
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(24);
        OutputStream out = new HorizontalDifferencingStream(bytes, 4, 3, 16, ByteOrder.BIG_ENDIAN);

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

        InputStream in = bytes.createInputStream();
        DataInput dataIn = new DataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(51556, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());
        assertEquals(51556, dataIn.readUnsignedShort());

        // Row 2
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testWrite3SPP16BPSLittleEndian() throws IOException {
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(24);

        OutputStream out = new HorizontalDifferencingStream(bytes, 4, 3, 16, ByteOrder.LITTLE_ENDIAN);
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

        InputStream in = bytes.createInputStream();
        DataInput dataIn = new LittleEndianDataInputStream(in);

        // Row 1
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(4660, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(51556, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());
        assertEquals(51556, dataIn.readUnsignedShort());

        // Row 2
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(30292, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(0, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());
        assertEquals(40196, dataIn.readUnsignedShort());

        // EOF
        assertEquals(-1, in.read());
    }

    @Test
    public void testWrite4SPP8BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 4, 4, 8, ByteOrder.BIG_ENDIAN);

        // Row 1
        stream.write(0xff);
        stream.write(0x00);
        stream.write(0x7f);
        stream.write(0x00);

        stream.write(0xfe);
        stream.write(0xff);
        stream.write(0x7e);
        stream.write(0xff);

        stream.write(0xfa);
        stream.write(0xfb);
        stream.write(0x7a);
        stream.write(0xfb);

        stream.write(0xfe);
        stream.write(0xff);
        stream.write(0x7e);
        stream.write(0xff);

        // Row 2
        stream.write(0x7f);
        stream.write(0x7f);
        stream.write(0x7f);
        stream.write(0x7f);

        stream.write(0x80);
        stream.write(0x80);
        stream.write(0x80);
        stream.write(0x80);

        stream.write(0x84);
        stream.write(0x84);
        stream.write(0x84);
        stream.write(0x84);

        stream.write(0x80);
        stream.write(0x80);
        stream.write(0x80);
        stream.write(0x80);

        // 4 samples per pixel, 8 bits per sample (RGBA)
        byte[] data = {
                (byte) 0xff, (byte) 0x00, (byte) 0x7f, 0x00, -1, -1, -1, -1, -4, -4, -4, -4, 4, 4, 4, 4,
                0x7f, 0x7f, 0x7f, 0x7f, 1, 1, 1, 1, 4, 4, 4, 4, -4, -4, -4, -4,
        };

        assertArrayEquals(data, bytes.toByteArray());
    }

    @Test
    public void testWriteArray4SPP8BPS() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream stream = new HorizontalDifferencingStream(bytes, 4, 4, 8, ByteOrder.BIG_ENDIAN);

        stream.write(
                new byte[] {
                        (byte) 0xff, 0x00, 0x7f, 0x00,
                        (byte) 0xfe, (byte) 0xff, 0x7e, (byte) 0xff,
                        (byte) 0xfa, (byte) 0xfb, 0x7a, (byte) 0xfb,
                        (byte) 0xfe, (byte) 0xff, 0x7e, (byte) 0xff,

                        0x7f, 0x7f, 0x7f, 0x7f,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                        (byte) 0x84, (byte) 0x84, (byte) 0x84, (byte) 0x84,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                }
        );

        // 4 samples per pixel, 8 bits per sample (RGBA)
        byte[] data = {
                (byte) 0xff, (byte) 0x00, (byte) 0x7f, 0x00, -1, -1, -1, -1, -4, -4, -4, -4, 4, 4, 4, 4,
                0x7f, 0x7f, 0x7f, 0x7f, 1, 1, 1, 1, 4, 4, 4, 4, -4, -4, -4, -4,
        };

        assertArrayEquals(data, bytes.toByteArray());
    }


}
