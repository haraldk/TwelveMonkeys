package com.twelvemonkeys.io.enc;

/**
 * PackBitsDecoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/PackBitsDecoderTestCase.java#1 $
 */
public class PackBitsDecoderTestCase extends DecoderAbstractTestCase {
    public Decoder createDecoder() {
        return new PackBitsDecoder();
    }

    public Encoder createCompatibleEncoder() {
        return new PackBitsEncoder();
    }
}
