/*
 * Copyright (c) 2015, Harald Kuhr
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

import com.twelvemonkeys.io.enc.Encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import static com.twelvemonkeys.imageio.plugins.tiff.LZWDecoder.LZWString;

/**
 * LZWEncoder
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWEncoder.java,v 1.0 02.12.13 14:13 haraldk Exp$
 */
final class LZWEncoder implements Encoder {
    // TODO: Consider extracting LZWStringTable from LZWDecoder

    /** Clear: Re-initialize tables. */
    static final int CLEAR_CODE = 256;
    /** End of Information. */
    static final int EOI_CODE = 257;

    private static final int MIN_BITS = 9;
    private static final int MAX_BITS = 12;

    private static final int TABLE_SIZE = 1 << MAX_BITS;

    private final LZWString[] table = new LZWString[TABLE_SIZE];
    private final Map<LZWString, Integer> reverseTable = new TreeMap<>(); // This is foobar
//    private final Map<LZWString, Integer> reverseTable = new HashMap<>(TABLE_SIZE); // This is foobar
    private int tableLength;
    LZWString omega = LZWString.EMPTY;

    int bitsPerCode;
    private int oldCode = CLEAR_CODE;
    private int maxCode;
    int bitMask;

    // Buffer for partial codes
    private int bits = 0;
    private int bitPos = 0;

    // Keep track of how many bytes we will write, to make sure we write EOI at correct position
    private long remaining;

    protected LZWEncoder(final int length) {
        this.remaining = length;

        // First 258 entries of table is always fixed
        for (int i = 0; i < 256; i++) {
            table[i] = new LZWString((byte) i);
        }

        init();
    }

    private void init() {
        tableLength = 258;
        bitsPerCode = MIN_BITS;
        bitMask = bitmaskFor(bitsPerCode);
        maxCode = maxCode();
        reverseTable.clear();
    }

    public void encode(final OutputStream stream, final ByteBuffer buffer) throws IOException {
//        InitializeStringTable();
//        WriteCode(ClearCode);
//        Ω = the empty string;
//        for each character in the strip {
//            K = GetNextCharacter();
//            if Ω+K is in the string table {
//                Ω = Ω+K;/* string concatenation */
//            }
//            else{
//                WriteCode (CodeFromString(    Ω));
//                AddTableEntry(Ω+K);
//                Ω=K;
//            } }/*end of for loop*/
//        WriteCode (CodeFromString(Ω));
//        WriteCode (EndOfInformation);

        if (remaining < 0) {
            throw new IOException("Write past end of stream");
        }

        // TODO: Write 9 bit clear code ONLY first time!
        if (oldCode == CLEAR_CODE) {
            writeCode(stream, CLEAR_CODE);
        }

        int len = buffer.remaining();

        while (buffer.hasRemaining()) {
            byte k = buffer.get();

            LZWString string = omega.concatenate(k);

            int tableIndex = isInTable(string);
            if (tableIndex >= 0) {
                omega = string;
                oldCode = tableIndex;
            }
            else {
                writeCode(stream, oldCode);
                addStringToTable(string);
                oldCode = k & 0xff;
                omega = table[k & 0xff];

                // Handle table (almost) full
                if (tableLength >= TABLE_SIZE - 2) {
                    writeCode(stream, CLEAR_CODE);
                    init();
                }
            }
        }

        remaining -= len;

        // Write EOI when er are done (the API isn't very supportive of this)
        if (remaining <= 0) {
            writeCode(stream, oldCode);
            writeCode(stream, EOI_CODE);
            if (bitPos > 0) {
                writeCode(stream, 0);
            }
        }
    }

    private int isInTable(final LZWString string) {
        if (string.length == 1) {
            return string.value & 0xff;
        }

        Integer index = reverseTable.get(string);
        return index != null ? index : -1;
    }

    private int addStringToTable(final LZWString string) {
        final int index = tableLength++;
        table[index] = string;
        reverseTable.put(string, index);

        if (tableLength > maxCode) {
            bitsPerCode++;

            if (bitsPerCode > MAX_BITS) {
                throw new IllegalStateException(String.format("TIFF LZW with more than %d bits per code encountered (table overflow)", MAX_BITS));
            }

            bitMask = bitmaskFor(bitsPerCode);
            maxCode = maxCode();
        }

        return index;
    }

    private void writeCode(final OutputStream stream, final int code) throws IOException {
//        System.err.printf("LZWEncoder.writeCode: 0x%04x\n", code);
        bits = (bits << bitsPerCode) | (code & bitMask);
        bitPos += bitsPerCode;

        while (bitPos >= 8) {
            int b = (bits >> (bitPos - 8)) & 0xff;
//            System.err.printf("write: 0x%02x\n", b);
            stream.write(b);
            bitPos -= 8;
        }

        bits &= bitmaskFor(bitPos);
    }

    private static int bitmaskFor(final int bits) {
        return (1 << bits) - 1;
    }

    protected int maxCode() {
        return bitMask;
    }
}
