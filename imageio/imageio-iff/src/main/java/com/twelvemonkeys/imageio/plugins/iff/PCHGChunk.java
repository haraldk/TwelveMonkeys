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
        /*int maxChangesPerLine = */pInput.readUnsignedShort(); // We don't really care, as we're not limited by the Amiga display hardware
        totalChanges = pInput.readInt();

        byte[] data;

        switch (compression) {
            case PCHG_COMP_NONE:
                data = new byte[chunkLength - 20];
                pInput.readFully(data);

                break;
            case PCHG_COMP_HUFFMAN:
                // NOTE: Huffman decompression is completely untested, due to lack of source data (read: Probably broken).
                int compInfoSize = pInput.readInt();
                int originalDataSize = pInput.readInt();

                short[] compTree = new short[compInfoSize / 2];
                for (int i = 0; i < compTree.length; i++) {
                    compTree[i] = pInput.readShort();
                }

                byte[] compData = new byte[chunkLength - 20 - 8 - compInfoSize];
                pInput.readFully(compData);

                data = new byte[originalDataSize];

                // decompress the change structure data
                decompressHuffman(compData, data, compTree, data.length);

            default:
                throw new IIOException("Unknown PCHG compression: " + compression);
        }

        changes = new MutableIndexColorModel.PaletteChange[startLine + lineCount][];

        if (startLine < 0) {
            int numChanges = maxReg - minReg + 1;

            initialChanges = new MutableIndexColorModel.PaletteChange[numChanges];
        }

        // TODO: Postpone conversion to when the data is actually needed
        parseChanges(data, flags);
    }

    static void decompressHuffman(byte[] src, byte[] dest, short[] tree, int origSize) {
        int i = 0;
        int bits = 0;
        int thisbyte = 0;

        int treeIdx = tree.length - 1;
        int srcIdx = 0;
        int destIdx = 0;

        while (i < origSize) {
            if (bits == 0) {
                thisbyte = src[srcIdx++];
                bits = 8;
            }

            if ((thisbyte & (1 << 7)) != 0) {
                if (tree[treeIdx] >= 0) {
                    dest[destIdx++] = (byte) tree[treeIdx];
                    i++;
                    treeIdx = tree.length - 1;
                }
                else {
                    treeIdx += tree[treeIdx] / 2;
                }
            }
            else {
                treeIdx--;

                if (tree[treeIdx] > 0 && (tree[treeIdx] & 0x100) != 0) {
                    dest[destIdx++] = (byte) tree[treeIdx];
                    i++;
                    treeIdx = tree.length - 1;
                }
            }

            thisbyte <<= 1;
            bits--;
        }
    }

    private void parseChanges(final byte[] data, int flags) throws IIOException {
        boolean small;

        if ((flags & PCHGF_12BIT) != 0) {
            small = true;
        }
        else if ((flags & PCHGF_32BIT) != 0) {
            if ((flags & PCHGF_USE_ALPHA) != 0) {
                // TODO: Warning, or actually implement
                new IIOException("Alpha currently not supported.").printStackTrace();
            }

            small = false;
        }
        else {
            throw new IIOException("Missing PCHG 12/32 bit flag.");
        }

        int thismask = 0;
        int changeCount;
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
                    throw new IIOException("Insufficient data in line mask");
                }

                thismask = data[maskIdx++];
                --maskBytesLeft;
                bits = 8;
            }

            if ((thismask & (1 << 7)) != 0) {
                if (dataBytesLeft < 2) {
                    throw new IIOException("Insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                }

                int changeCount16 = 0;
                if (small) {
                    changeCount16 = data[dataIdx++] & 0xff;
                    changeCount = changeCount16 + (data[dataIdx++] & 0xff);
                }
                else {
                    changeCount = toShort(data, dataIdx);
                    dataIdx += 2;
                }
                dataBytesLeft -= 2;

                for (int i = 0; i < changeCount; i++) {
                    if (totalchanges >= this.totalChanges) {
                        throw new IIOException("Insufficient data in SmallLineChanges structures (changeCount): " + totalchanges);
                    }
                    if (dataBytesLeft < 2) {
                        throw new IIOException("Insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                    }

                    // TODO: Make PaletteChange immutable with constructor params, assign outside test?
                    if (small) {
                        int smallChange = toShort(data, dataIdx);
                        dataIdx += 2;
                        dataBytesLeft -= 2;
                        int reg = ((smallChange & 0xf000) >> 12) + (i >= changeCount16 ? 16 : 0);
                        initialChanges[reg - minReg] = new MutableIndexColorModel.PaletteChange();
                        initialChanges[reg - minReg].index = reg;
                        initialChanges[reg - minReg].r = (byte) (((smallChange & 0x0f00) >> 8) * FACTOR_4BIT);
                        initialChanges[reg - minReg].g = (byte) (((smallChange & 0x00f0) >> 4) * FACTOR_4BIT);
                        initialChanges[reg - minReg].b = (byte) (((smallChange & 0x000f)     ) * FACTOR_4BIT);
                    }
                    else {
                        int reg = toShort(data, dataIdx);
                        dataIdx += 2;
                        initialChanges[reg - minReg] = new MutableIndexColorModel.PaletteChange();
                        initialChanges[reg - minReg].index = reg;
                        dataIdx++; /* skip alpha */
                        initialChanges[reg - minReg].r = data[dataIdx++];
                        initialChanges[reg - minReg].b = data[dataIdx++];    /* yes, RBG */
                        initialChanges[reg - minReg].g = data[dataIdx++];
                        dataBytesLeft -= 6;

                    }

                    ++totalchanges;
                }

                --changedlines;
            }

            thismask <<= 1;
            bits--;
        }

        for (int row = startLine; changedlines != 0 && row < changes.length; row++) {
            if (bits == 0) {
                if (maskBytesLeft == 0) {
                    throw new IIOException("Insufficient data in line mask");
                }

                thismask = data[maskIdx++];
                --maskBytesLeft;
                bits = 8;
            }

            if ((thismask & (1 << 7)) != 0) {
                if (dataBytesLeft < 2) {
                    throw new IIOException("Insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                }

                int changeCount16 = 0;
                if (small) {
                    changeCount16 = data[dataIdx++] & 0xff;
                    changeCount = changeCount16 + (data[dataIdx++] & 0xff);
                }
                else {
                    changeCount = toShort(data, dataIdx);
                    dataIdx += 2;
                }
                dataBytesLeft -= 2;

                changes[row] = new MutableIndexColorModel.PaletteChange[changeCount];

                for (int i = 0; i < changeCount; i++) {
                    if (totalchanges >= this.totalChanges) {
                        throw new IIOException("Insufficient data in SmallLineChanges structures (changeCount): " + totalchanges);
                    }

                    if (dataBytesLeft < 2) {
                        throw new IIOException("Insufficient data in SmallLineChanges structures: " + dataBytesLeft);
                    }

                    if (small) {
                        int smallChange = toShort(data, dataIdx);
                        dataIdx += 2;
                        dataBytesLeft -= 2;
                        int reg = ((smallChange & 0xf000) >> 12) + (i >= changeCount16 ? 16 : 0);

                        MutableIndexColorModel.PaletteChange paletteChange = new MutableIndexColorModel.PaletteChange();
                        paletteChange.index = reg;
                        paletteChange.r = (byte) (((smallChange & 0x0f00) >> 8) * FACTOR_4BIT);
                        paletteChange.g = (byte) (((smallChange & 0x00f0) >> 4) * FACTOR_4BIT);
                        paletteChange.b = (byte) (((smallChange & 0x000f)     ) * FACTOR_4BIT);

                        changes[row][i] = paletteChange;
                    }
                    else {
                        int reg = toShort(data, dataIdx);
                        dataIdx += 2;

                        MutableIndexColorModel.PaletteChange paletteChange = new MutableIndexColorModel.PaletteChange();
                        paletteChange.index = reg;
                        dataIdx++; /* skip alpha */
                        paletteChange.r = data[dataIdx++];
                        paletteChange.b = data[dataIdx++];    /* yes, RBG */
                        paletteChange.g = data[dataIdx++];
                        changes[row][i] = paletteChange;

                        dataBytesLeft -= 6;
                    }

                    ++totalchanges;
                }

                --changedlines;
            }

            thismask <<= 1;
            bits--;
        }

        if (totalchanges != this.totalChanges) {
            // TODO: Issue IIO warning
            new IIOException(String.format("Got %d change structures, chunk header reports %d", totalchanges, this.totalChanges)).printStackTrace();
        }
    }

    // TODO: Util method
    private static short toShort(byte[] bytes, int idx) {
        return (short) ((bytes[idx] & 0xff) << 8 | (bytes[idx + 1] & 0xff));
    }
}
