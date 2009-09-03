package com.twelvemonkeys.imageio.stream;

import static com.twelvemonkeys.imageio.stream.BufferedImageInputStreamTestCase.rangeEquals;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

/**
 * ByteArrayImageInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ByteArrayImageInputStreamTestCase.java,v 1.0 Apr 21, 2009 10:58:48 AM haraldk Exp$
 */
public class ByteArrayImageInputStreamTestCase extends TestCase {
    protected final Random mRandom = new Random();

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

    public void testRead() throws IOException {
        byte[] data = new byte[1024 * 1024];
        mRandom.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals("Data length should be same as stream length", data.length, stream.length());

        for (byte b : data) {
            assertEquals("Wrong data read", b & 0xff, stream.read());
        }
    }

    public void testReadArray() throws IOException {
        byte[] data = new byte[1024 * 1024];
        mRandom.nextBytes(data);

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);

        assertEquals("Data length should be same as stream length", data.length, stream.length());

        byte[] result = new byte[1024];

        for (int i = 0; i < data.length / result.length; i++) {
            stream.readFully(result);
            assertTrue("Wrong data read: " + i, rangeEquals(data, i * result.length, result, 0, result.length));
        }
    }

    public void testReadSkip() throws IOException {
        byte[] data = new byte[1024 * 14];
        mRandom.nextBytes(data);

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
        mRandom.nextBytes(data);

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
