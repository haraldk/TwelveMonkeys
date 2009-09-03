package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;

import java.io.Reader;
import java.io.IOException;

/**
 * ReaderAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/ReaderAbstractTestCase.java#1 $
 */
public abstract class ReaderAbstractTestCase extends ObjectAbstractTestCase {

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

    public void testSkipToEndAndRead() throws IOException {
        Reader reader = makeReader();

        int toSkip = mInput.length();
        while (toSkip > 0) {
            toSkip -= reader.skip(toSkip);
        }

        assertEquals(reader.read(), -1);
    }

    // TODO: It's possible to support reset and not mark (resets to beginning of stream, for example)
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
