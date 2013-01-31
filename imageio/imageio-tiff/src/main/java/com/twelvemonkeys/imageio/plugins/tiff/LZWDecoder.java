/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.io.enc.DecodeException;
import com.twelvemonkeys.io.enc.Decoder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implements Lempel-Ziv & Welch (LZW) decompression.
 * Inspired by libTiff's LZW decompression.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWDecoder.java,v 1.0 08.05.12 21:11 haraldk Exp$
 */
abstract class LZWDecoder implements Decoder {
    /** Clear: Re-initialize tables. */
    static final int CLEAR_CODE = 256;
    /** End of Information. */
    static final int EOI_CODE = 257;

    private static final int MIN_BITS = 9;
    private static final int MAX_BITS = 12;

    private final boolean compatibilityMode;

    private final String[] table;
    private int tableLength;
    int bitsPerCode;
    private int oldCode = CLEAR_CODE;
    private int maxCode;
    int bitMask;
    private int maxString;
    boolean eofReached;
    int nextData;
    int nextBits;


    protected LZWDecoder(final boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;

        table = new String[compatibilityMode ? 4096 + 1024 : 4096]; // libTiff adds 1024 "for compatibility"...

        // First 258 entries of table is always fixed
        for (int i = 0; i < 256; i++) {
            table[i] = new String((byte) i);
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

    public int decode(final InputStream stream, final byte[] buffer) throws IOException {
        // Adapted from the pseudo-code example found in the TIFF 6.0 Specification, 1992.
        // See Section 13: "LZW Compression"/"LZW Decoding", page 61+
        int bufferPos = 0;
        int code;

        while ((code = getNextCode(stream)) != EOI_CODE) {
            if (code == CLEAR_CODE) {
                init();
                code = getNextCode(stream);

                if (code == EOI_CODE) {
                    break;
                }

                bufferPos += table[code].writeTo(buffer, bufferPos);
            }
            else {
                if (isInTable(code)) {
                    bufferPos += table[code].writeTo(buffer, bufferPos);
                    addStringToTable(table[oldCode].concatenate(table[code].firstChar));
                }
                else {
                    String outString = table[oldCode].concatenate(table[oldCode].firstChar);

                    bufferPos += outString.writeTo(buffer, bufferPos);
                    addStringToTable(outString);
                }
            }

            oldCode = code;

            if (bufferPos >= buffer.length - maxString - 1) {
                // Buffer full, stop decoding for now
                break;
            }
        }

        return bufferPos;
    }

    private void addStringToTable(final String string) throws IOException {
        table[tableLength++] = string;

        if (tableLength > maxCode) {
            bitsPerCode++;

            if (bitsPerCode > MAX_BITS) {
                if (compatibilityMode) {
                    bitsPerCode--;
                }
                else {
                    throw new DecodeException(java.lang.String.format("TIFF LZW with more than %d bits per code encountered (table overflow)", MAX_BITS));
                }
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

    public static LZWDecoder create(boolean oldBitReversedStream) {
        return oldBitReversedStream ? new LZWCompatibilityDecoder() : new LZWSpecDecoder();
    }

    private static final class LZWSpecDecoder extends LZWDecoder {

        protected LZWSpecDecoder() {
            super(false);
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
            super(true);
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

    private static final class String {
        final String previous;

        final int length;
        final byte value;
        final byte firstChar; // Copied forward for fast access

        public String(final byte code) {
            this(code, code, 1, null);
        }

        private String(final byte value, final byte firstChar, final int length, final String previous) {
            this.value = value;
            this.firstChar = firstChar;
            this.length = length;
            this.previous = previous;
        }

        public final String concatenate(final byte firstChar) {
            return new String(firstChar, this.firstChar, length + 1, this);
        }

        public final int writeTo(final byte[] buffer, final int offset) {
            if (length == 0) {
                return 0;
            }
            else if (length == 1) {
                buffer[offset] = value;

                return 1;
            }
            else {
                String e = this;

                for (int i = length - 1; i >= 0; i--) {
                    buffer[offset + i] = e.value;
                    e = e.previous;
                }

                return length;
            }
        }
    }
}

