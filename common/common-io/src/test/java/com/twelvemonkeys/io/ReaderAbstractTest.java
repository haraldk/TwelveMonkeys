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
import java.io.Reader;

import static org.junit.Assert.*;

/**
 * ReaderAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/ReaderAbstractTestCase.java#1 $
 */
public abstract class ReaderAbstractTest extends ObjectAbstractTest {

    // Kindly provided by lipsum.org :-)
    protected final String mInput =
           "Cras tincidunt euismod tellus. Aenean a odio. " +
           "Aenean metus. Sed tristique est non purus. Class aptent " +
           "taciti sociosqu ad litora torquent per conubia nostra, per " +
           "inceptos hymenaeos. Fusce vulputate dolor non mauris. " +
           "Nullam nunc massa, pretium quis, ultricies a, varius quis, " +
           "neque. Nam id nulla eu ante malesuada fermentum. Sed " +
           "vulputate purus eget magna. Sed mollis. Curabitur enim " +
           "diam, faucibus ac, hendrerit eu, consequat nec, augue.";

    protected final Object makeObject() {
        return makeReader();
    }

    protected Reader makeReader() {
        return makeReader(mInput);
    }

    protected abstract Reader makeReader(String pInput);

    @Test
    public void testRead() throws IOException {
        Reader reader = makeReader();

        int count = 0;
        int ch;
        StringBuilder buffer = new StringBuilder(mInput.length());
        while ((ch = reader.read()) > 0) {
            count++;
            buffer.append((char) ch);
        }

        assertEquals(mInput.length(), count);
        assertEquals(mInput, buffer.toString());
    }

    @Test
    public void testReadBuffer() throws IOException {
        Reader reader = makeReader();

        char[] chars = new char[mInput.length()];
        StringBuilder buffer = new StringBuilder(mInput.length());

        int count;
        int offset = 0;
        int lenght = chars.length;
        while ((count = reader.read(chars, offset, lenght)) > 0) {
            buffer.append(chars, offset, count);
            offset += count;
            lenght -= count;
        }

        assertEquals(mInput, buffer.toString());
        assertEquals(mInput, new String(chars));
    }

    @Test
    public void testSkipToEnd() throws IOException {
        Reader reader = makeReader();

        int toSkip = mInput.length();
        while (toSkip > 0) {
            long skipped = reader.skip(toSkip);
            assertFalse("Skipped < 0", skipped < 0);
            toSkip -= skipped;
        }

        assertEquals(0, toSkip);
    }

    @Test
    public void testSkipToEndAndRead() throws IOException {
        Reader reader = makeReader();

        int toSkip = mInput.length();
        while (toSkip > 0) {
            toSkip -= reader.skip(toSkip);
        }

        assertEquals(reader.read(), -1);
    }

    // TODO: It's possible to support reset and not mark (resets to beginning of stream, for example)
    @Test
    public void testResetMarkSupported() throws IOException {
        Reader reader = makeReader();

        if (reader.markSupported()) {
            // Mark at 0
            reader.mark(mInput.length() / 4);

            // Read one char
            char ch = (char) reader.read();
            reader.reset();
            assertEquals(ch, (char) reader.read());
            reader.reset();

            // Read from start
            StringBuilder first = new StringBuilder(mInput.length() / 4);
            for (int i = 0; i < mInput.length() / 4; i++) {
                first.append((char) reader.read());
            }

            reader.reset(); // 0

            StringBuilder second = new StringBuilder(mInput.length() / 4);
            for (int i = 0; i < mInput.length() / 4; i++) {
                second.append((char) reader.read());
            }

            assertEquals(first.toString(), second.toString());

            // Mark at 1/4
            reader.mark(mInput.length() / 4);

            // Read from 1/4
            first = new StringBuilder(mInput.length() / 4);
            for (int i = 0; i < mInput.length() / 4; i++) {
                first.append((char) reader.read());
            }

            reader.reset(); // 1/4

            second = new StringBuilder(mInput.length() / 4);
            for (int i = 0; i < mInput.length() / 4; i++) {
                second.append((char) reader.read());
            }

            assertEquals(first.toString(), second.toString());

            // Read past limit
            reader.read();

            // This may or may not fail, depending on the stream
            try {
                reader.reset();
            }
            catch (IOException ioe) {
                assertNotNull(ioe.getMessage());
            }
        }
    }

    @Test
    public void testResetMarkNotSupported() throws IOException {
        Reader reader = makeReader();

        if (!reader.markSupported()) {
            try {
                reader.mark(mInput.length());
                fail("Mark set, while markSupprted is false");
            }
            catch (IOException e) {
                assertNotNull(e.getMessage());
            }

            // Read one char
            char ch = (char) reader.read();
            try {
                reader.reset();
                assertEquals(ch, (char) reader.read());
            }
            catch (IOException ioe) {
                assertNotNull(ioe.getMessage());
            }

            // Read from start
            StringBuilder first = new StringBuilder(mInput.length() / 4);
            for (int i = 0; i < mInput.length() / 4; i++) {
                first.append((char) reader.read());
            }

            try {
                reader.reset(); // 0

                StringBuilder second = new StringBuilder(mInput.length() / 4);
                for (int i = 0; i < mInput.length() / 4; i++) {
                    second.append((char) reader.read());
                }

                assertEquals(first.toString(), second.toString());
            }
            catch (IOException ioe) {
                assertNotNull(ioe.getMessage());
            }
        }
    }

    @Test
    public void testReadAfterClose() throws IOException {
        Reader reader = makeReader("foo bar");

        reader.close();

        try {
            reader.read();
            fail("Should not allow read after close");
        }
        catch (IOException ioe) {
            assertNotNull(ioe.getMessage());
        }
    }
}
