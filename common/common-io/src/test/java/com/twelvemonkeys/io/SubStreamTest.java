package com.twelvemonkeys.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * SubStreamTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SubStreamTest.java,v 1.0 07/11/2023 haraldk Exp$
 */
public class SubStreamTest {

    private final Random rng = new Random(2918475687L);

    @SuppressWarnings("resource")
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullStream() {
        new SubStream(null, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNegativeLength() {
        new SubStream(new ByteArrayInputStream(new byte[1]), -1);
    }

    @Test
    public void testReadAll() throws IOException {
        byte[] buf = new byte[128];
        rng.nextBytes(buf);

        try (InputStream stream = new SubStream(new ByteArrayInputStream(buf), buf.length)) {
            for (byte b : buf) {
                assertEquals(b, (byte) stream.read());
            }

            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testReadAllArray() throws IOException {
        byte[] buf = new byte[128];
        rng.nextBytes(buf);

        try (InputStream stream = new SubStream(new ByteArrayInputStream(buf), buf.length)) {
            byte[] temp = new byte[buf.length / 4];
            for (int i = 0; i < 4; i++) {
                assertEquals(temp.length, stream.read(temp)); // Depends on ByteArrayInputStream specifics...
                assertArrayEquals(Arrays.copyOfRange(buf, i * temp.length, (i + 1) * temp.length), temp);
            }

            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testSkipAll() throws IOException {
        byte[] buf = new byte[128];

        try (InputStream stream = new SubStream(new ByteArrayInputStream(buf), buf.length)) {
            assertEquals(128, stream.skip(buf.length)); // Depends on ByteArrayInputStream specifics...
            assertEquals(-1, stream.read());
        }
    }

    @SuppressWarnings("EmptyTryBlock")
    @Test
    public void testCloseConsumesAll() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[128]);

        try (InputStream ignore = new SubStream(stream, 128)) {
            // Nothing here...
        }

        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());
    }

    @SuppressWarnings("EmptyTryBlock")
    @Test
    public void testCloseConsumesAllLongStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[256]);

        try (InputStream ignore = new SubStream(stream, 128)) {
            // Nothing here...
        }

        assertEquals(128, stream.available());
        assertEquals(0, stream.read());
    }

    @SuppressWarnings("EmptyTryBlock")
    @Test(timeout = 500L)
    public void testCloseConsumesAllShortStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[13]);

        try (InputStream ignore = new SubStream(stream, 42)) {
            // Nothing here...
        }

        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());
    }
}