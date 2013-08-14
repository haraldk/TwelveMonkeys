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

import com.twelvemonkeys.lang.Validate;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * CCITT Modified Huffman RLE, Group 3 (T4) and Group 4 (T6) fax compression.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CCITTFaxDecoderStream.java,v 1.0 23.05.12 15:55 haraldk Exp$
 */
final class CCITTFaxDecoderStream extends FilterInputStream {
    // See TIFF 6.0 Specification, Section 10: "Modified Huffman Compression", page 43.

    private final int columns;
    private final byte[] decodedRow;

    private int decodedLength;
    private int decodedPos;

    private int bitBuffer;
    private int bitBufferLength;

    // Need to take fill order into account (?) (use flip table?)
    private final int fillOrder;
    private final int type;

    private final int[] changes;
    private int changesCount;

    private static final int EOL_CODE = 0x01; // 12 bit

    public CCITTFaxDecoderStream(final InputStream stream, final int columns, final int type, final int fillOrder) {
        super(Validate.notNull(stream, "stream"));

        this.columns = Validate.isTrue(columns > 0, columns, "width must be greater than 0");
        // We know this is only used for b/w (1 bit)
        this.decodedRow = new byte[(columns + 7) / 8];
        this.type = Validate.isTrue(type == TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, type, "Only CCITT Modified Huffman RLE compression (2) supported: %s"); // TODO: Implement group 3 and 4
        this.fillOrder = Validate.isTrue(fillOrder == 1, fillOrder, "Only fill order 1 supported: %s"); // TODO: Implement fillOrder == 2

        this.changes = new int[columns];
    }

    // IDEA: Would it be faster to keep all bit combos of each length (>=2) that is NOT a code, to find bit length, then look up value in table?
    // -- If white run, start at 4 bits to determine length, if black, start at 2 bits

    private void fetch() throws IOException {
        if (decodedPos >= decodedLength) {
            decodedLength = 0;
            try {
                decodeRow();
            }
            catch (EOFException e) {
                // TODO: Rewrite to avoid throw/catch for normal flow...
                if (decodedLength != 0) {
                    throw e;
                }

                // ..otherwise, just client code trying to read past the end of stream
                decodedLength = -1;
            }

            decodedPos = 0;
        }
    }

    private void decodeRow() throws IOException {
        resetBuffer();

        boolean literalRun = true;

        /*
        if (type == TIFFExtension.COMPRESSION_CCITT_T4) {
            int eol = readBits(12);
            System.err.println("eol: " + eol);
            while (eol != EOL_CODE) {
                eol = readBits(1);
                System.err.println("eol: " + eol);
//                throw new IOException("Missing EOL");
            }

            literalRun = readBits(1) == 1;
        }

        System.err.println("literalRun: " + literalRun);
        */
        int index = 0;

        if (literalRun) {
            changesCount = 0;
            boolean white = true;

            do {
                int completeRun = 0;

                int run;
                do {
                    if (white) {
                        run = decodeRun(WHITE_CODES, WHITE_RUN_LENGTHS, 4);
                    }
                    else {
                        run = decodeRun(BLACK_CODES, BLACK_RUN_LENGTHS, 2);
                    }

                    completeRun += run;
                }
                while (run >= 64); // Additional makeup codes are packed into both b/w codes, terminating codes are < 64 bytes

                changes[changesCount++] = index + completeRun;

//                System.err.printf("%s run: %d\n", white ? "white" : "black", run);

                // TODO: Optimize with lookup for 0-7 bits?
                // Fill bits to byte boundary...
                while (index % 8 != 0 && completeRun-- > 0) {
                    decodedRow[index++ / 8] |= (white ? 1 << 8 - (index % 8) : 0);
                }

                // ...then fill complete bytes to either 0xff or 0x00...
                if (index % 8 == 0) {
                    final byte value = (byte) (white ? 0xff : 0x00);

                    while (completeRun > 7) {
                        decodedRow[index / 8] = value;
                        completeRun -= 8;
                        index += 8;
                    }
                }

                // ...finally fill any remaining bits
                while (completeRun-- > 0) {
                    decodedRow[index++ / 8] |= (white ? 1 << 8 - (index % 8) : 0);
                }

                // Flip color for next run
                white = !white;
            }
            while (index < columns);
        }
        else {
            // non-literal run
        }

        if (type == TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE && index != columns) {
            throw new IOException("Sum of run-lengths does not equal scan line width: " + index + " > " + columns);
        }

        decodedLength = (index / 8) + 1;
    }

    private int decodeRun(short[][] codes, short[][] runLengths, int minCodeSize) throws IOException {
        // TODO: Optimize...
        // Looping and comparing is the most straight-forward, but probably not the most effective way...
        int code = readBits(minCodeSize);

        for (int bits = 0; bits < codes.length; bits++) {
            short[] bitCodes = codes[bits];

            for (int i = 0; i < bitCodes.length; i++) {
                if (bitCodes[i] == code) {
//                    System.err.println("code: " + code);

                    // Code found, return matching run length
                    return runLengths[bits][i];
                }
            }

            // No code found, read one more bit and try again
            code = fillOrder == 1 ? (code << 1) | readBits(1) : readBits(1) << (bits + minCodeSize) | code;
        }

        throw new IOException("Unknown code in Huffman RLE stream");
    }

    private void resetBuffer() {
        for (int i = 0; i < decodedRow.length; i++) {
            decodedRow[i] = 0;
        }

        bitBuffer = 0;
        bitBufferLength = 0;
    }

    private int readBits(int bitCount) throws IOException {
        while (bitBufferLength < bitCount) {
            int read = in.read();
            if (read == -1) {
                throw new EOFException("Unexpected end of Huffman RLE stream");
            }

            int bits = read & 0xff;
            bitBuffer = (bitBuffer << 8) | bits;
            bitBufferLength += 8;
        }

        // TODO: Take fill order into account
        bitBufferLength -= bitCount;
        int result = bitBuffer >> bitBufferLength;
        bitBuffer &= (1 << bitBufferLength) - 1;

        return result;
    }

    @Override
    public int read() throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        return decodedRow[decodedPos++] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        int read = Math.min(decodedLength - decodedPos, len);
        System.arraycopy(decodedRow, decodedPos, b, off, read);
        decodedPos += read;

        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if (decodedLength < 0) {
            return -1;
        }

        if (decodedPos >= decodedLength) {
            fetch();

            if (decodedLength < 0) {
                return -1;
            }
        }

        int skipped = (int) Math.min(decodedLength - decodedPos, n);
        decodedPos += skipped;

        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    static final short[][] BLACK_CODES = {
            { // 2 bits
                    0x2, 0x3,
            },
            { // 3 bits
                    0x2, 0x3,
            },
            { // 4 bits
                    0x2, 0x3,
            },
            { // 5 bits
                    0x3,
            },
            { // 6 bits
                    0x4, 0x5,
            },
            { // 7 bits
                    0x4, 0x5, 0x7,
            },
            { // 8 bits
                    0x4, 0x7,
            },
            { // 9 bits
                    0x18,
            },
            { // 10 bits
                    0x17, 0x18, 0x37, 0x8, 0xf,
            },
            { // 11 bits
                    0x17, 0x18, 0x28, 0x37, 0x67, 0x68, 0x6c, 0x8, 0xc, 0xd,
            },
            { // 12 bits
                    0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x1c, 0x1d, 0x1e, 0x1f, 0x24, 0x27, 0x28, 0x2b, 0x2c, 0x33,
                    0x34, 0x35, 0x37, 0x38, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b, 0x64, 0x65,
                    0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xd2, 0xd3,
                    0xd4, 0xd5, 0xd6, 0xd7, 0xda, 0xdb,
            },
            { // 13 bits
                    0x4a, 0x4b, 0x4c, 0x4d, 0x52, 0x53, 0x54, 0x55, 0x5a, 0x5b, 0x64, 0x65, 0x6c, 0x6d, 0x72, 0x73,
                    0x74, 0x75, 0x76, 0x77,
            }
    };
    static final short[][] BLACK_RUN_LENGTHS = {
            { // 2 bits
                    3, 2,
            },
            { // 3 bits
                    1, 4,
            },
            { // 4 bits
                    6, 5,
            },
            { // 5 bits
                    7,
            },
            { // 6 bits
                    9, 8,
            },
            { // 7 bits
                    10, 11, 12,
            },
            { // 8 bits
                    13, 14,
            },
            { // 9 bits
                    15,
            },
            { // 10 bits
                    16, 17, 0, 18, 64,
            },
            { // 11 bits
                    24, 25, 23, 22, 19, 20, 21, 1792, 1856, 1920,
            },
            { // 12 bits
                    1984, 2048, 2112, 2176, 2240, 2304, 2368, 2432, 2496, 2560, 52, 55, 56, 59, 60, 320,
                    384, 448, 53, 54, 50, 51, 44, 45, 46, 47, 57, 58, 61, 256, 48, 49,
                    62, 63, 30, 31, 32, 33, 40, 41, 128, 192, 26, 27, 28, 29, 34, 35,
                    36, 37, 38, 39, 42, 43,
            },
            { // 13 bits
                    640, 704, 768, 832, 1280, 1344, 1408, 1472, 1536, 1600, 1664, 1728, 512, 576, 896, 960,
                    1024, 1088, 1152, 1216,
            }
    };

    public static final short[][] WHITE_CODES = {
            { // 4 bits
                    0x7, 0x8, 0xb, 0xc, 0xe, 0xf,
            },
            { // 5 bits
                    0x12, 0x13, 0x14, 0x1b, 0x7, 0x8,
            },
            { // 6 bits
                    0x17, 0x18, 0x2a, 0x2b, 0x3, 0x34, 0x35, 0x7, 0x8,
            },
            { // 7 bits
                    0x13, 0x17, 0x18, 0x24, 0x27, 0x28, 0x2b, 0x3, 0x37, 0x4, 0x8, 0xc,
            },
            { // 8 bits
                    0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x1a, 0x1b, 0x2, 0x24, 0x25, 0x28, 0x29, 0x2a, 0x2b, 0x2c,
                    0x2d, 0x3, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x4, 0x4a, 0x4b, 0x5, 0x52, 0x53, 0x54, 0x55,
                    0x58, 0x59, 0x5a, 0x5b, 0x64, 0x65, 0x67, 0x68, 0xa, 0xb,
            },
            { // 9 bits
                    0x98, 0x99, 0x9a, 0x9b, 0xcc, 0xcd, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xdb,
            },
            { // 10 bits
            },
            { // 11 bits
                    0x8, 0xc, 0xd,
            },
            { // 12 bits
                    0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x1c, 0x1d, 0x1e, 0x1f,
            }
    };

    public static final short[][] WHITE_RUN_LENGTHS = {
            { // 4 bits
                    2, 3, 4, 5, 6, 7,
            },
            { // 5 bits
                    128, 8, 9, 64, 10, 11,
            },
            { // 6 bits
                    192, 1664, 16, 17, 13, 14, 15, 1, 12,
            },
            { // 7 bits
                    26, 21, 28, 27, 18, 24, 25, 22, 256, 23, 20, 19,
            },
            { // 8 bits
                    33, 34, 35, 36, 37, 38, 31, 32, 29, 53, 54, 39, 40, 41, 42, 43,
                    44, 30, 61, 62, 63, 0, 320, 384, 45, 59, 60, 46, 49, 50, 51,
                    52, 55, 56, 57, 58, 448, 512, 640, 576, 47, 48,
            },
            { // 9 bits
                    1472, 1536, 1600, 1728, 704, 768, 832, 896, 960, 1024, 1088, 1152, 1216, 1280, 1344, 1408,
            },
            { // 10 bits
            },
            { // 11 bits
                    1792, 1856, 1920,
            },
            { // 12 bits
                    1984, 2048, 2112, 2176, 2240, 2304, 2368, 2432, 2496, 2560,
            }
    };
}
