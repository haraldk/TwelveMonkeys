package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DeflateEncoder;
import com.twelvemonkeys.io.enc.Encoder;
import com.twelvemonkeys.io.enc.InflateDecoder;

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
