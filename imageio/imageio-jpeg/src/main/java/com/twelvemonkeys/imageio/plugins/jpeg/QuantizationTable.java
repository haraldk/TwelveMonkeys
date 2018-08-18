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

final class QuantizationTable extends Segment {

    private final int precision[] = new int[4]; // Quantization precision 8 or 16
    private final int[] tq = new int[4]; // 1: this table is presented

    final int quantTables[][] = new int[4][64]; // Tables

    QuantizationTable() {
        super(JPEG.DQT);

        tq[0] = 0;
        tq[1] = 0;
        tq[2] = 0;
        tq[3] = 0;
    }

    // TODO: Get rid of table param, make it a member?
    void enhanceTables(final int[] table) throws IOException {
        for (int t = 0; t < 4; t++) {
            if (tq[t] != 0) {
                enhanceQuantizationTable(quantTables[t], table);
            }
        }
    }

    private void enhanceQuantizationTable(final int qtab[], final int[] table) {
        for (int i = 0; i < 8; i++) {
            qtab[table[          i]] *=  90;
            qtab[table[(4 * 8) + i]] *=  90;
            qtab[table[(2 * 8) + i]] *= 118;
            qtab[table[(6 * 8) + i]] *=  49;
            qtab[table[(5 * 8) + i]] *=  71;
            qtab[table[(    8) + i]] *= 126;
            qtab[table[(7 * 8) + i]] *=  25;
            qtab[table[(3 * 8) + i]] *= 106;
        }

        for (int i = 0; i < 8; i++) {
            qtab[table[(    8 * i)]] *=  90;
            qtab[table[4 + (8 * i)]] *=  90;
            qtab[table[2 + (8 * i)]] *= 118;
            qtab[table[6 + (8 * i)]] *=  49;
            qtab[table[5 + (8 * i)]] *=  71;
            qtab[table[1 + (8 * i)]] *= 126;
            qtab[table[7 + (8 * i)]] *=  25;
            qtab[table[3 + (8 * i)]] *= 106;
        }

        for (int i = 0; i < 64; i++) {
            qtab[i] >>= 6;
        }
    }

    @Override
    public String toString() {
        // TODO: Tables...
        return "DQT[]";
    }

    public static QuantizationTable read(final DataInput data, final int length) throws IOException {
        int count = 2;

        QuantizationTable table = new QuantizationTable();
        while (count < length) {
            final int temp = data.readUnsignedByte();
            count++;
            final int t = temp & 0x0F;

            if (t > 3) {
                throw new IIOException("Unexpected JPEG Quantization Table Id (> 3): " + t);
            }

            table.precision[t] = temp >> 4;

            if (table.precision[t] == 0) {
                table.precision[t] = 8;
            }
            else if (table.precision[t] == 1) {
                table.precision[t] = 16;
            }
            else {
                throw new IIOException("Unexpected JPEG Quantization Table precision: " + table.precision[t]);
            }

            table.tq[t] = 1;

            if (table.precision[t] == 8) {
                for (int i = 0; i < 64; i++) {
                    if (count > length) {
                        throw new IIOException("JPEG Quantization Table format error");
                    }

                    table.quantTables[t][i] = data.readUnsignedByte();
                    count++;
                }
            }
            else {
                for (int i = 0; i < 64; i++) {
                    if (count > length) {
                        throw new IIOException("JPEG Quantization Table format error");
                    }

                    table.quantTables[t][i] = data.readUnsignedShort();
                    count += 2;
                }
            }
        }

        if (count != length) {
            throw new IIOException("JPEG Quantization Table error, bad segment length: " + length);
        }

        return table;
    }
}
