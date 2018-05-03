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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * YCbCrUpsamplerStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: YCbCrUpsamplerStreamTest.java,v 1.0 31.01.13 14:35 haraldk Exp$
 */
public class YCbCrUpsamplerStreamTest  {
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullStream() {
        new YCbCrUpsamplerStream(null, new int[2], 7, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullChroma() {
        new YCbCrUpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[3], 7, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateShortChroma() {
        new YCbCrUpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[1], 7, 5);
    }

    @Test
    public void testUpsample22() throws IOException {
        byte[] bytes = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 42, 96,
                108, 109, 110, 111, 112, 113, 114, 115, 43, 97
        };
        InputStream stream = new YCbCrUpsamplerStream(new ByteArrayInputStream(bytes), new int[] {2, 2}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 8);

        byte[] expected = new byte[] {
                1, 5, 6, 2, 5, 6, 7, 108, 109, 8, 108, 109, 110, 114, 115, 111, 114, 115, 43, 0, 0, 97, 0, 0,
                3, 5, 6, 4, 5, 6, 42, 108, 109, 96, 108, 109, 112, 114, 115, 113, 114, 115, 0, 0, 0, 0, 0, 0
        };
        byte[] upsampled = new byte[expected.length];

        new DataInputStream(stream).readFully(upsampled);


        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testUpsample21() throws IOException {
        byte[] bytes = new byte[] {
                1, 2, 3, 4, 42, 96, 77,
                112, 113, 114, 115, 43, 97, 43
        };
        InputStream stream = new YCbCrUpsamplerStream(new ByteArrayInputStream(bytes), new int[] {2, 1}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 4);

        byte[] expected = new byte[] {
                1, 3, 4, 2, 3, 4, 42, 77, 112, 96, 77, 112, 113, 115, 43, 114, 115, 43, 97, 77, 112, 43, 77, 112
        };
        byte[] upsampled = new byte[expected.length];

        new DataInputStream(stream).readFully(upsampled);

        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testUpsample12() throws IOException {
        byte[] bytes = new byte[] {
                1, 2, 3, 4, 42, 96, 77,
                112, 113, 114, 115, 43, 97, 43
        };
        InputStream stream = new YCbCrUpsamplerStream(new ByteArrayInputStream(bytes), new int[] {1, 2}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 4);

        byte[] expected = new byte[] {
                1, 3, 4, 42, 77, 112, 113, 115, 43, 97, 0, 0, 2, 3, 4, 96, 77, 112, 114, 115, 43, 43, 0, 0
        };
        byte[] upsampled = new byte[expected.length];

        new DataInputStream(stream).readFully(upsampled);

        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }
}
