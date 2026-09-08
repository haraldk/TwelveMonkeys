/*
 * Copyright (c) 2012, Harald Kuhr
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

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.io.enc.DecodeException;
import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DecoderAbstractTest;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LZWDecoderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWDecoderTest.java,v 1.0 08.05.12 23:44 haraldk Exp$
 */
public class LZWDecoderTest extends DecoderAbstractTest {

    public static final int SPEED_TEST_ITERATIONS = 1024;

    @Test
    public void testIsOldBitReversedStreamTrue() throws IOException {
        assertTrue(LZWDecoder.isOldBitReversedStream(getClass().getResourceAsStream("/lzw/lzw-short.bin")));
    }

    @Test
    public void testIsOldBitReversedStreamFalse() throws IOException {
        assertFalse(LZWDecoder.isOldBitReversedStream(getClass().getResourceAsStream("/lzw/lzw-long.bin")));
    }

    @Test
    public void testShortBitReversedStream() throws IOException {
        InputStream stream = new DecoderStream(getClass().getResourceAsStream("/lzw/lzw-short.bin"), LZWDecoder.create(true), 128);
        InputStream unpacked = new ByteArrayInputStream(new byte[512 * 3 * 5]); // Should be all 0's

        assertSameStreamContents(unpacked, stream);
    }

    @Test
    public void testLongStream() throws IOException {
        InputStream stream = new DecoderStream(getClass().getResourceAsStream("/lzw/lzw-long.bin"), LZWDecoder.create(false), 1024);
        InputStream unpacked = getClass().getResourceAsStream("/lzw/unpacked-long.bin");

        assertSameStreamContents(unpacked, stream);
    }

    @Test
    public void testTableOverflowDoesNotWritePastTable() throws IOException {
        // A stream that keeps extending the dictionary without ever emitting a CLEAR code
        // must be rejected with a DecodeException, not write one entry past the code table.
        byte[] packed = lzwStreamFillingTableWithoutClear(3839);

        InputStream stream = new DecoderStream(new ByteArrayInputStream(packed), LZWDecoder.create(false), 1024);

        DecodeException exception = assertThrows(DecodeException.class, () -> {
            byte[] sink = new byte[4096];
            while (stream.read(sink) != -1) {
                // drain
            }
        });
        assertTrue(exception.getMessage().contains("table overflow"), exception.getMessage());
    }

    /**
     * Packs an LZW (spec, MSB-first) stream that decodes to single-byte codes only, never emitting
     * a CLEAR code, so the decoder keeps adding dictionary entries until the table is full.
     * Code widths follow the same schedule the decoder uses to read them.
     */
    private static byte[] lzwStreamFillingTableWithoutClear(final int addingCodes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] state = {0, 0, 9, (1 << 9) - 1, (1 << 9) - 2, 258}; // bits, bitPos, bitsPerCode, bitMask, maxCode, tableLength

        writeCode(out, state, 256); // CLEAR
        writeCode(out, state, 0);   // first literal, read in CLEAR branch (no table growth)

        for (int i = 0; i < addingCodes; i++) {
            writeCode(out, state, (i & 1) == 0 ? 1 : 0); // single-byte codes, always in table
            // Mirror addStringToTable: grow table, widen code as the decoder does
            state[5]++;
            if (state[5] > state[4]) {
                if (state[2] < 12) {
                    state[2]++;
                }
                state[3] = (1 << state[2]) - 1;
                state[4] = state[3] - 1;
            }
        }

        if (state[1] > 0) {
            out.write((state[0] << (8 - state[1])) & 0xff);
        }

        return out.toByteArray();
    }

    private static void writeCode(final ByteArrayOutputStream out, final int[] state, final int code) {
        state[0] = (state[0] << state[2]) | (code & state[3]);
        state[1] += state[2];
        while (state[1] >= 8) {
            out.write((state[0] >> (state[1] - 8)) & 0xff);
            state[1] -= 8;
        }
        state[0] &= (1 << state[1]) - 1;
    }

    private void assertSameStreamContents(InputStream expected, InputStream actual) {
        int count = 0;
        int data;

        try {
            while ((data = actual.read()) !=  -1) {
                count++;

                assertEquals(expected.read(), data, String.format("Incorrect data at offset 0x%04x", count));
            }

            assertEquals(-1, data);
            assertEquals(expected.read(), actual.read());
        }
        catch (IOException e) {
            fail(String.format("Bad/corrupted data or EOF at offset 0x%04x: %s", count, e.getMessage()));
        }
    }

    @Override
    public Decoder createDecoder() {
        return LZWDecoder.create(false);
    }

    @Override
    public Encoder createCompatibleEncoder() {
        // TODO: Need to know length of data to compress in advance...
        return null;
    }

    @Disabled
    @Test
    public void testSpeed() throws IOException {
        assertTimeoutPreemptively(Duration.ofMillis(3000), () -> {
            byte[] bytes = FileUtil.read(getClass().getResourceAsStream("/lzw/lzw-long.bin"));


            for (int i = 0; i < SPEED_TEST_ITERATIONS; i++) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                LZWDecoder decoder = new LZWDecoder.LZWSpecDecoder();

                int read, total = 0;
                while ((read = decoder.decode(input, buffer)) > 0) {
                    buffer.clear();
                    total += read;
                }

                assertEquals(49152, total);
            }
        });
    }
}
