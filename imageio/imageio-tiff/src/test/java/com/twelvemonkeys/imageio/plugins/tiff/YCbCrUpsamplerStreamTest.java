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

import static org.junit.Assert.*;

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
        new YCbCrUpsamplerStream(null, new int[2], 7, 5, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullChroma() {
        new YCbCrUpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[3], 7, 5, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateShortChroma() {
        new YCbCrUpsamplerStream(new ByteArrayInputStream(new byte[0]), new int[1], 7, 5, null);
    }

    @Test
    public void testUpsample22() throws IOException {
        byte[] bytes = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 42, 96,
                108, 109, 110, 111, 112, 113, 114, 115, 43, 97
        };
        InputStream stream = new YCbCrUpsamplerStream(new ByteArrayInputStream(bytes), new int[] {2, 2}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 8, null);

        byte[] expected = new byte[] {
                0, -126, 0, 0, -125, 0, 0, 27, 0, 0, 28, 0, 92, 124, 85, 93, 125, 86, 0, -78, 0, 0, -24, 0,
                0, -124, 0, 0, -123, 0, 15, 62, 7, 69, 116, 61, 94, 126, 87, 95, 127, 88, 0, -121, 0, 0, -121, 0
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
        InputStream stream = new YCbCrUpsamplerStream(new ByteArrayInputStream(bytes), new int[] {2, 1}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 4, null);

        byte[] expected = new byte[] {
                0, -123, 0, 0, -122, 0, 20, 71, 0, 74, 125, 6, 0, -78, 90, 0, -77, 91, 75, 126, 7, 21, 72, 0
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
        InputStream stream = new YCbCrUpsamplerStream(new ByteArrayInputStream(bytes), new int[] {1, 2}, TIFFExtension.YCBCR_POSITIONING_CENTERED, 4, null);

        byte[] expected = new byte[] {
                0, -123, 0, 20, 71, 0, 0, -78, 90, 0, -24, 0, 0, -122, 0, 74, 125, 6, 0, -77, 91, 0, -78, 0
        };
        byte[] upsampled = new byte[expected.length];

        new DataInputStream(stream).readFully(upsampled);

        assertArrayEquals(expected, upsampled);
        assertEquals(-1, stream.read());
    }
}
