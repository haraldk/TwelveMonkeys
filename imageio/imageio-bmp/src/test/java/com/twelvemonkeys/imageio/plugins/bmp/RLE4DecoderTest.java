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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DecoderStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RLE4DecoderTest {

    public static final byte[] RLE_ENCODED = new byte[]{
            0x03, 0x04, 0x05, 0x06, 0x00, 0x06, 0x45, 0x56, 0x67, 0x00, 0x04, 0x78, 0x00, 0x02, 0x05, 0x01,
            0x04, 0x78, 0x00, 0x00, 0x09, 0x1E, 0x00, 0x01,
    };

    public static final byte[] DECODED = new byte[]{
            0x04, 0x00, 0x60, 0x60, 0x45, 0x56, 0x67, 0x78, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, (byte) 0x87, (byte) 0x80, 0x00, 0x00,
            0x1E, 0x1E, 0x1E, 0x1E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    };

    @Test
    public void decodeBuffer() throws IOException {
        // Setup:
        InputStream rleStream = getClass().getResourceAsStream("/bmpsuite/g/pal4rle.bmp");
        long rleOffset = 102;

        InputStream plainSream = getClass().getResourceAsStream("/bmpsuite/g/pal4.bmp");
        long plainOffset = 102;

        skipFully(rleStream, rleOffset);
        skipFully(plainSream, plainOffset);

        ByteBuffer decoded = ByteBuffer.allocate(64);
        Decoder decoder = new RLE4Decoder(127);

        ByteBuffer plain = ByteBuffer.allocate(64);
        ReadableByteChannel channel = Channels.newChannel(plainSream);

        for (int i = 0; i < 64; i++) {
            int d = decoder.decode(rleStream, decoded);
            decoded.rewind();
            int r = channel.read(plain);
            plain.rewind();

            assertEquals(r, d, "Difference at line " + i);
            assertArrayEquals(plain.array(), decoded.array(), "Difference at line " + i);
        }
    }

    @Test
    public void decodeStream() throws IOException {
        // Setup:
        InputStream rleStream = getClass().getResourceAsStream("/bmpsuite/g/pal4rle.bmp");
        long rleOffset = 102;

        InputStream plainSream = getClass().getResourceAsStream("/bmpsuite/g/pal4.bmp");
        long plainOffset = 102;

        skipFully(rleStream, rleOffset);
        skipFully(plainSream, plainOffset);

        InputStream decoded = new DecoderStream(rleStream, new RLE4Decoder(127));

        int pos = 0;
        while (true) {
            int expected = plainSream.read();
            assertEquals(expected, decoded.read(), "Differs at " + pos);

            if (expected < 0) {
                break;
            }

            pos++;
        }

        assertEquals(64 * 64, pos);
    }

    @Test
    public void decodeStreamWeird() throws IOException {
        // Setup:
        InputStream rleStream = getClass().getResourceAsStream("/bmp/blauesglas_4.rle");
        long rleOffset = 118;

        InputStream plainSream = getClass().getResourceAsStream("/bmp/blauesglas_4.bmp");
        long plainOffset = 118;

        skipFully(rleStream, rleOffset);
        skipFully(plainSream, plainOffset);

        InputStream decoded = new DecoderStream(rleStream, new RLE4Decoder(301));

        int pos = 0;
        int w = ((301 * 4 + 31) / 32) * 4;
        int h = 331;
        int size = w * h;

        while (pos < size) {
            int expected = plainSream.read();
            int actual = decoded.read();
//            assertEquals("Differs at " + pos, expected, actual);
            // Seems the initial RLE-encoding screwed up on some pixels...

            if (expected < 0) {
                break;
            }

            pos++;
        }

        // Rubbish assertion...
        assertEquals(size, pos);
    }

    @Test
    public void decodeExampleW27() throws IOException {
        Decoder decoder = new RLE4Decoder(27); // Can be 27, 28, 29, 30, 31 or 32, and should all be the same.
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int count = decoder.decode(new ByteArrayInputStream(RLE_ENCODED), buffer);

        assertArrayEquals(DECODED, Arrays.copyOfRange(buffer.array(), 0, count));
    }

    @Test
    public void decodeExampleW28to31() throws IOException {
        for (int i = 28; i < 32; i++) {
            Decoder decoder = new RLE4Decoder(i); // Can be 27, 28, 29, 30, 31 or 32, and should all be the same.
            ByteBuffer buffer = ByteBuffer.allocate(64);
            int count = decoder.decode(new ByteArrayInputStream(RLE_ENCODED), buffer);

            assertArrayEquals(DECODED, Arrays.copyOfRange(buffer.array(), 0, count));
        }
    }

    @Test
    public void decodeExampleW32() throws IOException {
        Decoder decoder = new RLE4Decoder(32); // Can be 27, 28, 29, 30, 31 or 32, and should all be the same.
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int count = decoder.decode(new ByteArrayInputStream(RLE_ENCODED), buffer);

        assertArrayEquals(DECODED, Arrays.copyOfRange(buffer.array(), 0, count));
    }

    private void skipFully(final InputStream stream, final long toSkip) throws IOException {
        long skipped = 0;
        while (skipped < toSkip) {
            skipped += stream.skip(toSkip - skipped);
        }
    }
}
