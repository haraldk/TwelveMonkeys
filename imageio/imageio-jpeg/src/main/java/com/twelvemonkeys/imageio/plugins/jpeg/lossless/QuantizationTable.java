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

package com.twelvemonkeys.imageio.plugins.jpeg.lossless;

import java.io.IOException;


public class QuantizationTable {

	private final int precision[] = new int[4]; // Quantization precision 8 or 16
	private final int[] tq = new int[4]; // 1: this table is presented

	protected final int quantTables[][] = new int[4][64]; // Tables



	public QuantizationTable() {
		tq[0] = 0;
		tq[1] = 0;
		tq[2] = 0;
		tq[3] = 0;
	}



	protected int read(final DataStream data, final int[] table) throws IOException {
		int count = 0;
		final int length = data.get16();
		count += 2;

		while (count < length) {
			final int temp = data.get8();
			count++;
			final int t = temp & 0x0F;

			if (t > 3) {
				throw new IOException("ERROR: Quantization table ID > 3");
			}

			precision[t] = temp >> 4;

			if (precision[t] == 0) {
				precision[t] = 8;
			} else if (precision[t] == 1) {
				precision[t] = 16;
			} else {
				throw new IOException("ERROR: Quantization table precision error");
			}

			tq[t] = 1;

			if (precision[t] == 8) {
				for (int i = 0; i < 64; i++) {
					if (count > length) {
						throw new IOException("ERROR: Quantization table format error");
					}

					quantTables[t][i] = data.get8();
					count++;
				}

				enhanceQuantizationTable(quantTables[t], table);
			} else {
				for (int i = 0; i < 64; i++) {
					if (count > length) {
						throw new IOException("ERROR: Quantization table format error");
					}

					quantTables[t][i] = data.get16();
					count += 2;
				}

				enhanceQuantizationTable(quantTables[t], table);
			}
		}

		if (count != length) {
			throw new IOException("ERROR: Quantization table error [count!=Lq]");
		}

		return 1;
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
}
