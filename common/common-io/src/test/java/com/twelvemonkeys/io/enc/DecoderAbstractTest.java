/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.ObjectAbstractTest;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * AbstractDecoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/DecoderAbstractTestCase.java#1 $
 */
public abstract class DecoderAbstractTest extends ObjectAbstractTest {

    public abstract Decoder createDecoder();
    public abstract Encoder createCompatibleEncoder();

    protected Object makeObject() {
        return createDecoder();
    }

    @Test(expected = NullPointerException.class)
    public final void testNullDecode() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[20]);

        decoder.decode(bytes, null);
        fail("null should throw NullPointerException");
    }

    @Test
    public final void testEmptyDecode() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[0]);

        try {
            int count = decoder.decode(bytes, ByteBuffer.allocate(128));
            assertEquals("Should not be able to read any bytes", 0, count);
        }
        catch (EOFException allowed) {
            // Okay
        }
    }

    private byte[] createData(int pLength) throws Exception {
        byte[] bytes = new byte[pLength];
        EncoderAbstractTest.RANDOM.nextBytes(bytes);
        return bytes;
    }

    private void runStreamTest(int pLength) throws Exception {
        byte[] data = createData(pLength);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        OutputStream out = new EncoderStream(outBytes, createCompatibleEncoder(), true);
        out.write(data);
        out.close();
        byte[] encoded = outBytes.toByteArray();

        byte[] decoded = FileUtil.read(new DecoderStream(new ByteArrayInputStream(encoded), createDecoder()));
        assertArrayEquals(String.format("Data %d", pLength), data, decoded);

        InputStream in = new DecoderStream(new ByteArrayInputStream(encoded), createDecoder());
        outBytes = new ByteArrayOutputStream();
        FileUtil.copy(in, outBytes);
        outBytes.close();
        in.close();

        decoded = outBytes.toByteArray();
        assertArrayEquals(String.format("Data %d", pLength), data, decoded);
    }

    @Test
    public final void testStreams() throws Exception {
        if (createCompatibleEncoder() == null) {
            return;
        }

        for (int i = 1; i < 100; i++) {
            try {
                runStreamTest(i);
            }
            catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
        }

        for (int i = 100; i < 2000; i += 250) {
            try {
                runStreamTest(i);
            }
            catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
        }

        for (int i = 2000; i < 80000; i += 1000) {
            try {
                runStreamTest(i);
            }
            catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
        }
    }
}
