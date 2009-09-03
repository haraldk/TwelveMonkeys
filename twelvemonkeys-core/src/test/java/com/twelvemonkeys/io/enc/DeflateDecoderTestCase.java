package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DeflateEncoder;
import com.twelvemonkeys.io.enc.Encoder;
import com.twelvemonkeys.io.enc.InflateDecoder;

/**
 * DeflateDecoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/DeflateDecoderTestCase.java#1 $
 */
public class DeflateDecoderTestCase extends EncoderAbstractTestCase {
    protected Encoder createEncoder() {
        return new DeflateEncoder();
    }

    protected Decoder createCompatibleDecoder() {
        return new InflateDecoder();
    }
}
