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


public class HuffmanTable {

	private final int l[][][] = new int[4][2][16];
	private final int th[] = new int[4]; // 1: this table is presented
	private final int v[][][][] = new int[4][2][16][200]; // tables
	private final int[][] tc = new int[4][2]; // 1: this table is presented

	public static final int MSB = 0x80000000;



	public HuffmanTable() {
		tc[0][0] = 0;
		tc[1][0] = 0;
		tc[2][0] = 0;
		tc[3][0] = 0;
		tc[0][1] = 0;
		tc[1][1] = 0;
		tc[2][1] = 0;
		tc[3][1] = 0;
		th[0] = 0;
		th[1] = 0;
		th[2] = 0;
		th[3] = 0;
	}



	protected int read(final DataStream data, final int[][][] HuffTab) throws IOException {
		int count = 0;
		final int length = data.get16();
		count += 2;

		while (count < length) {
			final int temp = data.get8();
			count++;
			final int t = temp & 0x0F;
			if (t > 3) {
				throw new IOException("ERROR: Huffman table ID > 3");
			}

			final int c = temp >> 4;
			if (c > 2) {
				throw new IOException("ERROR: Huffman table [Table class > 2 ]");
			}

			th[t] = 1;
			tc[t][c] = 1;

			for (int i = 0; i < 16; i++) {
				l[t][c][i] = data.get8();
				count++;
			}

			for (int i = 0; i < 16; i++) {
				for (int j = 0; j < l[t][c][i]; j++) {
					if (count > length) {
						throw new IOException("ERROR: Huffman table format error [count>Lh]");
					}
					v[t][c][i][j] = data.get8();
					count++;
				}
			}
		}

		if (count != length) {
			throw new IOException("ERROR: Huffman table format error [count!=Lf]");
		}

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 2; j++) {
				if (tc[i][j] != 0) {
					buildHuffTable(HuffTab[i][j], l[i][j], v[i][j]);
				}
			}
		}

		return 1;
	}



	//	Build_HuffTab()
	//	Parameter:  t       table ID
	//	            c       table class ( 0 for DC, 1 for AC )
	//	            L[i]    # of codewords which length is i
	//	            V[i][j] Huffman Value (length=i)
	//	Effect:
	//	    build up HuffTab[t][c] using L and V.
	private void buildHuffTable(final int tab[], final int L[], final int V[][]) throws IOException {
		int currentTable, temp;
		int k;
		temp = 256;
		k = 0;

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

		currentTable = 1;
		k = 0;

		for (int i = 8; i < 16; i++) { // i+1 is Code length
			for (int j = 0; j < L[i]; j++) {
				for (int n = 0; n < (temp >> (i - 7)); n++) {
					tab[(currentTable * 256) + k] = V[i][j] | ((i + 1) << 8);
					k++;
				}
				if (k >= 256) {
					if (k > 256) {
						throw new IOException("ERROR: Huffman table error(1)!");
					}
					k = 0;
					currentTable++;
				}
			}
		}
	}
}
