package com.twelvemonkeys.imageio.plugins.webp;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * LSBBitReader
 */
public final class LSBBitReader {
    // TODO: Consider creating an ImageInputStream wrapper with the WebP implementation of readBit(s)?

    private final ImageInputStream imageInput;
    int bitOffset = 0;

    public LSBBitReader(ImageInputStream imageInput) {
        this.imageInput = imageInput;
    }

    // TODO: Optimize this... Read many bits at once!
    public long readBits(int bits) throws IOException {
        long result = 0;
        for (int i = 0; i < bits; i++) {
            result |= readBit() << i;
        }

        return result;
    }

    // TODO: Optimize this...
    // TODO: Consider not reading value over and over....
    public int readBit() throws IOException {
        int bit = 7 - bitOffset;

        imageInput.setBitOffset(bit);

        // Compute final bit offset before we call read() and seek()
        int newBitOffset = (bitOffset + 1) & 0x7;

        int val = imageInput.read();
        if (val == -1) {
            throw new EOFException();
        }

        if (newBitOffset != 0) {
            // Move byte position back if in the middle of a byte
            // NOTE: RESETS bit offset!
            imageInput.seek(imageInput.getStreamPosition() - 1);
        }

        bitOffset = newBitOffset;

        // Shift the bit to be read to the rightmost position
        return (val >> (7 - bit)) & 0x1;
    }
}
