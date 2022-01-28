package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.io.enc.DecodeException;
import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DecoderAbstractTest;
import com.twelvemonkeys.io.enc.Encoder;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * RGB8RLEDecoderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RGB8RLEDecoderTest.java,v 1.0 28/01/2022 haraldk Exp$
 */
public class RGB8RLEDecoderTest extends DecoderAbstractTest {

    public static final int BUFFER_SIZE = 1024;

    @Override
    public Decoder createDecoder() {
        return new RGB8RLEDecoder();
    }

    @Override
    public Encoder createCompatibleEncoder() {
        return null;
    }

    @Test
    public final void testDecodeEmpty() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[0]);

        int count = decoder.decode(bytes, ByteBuffer.allocate(BUFFER_SIZE));
        assertEquals("Should not be able to read any bytes", 0, count);
    }

    @Test(expected = EOFException.class)
    public final void testDecodePartial() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[] {0});

        decoder.decode(bytes, ByteBuffer.allocate(BUFFER_SIZE));
        fail("Should not be able to read any bytes");
    }

    @Test(expected = EOFException.class)
    public final void testDecodePartialToo() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[] {0, 0, 0, 1, 0, 0});

        decoder.decode(bytes, ByteBuffer.allocate(BUFFER_SIZE));
        fail("Should not be able to read any bytes");
    }

    @Test(expected = DecodeException.class)
    public final void testDecodeZeroRun() throws IOException {
        // The spec says that 0-runs should be used to signal that the run is > 127,
        // and contained in the next byte, however, this is not used in practise and not supported.
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[] {0, 0, 0, 0});

        decoder.decode(bytes, ByteBuffer.allocate(BUFFER_SIZE));
        fail("Should not be able to read any bytes");
    }

    @Test
    public final void testDecodeSingleOpaque() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[] {0, 0, 0, 1});
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int count = decoder.decode(bytes, buffer);

        assertEquals(4, count);
        assertEquals(0x000000FF, buffer.getInt(0));
    }

    @Test
    public final void testDecodeSingleTransparent() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[] {0, 0, 0, (byte) 0x81});
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int count = decoder.decode(bytes, buffer);

        assertEquals(4, count);
        assertEquals(0x00000000, buffer.getInt(0));
    }

    @Test
    public final void testDecodeMaxRun() throws IOException {
        Decoder decoder = createDecoder();
        ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F});
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int count = decoder.decode(bytes, buffer);

        assertEquals(127 * 4, count);
        for (int i = 0; i < 127; i++) {
            assertEquals(0xFFFFFFFF, buffer.getInt(i));
        }
    }
}