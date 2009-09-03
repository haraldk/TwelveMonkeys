package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;
import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.Encoder;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.io.FileUtil;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

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

    public final void testNullEncode() throws IOException {
        Encoder base64 = createEncoder();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
            base64.encode(bytes, null, 0, 1);
            fail("null should throw NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    private byte[] createData(int pLength) throws Exception {
        byte[] bytes = new byte[pLength];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private void runStreamTest(int pLength) throws Exception {
        byte[] data = createData(pLength);
        ByteArrayOutputStream out_bytes = new ByteArrayOutputStream();
        OutputStream out = new EncoderStream(out_bytes, createEncoder(), true);
        out.write(data);
        out.close();
        byte[] encoded = out_bytes.toByteArray();
        byte[] decoded = FileUtil.read(new DecoderStream(new ByteArrayInputStream(encoded), createCompatibleDecoder()));
        assertTrue(Arrays.equals(data, decoded));

        InputStream in = new DecoderStream(new ByteArrayInputStream(encoded), createCompatibleDecoder());
        out_bytes = new ByteArrayOutputStream();
        /**
        byte[] buffer = new byte[3];
        for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
            out_bytes.write(buffer, 0, n);
        }
        //*/
        FileUtil.copy(in, out_bytes);

        out_bytes.close();
        in.close();
        decoded = out_bytes.toByteArray();
        assertTrue(Arrays.equals(data, decoded));
    }

    public final void testStreams() throws Exception {
        for (int i = 0; i < 100; ++i) {
            runStreamTest(i);
        }
        for (int i = 100; i < 2000; i += 250) {
            runStreamTest(i);
        }
        for (int i = 2000; i < 80000; i += 1000) {
            runStreamTest(i);
        }
    }
}
