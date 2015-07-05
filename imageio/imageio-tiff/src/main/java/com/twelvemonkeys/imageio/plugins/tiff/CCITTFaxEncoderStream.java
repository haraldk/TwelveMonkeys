package com.twelvemonkeys.imageio.plugins.tiff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.twelvemonkeys.lang.Validate;

public class CCITTFaxEncoderStream extends OutputStream {

    private int currentBufferLength = 0;
    private final byte[] inputBuffer;
    private final int inputBufferLength;
    private int columns;

    private int[] changesCurrentRow;
    private int[] changesReferenceRow;
    private int changesCurrentRowLength = 0;
    private int changesReferenceRowLength = 0;
    private byte outputBuffer = 0;
    private byte outputBufferBitLength = 0;
    private int type;
    private int fillOrder;
    private boolean optionG32D;
    private boolean optionG3Fill;
    private boolean optionUncompressed;
    private OutputStream stream;

    public CCITTFaxEncoderStream(final OutputStream stream, final int columns, final int type, final int fillOrder,
            final long options) {

        this.stream = stream;
        this.type = type;
        this.columns = columns;
        this.fillOrder = fillOrder;

        this.changesReferenceRow = new int[columns];
        this.changesCurrentRow = new int[columns];

        switch (type) {
        case TIFFExtension.COMPRESSION_CCITT_T4:
            optionG32D = (options & TIFFExtension.GROUP3OPT_2DENCODING) != 0;
            optionG3Fill = (options & TIFFExtension.GROUP3OPT_FILLBITS) != 0;
            optionUncompressed = (options & TIFFExtension.GROUP3OPT_UNCOMPRESSED) != 0;
            break;
        case TIFFExtension.COMPRESSION_CCITT_T6:
            optionUncompressed = (options & TIFFExtension.GROUP4OPT_UNCOMPRESSED) != 0;
            break;
        }

        inputBufferLength = (columns + 7) / 8;
        inputBuffer = new byte[inputBufferLength];

        Validate.isTrue(!optionUncompressed, optionUncompressed,
                "CCITT GROUP 3/4 OPTION UNCOMPRESSED is not supported");
    }

    @Override
    public void write(int b) throws IOException {
        inputBuffer[currentBufferLength] = (byte) b;
        currentBufferLength++;

        if (currentBufferLength == inputBufferLength) {
            encodeRow();
        }
    }

    // TODO: when to write end EOLs, half filled buffer bytes etc. on end?

    private void encodeRow() throws IOException {
        changesReferenceRow = changesCurrentRow;
        changesReferenceRowLength = changesCurrentRowLength;
        changesCurrentRowLength = 0;

        int index = 0;
        boolean white = true;
        while (index < columns) {
            int byteIndex = index / 8;
            int bit = index % 8;
            if ((((inputBuffer[byteIndex] >> (7 - bit)) & 1) == 1) != (!white)) {
                changesCurrentRow[changesCurrentRowLength] = index;
                changesCurrentRowLength++;
                white = !white;
            }
            index++;
        }

        switch (type) {
        case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
            encodeRowType2();
            break;
        case TIFFExtension.COMPRESSION_CCITT_T4:
            encodeRowType4();
            break;
        case TIFFExtension.COMPRESSION_CCITT_T6:
            encodeRowType6();
            break;
        }
    }

    private void encodeRowType2() throws IOException {
        encode1D();
        fill();
    }

    private void encodeRowType4() throws IOException {
        writeEOL();
        if (optionG32D) {
            // TODO decide whether 1d or 2d row, write k, encode
        } else {
            // TODO encode1d
        }
    }

    private void encodeRowType6() {
        encode2D();
    }

    private void encode1D() throws IOException {
        int index = 0;
        boolean white = true;
        while (index < columns) {
            int nextChange = columns;
            for (int i = 0; i < changesCurrentRowLength; i++) {
                if (index < changesCurrentRow[i]) {
                    nextChange = changesCurrentRow[i];
                }
            }
            int runLength = nextChange - index;

            int nonterm = runLength / 64;
            Code[] codes = white ? WHITE_NONTERMINATING_CODES : BLACK_NONTERMINATING_CODES;
            while (nonterm > 0) {
                if (nonterm >= codes.length) {
                    write(codes[codes.length-1].code,codes[codes.length-1].length);
                    nonterm -= codes.length - 1;
                } else {
                    write(codes[nonterm - 1].code,codes[nonterm - 1].length);
                    nonterm = 0;
                }
            }

            Code c = white ? WHITE_TERMINATING_CODES[runLength % 64] : BLACK_TERMINATING_CODES[runLength % 64];
            write(c.code, c.length);
        }
    }

    private void encode2D() {

    }

    private void write(int code, int codeLength) throws IOException {

        for (int i = 0; i < codeLength; i++) {
            boolean codeBit = ((code >> (i)) & 1) == 1;

            if (fillOrder == TIFFBaseline.FILL_LEFT_TO_RIGHT) {
                outputBuffer |= (codeBit ? 1 << (7 - ((outputBufferBitLength) % 8)) : 0);
            } else {
                outputBuffer |= (codeBit ? 1 << (((outputBufferBitLength) % 8)) : 0);
            }
            outputBufferBitLength++;

            if (outputBufferBitLength == 8) {
                stream.write(outputBuffer);
                clearOutputBuffer();
            }
        }
    }

    private void writeEOL() throws IOException {
        if (optionG3Fill) {
            // Fill up so EOL ends on a byte-boundary
            while (outputBufferBitLength != 4) {
                write(0, 1);
            }
        }
        write(1, 12);
    }

    private void fill() throws IOException {
        stream.write(outputBuffer);
        clearOutputBuffer();
    }

    private void clearOutputBuffer() {
        outputBuffer = 0;
        outputBufferBitLength = 0;
    }

    private static class Code {
        public static Code create(int code, int length) {
            Code c = new Code(code, length);
            return c;
        }

        private Code(int code, int length) {
            this.code = code;
            this.length = length;
        }

        final int code;
        final int length;
    }

    private Code[] WHITE_TERMINATING_CODES = {};

    private Code[] WHITE_NONTERMINATING_CODES = {};

    private Code[] BLACK_TERMINATING_CODES = {};

    private Code[] BLACK_NONTERMINATING_CODES = {};
}
