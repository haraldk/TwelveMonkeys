package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;
import java.io.InputStream;

final class Plain1BitDecoder extends InputStream {
    private final InputStream stream;
    private final int samplesPerRow; // Padded to byte boundary
    private int pos = 0;

    public Plain1BitDecoder(final InputStream in, final int samplesPerRow) {
        this.stream = in;
        this.samplesPerRow = samplesPerRow;
    }

    @Override public int read() throws IOException {
        // Each 0 or 1 represents one bit, whitespace is ignored. Padded to byte boundary for each row.
        // NOTE: White is 0, black is 1!
        int result = 0;

        for (int bitPos = 7; bitPos >= 0; bitPos--) {

            int read;
            while ((read = stream.read()) != -1 && Character.isWhitespace(read)) {
                // Skip whitespace
            }

            if (read == -1) {
                if (bitPos == 7) {
                    return -1;
                }

                break;
            }

            int val = read - '0';

            result |= val << bitPos;

            if (++pos >= samplesPerRow) {
                pos = 0;
                break;
            }
        }

        return result;
    }

    @Override public void close() throws IOException {
        stream.close();
    }
}
