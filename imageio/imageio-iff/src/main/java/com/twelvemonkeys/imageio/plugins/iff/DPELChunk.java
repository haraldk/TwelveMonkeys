package com.twelvemonkeys.imageio.plugins.iff;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * DPELChunk.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DPELChunk.java,v 1.0 01/02/2022 haraldk Exp$
 */
final class DPELChunk extends IFFChunk {
    /*
//
//   Chunk DPEL
//   ----------
    struct DPEL = {
//
// Number of pixel components
//
ULONG nElements;
    //
// The TypeDepth structure is repeated nElement times to identify
// the content of every pixel.  Pixels will always be padded to
// byte boundaries.  The DBOD chunk will be padded to an even
// longword boundary.
//
    struct TypeDepth = {
//
// Type of data
//
UWORD cType;
    //
// Bit depth of this type
//
    UWORD cBitDepth;
} typedepth[Nelements];
        };
     */
    TypeDepth[] typeDepths;

    DPELChunk(final int chunkLength) {
        super(IFF.CHUNK_DPEL, chunkLength);
    }

    @Override
    void readChunk(final DataInput input) throws IOException {
        int components = input.readInt(); // Strictly, it's unsigned, but that many components is unlikely...

        if (chunkLength != 4 + components * 4) {
            throw new IIOException("Unsupported DPEL chunk length: " + chunkLength);
        }

        typeDepths = new TypeDepth[components];

        for (int i = 0; i < components; i++) {
            typeDepths[i] = new TypeDepth(input.readUnsignedShort(), input.readUnsignedShort());
        }
    }

    @Override
    void writeChunk(final DataOutput output) {
        throw new InternalError("Not implemented: writeChunk()");
    }

    @Override
    public String toString() {
        return super.toString()
                + "{typeDepths=" + Arrays.toString(typeDepths) + '}';
    }

    public int bitsPerPixel() {
        int bitCount = 0;

        for (TypeDepth typeDepth : typeDepths) {
            bitCount += typeDepth.bitDepth;
        }

        return bitCount;
    }

    static class TypeDepth {
        final int type;
        final int bitDepth;

        TypeDepth(final int type, final int bitDepth) {
            this.type = type;
            this.bitDepth = bitDepth;
        }

        @Override
        public String toString() {
            return "TypeDepth{" +
                    "type=" + type +
                    ", bits=" + bitDepth +
                    '}';
        }
    }
}
