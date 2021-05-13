/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.io.enc.DecodeException;
import com.twelvemonkeys.io.enc.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Lempel–Ziv–Welch (LZW) decompression.
 * LZW is a universal loss-less data compression algorithm created by Abraham Lempel, Jacob Ziv, and Terry Welch.
 * Inspired by libTiff's LZW decompression.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWDecoder.java,v 1.0 08.05.12 21:11 haraldk Exp$
 * @see <a href="http://en.wikipedia.org/wiki/Lempel%E2%80%93Ziv%E2%80%93Welch">LZW (Wikipedia)</a>
 */
abstract class LZWDecoder implements Decoder {
    /** Clear: Re-initialize tables. */
    static final int CLEAR_CODE = 256;
    /** End of Information. */
    static final int EOI_CODE = 257;

    private static final int MIN_BITS = 9;
    private static final int MAX_BITS = 12;

    private static final int TABLE_SIZE = 1 << MAX_BITS;

    private final LZWString[] table;
    private int tableLength;
    int bitsPerCode;
    private int oldCode = CLEAR_CODE;
    private int maxCode;
    int bitMask;
    private int maxString;
    boolean eofReached;
    int nextData;
    int nextBits;

    protected LZWDecoder(int tableSize) {
        table = new LZWString[tableSize];

        // First 258 entries of table is always fixed
        for (int i = 0; i < 256; i++) {
            table[i] = new LZWString((byte) i);
        }

        init();
    }

    private static int bitmaskFor(final int bits) {
        return (1 << bits) - 1;
    }

    private void init() {
        tableLength = 258;
        bitsPerCode = MIN_BITS;
        bitMask = bitmaskFor(bitsPerCode);
        maxCode = maxCode();
        maxString = 1;
    }

    public int decode(final InputStream stream, final ByteBuffer buffer) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("buffer == null"); // As per contract
        }

        // Adapted from the pseudo-code example found in the TIFF 6.0 Specification, 1992.
        // See Section 13: "LZW Compression"/"LZW Decoding", page 61+
        int code;

        while ((code = getNextCode(stream)) != EOI_CODE) {
            if (code == CLEAR_CODE) {
                init();
                code = getNextCode(stream);

                if (code == EOI_CODE) {
                    break;
                }

                if (table[code] == null) {
                    throw new DecodeException(String.format("Corrupted TIFF LZW: code %d (table size: %d)", code, tableLength));
                }

                table[code].writeTo(buffer);
            }
            else {
                if (table[oldCode] == null) {
                    throw new DecodeException(String.format("Corrupted TIFF LZW: code %d (table size: %d)", oldCode, tableLength));
                }

                if (isInTable(code)) {
                    table[code].writeTo(buffer);
                    addStringToTable(table[oldCode].concatenate(table[code].firstChar));
                }
                else {
                    LZWString outString = table[oldCode].concatenate(table[oldCode].firstChar);

                    outString.writeTo(buffer);
                    addStringToTable(outString);
                }
            }

            oldCode = code;

            if (buffer.remaining() < maxString + 1) {
                // Buffer (almost) full, stop decoding for now
                break;
            }
        }

        return buffer.position();
    }

    private void addStringToTable(final LZWString string) throws IOException {
        if (tableLength > table.length) {
            throw new DecodeException(String.format("TIFF LZW with more than %d bits per code encountered (table overflow)", MAX_BITS));
        }

        table[tableLength++] = string;

        if (tableLength > maxCode) {
            bitsPerCode++;

            if (bitsPerCode > MAX_BITS) {
                // Continue reading MAX_BITS (12 bit) length codes
                bitsPerCode = MAX_BITS;
            }

            bitMask = bitmaskFor(bitsPerCode);
            maxCode = maxCode();
        }

        if (string.length > maxString) {
            maxString = string.length;
        }
    }

    protected abstract int maxCode();

    private boolean isInTable(int code) {
        return code < tableLength;
    }

    protected abstract int getNextCode(final InputStream stream) throws IOException;

    static boolean isOldBitReversedStream(final InputStream stream) throws IOException {
        stream.mark(2);

        try {
            int one = stream.read();
            int two = stream.read();

            return one == 0 && (two & 0x1) == 1; // => (reversed) 1 00000000 == 256 (CLEAR_CODE)
        }
        finally {
            stream.reset();
        }
    }

    public static Decoder create(boolean oldBitReversedStream) {
        return oldBitReversedStream ? new LZWCompatibilityDecoder() : new LZWSpecDecoder();
    }

    static final class LZWSpecDecoder extends LZWDecoder {

        protected LZWSpecDecoder() {
            super(TABLE_SIZE);
        }

        @Override
        protected int maxCode() {
            return bitMask - 1;
        }

        protected final int getNextCode(final InputStream stream) throws IOException {
            if (eofReached) {
                return EOI_CODE;
            }

            int code;
            int read = stream.read();
            if (read < 0) {
                eofReached = true;
                return EOI_CODE;
            }

            nextData = (nextData << 8) | read;
            nextBits += 8;

            if (nextBits < bitsPerCode) {
                read = stream.read();
                if (read < 0) {
                    eofReached = true;
                    return EOI_CODE;
                }

                nextData = (nextData << 8) | read;
                nextBits += 8;
            }

            code = ((nextData >> (nextBits - bitsPerCode)) & bitMask);
            nextBits -= bitsPerCode;

            return code;
        }
    }

    private static final class LZWCompatibilityDecoder extends LZWDecoder {
        // NOTE: This is a spec violation. However, libTiff reads such files.
        // TIFF 6.0 Specification, Section 13: "LZW Compression"/"The Algorithm", page 61, says:
        // "LZW compression codes are stored into bytes in high-to-low-order fashion, i.e., FillOrder
        // is assumed to be 1. The compressed codes are written as bytes (not words) so that the
        // compressed data will be identical whether it is an ‘II’ or ‘MM’ file."

        protected LZWCompatibilityDecoder() {
            super(TABLE_SIZE + 1024); // libTiff adds 1024 "for compatibility", this value seems to work fine...
        }

        @Override
        protected int maxCode() {
            return bitMask;
        }

        protected final int getNextCode(final InputStream stream) throws IOException {
            if (eofReached) {
                return EOI_CODE;
            }

            int code;
            int read = stream.read();
            if (read < 0) {
                eofReached = true;
                return EOI_CODE;
            }

            nextData |= read << nextBits;
            nextBits += 8;

            if (nextBits < bitsPerCode) {
                read = stream.read();
                if (read < 0) {
                    eofReached = true;
                    return EOI_CODE;
                }

                nextData |= read << nextBits;
                nextBits += 8;
            }

            code = (nextData & bitMask);
            nextData >>= bitsPerCode;
            nextBits -= bitsPerCode;

            return code;
        }
    }

    static final class LZWString implements Comparable<LZWString> {
        static final LZWString EMPTY = new LZWString((byte) 0, (byte) 0, 0, null);

        final LZWString previous;

        final int length;
        final byte value;
        final byte firstChar; // Copied forward for fast access

        public LZWString(final byte code) {
            this(code, code, 1, null);
        }

        private LZWString(final byte value, final byte firstChar, final int length, final LZWString previous) {
            this.value = value;
            this.firstChar = firstChar;
            this.length = length;
            this.previous = previous;
        }

        public final LZWString concatenate(final byte value) {
            if (this == EMPTY) {
                return new LZWString(value);
            }

            return new LZWString(value, this.firstChar, length + 1, this);
        }

        public final void writeTo(final ByteBuffer buffer) {
            if (length == 0) {
                return;
            }

            if (length == 1) {
                buffer.put(value);
            }
            else {
                LZWString e = this;
                final int offset = buffer.position();

                for (int i = length - 1; i >= 0; i--) {
                    buffer.put(offset + i, e.value);
                    e = e.previous;
                }

                buffer.position(offset + length);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("ZLWString[");
            int offset = builder.length();
            LZWString e = this;
            for (int i = length - 1; i >= 0; i--) {
                builder.insert(offset, String.format("%2x", e.value));
                e = e.previous;
            }
            builder.append("]");
            return builder.toString();
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            LZWString string = (LZWString) other;

            return firstChar == string.firstChar &&
                    length == string.length &&
                    value == string.value &&
//                    !(previous != null ? !previous.equals(string.previous) : string.previous != null);
                    previous == string.previous;
        }

        @Override
        public int hashCode() {
            int result = previous != null ? previous.hashCode() : 0;
            result = 31 * result + length;
            result = 31 * result + (int) value;
            result = 31 * result + (int) firstChar;
            return result;
        }

        @Override
        public int compareTo(final LZWString other) {
            if (other == this) {
                return 0;
            }

            if (length != other.length) {
                return other.length - length;
            }

            if (firstChar != other.firstChar) {
                return other.firstChar - firstChar;
            }

            LZWString t = this;
            LZWString o = other;

            for (int i = length - 1; i > 0; i--) {
                if (t.value != o.value) {
                    return o.value - t.value;
                }

                t = t.previous;
                o = o.previous;
            }

            return 0;
        }
    }
}

