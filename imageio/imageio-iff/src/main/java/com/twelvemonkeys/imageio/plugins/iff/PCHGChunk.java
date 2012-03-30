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
/*
 * Parts of this code is based on ilbmtoppm.c
 *
 * Copyright (C) 1989 by Jef Poskanzer.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation.  This software is provided "as is" without express or
 * implied warranty.
 *
 * Multipalette-support by Ingo Wilken (Ingo.Wilken@informatik.uni-oldenburg.de)
 */
package com.twelvemonkeys.imageio.plugins.iff;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.IOException;

/**
 * PCHGChunk
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PCHGChunk.java,v 1.0 27.03.12 13:02 haraldk Exp$
 */
final class PCHGChunk extends AbstractMultiPaletteChunk {
    // NOTE: Values from ilbm2ppm.
    final static int PCHG_COMP_NONE = 0;
    final static int PCHG_COMP_HUFFMAN = 1;

    /** Use SmallLineChanges */
    final static int PCHGF_12BIT = 1; // NOTE: The beta spec refers to this as PHCGF_4BIT
    /** Use BigLineChanges */
    final static int PCHGF_32BIT = 2;
    /** meaningful only if PCHG_32BIT is on: use the Alpha channel info */
    final static int PCHGF_USE_ALPHA = 4;

    private int startLine;
    private int changedLines;
    private int lineCount;
    private int totalChanges;
    private int minReg;

    public PCHGChunk(int pChunkLength) {
        super(IFF.CHUNK_PCHG, pChunkLength);
    }

    @Override
    void readChunk(final DataInput pInput) throws IOException {
        int compression = pInput.readUnsignedShort();
        int flags = pInput.readUnsignedShort();
        startLine = pInput.readShort();
        lineCount = pInput.readUnsignedShort();
        changedLines = pInput.readUnsignedShort();
        minReg = pInput.readUnsignedShort();
        int maxReg = pInput.readUnsignedShort();
        int maxChanges = pInput.readUnsignedShort(); // We don't really care, as we're not limited by the Amiga display hardware
        totalChanges = pInput.readInt();

//        System.err.println("compression: " + compression);
//        System.err.println("flags: " + Integer.toBinaryString(flags));
//        System.err.println("startLine: " + startLine);
//        System.err.println("lineCount: " + lineCount);
//        System.err.println("changedLines: " + changedLines);
//        System.err.println("minReg: " + minReg);
//        System.err.println("maxReg: " + maxReg);
//        System.err.println("maxChanges: " + maxChanges);
//        System.err.println("totalChanges: " + totalChanges);

        switch (compression) {
            case PCHG_COMP_NONE:
                byte[] data = new byte[chunkLength - 20];
                pInput.readFully(data);

                changes = new MutableIndexColorModel.PaletteChange[startLine + lineCount][];

                if (startLine < 0) {
                    int numChanges = maxReg - minReg + 1;

                    initialChanges = new MutableIndexColorModel.PaletteChange[numChanges];
                    for (int i = 0; i < initialChanges.length; i++) {
                        initialChanges[i] = new MutableIndexColorModel.PaletteChange();
                    }

                    for (int i = 0; i < numChanges; i++) {
                        initialChanges[i].index = MutableIndexColorModel.MP_REG_IGNORE;
                    }
                }

                // TODO: Postpone conversion to actually needed
                if ((flags & PCHGF_12BIT) != 0) {
                    convertSmallChanges(data);
                }
                else if ((flags & PCHGF_32BIT) != 0) {
                    System.err.println("BigLineChanges");

                    if ((flags & PCHGF_USE_ALPHA) != 0) {
                        System.err.println("Alpha should be used...");
                    }

                    // TODO: Implement 32 bit/alpha support
                    throw new UnsupportedOperationException("BigLineChanges not supported (yet)");
                }

                break;
            case PCHG_COMP_HUFFMAN:
                // TODO: Implement Huffman decoding
                throw new IIOException("Huffman PCHG compression not supported");
            default:
                throw new IIOException("Unknown PCHG compression: " + compression);
        }
    }

    private void convertSmallChanges(byte[] data) throws IIOException {
        int thismask = 0;
        int changeCount, reg;
        int changeCount16, changeCount32;
        int smallChange;
        int totalchanges = 0;
        int changedlines = changedLines;

        int maskBytesLeft = 4 * ((lineCount + 31) / 32);

        int maskIdx = 0;
        int dataIdx = maskBytesLeft;
        int dataBytesLeft = data.length - maskBytesLeft;

        int bits = 0;
        for (int row = startLine; changedlines != 0 && row < 0; row++) {
            if (bits == 0) {
                if (maskBytesLeft == 0) {
                    throw new IIOException("insufficient data in line mask");
                }

                thismask = data[maskIdx++];
                --maskBytesLeft;
                bits = 8;
            }

            if ((thismask & (1 << 7)) != 0) {
                if (dataBytesLeft < 2) {
                    throw new IIOException("insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                }

                changeCount16 = data[dataIdx++];
                changeCount32 = data[dataIdx++];
                dataBytesLeft -= 2;

                changeCount = changeCount16 + changeCount32;

                for (int i = 0; i < changeCount; i++) {
                    if (totalchanges >= this.totalChanges) {
                        throw new IIOException("insufficient data in SmallLineChanges structures (changeCount): " + totalchanges);
                    }
                    if (dataBytesLeft < 2) {
                        throw new IIOException("insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                    }

                    smallChange = toShort(data, dataIdx);
                    dataIdx += 2;
                    dataBytesLeft -= 2;
                    reg = ((smallChange & 0xf000) >> 12) + (i >= changeCount16 ? 16 : 0);
                    initialChanges[reg - minReg].index = reg;
                    initialChanges[reg - minReg].r = (byte) (((smallChange & 0x0f00) >> 8) * FACTOR_4BIT);
                    initialChanges[reg - minReg].g = (byte) (((smallChange & 0x00f0) >> 4) * FACTOR_4BIT);
                    initialChanges[reg - minReg].b = (byte) (((smallChange & 0x000f)     ) * FACTOR_4BIT);
                    ++totalchanges;
                }

                --changedlines;
            }

            thismask <<= 1;
            bits--;
        }

        for (int row = startLine; changedlines != 0 && row < this.changes.length; row++) {
            if (bits == 0) {
                if (maskBytesLeft == 0) {
                    throw new IIOException("insufficient data in line mask");
                }

                thismask = data[maskIdx++];
                --maskBytesLeft;
                bits = 8;
            }

            if ((thismask & (1 << 7)) != 0) {
                if (dataBytesLeft < 2) {
                    throw new IIOException("insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                }

                changeCount16 = data[dataIdx++];
                changeCount32 = data[dataIdx++];
                dataBytesLeft -= 2;

                changeCount = changeCount16 + changeCount32;

                changes[row] = new MutableIndexColorModel.PaletteChange[changeCount];

                for (int i = 0; i < changeCount; i++) {
                    changes[row][i] = new MutableIndexColorModel.PaletteChange();
                }

                for (int i = 0; i < changeCount; i++) {
                    if (totalchanges >= this.totalChanges) {
                        throw new IIOException("insufficient data in SmallLineChanges structures");
                    }
                    if (dataBytesLeft < 2) {
                        throw new IIOException("insufficient data in SmallLineChanges structures");
                    }

                    smallChange = toShort(data, dataIdx);
                    dataIdx += 2;
                    dataBytesLeft -= 2;
                    reg = ((smallChange & 0xf000) >> 12) + (i >= changeCount16 ? 16 : 0);
                    changes[row][i].index = reg;
                    changes[row][i].r = (byte) (((smallChange & 0x0f00) >> 8) * FACTOR_4BIT);
                    changes[row][i].g = (byte) (((smallChange & 0x00f0) >> 4) * FACTOR_4BIT);
                    changes[row][i].b = (byte) (((smallChange & 0x000f)     ) * FACTOR_4BIT);
                    ++totalchanges;
                }

                --changedlines;
            }

            thismask <<= 1;
            bits--;
        }

        if (totalchanges != this.totalChanges) {
            // TODO: Issue IIO warning
            System.err.printf("warning - got %d change structures, chunk header reports %d", totalchanges, this.totalChanges);
        }
    }

    // TODO: Util method
    private static short toShort(byte[] bytes, int idx) {
        return (short) ((bytes[idx] & 0xff) << 8 | (bytes[idx + 1] & 0xff));
    }
}
