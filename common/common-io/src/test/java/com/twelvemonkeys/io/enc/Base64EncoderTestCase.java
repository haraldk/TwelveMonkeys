package com.twelvemonkeys.io.enc;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;

/**
 * Base64EncoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/Base64EncoderTestCase.java#1 $
 */
public class Base64EncoderTestCase extends EncoderAbstractTestCase {

    protected Encoder createEncoder() {
        return new Base64Encoder();
    }

    protected Decoder createCompatibleDecoder() {
        return new Base64Decoder();
    }

    @Test
    public void testEmptyEncode() throws IOException {
        String data = "";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream out = new EncoderStream(bytes, createEncoder(), true);
        out.write(data.getBytes());

        assertEquals("Strings does not match", "", new String(bytes.toByteArray()));
    }

    @Test
    public void testShortEncode() throws IOException {
        String data = "test";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream out = new EncoderStream(bytes, createEncoder(), true);
        out.write(data.getBytes());

        assertEquals("Strings does not match", "dGVzdA==", new String(bytes.toByteArray()));
    }

    @Test
    public void testLongEncode() throws IOException {
        String data = "Lorem ipsum dolor sit amet, consectetuer adipiscing " +
                "elit. Fusce est. Morbi luctus consectetuer justo. Vivamus " +
                "dapibus laoreet purus. Nunc viverra dictum nisl. Integer " +
                "ullamcorper, nisi in dictum amet.";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream out = new EncoderStream(bytes, createEncoder(), true);
        out.write(data.getBytes());

        assertEquals("Strings does not match",
                     "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVlciBhZGlwaXNjaW5nIGVsaXQuIEZ1" +
                    "c2NlIGVzdC4gTW9yYmkgbHVjdHVzIGNvbnNlY3RldHVlciBqdXN0by4gVml2YW11cyBkYXBpYnVzIGxh" +
                    "b3JlZXQgcHVydXMuIE51bmMgdml2ZXJyYSBkaWN0dW0gbmlzbC4gSW50ZWdlciB1bGxhbWNvcnBlciwg" +
                    "bmlzaSBpbiBkaWN0dW0gYW1ldC4=",
                     new String(bytes.toByteArray()));
    }
}
