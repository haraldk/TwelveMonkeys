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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.ObjectAbstractTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * InputStreamAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/InputStreamAbstractTestCase.java#1 $
 */
public abstract class InputStreamAbstractTest extends ObjectAbstractTest {
    // TODO: FixMe! THIS TEST IS (WAS) COMPLETELY BROKEN...
    // It relies on the contents of the stream being a certain order byte0 == 0, byte1 == 1 etc..
    // But the subclasses don't implement this.. Need to fix.

    final static private long SEED = 29487982745l;
    final static Random sRandom = new Random(SEED);

    protected final Object makeObject() {
        return makeInputStream();
    }

    protected InputStream makeInputStream() {
        return makeInputStream(16);
    }

    protected InputStream makeInputStream(int pSize) {
        byte[] bytes = makeRandomArray(pSize);
        return makeInputStream(bytes);
    }

    protected abstract InputStream makeInputStream(byte[] pBytes);

    protected final byte[] makeRandomArray(final int pSize) {
        byte[] bytes = new byte[pSize];
        sRandom.nextBytes(bytes);
        return bytes;
    }

    protected final byte[] makeOrderedArray(final int pSize) {
        byte[] bytes = new byte[pSize];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    @Test
    public void testRead() throws Exception {
        int size = 5;
        InputStream input = makeInputStream(makeOrderedArray(size));
        for (int i = 0; i < size; i++) {
            assertTrue("Check Size [" + i + "]", (size - i) >= input.available());
            assertEquals("Check Value [" + i + "]", i, input.read());
        }
        assertEquals("Available after contents all read", 0, input.available());

        // Test reading after the end of file
        try {
            int result = input.read();
            assertEquals("Wrong value read after end of file", -1, result);
        }
        catch (IOException e) {
            fail("Should not have thrown an IOException: " + e.getMessage());
        }
    }

    @Test
    public void testAvailable() throws Exception {
        InputStream input = makeInputStream(1);
        assertFalse("Unexpected EOF", input.read() < 0);
        assertEquals("Available after contents all read", 0, input.available());

        // Check availbale is zero after End of file
        assertEquals("End of File", -1, input.read());
        assertEquals("Available after End of File", 0, input.available());
    }

    @Test
    public void testReadByteArray() throws Exception {
        byte[] bytes = new byte[10];
        byte[] data = makeOrderedArray(15);
        InputStream input = makeInputStream(data);

        // Read into array
        int count1 = input.read(bytes);
        assertEquals("Read 1", bytes.length, count1);
        for (int i = 0; i < count1; i++) {
            assertEquals("Check Bytes 1", i, bytes[i]);
        }

        // Read into array
        int count2 = input.read(bytes);
        assertEquals("Read 2", 5, count2);
        for (int i = 0; i < count2; i++) {
            assertEquals("Check Bytes 2", count1 + i, bytes[i]);
        }

        // End of File
        int count3 = input.read(bytes);
        assertEquals("Read 3 (EOF)", -1, count3);

        // Test reading after the end of file
        try {
            int result = input.read(bytes);
            assertEquals("Wrong value read after end of file", -1, result);
        }
        catch (IOException e) {
            fail("Should not have thrown an IOException: " + e.getMessage());
        }

        // Reset
        input = makeInputStream(data);

        // Read into array using offset & length
        int offset = 2;
        int lth = 4;
        int count5 = input.read(bytes, offset, lth);
        assertEquals("Read 5", lth, count5);
        for (int i = offset; i < lth; i++) {
            assertEquals("Check Bytes 2", i - offset, bytes[i]);
        }
    }

    @Test
    public void testEOF() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(2));
        assertEquals("Read 1", 0, input.read());
        assertEquals("Read 2", 1, input.read());
        assertEquals("Read 3", -1, input.read());
        assertEquals("Read 4", -1, input.read());
        assertEquals("Read 5", -1, input.read());
    }

    @Test
    public void testMarkResetUnsupported() throws IOException {
        InputStream input = makeInputStream(10);
        if (input.markSupported()) {
            return;
        }

        input.mark(100); // Should be a no-op

        int read = input.read();
        assertTrue(read >= 0);

        // TODO: According to InputStream#reset, it is allowed to do some
        // implementation specific reset, and still be correct...
        try {
            input.reset();
            fail("Should throw IOException");
        }
        catch (IOException e) {
            assertTrue("Wrong messge: " + e.getMessage(), e.getMessage().contains("reset"));
        }
    }

    @Test
    public void testResetNoMark() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(10));

        if (!input.markSupported()) {
            return; // Not supported, skip test
        }

        int read = input.read();
        assertEquals(0, read);

        // No mark may either throw exception, or reset to beginning of stream.
        try {
            input.reset();
            assertEquals("Re-read of reset data should be same", 0, input.read());
        }
        catch (Exception e) {
            assertTrue("Wrong no mark IOException message", e.getMessage().contains("mark"));
        }
    }

    @Test
    public void testMarkReset() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(25));

        if (!input.markSupported()) {
            return; // Not supported, skip test
        }

        int read = input.read();
        assertEquals(0, read);

        int position = 1;
        int readlimit = 10;

        // Mark
        input.mark(readlimit);

        // Read further
        for (int i = 0; i < 3; i++) {
            assertEquals("Read After Mark [" + i + "]", (position + i), input.read());
        }

        // Reset
        input.reset();

        // Read from marked position
        for (int i = 0; i < readlimit + 1; i++) {
            assertEquals("Read After Reset [" + i + "]", (position + i), input.read());
        }
    }

    @Test
    public void testResetAfterReadLimit() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(25));

        if (!input.markSupported()) {
            return; // Not supported, skip test
        }

        int read = input.read();
        assertEquals(0, read);

        int position = 1;
        int readlimit = 5;

        // Mark
        input.mark(readlimit);

        // Read past marked position
        for (int i = 0; i < readlimit + 1; i++) {
            assertEquals("Read After Reset [" + i + "]", (position + i), input.read());
        }

        // Reset after read limit passed, may either throw exception, or reset to last mark
        try {
            input.reset();
            assertEquals("Re-read of reset data should be same", 1, input.read());
        }
        catch (Exception e) {
            assertTrue("Wrong read-limit IOException message", e.getMessage().contains("mark"));
        }
    }

    @Test
    public void testResetAfterReset() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(25));

        if (!input.markSupported()) {
            return; // Not supported, skip test
        }

        int first = input.read();
        assertTrue("Expected to read positive value", first >= 0);

        int readlimit = 5;

        // Mark
        input.mark(readlimit);
        int read = input.read();
        assertTrue("Expected to read positive value", read >= 0);

        assertTrue(input.read() >= 0);
        assertTrue(input.read() >= 0);

        input.reset();
        assertEquals("Expected value read differs from actual", read, input.read());

        // Reset after read limit passed, may either throw exception, or reset to last good mark
        try {
            input.reset();
            int reRead = input.read();
            assertTrue("Re-read of reset data should be same as initially marked or first", reRead == read || reRead == first);
        }
        catch (Exception e) {
            assertTrue("Wrong read-limit IOException message", e.getMessage().contains("mark"));
        }
    }

    @Test
    public void testSkip() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(10));

        assertEquals("Unexpected value read", 0, input.read());
        assertEquals("Unexpected value read", 1, input.read());
        assertEquals("Unexpected number of bytes skipped", 5, input.skip(5));
        assertEquals("Unexpected value read", 7, input.read());

        assertEquals("Unexpected number of bytes skipped", 2, input.skip(5)); // only 2 left to skip
        assertEquals("Unexpected value read after EOF", -1, input.read());

        // Spec says skip might return 0 or negative after EOF...
        assertTrue("Positive value skipped after EOF", input.skip(5) <= 0); // End of file
        assertEquals("Unexpected value read after EOF", -1, input.read());
    }

    @Test
    public void testSanityOrdered() throws IOException {
        // This is to sanity check that the test itself is correct...
        byte[] bytes = makeOrderedArray(25);
        InputStream expected = new ByteArrayInputStream(bytes);
        InputStream actual = makeInputStream(bytes);

        for (byte b : bytes) {
            assertEquals((int) b, expected.read());
            assertEquals((int) b, actual.read());
        }
    }

    @Test
    public void testSanityOrdered2() throws IOException {
        // This is to sanity check that the test itself is correct...
        byte[] bytes = makeOrderedArray(25);
        InputStream expected = new ByteArrayInputStream(bytes);
        InputStream actual = makeInputStream(bytes);

        byte[] e = new byte[bytes.length];
        byte[] a = new byte[bytes.length];

        assertEquals(e.length, expected.read(e, 0, e.length));
        assertEquals(a.length, actual.read(a, 0, a.length));

        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], e[i]);
            assertEquals(bytes[i], a[i]);
        }
    }

    @Test
    public void testSanityNegative() throws IOException {
        // This is to sanity check that the test itself is correct...
        byte[] bytes = new byte[25];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (255 - i);
        }
        InputStream expected = new ByteArrayInputStream(bytes);
        InputStream actual = makeInputStream(bytes);

        for (byte b : bytes) {
            assertEquals(b & 0xff, expected.read());
            assertEquals(b & 0xff, actual.read());
        }
    }

    @Test
    public void testSanityNegative2() throws IOException {
        // This is to sanity check that the test itself is correct...
        byte[] bytes = new byte[25];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (255 - i);
        }
        InputStream expected = new ByteArrayInputStream(bytes);
        InputStream actual = makeInputStream(bytes);

        byte[] e = new byte[bytes.length];
        byte[] a = new byte[bytes.length];

        assertEquals(e.length, expected.read(e, 0, e.length));
        assertEquals(a.length, actual.read(a, 0, a.length));

        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], e[i]);
            assertEquals(bytes[i], a[i]);
        }
    }

    @Test
    public void testSanityRandom() throws IOException {
        // This is to sanity check that the test itself is correct...
        byte[] bytes = makeRandomArray(25);
        InputStream expected = new ByteArrayInputStream(bytes);
        InputStream actual = makeInputStream(bytes);

        for (byte b : bytes) {
            assertEquals(b & 0xff, expected.read());
            assertEquals(b & 0xff, actual.read());
        }
    }

    @Test
    public void testSanityRandom2() throws IOException {
        // This is to sanity check that the test itself is correct...
        byte[] bytes = makeRandomArray(25);
        InputStream expected = new ByteArrayInputStream(bytes);
        InputStream actual = makeInputStream(bytes);

        byte[] e = new byte[bytes.length];
        byte[] a = new byte[bytes.length];

        assertEquals(e.length, expected.read(e, 0, e.length));
        assertEquals(a.length, actual.read(a, 0, a.length));

        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], e[i]);
            assertEquals(bytes[i], a[i]);
        }
    }}
