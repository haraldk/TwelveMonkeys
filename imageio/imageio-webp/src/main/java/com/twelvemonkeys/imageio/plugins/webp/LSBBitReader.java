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
    private int bitOffset = 64;
    private long streamPosition = -1;

    /**
     * Pre buffers up to the next 8 Bytes in input.
     * Contains valid bits in bits 63 to {@code bitOffset} (inclusive).
     * Should always be refilled to have at least 56 valid bits (if possible)
     */
    private long buffer;

    public LSBBitReader(ImageInputStream imageInput) {
        this.imageInput = imageInput;
    }

    /**
     * Reads the specified number of bits from the stream in an LSB-first way and advances the bitOffset.
     * The underlying ImageInputStream will be advanced to the first not (completely) read byte.
     * Requesting more than 64 bits will advance the reader by the correct amount and return the lowest 64 bits of
     * the read number
     *
     * @param bits the number of bits to read
     * @return a signed long built from the requested bits (truncated to the low 64 bits)
     * @throws IOException if an I/O error occurs
     * @see LSBBitReader#peekBits
     */
    public long readBits(int bits) throws IOException {
        return readBits(bits, false);
    }

    /**
     * Reads the specified number of bits from the buffer in an LSB-first way.
     * Does not advance the bitOffset or the underlying input stream.
     * As only 56 bits are buffered (in the worst case) peeking more is not possible without advancing the reader and
     * as such disallowed.
     *
     * @param bits the number of bits to peek (max 56)
     * @return a signed long built from the requested bits
     * @throws IOException if an I/O error occurs
     * @see LSBBitReader#readBits
     */
    public long peekBits(int bits) throws IOException {
        if (bits > 56) {
            throw new IllegalArgumentException("Tried peeking over 56");
        }
        return readBits(bits, true);
    }

    //Driver
    private long readBits(int bits, boolean peek) throws IOException {
        if (bits <= 56) {

            /*
                Could eliminate if we never read from the underlying InputStream outside this class after the object is
                 created
            */
            long inputStreamPosition = imageInput.getStreamPosition();
            if (streamPosition != inputStreamPosition) {
                //Need to reset buffer as stream was read in the meantime
                resetBuffer();
            }

            long ret = (buffer >>> bitOffset) & ((1L << bits) - 1);

            if (!peek) {
                bitOffset += bits;
                refillBuffer();
            }

            return ret;
        }
        else {
            //FIXME Untested
            long lower = readBits(56);
            return (readBits(bits - 56) << (56)) | lower;
        }
    }

    private void refillBuffer() throws IOException {

        //Set to stream position consistent with buffered bytes
        imageInput.seek(streamPosition + 8);
        for (; bitOffset >= 8; bitOffset -= 8) {
            try {
                byte b = imageInput.readByte();
                buffer >>>= 8;
                streamPosition++;
                buffer |= ((long) b << 56);
            }
            catch (EOFException e) {
                imageInput.seek(streamPosition);
                return;
            }
        }
        /*
            Reset to guarantee stream position consistent with returned bytes
            Would not need to do this seeking around when the underlying ImageInputStream is never read from outside
            this class after the object is created.
        */
        imageInput.seek(streamPosition);
    }

    private void resetBuffer() throws IOException {

        long inputStreamPosition = imageInput.getStreamPosition();
        try {
            buffer = imageInput.readLong();
            bitOffset = 0;
            streamPosition = inputStreamPosition;
            imageInput.seek(inputStreamPosition);
        }
        catch (EOFException e) {
            //Retry byte by byte
            streamPosition = inputStreamPosition - 8;
            bitOffset = 64;
            refillBuffer();
        }

    }

    //Left for backwards compatibility / Compatibility with ImageInputStream interface
    public int readBit() throws IOException {
        return (int) readBits(1);
    }
}
