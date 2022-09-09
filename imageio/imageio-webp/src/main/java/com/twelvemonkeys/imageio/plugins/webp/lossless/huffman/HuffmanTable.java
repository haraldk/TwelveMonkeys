package com.twelvemonkeys.imageio.plugins.webp.lossless.huffman;

import com.twelvemonkeys.imageio.plugins.webp.LSBBitReader;

import javax.imageio.IIOException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a single huffman tree as a table.
 * <p>
 * Decoding a symbol just involves reading bits from the input stream and using that read value to index into the
 * lookup table.
 * <p>
 * Code length and the corresponding symbol are packed into one array element (int).
 * This is done to avoid the overhead and the fragmentation over the whole heap involved with creating objects
 * of a custom class. The upper 16 bits of each element are the code length and lower 16 bits are the symbol.
 * <p>
 * The max allowed code length by the WEBP specification is 15, therefore this would mean the table needs to have
 * 2^15 elements. To keep a reasonable memory usage, instead the lookup table only directly holds symbols with code
 * length up to {@code LEVEL1_BITS} (Currently 8 bits). For longer codes the lookup table stores a reference to a
 * second level lookup table. This reference consists of an element with length as the max length of the level 2
 * table and value as the index of the table in the list of level 2 tables.
 * <p>
 * Reading bits from the input is done in a least significant bit first way (LSB) way, therefore the prefix of the
 * read value of length i is the lowest i bits in inverse order.
 * The lookup table is directly indexed by the {@code LEVEL1_BITS} next bits read from the input (i.e. the bits
 * corresponding to next code are inverse suffix of the read value/index).
 * So for a code length of l all values with the lowest l bits the same need to decode to the same symbol
 * regardless of the {@code (LEVEL1_BITS - l)} higher bits. So the lookup table needs to have the entry of this symbol
 * repeated every 2^(l + 1) spots starting from the bitwise inverse of the code.
 */
public class HuffmanTable {

    private static final int LEVEL1_BITS = 8;
    /**
     * Symbols of the L-code in the order they need to be read
     */
    private static final int[] L_CODE_ORDER = {17, 18, 0, 1, 2, 3, 4, 5, 16, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private final int[] level1 = new int[1 << LEVEL1_BITS];
    private final List<int[]> level2 = new ArrayList<>();

    /**
     * Build a Huffman table by reading the encoded symbol lengths from the reader
     *
     * @param lsbBitReader the reader to read from
     * @param alphabetSize the number of symbols in the alphabet to be decoded by this huffman table
     * @throws IOException when reading produces an exception
     */
    public HuffmanTable(LSBBitReader lsbBitReader, int alphabetSize) throws IOException {

        boolean simpleLengthCode = lsbBitReader.readBit() == 1;

        if (simpleLengthCode) {
            int symbolNum = lsbBitReader.readBit() + 1;
            boolean first8Bits = lsbBitReader.readBit() == 1;
            short symbol1 = (short) lsbBitReader.readBits(first8Bits ? 8 : 1);

            if (symbolNum == 2) {
                short symbol2 = (short) lsbBitReader.readBits(8);

                for (int i = 0; i < (1 << LEVEL1_BITS); i += 2) {
                    level1[i] = 1 << 16 | symbol1;
                    level1[i + 1] = 1 << 16 | symbol2;
                }
            }
            else {
                Arrays.fill(level1, symbol1);
            }
        }
        else {
            /*
                code lengths also huffman coded
                first read the "first stage" code lengths
                In the following this is called the L-Code (for length code)
            */
            int numLCodeLengths = (int) (lsbBitReader.readBits(4) + 4);
            short[] lCodeLengths = new short[L_CODE_ORDER.length];
            int numPosCodeLens = 0;

            for (int i = 0; i < numLCodeLengths; i++) {
                short len = (short) lsbBitReader.readBits(3);
                lCodeLengths[L_CODE_ORDER[i]] = len;
                if (len > 0) {
                    numPosCodeLens++;
                }

            }

            //Use L-Code to read the actual code lengths
            short[] codeLengths = readCodeLengths(lsbBitReader, lCodeLengths, alphabetSize, numPosCodeLens);


            buildFromLengths(codeLengths);
        }
    }

    /**
     * Builds a Huffman table by using already given code lengths to generate the codes from
     *
     * @param codeLengths    the array specifying the bit length of the code for a symbol (i.e. {@code codeLengths[i]}
     *                       is the bit length of the code for the symbol i)
     * @param numPosCodeLens the number of positive (i.e. non-zero) codeLengths in the array (allows more efficient
     *                       table generation)
     */
    private HuffmanTable(short[] codeLengths, int numPosCodeLens) {
        buildFromLengths(codeLengths, numPosCodeLens);
    }


    /*
         Helper methods to allow reusing in different constructors
     */

    private void buildFromLengths(short[] codeLengths) {
        int numPosCodeLens = 0;
        for (short codeLength : codeLengths) {
            if (codeLength != 0) {
                numPosCodeLens++;
            }
        }
        buildFromLengths(codeLengths, numPosCodeLens);
    }

    private void buildFromLengths(short[] codeLengths, int numPosCodeLens) {

        //Pack code length and corresponding symbols as described above

        int[] lengthsAndSymbols = new int[numPosCodeLens];

        int index = 0;
        for (int i = 0; i < codeLengths.length; i++) {
            if (codeLengths[i] != 0) {
                lengthsAndSymbols[index++] = codeLengths[i] << 16 | i;
            }
        }

        //Special case: Only 1 code value
        if (numPosCodeLens == 1) {
            //Length is 0 so mask to clear length bits
            Arrays.fill(level1, lengthsAndSymbols[0] & 0xffff);
        }

        //Due to the layout of the elements this effectively first sorts by length and then symbol.
        Arrays.sort(lengthsAndSymbols);

        /*
            The next code, in the bit order it would appear on the input stream, i.e. it is reversed.
            Only the lowest bits (corresponding to the bit length of the code) are considered.
            Example: code 0..010 (length 2) would appear as 0..001.
        */
        int code = 0;

        //Used for level2 lookup
        int rootEntry = -1;
        int[] currentTable = null;

        for (int i = 0; i < lengthsAndSymbols.length; i++) {

            int lengthAndSymbol = lengthsAndSymbols[i];

            int length = lengthAndSymbol >>> 16;

            if (length <= LEVEL1_BITS) {
                for (int j = code; j < level1.length; j += 1 << length) {
                    level1[j] = lengthAndSymbol;
                }
            }
            else {
                //Existing level2 table not fitting
                if ((code & ((1 << LEVEL1_BITS) - 1)) != rootEntry) {
                    /*
                        Figure out needed table size.
                        Start at current symbol and length.
                        Every symbol uses 1 slot at the current bit length.
                        Going up 1 bit in length multiplies the slots by 2.
                        No more open slots indicate the table size to be big enough.
                    */
                    int maxLength = length;
                    for (int j = i, openSlots = 1 << (length - LEVEL1_BITS);
                         j < lengthsAndSymbols.length && openSlots > 0;
                         j++, openSlots--) {

                        int innerLength = lengthsAndSymbols[j] >>> 16;

                        while (innerLength != maxLength) {
                            maxLength++;
                            openSlots <<= 1;
                        }
                    }

                    int level2Size = maxLength - LEVEL1_BITS;

                    currentTable = new int[1 << level2Size];
                    rootEntry = code & ((1 << LEVEL1_BITS) - 1);
                    level2.add(currentTable);

                    //Set root table indirection
                    level1[rootEntry] = (LEVEL1_BITS + level2Size) << 16 | (level2.size() - 1);
                }

                //Add to existing (or newly generated) 2nd level table
                for (int j = (code >>> LEVEL1_BITS); j < currentTable.length; j += 1 << (length - LEVEL1_BITS)) {
                    currentTable[j] = (length - LEVEL1_BITS) << 16 | (lengthAndSymbol & 0xffff);
                }
            }

            code = nextCode(code, length);

        }
    }

    /**
     * Computes the next code
     *
     * @param code   the current code
     * @param length the currently valid length
     * @return {@code reverse(reverse(code, length) + 1, length)} where {@code reverse(a, b)} is the lowest b bits of
     * a in inverted order
     */
    private int nextCode(int code, int length) {
        int a = (~code) & ((1 << length) - 1);

        //This will result in the highest 0-bit in the lower length bits of code set (by construction of a)
        //I.e. the lowest 0-bit in the value code represents
        int step = Integer.highestOneBit(a);

        //In the represented value this clears the consecutive 1-bits starting at bit 0 and then sets the lowest 0 bit
        //This corresponds to adding 1 to the value
        return (code & (step - 1)) | step;
    }

    private static short[] readCodeLengths(LSBBitReader lsbBitReader, short[] aCodeLengths, int alphabetSize,
                                           int numPosCodeLens) throws IOException {

        HuffmanTable huffmanTable = new HuffmanTable(aCodeLengths, numPosCodeLens);

        //Not sure where this comes from. Just adapted from the libwebp implementation
        int codedSymbols;
        if (lsbBitReader.readBit() == 1) {
            int maxSymbolBitLength = (int) (2 + 2 * lsbBitReader.readBits(3));
            codedSymbols = (int) (2 + lsbBitReader.readBits(maxSymbolBitLength));
        }
        else {
            codedSymbols = alphabetSize;
        }

        short[] codeLengths = new short[alphabetSize];

        //Default code for repeating
        short prevLength = 8;

        for (int i = 0; i < alphabetSize && codedSymbols > 0; i++, codedSymbols--) {
            short len = huffmanTable.readSymbol(lsbBitReader);

            if (len < 16) { //Literal length
                codeLengths[i] = len;
                if (len != 0) {
                    prevLength = len;
                }
            }
            else {
                short repeatSymbol = 0;
                int extraBits;
                int repeatOffset;

                switch (len) {
                    case 16: //Repeat previous
                        repeatSymbol = prevLength;
                        extraBits = 2;
                        repeatOffset = 3;
                        break;
                    case 17: //Repeat 0 short
                        extraBits = 3;
                        repeatOffset = 3;
                        break;
                    case 18: //Repeat 0 long
                        extraBits = 7;
                        repeatOffset = 11;
                        break;
                    default:
                        throw new IIOException("Huffman: Unreachable: Decoded Code Length > 18.");
                }

                int repeatCount = (int) (lsbBitReader.readBits(extraBits) + repeatOffset);


                if (i + repeatCount > alphabetSize) {
                    throw new IIOException(
                            String.format(
                                    "Huffman: Code length repeat count overflows alphabet: Start index: %d, count: " +
                                            "%d, alphabet size: %d", i, repeatCount, alphabetSize)
                    );
                }

                Arrays.fill(codeLengths, i, i + repeatCount, repeatSymbol);
                i += repeatCount - 1;

            }
        }


        return codeLengths;
    }

    /**
     * Reads the next code symbol from the streaming and decode it using the Huffman table
     *
     * @param lsbBitReader the reader to read a symbol from (will be advanced accordingly)
     * @return the decoded symbol
     * @throws IOException when the reader throws one reading a symbol
     */
    public short readSymbol(LSBBitReader lsbBitReader) throws IOException {

        int index = (int) lsbBitReader.peekBits(LEVEL1_BITS);
        int lengthAndSymbol = level1[index];

        int length = lengthAndSymbol >>> 16;

        if (length > LEVEL1_BITS) {
            //Lvl2 lookup
            lsbBitReader.readBits(LEVEL1_BITS); //Consume bits of first level
            int level2Index = (int) lsbBitReader.peekBits(length - LEVEL1_BITS); //Peek remaining required bits
            lengthAndSymbol = level2.get(lengthAndSymbol & 0xffff)[level2Index];
            length = lengthAndSymbol >>> 16;
        }

        lsbBitReader.readBits(length); //Consume bits

        return (short) (lengthAndSymbol & 0xffff);
    }
}
