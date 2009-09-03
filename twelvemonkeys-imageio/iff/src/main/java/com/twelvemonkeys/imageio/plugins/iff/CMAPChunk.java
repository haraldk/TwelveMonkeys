/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.image.InverseColorMapIndexColorModel;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * CMAPChunk
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: CMAPChunk.java,v 1.0 28.feb.2006 00:38:05 haku Exp$
 */
class CMAPChunk extends IFFChunk {

//   typedef struct {
//      UBYTE red, green, blue;       /* color intensities 0..255 */
//   } ColorRegister;                 /* size = 3 bytes */
//
//   typedef ColorRegister ColorMap[n];  /* size = 3n bytes */


    byte[] mReds;
    byte[] mGreens;
    byte[] mBlues;

    boolean mEHB;

    final private BMHDChunk mHeader;
    final private CAMGChunk mCamg;
    private IndexColorModel mModel;

    protected CMAPChunk(int pChunkLength, BMHDChunk pHeader, CAMGChunk pCamg) {
        super(IFF.CHUNK_CMAP, pChunkLength);
        mHeader = pHeader;
        mCamg = pCamg;
    }

    public CMAPChunk(IndexColorModel pModel) {
        super(IFF.CHUNK_CMAP, pModel.getMapSize() * 3);
        mModel = pModel;
        mHeader = null;
        mCamg = null;
    }

    void readChunk(DataInput pInput) throws IOException {
        int numColors = mChunkLength / 3;
        int paletteSize = numColors;

        boolean isEHB = mCamg != null && mCamg.isEHB();
        if (isEHB) {
            if (numColors == 32) {
                paletteSize = 64;
            }
            else {
                throw new IIOException("Unknown number of colors for EHB: " + numColors);
            }
        }

        mReds = new byte[paletteSize];
        mGreens = mReds.clone();
        mBlues = mReds.clone();

        for (int i = 0; i < numColors; i++) {
            mReds[i] = pInput.readByte();
            mGreens[i] = pInput.readByte();
            mBlues[i] = pInput.readByte();
        }
        if (isEHB) {
            for (int i = 0; i < numColors; i++) {
                mReds[i + numColors] = (byte) ((mReds[i] & 0xff) / 2);
                mGreens[i + numColors] = (byte) ((mGreens[i] & 0xff) / 2);
                mBlues[i + numColors] = (byte) ((mBlues[i] & 0xff) / 2);
            }
        }

        // TODO: When reading in a CMAP for 8-bit-per-gun display or
        // manipulation, you may want to assume that any CMAP which has 0 values
        // for the low bits of all guns for all registers was stored shifted
        // rather than scaled, and provide your own scaling.
        // Use defaults if the color map is absent or has fewer color registers
        // than you need. Ignore any extra color registers.

        // R8 := (Rn x 255 ) / maxColor

        // All chunks are WORD aligned (even sized), may need to read pad...
        if (mChunkLength % 2 != 0) {
            pInput.readByte();
        }

        // TODO: Bitmask transparency
        // Would it work to double to numbers of colors, and create an indexcolormodel,
        // with alpha, where all colors above the original color is all transparent?
        // This is a waste of time and space, of course...
        int trans = mHeader.mMaskType == BMHDChunk.MASK_TRANSPARENT_COLOR ? mHeader.mTransparentIndex : -1;
        mModel = new InverseColorMapIndexColorModel(mHeader.mBitplanes, mReds.length, mReds, mGreens, mBlues, trans);
    }

    void writeChunk(DataOutput pOutput) throws IOException {
        pOutput.writeInt(mChunkId);
        pOutput.writeInt(mChunkLength);

        final int length = mModel.getMapSize();

        for (int i = 0; i < length; i++) {
            pOutput.writeByte(mModel.getRed(i));
            pOutput.writeByte(mModel.getGreen(i));
            pOutput.writeByte(mModel.getBlue(i));
        }

        if (mChunkLength % 2 != 0) {
            pOutput.writeByte(0); // PAD
        }
    }

    public String toString() {
        return super.toString() + " {colorMap=" + mModel + "}";
    }

    IndexColorModel getIndexColorModel() {
        return mModel;
    }

    public BufferedImage createPaletteImage() {
        // Create a 1 x colors.length image
        IndexColorModel cm = getIndexColorModel();
        WritableRaster raster = cm.createCompatibleWritableRaster(cm.getMapSize(), 1);
        byte[] pixel = null;
        for (int x = 0; x < cm.getMapSize(); x++) {
            pixel = (byte[]) cm.getDataElements(cm.getRGB(x), pixel);
            raster.setDataElements(x, 0, pixel);
        }

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }
}
