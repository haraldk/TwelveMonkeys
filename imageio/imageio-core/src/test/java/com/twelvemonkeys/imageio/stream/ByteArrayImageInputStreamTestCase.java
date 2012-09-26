package com.twelvemonkeys.imageio.stream;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

import static com.twelvemonkeys.imageio.stream.BufferedImageInputStreamTestCase.rangeEquals;

/**
 * ByteArrayImageInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ByteArrayImageInputStreamTestCase.java,v 1.0 Apr 21, 2009 10:58:48 AM haraldk Exp$
 */
public class ByteArrayImageInputStreamTestCase extends TestCase {
    protected final Random random = new Random();

    public void testCreate() {
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(new byte[0]);
        assertEquals("Data length should be same as stream length", 0, stream.length());
    }

    public void testCreateNull() {
        try {
            new ByteArrayImageInputStream(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("data"));
            assertTrue("Exception message does not contain null", message.contains("null"));
        }
    }

    public void testCreateNullOffLen() {
        try {
            new ByteArrayImageInputStream(null, 0, -1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("data"));
            assertTrue("Exception message does not contain null", message.contains("null"));
        }
    }

    public void testCreateNegativeOff() {
        try {
            new ByteArrayImageInputStream(new byte[0], -1, 1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("offset"));
            assertTrue("Exception message does not contain -1", message.contains("-1"));
        }
    }

    public void testCreateBadOff() {
        try {
            new ByteArrayImageInputStream(new byte[1], 2, 0);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("offset"));
            assertTrue("Exception message does not contain 2", message.contains("2"));
        }
    }

    public void testCreateNegativeLen() {
        try {
            new ByteArrayImageInputStream(new byte[0], 0, -1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("length"));
            assertTrue("Exception message does not contain -1", message.contains("-1"));
        }
    }

    public void testCreateBadLen() {
        try {
            new ByteArrayImageInputStream(new byte[1], 0, 2);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull("Null exception message", expected.getMessage());
            String message = expected.getMessage().toLowerCase();
            assertTrue("Exception message does not contain parameter name", message.contains("length"));
            assertTrue("Exception message does not contain ™", message.contains("2"));
        }
    }

    public void testRead() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals("Data length should be same as stream length", data.length, stream.length());

        for (byte b : data) {
            assertEquals("Wrong data read", b & 0xff, stream.read());
        }
    }

    public void testReadOffsetLen() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        int offset = random.nextInt(data.length / 10);
        int length = random.nextInt(data.length - offset);
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data, offset, length);

        assertEquals("Data length should be same as stream length", length, stream.length());

        for (int i = offset; i < offset + length; i++) {
            assertEquals("Wrong data read", data[i] & 0xff, stream.read());
        }
    }

    public void testReadArray() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals("Data length should be same as stream length", data.length, stream.length());

        byte[] result = new byte[1024];

        for (int i = 0; i < data.length / result.length; i++) {
            stream.readFully(result);
            assertTrue("Wrong data read: " + i, rangeEquals(data, i * result.length, result, 0, result.length));
        }
    }

    public void testReadArrayOffLen() throws IOException {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        int offset = random.nextInt(data.length - 10240);
        int length = 10240;
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data, offset, length);

        assertEquals("Data length should be same as stream length", length, stream.length());

        byte[] result = new byte[1024];

        for (int i = 0; i < length / result.length; i++) {
            stream.readFully(result);
            assertTrue("Wrong data read: " + i, rangeEquals(data, offset + i * result.length, result, 0, result.length));
        }
    }

    public void testReadSkip() throws IOException {
        byte[] data = new byte[1024 * 14];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals("Data length should be same as stream length", data.length, stream.length());

        byte[] result = new byte[7];

        for (int i = 0; i < data.length / result.length; i += 2) {
            stream.readFully(result);
            stream.skipBytes(result.length);
            assertTrue("Wrong data read: " + i, rangeEquals(data, i * result.length, result, 0, result.length));
        }
    }

    public void testReadSeek() throws IOException {
        byte[] data = new byte[1024 * 18];
        random.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals("Data length should be same as stream length", data.length, stream.length());

        byte[] result = new byte[9];

        for (int i = 0; i < data.length / result.length; i++) {
            // Read backwards
            long newPos = stream.length() - result.length - i * result.length;
            stream.seek(newPos);
            assertEquals("Wrong stream position", newPos, stream.getStreamPosition());
            stream.readFully(result);
            assertTrue("Wrong data read: " + i, rangeEquals(data, (int) newPos, result, 0, result.length));
        }
    }
}
