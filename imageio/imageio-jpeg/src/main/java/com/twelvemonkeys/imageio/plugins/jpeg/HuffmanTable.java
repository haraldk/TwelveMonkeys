/*
 * Copyright (c) 2016, Harald Kuhr
 * Copyright (C) 2015, Michael Martinez
 * Copyright (C) 2004, Helmut Dersch
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.IOException;

final class HuffmanTable extends Segment {

    private final int l[][][] = new int[4][2][16];
    private final int th[] = new int[4]; // 1: this table is present
    final int v[][][][] = new int[4][2][16][200]; // tables
    final int[][] tc = new int[4][2]; // 1: this table is present

    static final int MSB = 0x80000000;

    private HuffmanTable() {
        super(JPEG.DHT);
   }

    void buildHuffTables(final int[][][] HuffTab) throws IOException {
        for (int t = 0; t < 4; t++) {
            for (int c = 0; c < 2; c++) {
                if (tc[t][c] != 0) {
                    buildHuffTable(HuffTab[t][c], l[t][c], v[t][c]);
                }
            }
        }
    }

    //	Build_HuffTab()
    //	Parameter:  t       table ID
    //	            c       table class ( 0 for DC, 1 for AC )
    //	            L[i]    # of codewords which length is i
    //	            V[i][j] Huffman Value (length=i)
    //	Effect:
    //	    build up HuffTab[t][c] using L and V.
    private void buildHuffTable(final int tab[], final int L[], final int V[][]) throws IOException {
        int temp = 256;
        int k = 0;

        for (int i = 0; i < 8; i++) { // i+1 is Code length
            for (int j = 0; j < L[i]; j++) {
                for (int n = 0; n < (temp >> (i + 1)); n++) {
                    tab[k] = V[i][j] | ((i + 1) << 8);
                    k++;
                }
            }
        }

        for (int i = 1; k < 256; i++, k++) {
            tab[k] = i | MSB;
        }

        int currentTable = 1;
        k = 0;

        for (int i = 8; i < 16; i++) { // i+1 is Code length
            for (int j = 0; j < L[i]; j++) {
                for (int n = 0; n < (temp >> (i - 7)); n++) {
                    tab[(currentTable * 256) + k] = V[i][j] | ((i + 1) << 8);
                    k++;
                }
                if (k >= 256) {
                    if (k > 256) {
                        throw new IIOException("JPEG Huffman Table error");
                    }

                    k = 0;
                    currentTable++;
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DHT[");

        for (int t = 0; t < tc.length; t++) {
            for (int c = 0; c < tc[t].length; c++) {
                if (tc[t][c] != 0) {
                    if (builder.length() > 4) {
                        builder.append(", ");
                    }

                    builder.append("id: ");
                    builder.append(t);

                    builder.append(", class: ");
                    builder.append(c);
                }
            }
        }

        builder.append(']');

        return builder.toString();
    }

    public static Segment read(final DataInput data, final int length) throws IOException {
        int count = 2;

        HuffmanTable table = new HuffmanTable();

        while (count < length) {
            int temp = data.readUnsignedByte();
            count++;
            int t = temp & 0x0F;
            if (t > 3) {
                throw new IIOException("Unexpected JPEG Huffman Table Id (> 3):" + t);
            }

            int c = temp >> 4;
            if (c > 2) {
                throw new IIOException("Unexpected JPEG Huffman Table class (> 2): " + c);
            }

            table.th[t] = 1;
            table.tc[t][c] = 1;

            for (int i = 0; i < 16; i++) {
                table.l[t][c][i] = data.readUnsignedByte();
                count++;
            }

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < table.l[t][c][i]; j++) {
                    if (count > length) {
                        throw new IIOException("JPEG Huffman Table format error");
                    }
                    table.v[t][c][i][j] = data.readUnsignedByte();
                    count++;
                }
            }
        }

        if (count != length) {
            throw new IIOException("JPEG Huffman Table format error, bad segment length: " + length);
        }

        return table;
    }
}
