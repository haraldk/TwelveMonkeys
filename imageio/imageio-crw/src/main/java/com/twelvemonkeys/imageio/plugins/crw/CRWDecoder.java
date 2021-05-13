package com.twelvemonkeys.imageio.plugins.crw;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.stream.ImageInputStream;

/**
 * CRWDecoder
 *
 * @see <a href="http://cybercom.net/~dcoffin/dcraw/decompress.c">A simple reference decompressor for CRW files</a>
 * @author Harald Kuhr (Java port)
 * @author Dave Coffin (original decompress.c)
 */
final class CRWDecoder {

    static class Decode {
        Decode[] branch = new Decode[2];
        int leaf;
    }

    private final ImageInputStream imageInput;

    private final int height;
    private final int width;
    private final int table;

    private Decode[] first_decode = new Decode[32];
    private Decode[] second_decode = new Decode[512];

    CRWDecoder(ImageInputStream imageInput, int width, int height, int table) {
        this.imageInput = imageInput;
        this.width = width;
        this.height = height;
        this.table = table;
    }

    /**
     * Returns {@code false} if the image starts with compressed data,
     * {@code true} if it starts with uncompressed low-order bits.
     * <p>
     * In Canon compressed data, 0xff is always followed by 0x00.
     */
    private boolean canonHasLowbits() throws IOException {
        byte[] test = new byte[0x4000]; // TODO: Should probably be  (height * width / 4) * enough bytes...

        imageInput.seek(0);
        imageInput.readFully(test);

        boolean ret = true;
//        for (int i = 540; i < test.length - 1; i++) {
        for (int i = 0; i < test.length - 1; i++) { // Note: The original 540 is probably CIFF header length + offset (26 + 514)
            if ((test[i] & 0xff) == 0xff) {
                if (test[i + 1] != 0) {
                    return true;
                }
                ret = false;
            }
        }
        return ret;
    }


    /*
       A rough description of Canon's compression algorithm:

        +  Each pixel outputs a 10-bit sample, from 0 to 1023.
        +  Split the data into blocks of 64 samples each.
        +  Subtract from each sample the value of the sample two positions
           to the left, which has the same color filter.  From the two
           leftmost samples in each row, subtract 512.
        +  For each nonzero sample, make a token consisting of two four-bit
           numbers.  The low nibble is the number of bits required to
           represent the sample, and the high nibble is the number of
           zero samples preceding this sample.
        +  Output this token as a variable-length bitstring using
           one of three tablesets.  Follow it with a fixed-length
           bitstring containing the sample.

       The "first_decode" table is used for the first sample in each
       block, and the "second_decode" table is used for the others.
     */

    /**
     * Construct a decode tree according the specification in *source.
     * The first 16 bytes specify how many codes should be 1-bit, 2-bit
     * 3-bit, etc.  Bytes after that are the leaf values.
     * <p>
     * For example, if the source is
     * <p>
     * { 0,1,4,2,3,1,2,0,0,0,0,0,0,0,0,0,
     * 0x04,0x03,0x05,0x06,0x02,0x07,0x01,0x08,0x09,0x00,0x0a,0x0b,0xff  },
     * <p>
     * then the code is
     * <p>
     * 00		0x04
     * 010		0x03
     * 011		0x05
     * 100		0x06
     * 101		0x02
     * 1100		0x07
     * 1101		0x01
     * 11100		0x08
     * 11101		0x09
     * 11110		0x00
     * 111110		0x0a
     * 1111110		0x0b
     * 1111111		0xff
     */
    private Decode[] free;	/* Next unused node */
    private int freeIndex;
    private int leaf;		/* no. of leaves already added */

    //    private void make_decoder(struct decode *dest, const uchar *source, int level)
    private void make_decoder(Decode[] dest, int destIndex, final byte[] source, int level) {
//        static struct decode *free;	/* Next unused node */
//        static int leaf;		/* no. of leaves already added */
        int i, next;

        if (level==0) {
            free = dest;
            freeIndex = 0;

            leaf = 0;
        }
//        free++;
        freeIndex++;
        // At what level should the next leaf appear?
        for (i=next=0; i <= leaf && next < 16; ) {
            i += (source[next++] & 0xff);
        }

        if (i > leaf) {
            if (level < next) {        /* Are we there yet? */
//                dest->branch[0] = free;
//                make_decoder(free, source, level + 1);
                dest[destIndex].branch[0] = free[freeIndex];
                make_decoder(free, freeIndex, source, level + 1);
//                dest->branch[1] = free;
//                make_decoder(free, source, level + 1);
                dest[destIndex].branch[1] = free[freeIndex];
                make_decoder(free, freeIndex, source, level + 1);
            } else {
//                dest->leaf = source[16 + leaf++];
                dest[destIndex].leaf = (source[16 + leaf++] & 0xff);
            }
        }
    }

    static final byte[][] first_tree/*[3][29]*/ = {
            { 0,1,4,2,3,1,2,0,0,0,0,0,0,0,0,0,
                    0x04,0x03,0x05,0x06,0x02,0x07,0x01,0x08,0x09,0x00,0x0a,0x0b, (byte) 0xff},

            { 0,2,2,3,1,1,1,1,2,0,0,0,0,0,0,0,
                    0x03,0x02,0x04,0x01,0x05,0x00,0x06,0x07,0x09,0x08,0x0a,0x0b, (byte) 0xff},

            { 0,0,6,3,1,1,2,0,0,0,0,0,0,0,0,0,
                    0x06,0x05,0x07,0x04,0x08,0x03,0x09,0x02,0x00,0x0a,0x01,0x0b, (byte) 0xff},
    };

    static final byte[][] second_tree/*[3][180]*/ = {
            { 0,2,2,2,1,4,2,1,2,5,1,1,0,0,0, (byte) 139,
                    0x03,0x04,0x02,0x05,0x01,0x06,0x07,0x08,
                    0x12,0x13,0x11,0x14,0x09,0x15,0x22,0x00,0x21,0x16,0x0a, (byte) 0xf0,
                    0x23,0x17,0x24,0x31,0x32,0x18,0x19,0x33,0x25,0x41,0x34,0x42,
                    0x35,0x51,0x36,0x37,0x38,0x29,0x79,0x26,0x1a,0x39,0x56,0x57,
                    0x28,0x27,0x52,0x55,0x58,0x43,0x76,0x59,0x77,0x54,0x61, (byte) 0xf9,
                    0x71,0x78,0x75, (byte) 0x96, (byte) 0x97,0x49, (byte) 0xb7,0x53, (byte) 0xd7,0x74, (byte) 0xb6, (byte) 0x98,
                    0x47,0x48, (byte) 0x95,0x69, (byte) 0x99, (byte) 0x91, (byte) 0xfa, (byte) 0xb8,0x68, (byte) 0xb5, (byte) 0xb9, (byte) 0xd6,
                    (byte) 0xf7, (byte) 0xd8,0x67,0x46,0x45, (byte) 0x94,(byte) 0x89,(byte) 0xf8,(byte) 0x81,(byte) 0xd5,(byte) 0xf6,(byte) 0xb4,
                    (byte) 0x88,(byte) 0xb1,0x2a,0x44,0x72,(byte) 0xd9,(byte) 0x87,0x66,(byte) 0xd4,(byte) 0xf5,0x3a,(byte) 0xa7,
                    0x73,(byte) 0xa9,(byte) 0xa8,(byte) 0x86,0x62,(byte) 0xc7,0x65,(byte) 0xc8,(byte) 0xc9,(byte) 0xa1,(byte) 0xf4,(byte) 0xd1,
                    (byte) 0xe9,0x5a,(byte) 0x92,(byte) 0x85,(byte) 0xa6,(byte) 0xe7,(byte) 0x93,(byte) 0xe8,(byte) 0xc1,(byte) 0xc6,0x7a,0x64,
                    (byte) 0xe1,0x4a,0x6a,(byte) 0xe6,(byte) 0xb3,(byte) 0xf1,(byte) 0xd3,(byte) 0xa5,(byte) 0x8a,(byte) 0xb2,(byte) 0x9a,(byte) 0xba,
                    (byte) 0x84,(byte) 0xa4,0x63,(byte) 0xe5,(byte) 0xc5,(byte) 0xf3,(byte) 0xd2,(byte) 0xc4,(byte) 0x82,(byte) 0xaa,(byte) 0xda,(byte) 0xe4,
                    (byte) 0xf2,(byte) 0xca,(byte) 0x83,(byte) 0xa3,(byte) 0xa2,(byte) 0xc3,(byte) 0xea,(byte) 0xc2,(byte) 0xe2,(byte) 0xe3,(byte) 0xff,(byte) 0xff  },

            { 0,2,2,1,4,1,4,1,3,3,1,0,0,0,0, (byte) 140,
                    0x02,0x03,0x01,0x04,0x05,0x12,0x11,0x06,
                    0x13,0x07,0x08,0x14,0x22,0x09,0x21,0x00,0x23,0x15,0x31,0x32,
                    0x0a,0x16,(byte) 0xf0,0x24,0x33,0x41,0x42,0x19,0x17,0x25,0x18,0x51,
                    0x34,0x43,0x52,0x29,0x35,0x61,0x39,0x71,0x62,0x36,0x53,0x26,
                    0x38,0x1a,0x37,(byte) 0x81,0x27,(byte) 0x91,0x79,0x55,0x45,0x28,0x72,0x59,
                    (byte) 0xa1,(byte) 0xb1,0x44,0x69,0x54,0x58,(byte) 0xd1,(byte) 0xfa,0x57,(byte) 0xe1,(byte) 0xf1,(byte) 0xb9,
                    0x49,0x47,0x63,0x6a,(byte) 0xf9,0x56,0x46,(byte) 0xa8,0x2a,0x4a,0x78,(byte) 0x99,
                    0x3a,0x75,0x74,(byte) 0x86,0x65,(byte) 0xc1,0x76,(byte) 0xb6,(byte) 0x96,(byte) 0xd6,(byte) 0x89,(byte) 0x85,
                    (byte) 0xc9,(byte) 0xf5,(byte) 0x95,(byte) 0xb4,(byte) 0xc7,(byte) 0xf7,(byte) 0x8a,(byte) 0x97,(byte) 0xb8,0x73,(byte) 0xb7,(byte) 0xd8,
                    (byte) 0xd9,(byte) 0x87,(byte) 0xa7,0x7a,0x48,(byte) 0x82,(byte) 0x84,(byte) 0xea,(byte) 0xf4,(byte) 0xa6,(byte) 0xc5,0x5a,
                    (byte) 0x94,(byte) 0xa4,(byte) 0xc6,(byte) 0x92,(byte) 0xc3,0x68,(byte) 0xb5,(byte) 0xc8,(byte) 0xe4,(byte) 0xe5,(byte) 0xe6,(byte) 0xe9,
                    (byte) 0xa2,(byte) 0xa3,(byte) 0xe3,(byte) 0xc2,0x66,0x67,(byte) 0x93,(byte) 0xaa,(byte) 0xd4,(byte) 0xd5,(byte) 0xe7,(byte) 0xf8,
                    (byte) 0x88,(byte) 0x9a,(byte) 0xd7,0x77,(byte) 0xc4,0x64,(byte) 0xe2,(byte) 0x98,(byte) 0xa5,(byte) 0xca,(byte) 0xda,(byte) 0xe8,
                    (byte) 0xf3,(byte) 0xf6,(byte) 0xa9,(byte) 0xb2,(byte) 0xb3,(byte) 0xf2,(byte) 0xd2,(byte) 0x83,(byte) 0xba,(byte) 0xd3,(byte) 0xff,(byte) 0xff  },

            { 0,0,6,2,1,3,3,2,5,1,2,2,8,10,0,117,
                    0x04,0x05,0x03,0x06,0x02,0x07,0x01,0x08,
                    0x09,0x12,0x13,0x14,0x11,0x15,0x0a,0x16,0x17,(byte) 0xf0,0x00,0x22,
                    0x21,0x18,0x23,0x19,0x24,0x32,0x31,0x25,0x33,0x38,0x37,0x34,
                    0x35,0x36,0x39,0x79,0x57,0x58,0x59,0x28,0x56,0x78,0x27,0x41,
                    0x29,0x77,0x26,0x42,0x76,(byte) 0x99,0x1a,0x55,(byte) 0x98,(byte) 0x97,(byte) 0xf9,0x48,
                    0x54,(byte) 0x96,(byte) 0x89,0x47,(byte) 0xb7,0x49,(byte) 0xfa,0x75,0x68,(byte) 0xb6,0x67,0x69,
                    (byte) 0xb9,(byte) 0xb8,(byte) 0xd8,0x52,(byte) 0xd7,(byte) 0x88,(byte) 0xb5,0x74,0x51,0x46,(byte) 0xd9,(byte) 0xf8,
                    0x3a,(byte) 0xd6,(byte) 0x87,0x45,0x7a,(byte) 0x95,(byte) 0xd5,(byte) 0xf6,(byte) 0x86,(byte) 0xb4,(byte) 0xa9,(byte) 0x94,
                    0x53,0x2a,(byte) 0xa8,0x43,(byte) 0xf5,(byte) 0xf7,(byte) 0xd4,0x66,(byte) 0xa7,0x5a,0x44,(byte) 0x8a,
                    (byte) 0xc9,(byte) 0xe8,(byte) 0xc8,(byte) 0xe7,(byte) 0x9a,0x6a,0x73,0x4a,0x61,(byte) 0xc7,(byte) 0xf4,(byte) 0xc6,
                    0x65,(byte) 0xe9,0x72,(byte) 0xe6,0x71,(byte) 0x91,(byte) 0x93,(byte) 0xa6,(byte) 0xda,(byte) 0x92,(byte) 0x85,0x62,
                    (byte) 0xf3,(byte) 0xc5,(byte) 0xb2,(byte) 0xa4,(byte) 0x84,(byte) 0xba,0x64,(byte) 0xa5,(byte) 0xb3,(byte) 0xd2,(byte) 0x81,(byte) 0xe5,
                    (byte) 0xd3,(byte) 0xaa,(byte) 0xc4,(byte) 0xca,(byte) 0xf2,(byte) 0xb1,(byte) 0xe4,(byte) 0xd1,(byte) 0x83,0x63,(byte) 0xea,(byte) 0xc3,
                    (byte) 0xe2,(byte) 0x82,(byte) 0xf1,(byte) 0xa3,(byte) 0xc2,(byte) 0xa1,(byte) 0xc1,(byte) 0xe3,(byte) 0xa2,(byte) 0xe1,(byte) 0xff,(byte) 0xff  }
    };

    private void init_tables(int table) {
        if (table > 2) table = 2;
//        memset( first_decode, 0, sizeof first_decode);
//        memset(second_decode, 0, sizeof second_decode);
//        make_decoder( first_decode, first_tree[table], 0);
//        make_decoder(second_decode, second_tree[table], 0);

        for (int i = 0; i < first_decode.length; i++) {
            first_decode[i] = new Decode();
        }
        for (int i = 0; i < second_decode.length; i++) {
            second_decode[i] = new Decode();
        }

        make_decoder( first_decode, 0, first_tree[table], 0);
        make_decoder(second_decode, 0, second_tree[table], 0);
    }

//#if 0
//    writebits (int val, int nbits)
//    {
//        val <<= 32 - nbits;
//        while (nbits--) {
//            putchar(val & 0x80000000 ? '1':'0');
//            val <<= 1;
//        }
//    }
//#endif

/*
   getbits(-1) initializes the buffer
   getbits(n) where 0 <= n <= 25 returns an n-bit integer
*/
    private int bitbuf=0;
    private int vbits=0;

    int getbits(int nbits) throws IOException {
        int  c;

        if (nbits == 0) return 0;

        int ret;

        if (nbits == -1) {
            ret = bitbuf = vbits = 0;
        }
        else {
//            ret = bitbuf << (32 - vbits) >> (32 - nbits);
            ret = bitbuf << (32 - vbits) >>> (32 - nbits);
            vbits -= nbits;
        }
        while (vbits < 25) {
//            c=fgetc(ifp);
            c=imageInput.readUnsignedByte();
            bitbuf = (bitbuf << 8) + c;
//            if (c == 0xff) fgetc(ifp);	/* always extra 00 after ff */
            if (c == 0xff) {
                imageInput.readUnsignedByte();	// always extra 00 after ff
            }
            vbits += 8;
        }

        return ret;
    }

    short[] decode() throws IOException {
        short[] result = new short[width * height];

//        struct Decode *decode, *dindex;
        Decode decode;
        Decode dindex;
        int i, j, leaf, len, diff, r;
                long save;
        int[] diffbuf = new int[64]; // h * w = 8 * 8 for each compressed block
        int carry=0, column=0;
        int[] base = new int[2];
//        unsigned short outbuf[64];
        short[] outbuf = new short[64];

        int c;

        boolean lowbits = canonHasLowbits();

        init_tables(table);

//        fseek (ifp, 540 + lowbits*height*width/4, SEEK_SET);
//        imageInput.seek(540 + (lowbits ? 1 : 0) * height * width / 4);
        imageInput.seek((lowbits ? 1 : 0) * height * width / 4); // NOTE: The original 540 offset is probably CIFF header length: 26 + DecoderTable[2]: 514
        getbits(-1);			/* Prime the bit buffer */

        while (column < width * height) {
//            memset(diffbuf,0,sizeof diffbuf);
            Arrays.fill(diffbuf, 0);

//            decode = first_decode;
            decode = first_decode[0];
            for (i = 0; i < 64; i++) {

//                for (dindex=decode; dindex->branch[0]; )
//                    dindex = dindex->branch[getbits(1)];
                for (dindex = decode; dindex.branch[0] != null; ) {
                    dindex = dindex.branch[getbits(1)];
                }

//                leaf = dindex->leaf;
                leaf = dindex.leaf;
//                decode = second_decode;
                decode = second_decode[0];

                if (leaf == 0 && i != 0) {
                    break;
                }
                if (leaf == 0xff) {
                    continue;
                }

                i += (leaf >> 4);
                len = leaf & 15;

                if (len == 0) {
                    continue;
                }
                diff = getbits(len);
                if ((diff & (1 << (len - 1))) == 0) {
                    diff -= (1 << len) - 1;
                }

                if (i < 64) {
                    diffbuf[i] = diff;
                }
            }

            // --- Ok ---

            diffbuf[0] += carry;
            carry = diffbuf[0];

            for (i = 0; i < 64; i++) {
                if (column++ % width == 0) {
                    base[0] = base[1] = 512;
                }

                // TODO: The original C code uses unsigned short, so this may overflow differently
                // outbuf[i] = ( base[i & 1] += diffbuf[i] );

                outbuf[i] = (short) (base[i & 1] += diffbuf[i]);
            }

            // -- Ok ---

            if (lowbits) {
//                save = ftell(ifp);
                save = imageInput.getStreamPosition();
//                fseek (ifp, (column-64)/4 + 26, SEEK_SET);
//                    imageInput.seek((column - 64) / 4 + 26);
                imageInput.seek((column - 64) / 4); // Note: The original 26 is CIFF header length (?)
                for (i = j = 0; j < 64 / 4; j++) {
//                    c = fgetc(ifp);
                    c = imageInput.readUnsignedByte();
                    for (r = 0; r < 8; r += 2) {
                        // TODO: Is this correct? The original C code is on one line, the Java equivalent then throws an AIOOBE...
//                            outbuf[i++] = (outbuf[i] << 2) + ((c >> r) & 3);
                        short sample = (short) ((outbuf[i] << 2) + ((c >> r) & 3));
                        outbuf[i++] = sample;
                    }
                }
//                fseek (ifp, save, SEEK_SET);
                imageInput.seek(save);
            }

//            fwrite(outbuf,2,64,stdout);
            System.arraycopy(outbuf, 0, result, column - 64, 64);
        }

        return result;
    }
}
