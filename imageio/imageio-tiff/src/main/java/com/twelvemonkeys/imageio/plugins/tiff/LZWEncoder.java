/*
 * Copyright (c) 2015, Harald Kuhr
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

import com.twelvemonkeys.io.enc.Encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * LZWEncoder
 * <p/>
 * Inspired by LZWTreeEncoder by <a href="mailto:yuwen_66@yahoo.com">Wen Yu</a> and the
 * <a href="http://gingko.homeip.net/docs/file_formats/lzwgif.html#bob">algorithm described by Bob Montgomery</a>
 * which
 * "[...] uses a tree method to search if a new string is already in the table,
 * which is much simpler, faster, and easier to understand than hashing."
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LZWEncoder.java,v 1.0 02.12.13 14:13 haraldk Exp$
 */
final class LZWEncoder implements Encoder {
    /** Clear: Re-initialize tables. */
    static final int CLEAR_CODE = 256;
    /** End of Information. */
    static final int EOI_CODE = 257;

    private static final int MIN_BITS = 9;
    private static final int MAX_BITS = 12;

    private static final int TABLE_SIZE = 1 << MAX_BITS;

    // A child is made up of a parent (or prefix) code plus a suffix byte
    // and siblings are strings with a common parent(or prefix) and different
    // suffix bytes
    private final short[] CHILDREN = new short[TABLE_SIZE];
    private final short[] SIBLINGS = new short[TABLE_SIZE];
    private final short[] SUFFIXES = new short[TABLE_SIZE];

    // Initial setup
    private int parent = -1;
    private int bitsPerCode = MIN_BITS;
    private int nextValidCode = EOI_CODE + 1;
    private int maxCode = maxValue(bitsPerCode);

    // Buffer for partial codes
    private int bits = 0;
    private int bitPos = 0;

    // Keep track of how many bytes we will write, to make sure we write EOI at correct position
    private long remaining;

    LZWEncoder(final long length) {
        remaining = length;
    }

    @Override
    public void encode(final OutputStream stream, final ByteBuffer buffer) throws IOException {
        encodeBytes(stream, buffer);

        if (remaining <= 0) {
            // Write EOI when er are done (the API isn't very supportive of this at the moment)
            writeCode(stream, parent);
            writeCode(stream, EOI_CODE);

            // Flush partial codes by writing 0 pad
            if (bitPos > 0) {
                writeCode(stream, 0);
            }
        }
    }

    void encodeBytes(final OutputStream stream, final ByteBuffer buffer) throws IOException {
        int length = buffer.remaining();

        if (length == 0) {
            return;
        }

        if (parent == -1) {
            // Init stream
            writeCode(stream, CLEAR_CODE);
            parent = buffer.get() & 0xff;
        }

        while (buffer.hasRemaining()) {
            int value = buffer.get() & 0xff;
            int child = CHILDREN[parent];

            if (child > 0) {
                if (SUFFIXES[child] == value) {
                    parent = child;
                }
                else {
                    int sibling = child;

                    while (true) {
                        if (SIBLINGS[sibling] > 0) {
                            sibling = SIBLINGS[sibling];

                            if (SUFFIXES[sibling] == value) {
                                parent = sibling;
                                break;
                            }
                        }
                        else {
                            SIBLINGS[sibling] = (short) nextValidCode;
                            SUFFIXES[nextValidCode] = (short) value;
                            writeCode(stream, parent);
                            parent = value;
                            nextValidCode++;

                            increaseCodeSizeOrResetIfNeeded(stream);

                            break;
                        }
                    }
                }
            }
            else {
                CHILDREN[parent] = (short) nextValidCode;
                SUFFIXES[nextValidCode] = (short) value;
                writeCode(stream, parent);
                parent = value;
                nextValidCode++;

                increaseCodeSizeOrResetIfNeeded(stream);
            }
        }

        remaining -= length;
    }

    private void increaseCodeSizeOrResetIfNeeded(final OutputStream stream) throws IOException {
        if (nextValidCode > maxCode) {
            if (bitsPerCode == MAX_BITS) {
                // Reset stream by writing Clear code
                writeCode(stream, CLEAR_CODE);

                // Reset tables
                resetTables();
            }
            else {
                // Increase code size
                bitsPerCode++;
                maxCode = maxValue(bitsPerCode);
            }
        }
    }

    private void resetTables() {
        Arrays.fill(CHILDREN, (short) 0);
        Arrays.fill(SIBLINGS, (short) 0);

        bitsPerCode = MIN_BITS;
        maxCode = maxValue(bitsPerCode);
        nextValidCode = EOI_CODE + 1;
    }

    private void writeCode(final OutputStream stream, final int code) throws IOException {
//        System.err.printf("LZWEncoder.writeCode: 0x%04x\n", code);
        bits = (bits << bitsPerCode) | (code & maxCode);
        bitPos += bitsPerCode;

        while (bitPos >= 8) {
            int b = (bits >> (bitPos - 8)) & 0xff;
//            System.err.printf("write: 0x%02x\n", b);
            stream.write(b);
            bitPos -= 8;
        }

        bits &= bitmaskFor(bitPos);
    }

    private static int maxValue(final int codeLen) {
        return (1 << codeLen) - 1;
    }

    private static int bitmaskFor(final int bits) {
        return maxValue(bits);
    }
}