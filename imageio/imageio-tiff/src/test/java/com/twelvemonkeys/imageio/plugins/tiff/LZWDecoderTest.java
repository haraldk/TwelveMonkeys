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
import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DecoderAbstractTest;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.Encoder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

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

    private void assertSameStreamContents(InputStream expected, InputStream actual) {
        int count = 0;
        int data;

        try {
            while ((data = actual.read()) !=  -1) {
                count++;

                assertEquals(String.format("Incorrect data at offset 0x%04x", count), expected.read(), data);
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

    @Ignore
    @Test(timeout = 3000)
    public void testSpeed() throws IOException {
        byte[] bytes = FileUtil.read(getClass().getResourceAsStream("/lzw/lzw-long.bin"));


        for (int i = 0; i < SPEED_TEST_ITERATIONS; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            LZWDecoder decoder = new LZWDecoder.LZWSpecDecoder();

            int read, total = 0;
            while((read = decoder.decode(input, buffer)) > 0) {
                buffer.clear();
                total += read;
            }

            assertEquals(49152, total);
        }
    }
}
