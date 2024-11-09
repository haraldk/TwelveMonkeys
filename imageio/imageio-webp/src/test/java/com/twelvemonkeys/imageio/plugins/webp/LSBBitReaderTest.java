package com.twelvemonkeys.imageio.plugins.webp;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LSBBitReaderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LSBBitReaderTest.java,v 1.0 16/10/2022 haraldk Exp$
 */
public class LSBBitReaderTest {
    @Test
    public void testReadBit() throws IOException {
        final LSBBitReader bitReader = createBitReader(new byte[] {
                0b00010010, 0b00100001, 0b00001000, 0b00000100,
                /*TODO: Remove these, should not be needed... */ 0, 0, 0, 0
        });

        assertEquals(0, bitReader.readBit());
        assertEquals(1, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

        assertEquals(1, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

        assertEquals(1, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

        assertEquals(0, bitReader.readBit());
        assertEquals(1, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(1, bitReader.readBit());

        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(1, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());
        assertEquals(0, bitReader.readBit());

//         assertThrows(EOFException.class, new ThrowingRunnable() {
//             @Override
//             public void run() throws Throwable {
//                 bitReader.readBits(1);
//             }
//         });
    }

    @Test
    public void testReadBits() throws IOException {
        final LSBBitReader bitReader = createBitReader(new byte[] {
                0b00100101, 0b01000010, 0b00010000, 0b00001000,
                0b00001000, 0b00010000, 0b01000000, 0b00000000,
                0b00000010, 0b00100000, 0b00000000, 0b00000100,
                0b00000000, 0b00000001, (byte) 0b10000000,
        });

        assertEquals(1, bitReader.readBits(1));
        assertEquals(2, bitReader.readBits(2));
        assertEquals(4, bitReader.readBits(3));
        assertEquals(8, bitReader.readBits(4));
        assertEquals(16, bitReader.readBits(5));
        assertEquals(32, bitReader.readBits(6));
        assertEquals(64, bitReader.readBits(7));
        assertEquals(128, bitReader.readBits(8));
        assertEquals(256, bitReader.readBits(9));
        assertEquals(512, bitReader.readBits(10));
        assertEquals(1024, bitReader.readBits(11));
        assertEquals(2048, bitReader.readBits(12));
        assertEquals(4096, bitReader.readBits(13));
        assertEquals(8192, bitReader.readBits(14));
        assertEquals(16384, bitReader.readBits(15));

//         assertThrows(EOFException.class, new ThrowingRunnable() {
//             @Override
//             public void run() throws Throwable {
//                 bitReader.readBits(1);
//             }
//         });
    }

    @Test
    public void testPeekBits() throws IOException {
        final LSBBitReader bitReader = createBitReader(new byte[] {
                0b00100101, 0b01000010, 0b00010000, 0b00001000,
                0b00001000, 0b00010000, 0b01000000, 0b00000000,
                0b00000010, 0b00100000, 0b00000000, 0b00000100,
                0b00000000, 0b00000001, (byte) 0b10000000
        });

        assertEquals(1, bitReader.peekBits(1));
        assertEquals(1, bitReader.peekBits(1));
        assertEquals(1, bitReader.readBits(1));

        assertEquals(2, bitReader.peekBits(2));
        assertEquals(2, bitReader.readBits(2));

        assertEquals(4, bitReader.readBits(3));

        assertEquals(8, bitReader.peekBits(4));
        assertEquals(8, bitReader.readBits(4));

        assertEquals(16, bitReader.peekBits(5));
        assertEquals(16, bitReader.peekBits(5));
        assertEquals(16, bitReader.readBits(5));

        assertEquals(32, bitReader.peekBits(6));
        assertEquals(32, bitReader.readBits(6));

        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.readBits(7));

        assertEquals(128, bitReader.peekBits(8));
        assertEquals(128, bitReader.readBits(8));

        assertEquals(256, bitReader.readBits(9));

        assertEquals(512, bitReader.peekBits(10));
        assertEquals(512, bitReader.readBits(10));

        assertEquals(1024, bitReader.peekBits(11));
        assertEquals(1024, bitReader.readBits(11));

        assertEquals(2048, bitReader.peekBits(12));
        assertEquals(2048, bitReader.peekBits(12));
        assertEquals(2048, bitReader.peekBits(12));
        assertEquals(2048, bitReader.readBits(12));

        assertEquals(4096, bitReader.peekBits(13));
        assertEquals(4096, bitReader.readBits(13));

        assertEquals(8192, bitReader.readBits(14));

        assertEquals(16384, bitReader.peekBits(15));
        assertEquals(16384, bitReader.peekBits(15));
        assertEquals(16384, bitReader.readBits(15));

//         assertThrows(EOFException.class, new ThrowingRunnable() {
//             @Override
//             public void run() throws Throwable {
//                 bitReader.readBits(1);
//             }
//         });
    }

    @Test
    public void testReadBetweenBits() throws IOException {
        ImageInputStream stream = createStream(new byte[] {
                0b00100101, 0b01000010, 0b00010000, 0b00001000,
                0b00001000, 0b00010000, 0b01000000, 0b00000000,
                0b00000010, 0b00100000, 0b00000000, 0b00000100,
                0b00000000, 0b00000001, (byte) 0b10000000
        });
        final LSBBitReader bitReader = new LSBBitReader(stream);

        assertEquals(1, bitReader.peekBits(1));
        assertEquals(1, bitReader.peekBits(1));
        assertEquals(1, bitReader.readBits(1));

        assertEquals(2, bitReader.peekBits(2));
        assertEquals(2, bitReader.readBits(2));

        assertEquals(4, bitReader.readBits(3));

        // We've read 6 bits, but still on the 1st byte
        assertEquals(0b00100101, stream.readByte());

        // Start reading from the second byte (10 == 2)
        assertEquals(2, bitReader.readBits(2));

        assertEquals(16, bitReader.peekBits(5));
        assertEquals(16, bitReader.peekBits(5));
        assertEquals(16, bitReader.readBits(5));

        // We've now read 7 bits, but still on the second byte
        assertEquals(1, stream.getStreamPosition());
        assertEquals(0b01000010, stream.readByte());
        assertEquals(2, stream.getStreamPosition());

        assertEquals(16, bitReader.peekBits(11));

        assertEquals(0b00010000, stream.readByte());
        assertEquals(3, stream.getStreamPosition());
        stream.seek(2);
        assertEquals(2, stream.getStreamPosition());

        // Start reading from the third byte (10000 == 16)
        assertEquals(16, bitReader.peekBits(5));
        assertEquals(16, bitReader.readBits(5));

        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.peekBits(7));
        assertEquals(64, bitReader.readBits(7));

        assertEquals(128, bitReader.peekBits(8));
        assertEquals(128, bitReader.readBits(8));

        assertEquals(256, bitReader.readBits(9));

        assertEquals(512, bitReader.peekBits(10));
        assertEquals(512, bitReader.readBits(10));

        assertEquals(1024, bitReader.peekBits(11));
        assertEquals(1024, bitReader.readBits(11));

        assertEquals(2048, bitReader.peekBits(12));
        assertEquals(2048, bitReader.peekBits(12));
        assertEquals(2048, bitReader.peekBits(12));
        assertEquals(2048, bitReader.readBits(12));

        assertEquals(4096, bitReader.peekBits(13));
        assertEquals(4096, bitReader.readBits(13));

        assertEquals(8192, bitReader.readBits(14));

        assertEquals(16384, bitReader.peekBits(15));
        assertEquals(16384, bitReader.peekBits(15));
        assertEquals(16384, bitReader.readBits(15));

//         assertThrows(EOFException.class, new ThrowingRunnable() {
//             @Override
//             public void run() throws Throwable {
//                 bitReader.readBits(1);
//             }
//         });
    }

    private static LSBBitReader createBitReader(final byte[] data) {
        ImageInputStream stream = createStream(data);
        return new LSBBitReader(stream);
    }

    private static ImageInputStream createStream(byte[] data) {
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(data);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return stream;
    }
}