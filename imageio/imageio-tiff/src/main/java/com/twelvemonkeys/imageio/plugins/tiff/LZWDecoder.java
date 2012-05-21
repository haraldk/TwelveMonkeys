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
import java.util.Arrays;

/**
 * LZWDecoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWDecoder.java,v 1.0 08.05.12 21:11 haraldk Exp$
 */
final class LZWDecoder implements Decoder {
    /** Clear: Re-initialize tables. */
    static final int CLEAR_CODE = 256;
    /** End of Information. */
    static final int EOI_CODE = 257;

    private static final int MIN_BITS = 9;
    private static final int MAX_BITS = 12;

    private final boolean reverseBitOrder;

    private int currentByte = -1;
    private int bitPos;

    // TODO: Consider speeding things up with a "string" type (instead of the inner byte[]),
    // that uses variable size/dynamic allocation, to avoid the excessive array copying?
//    private final byte[][] table = new byte[4096][0]; // libTiff adds another 1024 "for compatibility"...
    private final byte[][] table = new byte[4096 + 1024][0]; // libTiff adds another 1024 "for compatibility"...
    private int tableLength;
    private int bitsPerCode;
    private int oldCode = CLEAR_CODE;
    private int maxCode;
    private int maxString;
    private boolean eofReached;

    LZWDecoder(final boolean reverseBitOrder) {
        this.reverseBitOrder = reverseBitOrder;

        for (int i = 0; i < 256; i++) {
            table[i] = new byte[] {(byte) i};
        }

        init();
    }

    LZWDecoder() {
        this(false);
    }


    private int maxCodeFor(final int bits) {
        return reverseBitOrder ? (1 << bits) - 2 : (1 << bits) - 1;
    }

    private void init() {
        tableLength = 258;
        bitsPerCode = MIN_BITS;
        maxCode = maxCodeFor(bitsPerCode);
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

                bufferPos += writeString(table[code], buffer, bufferPos);
            }
            else {
                if (code > tableLength + 1 || oldCode >= tableLength) {
                    // TODO: FixMe for old, borked streams
                    System.err.println("code: " + code);
                    System.err.println("oldCode: " + oldCode);
                    System.err.println("tableLength: " + tableLength);
                    throw new DecodeException("Corrupted LZW table");
                }

                if (isInTable(code)) {
                    bufferPos += writeString(table[code], buffer, bufferPos);
                    addStringToTable(concatenate(table[oldCode], table[code][0]));
                }
                else {
                    byte[] outString = concatenate(table[oldCode], table[oldCode][0]);

                    bufferPos += writeString(outString, buffer, bufferPos);
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

    private byte[] concatenate(final byte[] string, final byte firstChar) {
        byte[] result = Arrays.copyOf(string, string.length + 1);
        result[string.length] = firstChar;

        return result;
    }

    private void addStringToTable(final byte[] string) throws IOException {
        table[tableLength++] = string;

        if (tableLength >= maxCode) {
            bitsPerCode++;

            if (bitsPerCode > MAX_BITS) {
                if (reverseBitOrder) {
                    bitsPerCode--;
                }
                else {
                    throw new DecodeException(String.format("TIFF LZW with more than %d bits per code encountered (table overflow)", MAX_BITS));
                }
            }

            maxCode = maxCodeFor(bitsPerCode);
        }

        if (string.length > maxString) {
            maxString = string.length;
        }
    }

    private int writeString(final byte[] string, final byte[] buffer, final int bufferPos) {
        if (string.length == 0) {
            return 0;
        }
        else if (string.length == 1) {
            buffer[bufferPos] = string[0];

            return 1;
        }
        else {
            System.arraycopy(string, 0, buffer, bufferPos, string.length);

            return string.length;
        }
    }

    private boolean isInTable(int code) {
        return code < tableLength;
    }

    private int getNextCode(final InputStream stream) throws IOException {
        if (eofReached) {
            return EOI_CODE;
        }

        int bitsToFill = bitsPerCode;
        int value = 0;

        while (bitsToFill > 0) {
            int nextBits;
            if (bitPos == 0) {
                nextBits = stream.read();

                if (nextBits == -1) {
                    // This is really a bad stream, but should be safe to handle this way, rather than throwing an EOFException.
                    // An EOFException will be thrown by the decoder stream later, if further reading is attempted.
                    eofReached = true;
                    return EOI_CODE;
                }
            }
            else {
                nextBits = currentByte;
            }

            int bitsFromHere = 8 - bitPos;
            if (bitsFromHere > bitsToFill) {
                bitsFromHere = bitsToFill;
            }

            if (reverseBitOrder) {
                // NOTE: This is a spec violation. However, libTiff reads such files.
                // TIFF 6.0 Specification, Section 13: "LZW Compression"/"The Algorithm", page 61, says:
                // "LZW compression codes are stored into bytes in high-to-low-order fashion, i.e., FillOrder
                // is assumed to be 1. The compressed codes are written as bytes (not words) so that the
                // compressed data will be identical whether it is an ‘II’ or ‘MM’ file."

                // Fill bytes from right-to-left
                for (int i = 0; i < bitsFromHere; i++) {
                    int destBitPos = bitsPerCode - bitsToFill + i;
                    int srcBitPos = bitPos + i;
                    value |= ((nextBits & (1 << srcBitPos)) >> srcBitPos) << destBitPos;
                }
            }
            else {
                value |= (nextBits >> 8 - bitPos - bitsFromHere & 0xff >> 8 - bitsFromHere) << bitsToFill - bitsFromHere;
            }

            bitsToFill -= bitsFromHere;
            bitPos += bitsFromHere;

            if (bitPos >= 8) {
                bitPos = 0;
            }

            currentByte = nextBits;
        }

        if (value == EOI_CODE) {
            eofReached = true;
        }

        return value;
    }

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
}

