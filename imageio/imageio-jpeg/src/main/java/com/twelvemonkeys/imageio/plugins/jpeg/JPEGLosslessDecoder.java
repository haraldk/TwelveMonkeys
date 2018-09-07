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
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class JPEGLosslessDecoder {

    private final ImageInputStream input;
    private final JPEGImageReader listenerDelegate;

    private final Frame frame;
    private final List<HuffmanTable> huffTables;
    private final QuantizationTable quantTable;
    private Scan scan;

    private final int HuffTab[][][] = new int[4][2][MAX_HUFFMAN_SUBTREE * 256];
    private final int IDCT_Source[] = new int[64];
    private final int nBlock[] = new int[10]; // number of blocks in the i-th Comp in a scan
    private final int[] acTab[] = new int[10][]; // ac HuffTab for the i-th Comp in a scan
    private final int[] dcTab[] = new int[10][]; // dc HuffTab for the i-th Comp in a scan
    private final int[] qTab[] = new int[10][]; // quantization table for the i-th Comp in a scan

    private boolean restarting;
    private int marker;
    private int markerIndex;
    private int numComp;
    private int restartInterval;
    private int selection;
    private int xDim, yDim;
    private int xLoc;
    private int yLoc;
    private int mask;
    private int[][] outputData;

    private static final int IDCT_P[] = {
             0,  5, 40, 16, 45,  2,  7, 42,
            21, 56,  8, 61, 18, 47,  1,  4,
            41, 23, 58, 13, 32, 24, 37, 10,
            63, 17, 44,  3,  6, 43, 20, 57,
            15, 34, 29, 48, 53, 26, 39,  9,
            60, 19, 46, 22, 59, 12, 33, 31,
            50, 55, 25, 36, 11, 62, 14, 35,
            28, 49, 52, 27, 38, 30, 51, 54
    };
    private static final int TABLE[] = {
             0,  1,  5,  6, 14, 15, 27, 28,
             2,  4,  7, 13, 16, 26, 29, 42,
             3,  8, 12, 17, 25, 30, 41, 43,
             9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63
    };

    private static final int RESTART_MARKER_BEGIN = 0xFFD0;
    private static final int RESTART_MARKER_END = 0xFFD7;
    private static final int MAX_HUFFMAN_SUBTREE = 50;
    private static final int MSB = 0x80000000;

    int getDimX() {
        return xDim;
    }

    int getDimY() {
        return yDim;
    }

    JPEGLosslessDecoder(final List<Segment> segments, final ImageInputStream data, final JPEGImageReader listenerDelegate) {
        Validate.notNull(segments);

        frame = get(segments, Frame.class);
        scan = get(segments, Scan.class);

        QuantizationTable qt = get(segments, QuantizationTable.class);
        quantTable = qt != null ? qt : new QuantizationTable(); // For lossless there are no DQTs
        huffTables = getAll(segments, HuffmanTable.class); // For lossless there's usually only one, and only DC tables

        RestartInterval dri = get(segments, RestartInterval.class);
        restartInterval = dri != null ? dri.interval : 0;

        input = data;
        this.listenerDelegate = listenerDelegate;
    }

    private <T> List<T> getAll(final List<Segment> segments, final Class<T> type) {
        ArrayList<T> list = new ArrayList<>();

        for (Segment segment : segments) {
            if (type.isInstance(segment)) {
                list.add(type.cast(segment));
            }
        }

        return list;
    }

    private <T> T get(final List<Segment> segments, final Class<T> type) {
        for (Segment segment : segments) {
            if (type.isInstance(segment)) {
                return type.cast(segment);
            }
        }

        return null;
    }

    int[][] decode() throws IOException {
        int current, scanNum = 0;

        xLoc = 0;
        yLoc = 0;
        current = input.readUnsignedShort();

        if (current != JPEG.SOI) { // SOI
            throw new IIOException("Not a JPEG file, does not start with 0xFFD8");
        }

        for (HuffmanTable huffTable : huffTables) {
            huffTable.buildHuffTables(HuffTab);
        }

        quantTable.enhanceTables(TABLE);

        current = input.readUnsignedShort();

        do {
            // Skip until first SOS
            while (current != JPEG.SOS) {
                input.skipBytes(input.readUnsignedShort() - 2);
                current = input.readUnsignedShort();
            }

            int precision = frame.samplePrecision;

            if (precision == 8) {
                mask = 0xFF;
            }
            else {
                mask = 0xFFFF;
            }

            Frame.Component[] components = frame.components;

            scan = readScan();
            numComp = scan.components.length;
            selection = scan.spectralSelStart;

            final Scan.Component[] scanComps = scan.components;
            final int[][] quantTables = quantTable.quantTables;

            for (int i = 0; i < numComp; i++) {
                Frame.Component component = getComponentSpec(components, scanComps[i].scanCompSel);
                qTab[i] = quantTables[component.qtSel];
                nBlock[i] = component.vSub * component.hSub;

                int dcTabSel = scanComps[i].dcTabSel;
                int acTabSel = scanComps[i].acTabSel;

                // NOTE: If we don't find any DC tables for lossless operation, this file isn't any good.
                // However, we have seen files with AC tables only, we'll treat these as if the AC was DC
                if (useACForDC(dcTabSel)) {
                    processWarningOccured("Lossless JPEG with no DC tables encountered. Assuming only tables present to be DC tables.");

                    dcTab[i] = HuffTab[dcTabSel][1];
                    acTab[i] = HuffTab[acTabSel][0];
                }
                else {
                    // All good
                    dcTab[i] = HuffTab[dcTabSel][0];
                    acTab[i] = HuffTab[acTabSel][1];
                }
            }

            xDim = frame.samplesPerLine;
            yDim = frame.lines;

            outputData = new int[numComp][];

            for (int componentIndex = 0; componentIndex < numComp; ++componentIndex) {
                // not a good use of memory, but I had trouble packing bytes into int.  some values exceeded 255.
                outputData[componentIndex] = new int[xDim * yDim];
            }

            final int firstValue[] = new int[numComp];
            for (int i = 0; i < numComp; i++) {
                firstValue[i] = (1 << (precision - 1));
            }

            final int pred[] = new int[numComp];

            scanNum++;

            while (true) { // Decode one scan
                int temp[] = new int[1]; // to store remainder bits
                int index[] = new int[1];

                System.arraycopy(firstValue, 0, pred, 0, numComp);

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
                    }
                    else {
                        current = input.readUnsignedShort();
                    }
                }

                if ((current < RESTART_MARKER_BEGIN) || (current > RESTART_MARKER_END)) {
                    break; //current=MARKER
                }
            }

            if ((current == JPEG.DNL) && (scanNum == 1)) { //DNL
                readNumber();
                current = input.readUnsignedShort();
            }
        // TODO oe: 05.05.2018 Is it correct loop? Content of outputData from previous iteration is always lost.
        } while ((current != JPEG.EOI) && ((xLoc < xDim) && (yLoc < yDim)) && (scanNum == 0));

        return outputData;
    }

    private void processWarningOccured(String warning) {
        listenerDelegate.processWarningOccurred(warning);
    }

    private boolean useACForDC(final int dcTabSel) {
        if (isLossless()) {
            for (HuffmanTable huffTable : huffTables) {
                if (huffTable.tc[dcTabSel][0] == 0 && huffTable.tc[dcTabSel][1] != 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isLossless() {
        switch (frame.marker) {
            case JPEG.SOF3:
            case JPEG.SOF7:
            case JPEG.SOF11:
            case JPEG.SOF15:
                return true;
            default:
                return false;
        }
    }

    private Frame.Component getComponentSpec(Frame.Component[] components, int sel) {
        for (Frame.Component component : components) {
            if (component.id == sel) {
                return component;
            }
        }

        throw new IllegalArgumentException("No such component id: " + sel);
    }

    private Scan readScan() throws IOException {
        int length = input.readUnsignedShort();
        return Scan.read(input, length);
    }

    private int decode(final int prev[], final int temp[], final int index[]) throws IOException {
        if (numComp == 1) {
            return decodeSingle(prev, temp, index);
        }
        else if (numComp == 3) {
            return decodeRGB(prev, temp, index);
        }
        else {
            return decodeAny(prev, temp, index);
        }
    }

    private int decodeSingle(final int prev[], final int temp[], final int index[]) throws IOException {
        // At the beginning of the first line and
        // at the beginning of each restart interval the prediction value of 2P – 1 is used, where P is the input precision.
        if (restarting) {
            restarting = false;
            prev[0] = (1 << (frame.samplePrecision - 1));
        }
        else {
            final int[] outputData = this.outputData[0];
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
            int value = getHuffmanValue(dcTab[0], temp, index);

            if (value >= 0xFF00) {
                return value;
            }

            int n = getn(prev, value, temp, index);

            int nRestart = (n >> 8);
            if ((nRestart >= RESTART_MARKER_BEGIN) && (nRestart <= RESTART_MARKER_END)) {
                return nRestart;
            }

            prev[0] += n;
        }

        return 0;
    }

    private int decodeRGB(final int prev[], final int temp[], final int index[]) throws IOException {
        final int[] outputRedData = outputData[0];
        final int[] outputGreenData = outputData[1];
        final int[] outputBlueData = outputData[2];
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

        return decode0(prev, temp, index);
    }

    private int decodeAny(final int prev[], final int temp[], final int index[]) throws IOException {
        for (int componentIndex = 0; componentIndex < outputData.length; ++componentIndex) {
            final int[] outputData = this.outputData[componentIndex];
            final int previous;
            switch (selection) {
                case 2:
                    previous = getPreviousY(outputData);
                    break;
                case 3:
                    previous = getPreviousXY(outputData);
                    break;
                case 4:
                    previous = (getPreviousX(outputData) + getPreviousY(outputData)) - getPreviousXY(outputData);
                    break;
                case 5:
                    previous = getPreviousX(outputData) + ((getPreviousY(outputData) - getPreviousXY(outputData)) >> 1);
                    break;
                case 6:
                    previous = getPreviousY(outputData) + ((getPreviousX(outputData) - getPreviousXY(outputData)) >> 1);
                    break;
                case 7:
                    previous = (int) (((long) getPreviousX(outputData) + getPreviousY(outputData)) / 2);
                    break;
                default:
                    previous = getPreviousX(outputData);
                    break;
            }
            prev[componentIndex] = previous;
        }

        return decode0(prev, temp, index);
    }

    private int decode0(int[] prev, int[] temp, int[] index) throws IOException {
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
                    }
                    else {
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
            input = this.input.readUnsignedByte();
            if (input == 0xFF) {
                marker = this.input.readUnsignedByte();
                if (marker != 0) {
                    markerIndex = 9;
                }
            }
            temp[0] |= input;
        }
        else {
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
            input = this.input.readUnsignedByte();

            if (input == 0xFF) {
                marker = this.input.readUnsignedByte();
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
            throw new IIOException("index=" + index[0] + " temp=" + temp[0] + " code=" + code + " in HuffmanValue()");
        }

        if (index[0] < markerIndex) {
            markerIndex = 0;
            return 0xFF00 | marker;
        }

        temp[0] &= (mask >> (16 - index[0]));
        return code & 0xFF;
    }

    private int getn(final int[] pred, final int n, final int temp[], final int index[]) throws IOException {
        int result;
        final int one = 1;
        final int n_one = -1;
        final int mask = 0xFFFF;
        int input;

        if (n == 0) {
            return 0;
        }

        if (n == 16) {
            if (pred[0] >= 0) {
                return -32768;
            }
            else {
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
        }
        else {
            temp[0] <<= 8;
            input = this.input.readUnsignedByte();

            if (input == 0xFF) {
                marker = this.input.readUnsignedByte();
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
                input = this.input.readUnsignedByte();

                if (input == 0xFF) {
                    marker = this.input.readUnsignedByte();
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
        }
        else if (yLoc > 0) {
            return getPreviousY(data);
        }
        else {
            return (1 << (frame.samplePrecision - 1));
        }
    }

    private int getPreviousXY(final int data[]) {
        if ((xLoc > 0) && (yLoc > 0)) {
            return data[(((yLoc - 1) * xDim) + xLoc) - 1];
        }
        else {
            return getPreviousY(data);
        }
    }

    private int getPreviousY(final int data[]) {
        if (yLoc > 0) {
            return data[((yLoc - 1) * xDim) + xLoc];
        }
        else {
            return getPreviousX(data);
        }
    }

    private boolean isLastPixel() {
        return (xLoc == (xDim - 1)) && (yLoc == (yDim - 1));
    }

    private void output(final int pred[]) {
        if (numComp == 1) {
            outputSingle(pred);
        }
        else if (numComp == 3) {
            outputRGB(pred);
        }
        else {
            outputAny(pred);
        }
    }

    private void outputSingle(final int pred[]) {
        if ((xLoc < xDim) && (yLoc < yDim)) {
            outputData[0][(yLoc * xDim) + xLoc] = mask & pred[0];
            xLoc++;

            if (xLoc >= xDim) {
                yLoc++;
                xLoc = 0;
            }
        }
    }

    private void outputRGB(final int pred[]) {
        if ((xLoc < xDim) && (yLoc < yDim)) {
            final int index = (yLoc * xDim) + xLoc;
            outputData[0][index] = pred[0];
            outputData[1][index] = pred[1];
            outputData[2][index] = pred[2];
            xLoc++;

            if (xLoc >= xDim) {
                yLoc++;
                xLoc = 0;
            }
        }
    }

    private void outputAny(final int pred[]) {
        if ((xLoc < xDim) && (yLoc < yDim)) {
            final int index = (yLoc * xDim) + xLoc;
            for (int componentIndex = 0; componentIndex < outputData.length; ++componentIndex) {
                outputData[componentIndex][index] = pred[componentIndex];
            }
            xLoc++;

            if (xLoc >= xDim) {
                yLoc++;
                xLoc = 0;
            }
        }
    }

    private int readNumber() throws IOException {
        final int Ld = input.readUnsignedShort();

        if (Ld != 4) {
            throw new IOException("ERROR: Define number format throw new IOException [Ld!=4]");
        }

        return input.readUnsignedShort();
    }

    int getNumComponents() {
        return numComp;
    }

    int getPrecision() {
        return frame.samplePrecision;
    }
}
