package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;

import java.io.OutputStream;
import java.io.IOException;

/**
 * InputStreamAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/OutputStreamAbstractTestCase.java#1 $
 */
public abstract class OutputStreamAbstractTestCase extends ObjectAbstractTestCase {
    protected abstract OutputStream makeObject();

    public void testWrite() throws IOException {
        OutputStream os = makeObject();

        for (int i = 0; i < 256; i++) {
            os.write((byte) i);
        }
    }

    public void testWriteByteArray() throws IOException {
        OutputStream os = makeObject();

        os.write(new byte[256]);
    }

    public void testWriteByteArrayNull() {
        OutputStream os = makeObject();
        try {
            os.write(null);
            fail("Should not accept null-argument");
        }
        catch (IOException e) {
            fail("Should not throw IOException of null-arguemnt: " + e.getMessage());
        }
        catch (NullPointerException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw NullPointerException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void testWriteByteArrayOffsetLenght() throws IOException {
        byte[] input = new byte[256];

        OutputStream os = makeObject();

        // TODO: How to test that data is actually written!?
        for (int i = 0; i < 256; i++) {
            input[i] = (byte) i;
        }

        for (int i = 0; i < 256; i++) {
            os.write(input, i, 256 - i);
        }

        for (int i = 0; i < 4; i++) {
            os.write(input, i * 64, 64);
        }
    }

    public void testWriteByteArrayZeroLenght() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[1], 0, 0);
        }
        catch (Exception e) {
            fail("Should not throw Exception: " + e.getMessage());
        }
    }

    public void testWriteByteArrayOffsetLenghtNull() {
        OutputStream os = makeObject();
        try {
            os.write(null, 5, 10);
            fail("Should not accept null-argument");
        }
        catch (IOException e) {
            fail("Should not throw IOException of null-arguemnt: " + e.getMessage());
        }
        catch (NullPointerException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw NullPointerException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void testWriteByteArrayNegativeOffset() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], -3, 5);
            fail("Should not accept negative offset");
        }
        catch (IOException e) {
            fail("Should not throw IOException negative offset: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void testWriteByteArrayNegativeLength() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], 2, -5);
            fail("Should not accept negative length");
        }
        catch (IOException e) {
            fail("Should not throw IOException negative length: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void testWriteByteArrayOffsetOutOfBounds() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], 5, 1);
            fail("Should not accept offset out of bounds");
        }
        catch (IOException e) {
            fail("Should not throw IOException offset out of bounds: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void testWriteByteArrayLengthOutOfBounds() {
        OutputStream os = makeObject();
        try {
            os.write(new byte[5], 1, 5);
            fail("Should not accept length out of bounds");
        }
        catch (IOException e) {
            fail("Should not throw IOException length out of bounds: " + e.getMessage());
        }
        catch (IndexOutOfBoundsException e) {
            assertNotNull(e);
        }
        catch (RuntimeException e) {
            fail("Should only throw IndexOutOfBoundsException: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public void testFlush() {
        // TODO: Implement
    }

    public void testClose() {
        // TODO: Implement
    }

    public void testWriteAfterClose() throws IOException {
        OutputStream os = makeObject();

        os.close();

        boolean success = false;
        try {
            os.write(0);
            success  = true;
            // TODO: Not all streams throw exception! (ByteArrayOutputStream)
            //fail("Write after close");
        }
        catch (IOException e) {
            assertNotNull(e.getMessage());
        }

        try {
            os.write(new byte[16]);
            // TODO: Not all streams throw exception! (ByteArrayOutputStream)
            //fail("Write after close");
            if (!success) {
                fail("Inconsistent write(int)/write(byte[]) after close");
            }
        }
        catch (IOException e) {
            assertNotNull(e.getMessage());
            if (success) {
                fail("Inconsistent write(int)/write(byte[]) after close");
            }
        }
    }

    public void testFlushAfterClose() throws IOException {
        OutputStream os = makeObject();

        os.close();

        try {
            os.flush();
            // TODO: Not all streams throw exception! (ByteArrayOutputStream)
            //fail("Flush after close");
            try {
                os.write(0);
            }
            catch (IOException e) {
                fail("Inconsistent write/flush after close");
            }
        }
        catch (IOException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testCloseAfterClose() throws IOException {
        OutputStream os = makeObject();

        os.close();

        try {
            os.close();
        }
        catch (IOException e) {
            fail("Close after close, failed: " + e.getMessage());
        }
    }
}
