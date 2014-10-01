package com.twelvemonkeys.imageio.plugins.pcx;

import com.twelvemonkeys.io.enc.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class RLEDecoder implements Decoder {

    static final int COMPRESSED_RUN_MASK = 0xc0;

    // A rather strange and inefficient RLE encoding, but it probably made sense at the time...
    // Uses the upper two bits to flag if the next values are to be treated as a compressed run.
    // This means that any value above 0b11000000/0xc0/192 must be encoded as a compressed run,
    // even if this will make the output larger.
    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.remaining() >= 64) {
            int val = stream.read();
            if (val < 0) {
                break; // EOF
            }

            if ((val & COMPRESSED_RUN_MASK) == COMPRESSED_RUN_MASK) {
                int count = val & ~COMPRESSED_RUN_MASK;

                int pixel = stream.read();
                if (pixel < 0) {
                    break; // EOF
                }

                for (int i = 0; i < count; i++) {
                    buffer.put((byte) pixel);
                }
            }
            else {
                buffer.put((byte) val);
            }
        }

        return buffer.position();
    }
}
