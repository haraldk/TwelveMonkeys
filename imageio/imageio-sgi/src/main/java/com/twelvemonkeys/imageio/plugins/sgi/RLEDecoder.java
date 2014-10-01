package com.twelvemonkeys.imageio.plugins.sgi;

import com.twelvemonkeys.io.enc.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class RLEDecoder implements Decoder {

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        // Adapted from c code sample in tgaffs.pdf
        while (buffer.remaining() >= 0x7f) {
            int val = stream.read();
            if (val < 0) {
                break; // EOF
            }

            int count = val & 0x7f;

            if (count == 0) {
                break; // No more data
            }

            if ((val & 0x80) != 0) {
                for (int i = 0; i < count; i++) {
                    int pixel = stream.read();
                    if (pixel < 0) {
                        break; // EOF
                    }

                    buffer.put((byte) pixel);
                }
            }
            else {
                int pixel = stream.read();
                if (pixel < 0) {
                    break; // EOF
                }

                for (int i = 0; i < count; i++) {
                    buffer.put((byte) pixel);
                }
            }
        }

        return buffer.position();
    }
}
