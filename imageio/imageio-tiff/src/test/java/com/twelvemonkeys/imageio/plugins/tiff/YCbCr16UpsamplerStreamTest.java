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

import com.twelvemonkeys.io.LittleEndianDataInputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

/**
 * YCbCr16UpsamplerStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: YCbCr16UpsamplerStreamTest.java,v 1.0 31.01.13 14:35 haraldk Exp$
 */
public class YCbCr16UpsamplerStreamTest {
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullStream() {
        new YCbCr16UpsamplerStream(null, new int[2], 7, 5, null, ByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullChroma() {
        new YCbCr16UpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[3], 7, 5, null, ByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateShortChroma() {
        new YCbCr16UpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[1], 7, 5, null, ByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNoByteOrder() {
        new YCbCr16UpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[] {2, 2}, 7, 5, null, null);
    }

    // TODO: The expected values seems bogus...
    // But visually, it looks okay for the one and only sample image I've got...

    @Test
    public void testUpsample22() throws IOException {
        short[] shorts = new short[] {
                1, 2, 3, 4, 5, 6, 7, 8, 42, 96,
                108, 109, 110, 111, 112, 113, 114, 115, 43, 97
        };

        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);

        InputStream stream = new YCbCr16UpsamplerStream(new ByteArrayInputStream(bytes), new int[] {2, 2}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 8, null, ByteOrder.LITTLE_ENDIAN);

        short[] expected = new short[] {
                0, -30864, 0, 0, -30863, 0, 0, -30966, 0, 0, -30965, 0, 0, -30870, 0, 0, -30869, 0, 0, -30815, 0, 0, -30761, 0,
                0, -30862, 0, 0, -30861, 0, 0, -30931, 0, 0, -30877, 0, 0, -30868, 0, 0, -30867, 0, 0, -30858, 0, 0, -30858, 0
        };
        short[] upsampled = new short[expected.length];

        LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(stream);
        for (int i = 0; i < upsampled.length; i++) {
            upsampled[i] = dataInput.readShort();
        }

        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testUpsample21() throws IOException {
        short[] shorts = new short[] {
                1, 2, 3, 4, 42, 96, 77,
                112, 113, 114, 115, 43, 97, 43
        };

        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(shorts);
        InputStream stream = new YCbCr16UpsamplerStream(new ByteArrayInputStream(bytes), new int[] {2, 1}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 4, null, ByteOrder.BIG_ENDIAN);

        short[] expected = new short[] {
                0, -30861, 0, 0, -30860, 0, 0, -30923, 0, 0, -30869, 0, 0, -30816, 0, 0, -30815, 0, 0, -30868, 0, 0, -30922, 0
        };
        short[] upsampled = new short[expected.length];

        DataInputStream dataInput = new DataInputStream(stream);
        for (int i = 0; i < upsampled.length; i++) {
            upsampled[i] = dataInput.readShort();
        }

        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testUpsample12() throws IOException {
        short[] shorts = new short[] {
                1, 2, 3, 4, 42, 96, 77,
                112, 113, 114, 115, 43, 97, 43
        };

        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(shorts);
        InputStream stream = new YCbCr16UpsamplerStream(new ByteArrayInputStream(bytes), new int[] {1, 2}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 4, null, ByteOrder.BIG_ENDIAN);

        short[] expected = new short[] {
                0, -30861, 0, 0, -30923, 0, 0, -30816, 0, 0, -30761, 0, 0, -30860, 0, 0, -30869, 0, 0, -30815, 0, 0, -30815, 0
        };
        short[] upsampled = new short[expected.length];

        DataInputStream dataInput = new DataInputStream(stream);
        for (int i = 0; i < upsampled.length; i++) {
            upsampled[i] = dataInput.readShort();
        }

        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }
}
