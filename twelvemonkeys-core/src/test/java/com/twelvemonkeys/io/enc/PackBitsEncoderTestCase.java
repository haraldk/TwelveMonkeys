package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.Encoder;
import com.twelvemonkeys.io.enc.PackBitsDecoder;
import com.twelvemonkeys.io.enc.PackBitsEncoder;

/**
 * PackBitsEncoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/PackBitsEncoderTestCase.java#1 $
 */
public class PackBitsEncoderTestCase extends EncoderAbstractTestCase {
    protected Encoder createEncoder() {
        return new PackBitsEncoder();
    }

    protected Decoder createCompatibleDecoder() {
        return new PackBitsDecoder();
    }
}
