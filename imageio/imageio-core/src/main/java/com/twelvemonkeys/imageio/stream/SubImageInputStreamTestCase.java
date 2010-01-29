package com.twelvemonkeys.imageio.stream;

import junit.framework.TestCase;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * SubImageInputStreamTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SubImageInputStreamTestCase.java,v 1.0 Nov 8, 2009 3:03:32 PM haraldk Exp$
 */
public class SubImageInputStreamTestCase extends TestCase {
    // TODO: Extract super test case for all stream tests
    private final Random mRandom = new Random(837468l);

    private ImageInputStream createStream(final int pSize) {
        byte[] bytes = new byte[pSize];

        mRandom.nextBytes(bytes);

        return new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            public long length() {
                return pSize;
            }
        };
    }

    public void testCreateNullStream() throws IOException {
        try {
            new SubImageInputStream(null, 1);
            fail("Expected IllegalArgumentException with null stream");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testCreateNegativeLength() throws IOException {
        try {
            new SubImageInputStream(createStream(0), -1);
            fail("Expected IllegalArgumentException with negative length");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testCreate() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(11), 7);

        assertEquals(0, stream.getStreamPosition());
        assertEquals(7, stream.length());
    }

    public void testWraphBeyondWrappedLength() throws IOException {
        SubImageInputStream stream = new SubImageInputStream(createStream(5), 6);
        assertEquals(5, stream.length());
    }

    public void testWrapUnknownLength() throws IOException {
        SubImageInputStream stream = new SubImageInputStream(new ImageInputStreamImpl() {
            @Override
            public int read() throws IOException {
                throw new UnsupportedOperationException("Method read not implemented");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new UnsupportedOperationException("Method read not implemented");
            }

            @Override
            public long length() {
                return -1;
            }
        }, 6);

        assertEquals(-1, stream.length());
    }

    public void testRead() throws IOException {
        ImageInputStream wrapped = createStream(42);

        wrapped.skipBytes(13);

        ImageInputStream stream = new SubImageInputStream(wrapped, 27);

        assertEquals(0, stream.getStreamPosition());
        assertEquals(27, stream.length());

        stream.read();
        assertEquals(1, stream.getStreamPosition());
        assertEquals(27, stream.length());

        stream.readFully(new byte[11]);
        assertEquals(12, stream.getStreamPosition());
        assertEquals(27, stream.length());

        assertEquals(25, wrapped.getStreamPosition());
    }

    public void testReadResetRead() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(32), 16);
        stream.mark();

        byte[] first = new byte[16];
        stream.readFully(first);

        stream.reset();

        byte[] second = new byte[16];
        stream.readFully(second);

        assertTrue(Arrays.equals(first, second));
    }

    public void testSeekNegative() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        try {
            stream.seek(-2);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(0, stream.getStreamPosition());
    }

    public void testSeekBeforeFlushedPos() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        stream.seek(3);
        stream.flushBefore(3);

        assertEquals(3, stream.getStreamPosition());

        try {
            stream.seek(0);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(3, stream.getStreamPosition());
    }

    public void testSeekAfterEOF() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        stream.seek(6);

        assertEquals(-1, stream.read());
    }

    public void testSeek() throws IOException {
        ImageInputStream stream = new SubImageInputStream(createStream(7), 5);
        stream.seek(5);
        assertEquals(5, stream.getStreamPosition());

        stream.seek(1);
        assertEquals(1, stream.getStreamPosition());
    }
}
