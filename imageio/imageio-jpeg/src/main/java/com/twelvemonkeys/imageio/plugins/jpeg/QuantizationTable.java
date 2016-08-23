/*
 * Copyright (C) 2015 Michael Martinez
 * Changes: Added support for selection values 2-7, fixed minor bugs &
 * warnings, split into multiple class files, and general clean up.
 *
 * 08-25-2015: Helmut Dersch agreed to a license change from LGPL to MIT.
 */

/*
 * Copyright (C) Helmut Dersch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;

import javax.imageio.stream.ImageInputStream;
import java.io.DataInput;
import java.io.IOException;

final class QuantizationTable extends Segment {

    private final int precision[] = new int[4]; // Quantization precision 8 or 16
    private final int[] tq = new int[4]; // 1: this table is presented

    protected final int quantTables[][] = new int[4][64]; // Tables

    QuantizationTable() {
        super(JPEG.DQT);

        tq[0] = 0;
        tq[1] = 0;
        tq[2] = 0;
        tq[3] = 0;
    }

    // TODO: Get rid of table param, make it a member?
    protected void enhanceTables(final int[] table) throws IOException {
        for (int t = 0; t < 4; t++) {
            if (tq[t] != 0) {
                enhanceQuantizationTable(quantTables[t], table);
            }
        }
    }

    private void enhanceQuantizationTable(final int qtab[], final int[] table) {
        for (int i = 0; i < 8; i++) {
            qtab[table[(0 * 8) + i]] *= 90;
            qtab[table[(4 * 8) + i]] *= 90;
            qtab[table[(2 * 8) + i]] *= 118;
            qtab[table[(6 * 8) + i]] *= 49;
            qtab[table[(5 * 8) + i]] *= 71;
            qtab[table[(1 * 8) + i]] *= 126;
            qtab[table[(7 * 8) + i]] *= 25;
            qtab[table[(3 * 8) + i]] *= 106;
        }

        for (int i = 0; i < 8; i++) {
            qtab[table[0 + (8 * i)]] *= 90;
            qtab[table[4 + (8 * i)]] *= 90;
            qtab[table[2 + (8 * i)]] *= 118;
            qtab[table[6 + (8 * i)]] *= 49;
            qtab[table[5 + (8 * i)]] *= 71;
            qtab[table[1 + (8 * i)]] *= 126;
            qtab[table[7 + (8 * i)]] *= 25;
            qtab[table[3 + (8 * i)]] *= 106;
        }

        for (int i = 0; i < 64; i++) {
            qtab[i] >>= 6;
        }
    }

    public static QuantizationTable read(final DataInput data, final int length) throws IOException {
        int count = 0; // TODO: Could probably use data.getPosition for this

        QuantizationTable table = new QuantizationTable();
        while (count < length) {
            final int temp = data.readUnsignedByte();
            count++;
            final int t = temp & 0x0F;

            if (t > 3) {
                throw new IOException("ERROR: Quantization table ID > 3");
            }

            table.precision[t] = temp >> 4;

            if (table.precision[t] == 0) {
                table.precision[t] = 8;
            }
            else if (table.precision[t] == 1) {
                table.precision[t] = 16;
            }
            else {
                throw new IOException("ERROR: Quantization table precision error");
            }

            table.tq[t] = 1;

            if (table.precision[t] == 8) {
                for (int i = 0; i < 64; i++) {
                    if (count > length) {
                        throw new IOException("ERROR: Quantization table format error");
                    }

                    table.quantTables[t][i] = data.readUnsignedByte();
                    count++;
                }

//                table.enhanceQuantizationTable(table.quantTables[t], table);
            }
            else {
                for (int i = 0; i < 64; i++) {
                    if (count > length) {
                        throw new IOException("ERROR: Quantization table format error");
                    }

                    table.quantTables[t][i] = data.readUnsignedShort();
                    count += 2;
                }

//                table.enhanceQuantizationTable(table.quantTables[t], table);
            }
        }

        if (count != length) {
            throw new IOException("ERROR: Quantization table error [count!=Lq]");
        }

        return table;
    }
}
