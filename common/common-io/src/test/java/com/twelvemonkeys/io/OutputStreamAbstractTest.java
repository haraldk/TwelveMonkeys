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

package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.ObjectAbstractTest;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * InputStreamAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/OutputStreamAbstractTestCase.java#1 $
 */
public abstract class OutputStreamAbstractTest extends ObjectAbstractTest {
    protected abstract OutputStream makeObject();

    @Test
    public void testWrite() throws IOException {
        OutputStream os = makeObject();

        for (int i = 0; i < 256; i++) {
            os.write((byte) i);
        }
    }

    @Test
    public void testWriteByteArray() throws IOException {
        OutputStream os = makeObject();

        os.write(new byte[256]);
    }

    @Test
    public void testWriteByteArrayNull() {
        OutputStream os = makeObject();
        try {
            os.write(null);
            fail("Should not accept null-argument");
        }
        catch (IOException e) {
            fail("Should not throw IOException of null-arguemnt: " + e.getMessage());
        }
        catch (NullPointerException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw NullPointerException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void testWriteByteArrayOffsetLength() throws IOException {
        byte[] input = new byte[256];

        OutputStream os = makeObject();

        // TODO: How to test that data is actually written!?
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }

        for (int i = 0; i < 256; i++) {
            os.write(input, i, 256 - i);
        }

        for (int i = 0; i < 4; i++) {
            os.write(input, i * 64, 64);
        }
    }

    @Test
    public void testWriteByteArrayZeroLength() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[1], 0, 0);
        }
        catch (Exception e) {
            fail("Should not throw Exception: " + e.getMessage());
        }
    }

    @Test
    public void testWriteByteArrayOffsetLengthNull() {
        OutputStream os = makeObject();
        try {
            os.write(null, 5, 10);
            fail("Should not accept null-argument");
        }
        catch (IOException e) {
            fail("Should not throw IOException of null-arguemnt: " + e.getMessage());
        }
        catch (NullPointerException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw NullPointerException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void testWriteByteArrayNegativeOffset() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], -3, 5);
            fail("Should not accept negative offset");
        }
        catch (IOException e) {
            fail("Should not throw IOException negative offset: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void testWriteByteArrayNegativeLength() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], 2, -5);
            fail("Should not accept negative length");
        }
        catch (IOException e) {
            fail("Should not throw IOException negative length: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void testWriteByteArrayOffsetOutOfBounds() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], 5, 1);
            fail("Should not accept offset out of bounds");
        }
        catch (IOException e) {
            fail("Should not throw IOException offset out of bounds: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void testWriteByteArrayLengthOutOfBounds() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], 1, 5);
            fail("Should not accept length out of bounds");
        }
        catch (IOException e) {
            fail("Should not throw IOException length out of bounds: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void testFlush() {
        // TODO: Implement
    }

    @Test
    public void testClose() {
        // TODO: Implement
    }

    @Test
    public void testWriteAfterClose() throws IOException {
        OutputStream os = makeObject();

        os.close();

        boolean success = false;
        try {
            os.write(0);
            success  = true;
            // TODO: Not all streams throw exception! (ByteArrayOutputStream)
            //fail("Write after close");
        }
        catch (IOException e) {
            assertNotNull(e.getMessage());
        }

        try {
            os.write(new byte[16]);
            // TODO: Not all streams throw exception! (ByteArrayOutputStream)
            //fail("Write after close");
            if (!success) {
                fail("Inconsistent write(int)/write(byte[]) after close");
            }
        }
        catch (IOException e) {
            assertNotNull(e.getMessage());
            if (success) {
                fail("Inconsistent write(int)/write(byte[]) after close");
            }
        }
    }

    @Test
    public void testFlushAfterClose() throws IOException {
        OutputStream os = makeObject();

        os.close();

        try {
            os.flush();
            // TODO: Not all streams throw exception! (ByteArrayOutputStream)
            //fail("Flush after close");
            try {
                os.write(0);
            }
            catch (IOException e) {
                fail("Inconsistent write/flush after close");
            }
        }
        catch (IOException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testCloseAfterClose() throws IOException {
        OutputStream os = makeObject();

        os.close();

        try {
            os.close();
        }
        catch (IOException e) {
            fail("Close after close, failed: " + e.getMessage());
        }
    }
}
