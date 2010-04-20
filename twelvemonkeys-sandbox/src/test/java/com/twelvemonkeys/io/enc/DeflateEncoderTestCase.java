package com.twelvemonkeys.io.enc;

/**
 * DeflateEncoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/DeflateDecoderTestCase.java#1 $
 */
public class DeflateEncoderTestCase extends EncoderAbstractTestCase {
    protected Encoder createEncoder() {
        return new DeflateEncoder();
    }

    protected Decoder createCompatibleDecoder() {
        return new InflateDecoder();
    }
}
