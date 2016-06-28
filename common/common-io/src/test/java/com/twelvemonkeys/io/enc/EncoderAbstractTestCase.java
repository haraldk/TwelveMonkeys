package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.ObjectAbstractTestCase;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * AbstractEncoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/EncoderAbstractTestCase.java#1 $
 */
public abstract class EncoderAbstractTestCase extends ObjectAbstractTestCase {
    // Use seed to make sure we create same number all the time
    static final long SEED = 12345678;
    static final Random RANDOM = new Random(SEED);

    protected abstract Encoder createEncoder();
    protected abstract Decoder createCompatibleDecoder();

    protected Object makeObject() {
        return createEncoder();
    }

    @Test
    public final void testNullEncode() throws IOException {
        Encoder encoder = createEncoder();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
            encoder.encode(bytes, null);
            fail("null should throw NullPointerException");
        }
        catch (NullPointerException expected) {
        }
    }

    private byte[] createData(final int pLength) throws Exception {
        byte[] bytes = new byte[pLength];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private void runStreamTest(final int pLength) throws Exception {
        byte[] data = createData(pLength);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        OutputStream out = new EncoderStream(outBytes, createEncoder(), true);

        try {
            // Provoke failure for encoders that doesn't take array offset properly into account
            int off = (data.length + 1) / 2;
            out.write(data, 0, off);
            if (data.length > off) {
                out.write(data, off, data.length - off);
            }
        }
        finally {
            out.close();
        }

        byte[] encoded = outBytes.toByteArray();

//        System.err.println("encoded.length: " + encoded.length);
//        System.err.println("encoded: " + Arrays.toString(encoded));

        byte[] decoded = FileUtil.read(new DecoderStream(new ByteArrayInputStream(encoded), createCompatibleDecoder()));
        assertTrue(Arrays.equals(data, decoded));

        InputStream in = new DecoderStream(new ByteArrayInputStream(encoded), createCompatibleDecoder());
        outBytes = new ByteArrayOutputStream();

        try {
            FileUtil.copy(in, outBytes);
        }
        finally {
            outBytes.close();
            in.close();
        }

        decoded = outBytes.toByteArray();
        assertTrue(Arrays.equals(data, decoded));
    }

    @Test
    public final void testStreams() throws Exception {
        for (int i = 0; i < 100; i++) {
            try {
                runStreamTest(i);
            }
            catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
            catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
        }

        for (int i = 100; i < 2000; i += 250) {
            try {
                runStreamTest(i);
            }
            catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
            catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
        }

        for (int i = 2000; i < 80000; i += 1000) {
            try {
                runStreamTest(i);
            }
            catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
            catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage() + ": " + i);
            }
        }
    }

    // TODO: Test that the transition from byte[] to ByteBuffer didn't introduce bugs when writing to a wrapped array with offset.


}
