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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * SeekableInputStreamAbstractTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/SeekableInputStreamAbstractTestCase.java#4 $
 */
public abstract class SeekableInputStreamAbstractTest extends InputStreamAbstractTest implements SeekableInterfaceTest {
    //// TODO: Figure out a better way of creating interface tests without duplicating code
    final SeekableAbstractTest seekableTestCase = new SeekableAbstractTest() {
        protected Seekable createSeekable() {
            return makeInputStream();
        }
    };

    @Override
    protected SeekableInputStream makeInputStream() {
        return (SeekableInputStream) super.makeInputStream();
    }

    @Override
    protected SeekableInputStream makeInputStream(final int pSize) {
        return (SeekableInputStream) super.makeInputStream(pSize);
    }

    protected SeekableInputStream makeInputStream(byte[] pBytes) {
        return makeInputStream(new ByteArrayInputStream(pBytes));
    }

    protected abstract SeekableInputStream makeInputStream(InputStream pStream);

    @Test
    @Override
    public void testResetAfterReset() throws Exception {
        InputStream input = makeInputStream(makeOrderedArray(25));

        if (!input.markSupported()) {
            return; // Not supported, skip test
        }

        assertTrue("Expected to read positive value", input.read() >= 0);

        int readlimit = 5;

        // Mark
        input.mark(readlimit);
        int read = input.read();
        assertTrue("Expected to read positive value", read >= 0);

        input.reset();
        assertEquals("Expected value read differs from actual", read, input.read());

        // Reset after read limit passed, may either throw exception, or reset to last good mark
        try {
            input.reset();
            assertEquals("Re-read of reset data should be first", 0, input.read());
        }
        catch (Exception e) {
            assertTrue("Wrong read-limit IOException message", e.getMessage().contains("mark"));
        }
    }

    @Test
    public void testSeekable() {
        seekableTestCase.testSeekable();
    }

    @Test
    public void testFlushBeyondCurrentPos() throws Exception {
        SeekableInputStream seekable = makeInputStream(20);

        int pos = 10;
        try {
            seekable.flushBefore(pos);
            fail("Flush beyond current position should throw IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            // Ignore
        }
    }

    @Test
    public void testSeek() throws Exception {
        SeekableInputStream seekable = makeInputStream(55);
        int pos = 37;

        seekable.seek(pos);
        long streamPos = seekable.getStreamPosition();
        assertEquals("Stream positon should match seeked position", pos, streamPos);
    }

    @Test
    public void testSeekFlush() throws Exception {
        SeekableInputStream seekable = makeInputStream(133);
        int pos = 45;
        seekable.seek(pos);
        seekable.flushBefore(pos);
        long flushedPos = seekable.getFlushedPosition();
        assertEquals("Flushed positon should match position", pos, flushedPos);

        try {
            seekable.seek(pos - 1);
            fail("Read before flushed position succeeded");
        }
        catch (IndexOutOfBoundsException e) {
            // Ignore
        }
    }

    @Test
    public void testMarkFlushReset() throws Exception {
        SeekableInputStream seekable = makeInputStream(77);

        seekable.mark();

        int position = 55;
        seekable.seek(position);
        seekable.flushBefore(position);

        try {
            seekable.reset();
            fail("Reset before flushed position succeeded");
        }
        catch (IOException e) {
            // Ignore
        }

        assertEquals(position, seekable.getStreamPosition());
    }

    @Test
    public void testSeekSkipRead() throws Exception {
        SeekableInputStream seekable = makeInputStream(133);
        int pos = 45;
        for (int i = 0; i < 10; i++) {
            seekable.seek(pos);
            //noinspection ResultOfMethodCallIgnored
            seekable.skip(i);
            byte[] bytes = FileUtil.read(seekable);
            assertEquals(133, seekable.getStreamPosition());
            assertEquals(133 - 45- i, bytes.length);
        }
    }

    protected void testSeekSkip(SeekableInputStream pSeekable, String pStr) throws IOException {
        System.out.println();
        pSeekable.seek(pStr.length());
        FileUtil.read(pSeekable);
        for (int i = 0; i < 10; i++) {
            byte[] bytes = FileUtil.read(pSeekable);
            int len = bytes.length;
            if (len != 0) {
                System.err.println("Error in buffer length after full read...");
                System.err.println("len: " + len);
                System.err.println("bytes: \"" + new String(bytes) + "\"");
                break;
            }
        }

        System.out.println();

        for (int i = 0; i < 10; i++) {
            pSeekable.seek(0);
            int skip = i * 3;
            //noinspection ResultOfMethodCallIgnored
            pSeekable.skip(skip);
            String str = new String(FileUtil.read(pSeekable));
            System.out.println(str);
            if (str.length() != pStr.length() - skip) {
                throw new Error("Error in buffer length after skip");
            }
        }

        System.out.println();
        System.out.println("seek/skip ok!");
        System.out.println();
    }

    protected static void markReset(SeekableInputStream pSeekable) throws IOException {
        for (int i = 0; i < 10; i++) {
            pSeekable.mark();
            System.out.println(new String(FileUtil.read(pSeekable)));
            pSeekable.reset();
        }

        System.out.println();
        System.out.println("mark/reset ok!");
    }

    protected static void timeRead(SeekableInputStream pSeekable) throws IOException {
        for (int i = 0; i < 5000; i++) {
            pSeekable.mark();
            FileUtil.read(pSeekable);
            pSeekable.reset();
        }

        long start = System.currentTimeMillis();
        final int times = 200000;
        for (int i = 0; i < times; i++) {
            pSeekable.mark();
            FileUtil.read(pSeekable);
            pSeekable.reset();
        }
        long time = System.currentTimeMillis() - start;

        System.out.println("Time; " + time + "ms (" + (time / (float) times) + "ms/inv)");
    }

    /*

    // Test code below...
    protected final static String STR = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Fusce massa orci, adipiscing vel, dapibus et, vulputate tristique, tortor. Quisque sodales. Mauris varius turpis et pede. Nam ac dolor vel diam condimentum elementum. Pellentesque eget tellus. Praesent magna. Sed fringilla. Proin ullamcorper tincidunt ante. Fusce dapibus nibh nec dolor. Etiam erat. Nullam dignissim laoreet nibh. Maecenas scelerisque. Pellentesque in quam. Maecenas sollicitudin, magna nec imperdiet facilisis, metus quam tristique ipsum, vitae consequat massa purus eget leo. Nulla ipsum. Proin non purus eget tellus lobortis iaculis. In lorem justo, posuere id, vulputate at, adipiscing ut, nisl. Nunc dui erat, tincidunt ac, interdum quis, rutrum et, libero. Etiam lectus dui, viverra sit amet, elementum ut, malesuada sed, massa. Vestibulum mi nulla, sodales vel, vestibulum sed, congue blandit, velit.";

    protected static void flushSeek(SeekableInputStream pSeekable, String pStr) throws IOException {
        pSeekable.seek(0);
        pSeekable.mark();
        int pos = pStr.length() / 2;
        try {
            pSeekable.flushBefore(pos);
            System.err.println("Error in flush/seek");
        }
        catch (IndexOutOfBoundsException e) {
            // Ignore
        }
        pSeekable.seek(pos);
        long streamPos = pSeekable.getStreamPosition();
        if (streamPos != pos) {
            System.err.println("Streampos not equal seeked pos");
        }

        pSeekable.flushBefore(pos);
        long flushedPos = pSeekable.getFlushedPosition();
        if (flushedPos != pos) {
            System.err.println("flushedpos not equal set flushed pos");
        }

        for (int i = 0; i < 10; i++) {
            pSeekable.seek(pos);
            //noinspection ResultOfMethodCallIgnored
            pSeekable.skip(i);
            System.out.println(new String(FileUtil.read(pSeekable)));
        }

        try {
            pSeekable.seek(pos - 1);
            System.err.println("Error in flush/seek");
        }
        catch (IndexOutOfBoundsException e) {
            // Ignore
        }
        try {
            pSeekable.reset();
            System.err.println("Error in flush/seek");
        }
        catch (IOException e) {
            // Ignore
        }

        System.out.println();
        System.out.println("flush/seek ok!");
    }

    protected static void seekSkip(SeekableInputStream pSeekable, String pStr) throws IOException {
        System.out.println();
        pSeekable.seek(pStr.length());
        FileUtil.read(pSeekable);
        for (int i = 0; i < 10; i++) {
            byte[] bytes = FileUtil.read(pSeekable);
            int len = bytes.length;
            if (len != 0) {
                System.err.println("Error in buffer length after full read...");
                System.err.println("len: " + len);
                System.err.println("bytes: \"" + new String(bytes) + "\"");
                break;
            }
        }

        System.out.println();

        for (int i = 0; i < 10; i++) {
            pSeekable.seek(0);
            int skip = i * 3;
            //noinspection ResultOfMethodCallIgnored
            pSeekable.skip(skip);
            String str = new String(FileUtil.read(pSeekable));
            System.out.println(str);
            if (str.length() != pStr.length() - skip) {
                throw new Error("Error in buffer length after skip");
            }
        }

        System.out.println();
        System.out.println("seek/skip ok!");
        System.out.println();
    }

    protected static void markReset(SeekableInputStream pSeekable) throws IOException {
        for (int i = 0; i < 10; i++) {
            pSeekable.mark();
            System.out.println(new String(FileUtil.read(pSeekable)));
            pSeekable.reset();
        }

        System.out.println();
        System.out.println("mark/reset ok!");
    }

    protected static void timeRead(SeekableInputStream pSeekable) throws IOException {
        for (int i = 0; i < 5000; i++) {
            pSeekable.mark();
            FileUtil.read(pSeekable);
            pSeekable.reset();
        }

        long start = System.currentTimeMillis();
        final int times = 200000;
        for (int i = 0; i < times; i++) {
            pSeekable.mark();
            FileUtil.read(pSeekable);
            pSeekable.reset();
        }
        long time = System.currentTimeMillis() - start;

        System.out.println("Time; " + time + "ms (" + (time / (float) times) + "ms/inv)");
    }
     */

    @Test
    public void testReadResetReadDirectBufferBug() throws IOException {
        // Make sure we use the exact size of the buffer
        final int size = 1024;

        // Fill bytes
        byte[] bytes = new byte[size * 2];
        sRandom.nextBytes(bytes);

        // Create wrapper stream
        SeekableInputStream stream = makeInputStream(bytes);

        // Read to fill the buffer, then reset
        int val;

        val = stream.read();
        assertFalse("Unexepected EOF", val == -1);
        val = stream.read();
        assertFalse("Unexepected EOF", val == -1);
        val = stream.read();
        assertFalse("Unexepected EOF", val == -1);
        val = stream.read();
        assertFalse("Unexepected EOF", val == -1);

        stream.seek(0);

        // Read fully and compare
        byte[] result = new byte[size];

        readFully(stream, result);
        assertTrue(rangeEquals(bytes, 0, result, 0, size));

        readFully(stream, result);
        assertTrue(rangeEquals(bytes, size, result, 0, size));
    }

    @Test
    public void testReadAllByteValuesRegression() throws IOException {
        final int size = 128;

        // Fill bytes
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        // Create wrapper stream
        SeekableInputStream stream = makeInputStream(bytes);

        // Fill buffer
        byte[] buffer = new byte[size];
        while (stream.read(buffer) >= 0) {
        }

        stream.seek(0);
        for (int i = 0; i < bytes.length; i += 2) {
            assertEquals("Wrong stream position", i, stream.getStreamPosition());
            int count = stream.read(buffer, 0, 2);
            assertEquals(2, count);
            assertEquals(String.format("Wrong value read at pos %d", stream.getStreamPosition()), bytes[i], buffer[0]);
            assertEquals(String.format("Wrong value read at pos %d", stream.getStreamPosition()), bytes[i + 1], buffer[1]);
        }

        stream.seek(0);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals("Wrong stream position", i, stream.getStreamPosition());
            int actual = stream.read();
            assertEquals(String.format("Wrong value read at pos %d", stream.getStreamPosition()), bytes[i] & 0xff, actual);
            assertEquals(String.format("Wrong value read at pos %d", stream.getStreamPosition()), bytes[i], (byte) actual);
        }

    }

    @Test
    public void testCloseUnderlyingStream() throws IOException {
        final boolean[] closed = new boolean[1];

        ByteArrayInputStream input = new ByteArrayInputStream(makeRandomArray(256)) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        SeekableInputStream stream = makeInputStream(input);

        try {
            FileUtil.read(stream); // Read until EOF

            assertEquals("EOF not reached (test case broken)", -1, stream.read());
            assertFalse("Underlying stream closed before close", closed[0]);
        }
        finally {
            stream.close();
        }

        assertTrue("Underlying stream not closed", closed[0]);

    }

    private void readFully(InputStream pStream, byte[] pResult) throws IOException {
        int pos = 0;
        while (pos < pResult.length) {
            int read = pStream.read(pResult,  pos, pResult.length - pos);
            if (read == -1) {
                throw new EOFException();
            }
            pos += read;
        }
    }

    /**
     * Test two arrays for range equality. That is, they contain the same elements for some specified range.
     *
     * @param pFirst one array to test for equality
     * @param pFirstOffset the offset into the first array to start testing for equality
     * @param pSecond the other array to test for equality
     * @param pSecondOffset the offset into the second array to start testing for equality
     * @param pLength the length of the range to check for equality
     *
     * @return {@code true} if both arrays are non-{@code null}
     * and have at least {@code offset + pLength} elements
     * and all elements in the range from the first array is equal to the elements from the second array,
     * or if {@code pFirst == pSecond} (including both arrays being {@code null})
     * and {@code pFirstOffset == pSecondOffset}.
     * Otherwise {@code false}.
     */
    static boolean rangeEquals(byte[] pFirst, int pFirstOffset, byte[] pSecond, int pSecondOffset, int pLength) {
        if (pFirst == pSecond && pFirstOffset == pSecondOffset) {
            return true;
        }

        if (pFirst == null || pSecond == null) {
            return false;
        }

        if (pFirst.length < pFirstOffset + pLength || pSecond.length < pSecondOffset + pLength) {
            return false;
        }

        for (int i = 0; i < pLength; i++) {
            if (pFirst[pFirstOffset + i] != pSecond[pSecondOffset + i]) {
                return false;
            }
        }

        return true;
    }
}
