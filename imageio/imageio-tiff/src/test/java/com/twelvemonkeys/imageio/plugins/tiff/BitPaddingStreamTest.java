package com.twelvemonkeys.imageio.plugins.tiff;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

/**
 * BitPaddingStreamTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: BitPaddingStreamTest.java,v 1.0 16/11/2016 harald.kuhr Exp$
 */
public class BitPaddingStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullStream() {
        new BitPaddingStream(null, 1, 12, 4, ByteOrder.BIG_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBadBits() {
        new BitPaddingStream(new ByteArrayInputStream(new byte[6]), 1, 7, 4, ByteOrder.BIG_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBadBitsLarge() {
        new BitPaddingStream(new ByteArrayInputStream(new byte[6]), 1, 37, 4, ByteOrder.BIG_ENDIAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullByteOrder() {
        new BitPaddingStream(new ByteArrayInputStream(new byte[6]), 1, 12, 4, null);
    }

    @Test
    public void testRead() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.BIG_ENDIAN);
        assertEquals(0x0f, stream.read());
        assertEquals(0xff, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x06, stream.read());
        assertEquals(0x66, stream.read());

        assertEquals(0x07, stream.read());
        assertEquals(0x89, stream.read());

        assertEquals(-1, stream.read());
    }

    // TODO: Test read 10, 14, etc bits....

    @Test
    public void testReadLittleEndian() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.LITTLE_ENDIAN);
        assertEquals(0xff, stream.read());
        assertEquals(0x0f, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x66, stream.read());
        assertEquals(0x06, stream.read());

        assertEquals(0x89, stream.read());
        assertEquals(0x07, stream.read());

        assertEquals(-1, stream.read());
    }

    @Test
    public void testRead3Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x60};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 3, 12, 1, ByteOrder.BIG_ENDIAN);
        assertEquals(0x0f, stream.read());
        assertEquals(0xff, stream.read());

        assertEquals(0x00, stream.read());
        assertEquals(0x00, stream.read());

        assertEquals(0x06, stream.read());
        assertEquals(0x66, stream.read());

        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadArray() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66, 0x07, (byte) 0x89}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArrayLittleEndian() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.LITTLE_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {(byte) 0xff, 0x0f, 0x00, 0x00, 0x66, 0x06, (byte) 0x89, 0x07}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArray2Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 2, 12, 2, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66, 0x07, (byte) 0x89}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArray3Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x6f};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 3, 12, 1, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[6];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testReadArray4Components() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 4, 12, 1, ByteOrder.BIG_ENDIAN);

        byte[] result = new byte[8];
        new DataInputStream(stream).readFully(result);

        assertArrayEquals(new byte[] {0x0f, (byte) 0xff, 0x00, 0x00, 0x06, 0x66, 0x07, (byte) 0x89}, result);

        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[4]));
    }

    @Test
    public void testSkip() throws IOException {
        byte[] bytes = {(byte) 0xff, (byte) 0xf0, 0x00, 0x66, 0x67, (byte) 0x89};

        BitPaddingStream stream = new BitPaddingStream(new ByteArrayInputStream(bytes), 1, 12, 4, ByteOrder.BIG_ENDIAN);

        assertEquals(4, stream.skip(4));   // Normal skip
        assertEquals(0x06, stream.read()); // Verify position after skip
        assertEquals(3, stream.skip(4));   // Partial skip
        assertEquals(-1, stream.read());   // Verify position (EOF)
    }
}