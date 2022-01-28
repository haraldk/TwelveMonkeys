package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.io.enc.DecodeException;
import com.twelvemonkeys.io.enc.Decoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Decoder implementation for Impulse FORM RGB8 RLE compression (type 4).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: RGB8Stream.java,v 1.0 28/01/2022 haraldk Exp$
 *
 * @see <a href="https://wiki.amigaos.net/wiki/RGBN_and_RGB8_IFF_Image_Data">RGBN and RGB8 IFF Image Data</a>
 */
final class RGB8RLEDecoder implements Decoder {
    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        while (buffer.remaining() >= 127 * 4) {
            int r = stream.read();
            int g = stream.read();
            int b = stream.read();
            int a = stream.read();

            if (a < 0) {
                // Normal EOF
                if (r == -1) {
                    break;
                }

                // Partial pixel read...
                throw new EOFException();
            }

            // Get "genlock" (transparency) bit + count
            boolean alpha = (a & 0x80) != 0;
            int count = a & 0x7f;
            a = alpha ? 0 : (byte) 0xff; // convert to full transparent/opaque;

            if (count == 0) {
                throw new DecodeException("Multi-byte counts not supported");
            }

            for (int i = 0; i < count; i++) {
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
        }

        return buffer.position();
    }
}
