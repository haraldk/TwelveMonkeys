/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

import javax.imageio.IIOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * BMHDChunk
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: BMHDChunk.java,v 1.0 28.feb.2006 00:04:32 haku Exp$
 */
final class BMHDChunk extends IFFChunk {
//
//    typedef UBYTE Masking;  /* Choice of masking technique. */
//
//   #define mskNone   0
//   #define mskHasMask   1
//   #define mskHasTransparentColor   2
//   #define mskLasso  3
//
//   typedef UBYTE Compression;    /* Choice of compression algorithm
//      applied to the rows of all source and mask planes.  "cmpByteRun1"
//      is the byte run encoding described in Appendix C.  Do not compress
//      across rows! */
//   #define cmpNone   0
//   #define cmpByteRun1  1
//
//   typedef struct {
//      UWORD w, h;             /* raster width & height in pixels      */
//      WORD  x, y;             /* pixel position for this image        */
//      UBYTE nPlanes;          /* # source bitplanes                   */
//      Masking masking;
//      Compression compression;
//      UBYTE pad1;             /* unused; ignore on read, write as 0   */
//      UWORD transparentColor; /* transparent "color number" (sort of) */
//      UBYTE xAspect, yAspect; /* pixel aspect, a ratio width : height */
//      WORD  pageWidth, pageHeight;  /* source "page" size in pixels   */
//   } BitMapHeader;*/

    static final int MASK_NONE = 0;
    static final int MASK_HAS_MASK = 1;
    static final int MASK_TRANSPARENT_COLOR = 2;
    static final int MASK_LASSO = 3;

    static final int COMPRESSION_NONE = 0;
    // RLE
    static final int COMPRESSION_BYTE_RUN = 1;

    // NOTE:  Each row of the image is stored in an integral number of 16 bit
    // words. The number of words per row is words=((w+15)/16)

    // Dimensions of raster
    int width;
    int height;

    // Source offsets
    // Hmm.. Consider making these Image.properties?
    int xPos;
    int yPos;

    // The number of source bitplanes in the BODY chunk (see below) is stored in
    // nPlanes. An ILBM with a CMAP but no BODY and nPlanes = 0 is the
    // recommended way to store a color map.
    int bitplanes;

    int maskType;
    int compressionType;

    int transparentIndex;

    // NOTE: Typical values are 10:11 (320 x 200)
    int xAspect;
    int yAspect;

    // Source page dimension
    // NOTE: The image may be larger than the page, probably ignore these
    int pageWidth;
    int pageHeight;

    protected BMHDChunk(int pChunkLength) {
        super(IFF.CHUNK_BMHD, pChunkLength);
    }

    protected BMHDChunk(int pWidth, int pHeight, int pBitplanes, int pMaskType, int pCompressionType, int pTransparentIndex) {
        super(IFF.CHUNK_BMHD, 20);
        width = pWidth;
        height = pHeight;
        xPos = 0;
        yPos = 0;
        bitplanes = pBitplanes;
        maskType = pMaskType;
        compressionType = pCompressionType;
        transparentIndex = pTransparentIndex;
        xAspect = 1;
        yAspect = 1;
        pageWidth = Math.min(pWidth, Short.MAX_VALUE); // For some reason, these are signed?
        pageHeight = Math.min(pHeight, Short.MAX_VALUE);
    }

    void readChunk(DataInput pInput) throws IOException {
        if (chunkLength != 20) {
            throw new IIOException("Unknown BMHD chunk length: " + chunkLength);
        }
        width = pInput.readUnsignedShort();
        height = pInput.readUnsignedShort();
        xPos = pInput.readShort();
        yPos = pInput.readShort();
        bitplanes = pInput.readUnsignedByte();
        maskType = pInput.readUnsignedByte();
        compressionType = pInput.readUnsignedByte();
        pInput.readByte(); // PAD
        transparentIndex = pInput.readUnsignedShort();
        xAspect = pInput.readUnsignedByte();
        yAspect = pInput.readUnsignedByte();
        pageWidth = pInput.readShort();
        pageHeight = pInput.readShort();
    }

    void writeChunk(DataOutput pOutput) throws IOException {
        pOutput.writeInt(chunkId);
        pOutput.writeInt(chunkLength);

        pOutput.writeShort(width);
        pOutput.writeShort(height);
        pOutput.writeShort(xPos);
        pOutput.writeShort(yPos);
        pOutput.writeByte(bitplanes);
        pOutput.writeByte(maskType);
        pOutput.writeByte(compressionType);
        pOutput.writeByte(0); // PAD
        pOutput.writeShort(transparentIndex);
        pOutput.writeByte(xAspect);
        pOutput.writeByte(yAspect);
        pOutput.writeShort(pageWidth);
        pOutput.writeShort(pageHeight);
    }

    public String toString() {
        return super.toString()
                + " {w=" + width + ", h=" + height
                + ", x=" + xPos + ", y=" + yPos
                + ", planes=" + bitplanes + ", mask=" + maskType
                + ", compression=" + compressionType + ", trans=" + transparentIndex
                + ", xAspect=" + xAspect + ", yAspect=" + yAspect
                + ", pageWidth=" + pageWidth + ", pageHeight=" + pageHeight + "}";
    }
}
