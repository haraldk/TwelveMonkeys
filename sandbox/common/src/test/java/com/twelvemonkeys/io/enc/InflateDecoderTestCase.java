package com.twelvemonkeys.io.enc;

/**
 * InflateEncoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/InflateDecoderTestCase.java#1 $
 */
public class InflateDecoderTestCase extends DecoderAbstractTestCase {
    public Decoder createDecoder() {
        return new InflateDecoder();
    }

    public Encoder createCompatibleEncoder() {
        return new DeflateEncoder();
    }
}
