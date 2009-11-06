package com.twelvemonkeys.io.enc;


import com.twelvemonkeys.io.FileUtil;

import java.io.*;

/**
 * Base64DecoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/Base64DecoderTestCase.java#1 $
 */
public class Base64DecoderTestCase extends DecoderAbstractTestCase {

    public Decoder createDecoder() {
        return new Base64Decoder();
    }

    public Encoder createCompatibleEncoder() {
        return new Base64Encoder();
    }

    public void testEmptyDecode2() throws IOException {
        String data = "";

        InputStream in = new DecoderStream(new ByteArrayInputStream(data.getBytes()), createDecoder());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        FileUtil.copy(in, bytes);

        assertEquals("Strings does not match", "", new String(bytes.toByteArray()));
    }

    public void testShortDecode() throws IOException {
        String data = "dGVzdA==";

        InputStream in = new DecoderStream(new ByteArrayInputStream(data.getBytes()), createDecoder());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        FileUtil.copy(in, bytes);

        assertEquals("Strings does not match", "test", new String(bytes.toByteArray()));
    }

    public void testLongDecode() throws IOException {
        String data = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVlciBhZGlwaXNjaW5nIGVsaXQuIEZ1" +
                "c2NlIGVzdC4gTW9yYmkgbHVjdHVzIGNvbnNlY3RldHVlciBqdXN0by4gVml2YW11cyBkYXBpYnVzIGxh" +
                "b3JlZXQgcHVydXMuIE51bmMgdml2ZXJyYSBkaWN0dW0gbmlzbC4gSW50ZWdlciB1bGxhbWNvcnBlciwg" +
                "bmlzaSBpbiBkaWN0dW0gYW1ldC4=";

        InputStream in = new DecoderStream(new ByteArrayInputStream(data.getBytes()), createDecoder());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        FileUtil.copy(in, bytes);

        assertEquals("Strings does not match",
                     "Lorem ipsum dolor sit amet, consectetuer adipiscing " +
                     "elit. Fusce est. Morbi luctus consectetuer justo. Vivamus " +
                     "dapibus laoreet purus. Nunc viverra dictum nisl. Integer " +
                     "ullamcorper, nisi in dictum amet.",
                     new String(bytes.toByteArray()));
    }
}
