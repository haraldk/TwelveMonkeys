package com.twelvemonkeys.imageio.plugins.jpeg.lossless;

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

import java.io.IOException;
import java.nio.ByteBuffer;


public class JPEGLosslessDecoder implements DataStream {

	private final ByteBuffer buffer;
	private final FrameHeader frame;
	private final HuffmanTable huffTable;
	private final QuantizationTable quantTable;
	private final ScanHeader scan;
	private final int HuffTab[][][] = new int[4][2][MAX_HUFFMAN_SUBTREE * 256];
	private final int IDCT_Source[] = new int[64];
	private final int nBlock[] = new int[10]; // number of blocks in the i-th Comp in a scan
	private final int[] acTab[] = new int[10][]; // ac HuffTab for the i-th Comp in a scan
	private final int[] dcTab[] = new int[10][]; // dc HuffTab for the i-th Comp in a scan
	private final int[] qTab[] = new int[10][]; // quantization table for the i-th Comp in a scan

	private boolean restarting;
	private int dataBufferIndex;
	private int marker;
	private int markerIndex;
	private int numComp;
	private int restartInterval;
	private int selection;
	private int xDim, yDim;
	private int xLoc;
	private int yLoc;
	private int mask;
	private int[] outputData;
	private int[] outputRedData;
	private int[] outputGreenData;
	private int[] outputBlueData;

	private static final int IDCT_P[] = { 0, 5, 40, 16, 45, 2, 7, 42, 21, 56, 8, 61, 18, 47, 1, 4, 41, 23, 58, 13, 32, 24, 37, 10, 63, 17, 44, 3, 6, 43, 20,
			57, 15, 34, 29, 48, 53, 26, 39, 9, 60, 19, 46, 22, 59, 12, 33, 31, 50, 55, 25, 36, 11, 62, 14, 35, 28, 49, 52, 27, 38, 30, 51, 54 };
	private static final int TABLE[] = { 0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24, 31, 40, 44, 53,
			10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61, 35, 36, 48, 49, 57, 58, 62, 63 };

	public static final int RESTART_MARKER_BEGIN = 0xFFD0;
	public static final int RESTART_MARKER_END = 0xFFD7;
	public static final int MAX_HUFFMAN_SUBTREE = 50;
	public static final int MSB = 0x80000000;


	public int getDimX(){
		return xDim;
	}
	public int getDimY(){
		return yDim;
	}

	public JPEGLosslessDecoder(final byte[] data) {
		buffer = ByteBuffer.wrap(data);
		frame = new FrameHeader();
		scan = new ScanHeader();
		quantTable = new QuantizationTable();
		huffTable = new HuffmanTable();
	}



	public int[][] decode() throws IOException {
		int current, scanNum = 0;
		final int pred[] = new int[10];
		int[][] outputRef = null;

		xLoc = 0;
		yLoc = 0;
		current = get16();

		if (current != 0xFFD8) { // SOI
			throw new IOException("Not a JPEG file");
		}

		current = get16();

		while (((current >> 4) != 0x0FFC) || (current == 0xFFC4)) { // SOF 0~15
			switch (current) {
				case 0xFFC4: // DHT
					huffTable.read(this, HuffTab);
					break;
				case 0xFFCC: // DAC
					throw new IOException("Program doesn't support arithmetic coding. (format throw new IOException)");
				case 0xFFDB:
					quantTable.read(this, TABLE);
					break;
				case 0xFFDD:
					restartInterval = readNumber();
					break;
				case 0xFFE0:
				case 0xFFE1:
				case 0xFFE2:
				case 0xFFE3:
				case 0xFFE4:
				case 0xFFE5:
				case 0xFFE6:
				case 0xFFE7:
				case 0xFFE8:
				case 0xFFE9:
				case 0xFFEA:
				case 0xFFEB:
				case 0xFFEC:
				case 0xFFED:
				case 0xFFEE:
				case 0xFFEF:
					readApp();
					break;
				case 0xFFFE:
					readComment();
					break;
				default:
					if ((current >> 8) != 0xFF) {
						throw new IOException("ERROR: format throw new IOException! (decode)");
					}
			}

			current = get16();
		}

		if ((current < 0xFFC0) || (current > 0xFFC7)) {
			throw new IOException("ERROR: could not handle arithmetic code!");
		}

		frame.read(this);
		current = get16();

		do {
			while (current != 0x0FFDA) { //SOS
				switch (current) {
					case 0xFFC4: //DHT
						huffTable.read(this, HuffTab);
						break;
					case 0xFFCC: //DAC
						throw new IOException("Program doesn't support arithmetic coding. (format throw new IOException)");
					case 0xFFDB:
						quantTable.read(this, TABLE);
						break;
					case 0xFFDD:
						restartInterval = readNumber();
						break;
					case 0xFFE0:
					case 0xFFE1:
					case 0xFFE2:
					case 0xFFE3:
					case 0xFFE4:
					case 0xFFE5:
					case 0xFFE6:
					case 0xFFE7:
					case 0xFFE8:
					case 0xFFE9:
					case 0xFFEA:
					case 0xFFEB:
					case 0xFFEC:
					case 0xFFED:
					case 0xFFEE:
					case 0xFFEF:
						readApp();
						break;
					case 0xFFFE:
						readComment();
						break;
					default:
						if ((current >> 8) != 0xFF) {
							throw new IOException("ERROR: format throw new IOException! (Parser.decode)");
						}
				}

				current = get16();
			}

			final int precision = frame.getPrecision();

			if (precision == 8) {
				mask = 0xFF;
			} else {
				mask = 0xFFFF;
			}

			final ComponentSpec[] components = frame.getComponents();

			scan.read(this);
			numComp = scan.getNumComponents();
			selection = scan.getSelection();

			final ScanComponent[] scanComps = scan.components;
			final int[][] quantTables = quantTable.quantTables;

			for (int i = 0; i < numComp; i++) {
				final int compN = scanComps[i].getScanCompSel();
				qTab[i] = quantTables[components[compN].quantTableSel];
				nBlock[i] = components[compN].vSamp * components[compN].hSamp;
				dcTab[i] = HuffTab[scanComps[i].getDcTabSel()][0];
				acTab[i] = HuffTab[scanComps[i].getAcTabSel()][1];
			}

			xDim = frame.getDimX();
			yDim = frame.getDimY();

			outputRef = new int[numComp][];

			if (numComp == 1) {
				outputData = new int[xDim * yDim];
				outputRef[0] = outputData;
			} else {
				outputRedData = new int[xDim * yDim]; // not a good use of memory, but I had trouble packing bytes into int.  some values exceeded 255.
				outputGreenData = new int[xDim * yDim];
				outputBlueData = new int[xDim * yDim];

				outputRef[0] = outputRedData;
				outputRef[1] = outputGreenData;
				outputRef[2] = outputBlueData;
			}

			scanNum++;

			while (true) { // Decode one scan
				final int temp[] = new int[1]; // to store remainder bits
				final int index[] = new int[1];
				temp[0] = 0;
				index[0] = 0;

				for (int i = 0; i < 10; i++) {
					pred[i] = (1 << (precision - 1));
				}

				if (restartInterval == 0) {
					current = decode(pred, temp, index);

					while ((current == 0) && ((xLoc < xDim) && (yLoc < yDim))) {
						output(pred);
						current = decode(pred, temp, index);
					}

					break; //current=MARKER
				}

				for (int mcuNum = 0; mcuNum < restartInterval; mcuNum++) {
					restarting = (mcuNum == 0);
					current = decode(pred, temp, index);
					output(pred);

					if (current != 0) {
						break;
					}
				}

				if (current == 0) {
					if (markerIndex != 0) {
						current = (0xFF00 | marker);
						markerIndex = 0;
					} else {
						current = get16();
					}
				}

				if ((current >= RESTART_MARKER_BEGIN) && (current <= RESTART_MARKER_END)) {
					//empty
				} else {
					break; //current=MARKER
				}
			}

			if ((current == 0xFFDC) && (scanNum == 1)) { //DNL
				readNumber();
				current = get16();
			}
		} while ((current != 0xFFD9) && ((xLoc < xDim) && (yLoc < yDim)) && (scanNum == 0));

		return outputRef;
	}



	@Override
	public final int get16() {
		final int value = (buffer.getShort(dataBufferIndex) & 0xFFFF);
		dataBufferIndex += 2;
		return value;
	}



	@Override
	public final int get8() {
		return buffer.get(dataBufferIndex++) & 0xFF;
	}



	private int decode(final int prev[], final int temp[], final int index[]) throws IOException {
		if (numComp == 1) {
			return decodeSingle(prev, temp, index);
		} else if (numComp == 3) {
			return decodeRGB(prev, temp, index);
		} else {
			return -1;
		}
	}



	private int decodeSingle(final int prev[], final int temp[], final int index[]) throws IOException {
		//		At the beginning of the first line and
		//		at the beginning of each restart interval the prediction value of 2P – 1 is used, where P is the input precision.
		if (restarting) {
			restarting = false;
			prev[0] = (1 << (frame.getPrecision() - 1));
		} else {
			switch (selection) {
				case 2:
					prev[0] = getPreviousY(outputData);
					break;
				case 3:
					prev[0] = getPreviousXY(outputData);
					break;
				case 4:
					prev[0] = (getPreviousX(outputData) + getPreviousY(outputData)) - getPreviousXY(outputData);
					break;
				case 5:
					prev[0] = getPreviousX(outputData) + ((getPreviousY(outputData) - getPreviousXY(outputData)) >> 1);
					break;
				case 6:
					prev[0] = getPreviousY(outputData) + ((getPreviousX(outputData) - getPreviousXY(outputData)) >> 1);
					break;
				case 7:
					prev[0] = (int) (((long) getPreviousX(outputData) + getPreviousY(outputData)) / 2);
					break;
				default:
					prev[0] = getPreviousX(outputData);
					break;
			}
		}

		for (int i = 0; i < nBlock[0]; i++) {
			final int value = getHuffmanValue(dcTab[0], temp, index);

			if (value >= 0xFF00) {
				return value;
			}

			final int n = getn(prev, value, temp, index);

			final int nRestart = (n >> 8);
			if ((nRestart >= RESTART_MARKER_BEGIN) && (nRestart <= RESTART_MARKER_END)) {
				return nRestart;
			}

			prev[0] += n;
		}

		return 0;
	}



	private int decodeRGB(final int prev[], final int temp[], final int index[]) throws IOException {
		switch (selection) {
			case 2:
				prev[0] = getPreviousY(outputRedData);
				prev[1] = getPreviousY(outputGreenData);
				prev[2] = getPreviousY(outputBlueData);
				break;
			case 3:
				prev[0] = getPreviousXY(outputRedData);
				prev[1] = getPreviousXY(outputGreenData);
				prev[2] = getPreviousXY(outputBlueData);
				break;
			case 4:
				prev[0] = (getPreviousX(outputRedData) + getPreviousY(outputRedData)) - getPreviousXY(outputRedData);
				prev[1] = (getPreviousX(outputGreenData) + getPreviousY(outputGreenData)) - getPreviousXY(outputGreenData);
				prev[2] = (getPreviousX(outputBlueData) + getPreviousY(outputBlueData)) - getPreviousXY(outputBlueData);
				break;
			case 5:
				prev[0] = getPreviousX(outputRedData) + ((getPreviousY(outputRedData) - getPreviousXY(outputRedData)) >> 1);
				prev[1] = getPreviousX(outputGreenData) + ((getPreviousY(outputGreenData) - getPreviousXY(outputGreenData)) >> 1);
				prev[2] = getPreviousX(outputBlueData) + ((getPreviousY(outputBlueData) - getPreviousXY(outputBlueData)) >> 1);
				break;
			case 6:
				prev[0] = getPreviousY(outputRedData) + ((getPreviousX(outputRedData) - getPreviousXY(outputRedData)) >> 1);
				prev[1] = getPreviousY(outputGreenData) + ((getPreviousX(outputGreenData) - getPreviousXY(outputGreenData)) >> 1);
				prev[2] = getPreviousY(outputBlueData) + ((getPreviousX(outputBlueData) - getPreviousXY(outputBlueData)) >> 1);
				break;
			case 7:
				prev[0] = (int) (((long) getPreviousX(outputRedData) + getPreviousY(outputRedData)) / 2);
				prev[1] = (int) (((long) getPreviousX(outputGreenData) + getPreviousY(outputGreenData)) / 2);
				prev[2] = (int) (((long) getPreviousX(outputBlueData) + getPreviousY(outputBlueData)) / 2);
				break;
			default:
				prev[0] = getPreviousX(outputRedData);
				prev[1] = getPreviousX(outputGreenData);
				prev[2] = getPreviousX(outputBlueData);
				break;
		}

		int value, actab[], dctab[];
		int qtab[];

		for (int ctrC = 0; ctrC < numComp; ctrC++) {
			qtab = qTab[ctrC];
			actab = acTab[ctrC];
			dctab = dcTab[ctrC];
			for (int i = 0; i < nBlock[ctrC]; i++) {
				for (int k = 0; k < IDCT_Source.length; k++) {
					IDCT_Source[k] = 0;
				}

				value = getHuffmanValue(dctab, temp, index);

				if (value >= 0xFF00) {
					return value;
				}

				prev[ctrC] = IDCT_Source[0] = prev[ctrC] + getn(index, value, temp, index);
				IDCT_Source[0] *= qtab[0];

				for (int j = 1; j < 64; j++) {
					value = getHuffmanValue(actab, temp, index);

					if (value >= 0xFF00) {
						return value;
					}

					j += (value >> 4);

					if ((value & 0x0F) == 0) {
						if ((value >> 4) == 0) {
							break;
						}
					} else {
						IDCT_Source[IDCT_P[j]] = getn(index, value & 0x0F, temp, index) * qtab[j];
					}
				}
			}
		}

		return 0;
	}



	//	Huffman table for fast search: (HuffTab) 8-bit Look up table 2-layer search architecture, 1st-layer represent 256 node (8 bits) if codeword-length > 8
	//	bits, then the entry of 1st-layer = (# of 2nd-layer table) | MSB and it is stored in the 2nd-layer Size of tables in each layer are 256.
	//	HuffTab[*][*][0-256] is always the only 1st-layer table.
	//	 
	//	An entry can be: (1) (# of 2nd-layer table) | MSB , for code length > 8 in 1st-layer (2) (Code length) << 8 | HuffVal
	//	 
	//	HuffmanValue(table   HuffTab[x][y] (ex) HuffmanValue(HuffTab[1][0],...)
	//	                ):
	//	    return: Huffman Value of table
	//	            0xFF?? if it receives a MARKER
	//	    Parameter:  table   HuffTab[x][y] (ex) HuffmanValue(HuffTab[1][0],...)
	//	                temp    temp storage for remainded bits
	//	                index   index to bit of temp
	//	                in      FILE pointer
	//	    Effect:
	//	        temp  store new remainded bits
	//	        index change to new index
	//	        in    change to new position
	//	    NOTE:
	//	      Initial by   temp=0; index=0;
	//	    NOTE: (explain temp and index)
	//	      temp: is always in the form at calling time or returning time
	//	       |  byte 4  |  byte 3  |  byte 2  |  byte 1  |
	//	       |     0    |     0    | 00000000 | 00000??? |  if not a MARKER
	//	                                               ^index=3 (from 0 to 15)
	//	                                               321
	//	    NOTE (marker and marker_index):
	//	      If get a MARKER from 'in', marker=the low-byte of the MARKER
	//	        and marker_index=9
	//	      If marker_index=9 then index is always > 8, or HuffmanValue()
	//	        will not be called
	private int getHuffmanValue(final int table[], final int temp[], final int index[]) throws IOException {
		int code, input;
		final int mask = 0xFFFF;

		if (index[0] < 8) {
			temp[0] <<= 8;
			input = get8();
			if (input == 0xFF) {
				marker = get8();
				if (marker != 0) {
					markerIndex = 9;
				}
			}
			temp[0] |= input;
		} else {
			index[0] -= 8;
		}

		code = table[temp[0] >> index[0]];

		if ((code & MSB) != 0) {
			if (markerIndex != 0) {
				markerIndex = 0;
				return 0xFF00 | marker;
			}

			temp[0] &= (mask >> (16 - index[0]));
			temp[0] <<= 8;
			input = get8();

			if (input == 0xFF) {
				marker = get8();
				if (marker != 0) {
					markerIndex = 9;
				}
			}

			temp[0] |= input;
			code = table[((code & 0xFF) * 256) + (temp[0] >> index[0])];
			index[0] += 8;
		}

		index[0] += 8 - (code >> 8);

		if (index[0] < 0) {
			throw new IOException("index=" + index[0] + " temp=" + temp[0] + " code=" + code + " in HuffmanValue()");
		}

		if (index[0] < markerIndex) {
			markerIndex = 0;
			return 0xFF00 | marker;
		}

		temp[0] &= (mask >> (16 - index[0]));
		return code & 0xFF;
	}



	private int getn(final int[] PRED, final int n, final int temp[], final int index[]) throws IOException {
		int result;
		final int one = 1;
		final int n_one = -1;
		final int mask = 0xFFFF;
		int input;

		if (n == 0) {
			return 0;
		}

		if (n == 16) {
			if (PRED[0] >= 0) {
				return -32768;
			} else {
				return 32768;
			}
		}

		index[0] -= n;

		if (index[0] >= 0) {
			if ((index[0] < markerIndex) && !isLastPixel()) { // this was corrupting the last pixel in some cases
				markerIndex = 0;
				return (0xFF00 | marker) << 8;
			}

			result = temp[0] >> index[0];
			temp[0] &= (mask >> (16 - index[0]));
		} else {
			temp[0] <<= 8;
			input = get8();

			if (input == 0xFF) {
				marker = get8();
				if (marker != 0) {
					markerIndex = 9;
				}
			}

			temp[0] |= input;
			index[0] += 8;

			if (index[0] < 0) {
				if (markerIndex != 0) {
					markerIndex = 0;
					return (0xFF00 | marker) << 8;
				}

				temp[0] <<= 8;
				input = get8();

				if (input == 0xFF) {
					marker = get8();
					if (marker != 0) {
						markerIndex = 9;
					}
				}

				temp[0] |= input;
				index[0] += 8;
			}

			if (index[0] < 0) {
				throw new IOException("index=" + index[0] + " in getn()");
			}

			if (index[0] < markerIndex) {
				markerIndex = 0;
				return (0xFF00 | marker) << 8;
			}

			result = temp[0] >> index[0];
			temp[0] &= (mask >> (16 - index[0]));
		}

		if (result < (one << (n - 1))) {
			result += (n_one << n) + 1;
		}

		return result;
	}



	private int getPreviousX(final int data[]) {
		if (xLoc > 0) {
			return data[((yLoc * xDim) + xLoc) - 1];
		} else if (yLoc > 0) {
			return getPreviousY(data);
		} else {
			return (1 << (frame.getPrecision() - 1));
		}
	}



	private int getPreviousXY(final int data[]) {
		if ((xLoc > 0) && (yLoc > 0)) {
			return data[(((yLoc - 1) * xDim) + xLoc) - 1];
		} else {
			return getPreviousY(data);
		}
	}



	private int getPreviousY(final int data[]) {
		if (yLoc > 0) {
			return data[((yLoc - 1) * xDim) + xLoc];
		} else {
			return getPreviousX(data);
		}
	}



	private boolean isLastPixel() {
		return (xLoc == (xDim - 1)) && (yLoc == (yDim - 1));
	}



	private void output(final int PRED[]) {
		if (numComp == 1) {
			outputSingle(PRED);
		} else {
			outputRGB(PRED);
		}
	}



	private void outputSingle(final int PRED[]) {
		if ((xLoc < xDim) && (yLoc < yDim)) {
			outputData[(yLoc * xDim) + xLoc] = mask & PRED[0];
			xLoc++;

			if (xLoc >= xDim) {
				yLoc++;
				xLoc = 0;
			}
		}
	}



	private void outputRGB(final int PRED[]) {
		if ((xLoc < xDim) && (yLoc < yDim)) {
			outputRedData[(yLoc * xDim) + xLoc] = PRED[0];
			outputGreenData[(yLoc * xDim) + xLoc] = PRED[1];
			outputBlueData[(yLoc * xDim) + xLoc] = PRED[2];
			xLoc++;

			if (xLoc >= xDim) {
				yLoc++;
				xLoc = 0;
			}
		}
	}



	private int readApp() throws IOException {
		int count = 0;
		final int length = get16();
		count += 2;

		while (count < length) {
			get8();
			count++;
		}

		return length;
	}



	private String readComment() throws IOException {
		final StringBuffer sb = new StringBuffer();
		int count = 0;

		final int length = get16();
		count += 2;

		while (count < length) {
			sb.append((char) get8());
			count++;
		}

		return sb.toString();
	}



	private int readNumber() throws IOException {
		final int Ld = get16();

		if (Ld != 4) {
			throw new IOException("ERROR: Define number format throw new IOException [Ld!=4]");
		}

		return get16();
	}



	public int getNumComponents() {
		return numComp;
	}



	public int getPrecision() {
		return frame.getPrecision();
	}
}
