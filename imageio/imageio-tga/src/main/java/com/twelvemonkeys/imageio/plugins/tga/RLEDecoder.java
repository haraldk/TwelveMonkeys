package com.twelvemonkeys.imageio.plugins.tga;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.lang.Validate;

final class RLEDecoder implements Decoder {
    private final byte[] pixel;

    RLEDecoder(final int pixelDepth) {
        Validate.isTrue(pixelDepth % Byte.SIZE == 0, "Depth must be a multiple of bytes (8 bits)");
        pixel = new byte[pixelDepth / Byte.SIZE];
    }

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.remaining() >= 128 * pixel.length) {
            int val = stream.read();
            if (val < 0) {
                break; // EOF
            }

            int pixelCount = (val & 0x7f) + 1;

            if ((val & 0x80) == 0) {
                for (int i = 0; i < pixelCount * pixel.length; i++) {
                    int data = stream.read();
                    if (data < 0) {
                        break; // EOF
                    }

                    buffer.put((byte) data);
                }
            } else {
                for (int b = 0; b < pixel.length; b++) {
                    int data = stream.read();
                    if (data < 0) {
                        break; // EOF
                    }

                    pixel[b] = (byte) data;
                }

                for (int i = 0; i < pixelCount; i++) {
                    buffer.put(pixel);
                }
            }
        }

        return buffer.position();
    }
}
