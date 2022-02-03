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
import java.awt.image.IndexColorModel;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * CMAPChunk
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: CMAPChunk.java,v 1.0 28.feb.2006 00:38:05 haku Exp$
 */
final class CMAPChunk extends IFFChunk {

//   typedef struct {
//      UBYTE red, green, blue;       /* color intensities 0..255 */
//   } ColorRegister;                 /* size = 3 bytes */
//
//   typedef ColorRegister ColorMap[n];  /* size = 3n bytes */

    byte[] reds;
    byte[] greens;
    byte[] blues;

    private IndexColorModel model;

    CMAPChunk(final int pChunkLength) {
        super(IFF.CHUNK_CMAP, pChunkLength);
    }

    public CMAPChunk(final IndexColorModel pModel) {
        super(IFF.CHUNK_CMAP, pModel.getMapSize() * 3);
        model = pModel;
    }

    @Override
    void readChunk(final DataInput input) throws IOException {
        int numColors = chunkLength / 3;

        reds = new byte[numColors];
        greens = reds.clone();
        blues = reds.clone();

        for (int i = 0; i < numColors; i++) {
            reds[i] = input.readByte();
            greens[i] = input.readByte();
            blues[i] = input.readByte();
        }

        // TODO: When reading in a CMAP for 8-bit-per-gun display or
        // manipulation, you may want to assume that any CMAP which has 0 values
        // for the low bits of all guns for all registers was stored shifted
        // rather than scaled, and provide your own scaling.
        // Use defaults if the color map is absent or has fewer color registers
        // than you need. Ignore any extra color registers.
        // R8 := (Rn x 255 ) / maxColor

        // All chunks are WORD aligned (even sized), may need to read pad...
        if (chunkLength % 2 != 0) {
            input.readByte();
        }
    }

    @Override
    void writeChunk(final DataOutput output) throws IOException {
        output.writeInt(chunkId);
        output.writeInt(chunkLength);

        final int length = model.getMapSize();

        for (int i = 0; i < length; i++) {
            output.writeByte(model.getRed(i));
            output.writeByte(model.getGreen(i));
            output.writeByte(model.getBlue(i));
        }

        if (chunkLength % 2 != 0) {
            output.writeByte(0); // PAD
        }
    }

    @Override
    public String toString() {
        return super.toString() + " {colorMap=" + model + "}";
    }

    public IndexColorModel getIndexColorModel(final Form.ILBMForm header) throws IIOException {
        if (model == null) {
            int numColors = reds.length; // All arrays are same size

            if (header.isEHB()) {
                if (numColors == 32) {
                    reds = Arrays.copyOf(reds,  numColors * 2);
                    blues = Arrays.copyOf(blues,  numColors * 2);
                    greens = Arrays.copyOf(greens,  numColors * 2);
                }
                else if (numColors != 64) {
                    throw new IIOException("Unknown number of colors for EHB: " + numColors);
                }

                // Create the half-brite colors
                // We do this regardless of the colors read, as the color map may contain trash values
                for (int i = 0; i < 32; i++) {
                    reds[i + 32] = (byte) ((reds[i] & 0xff) / 2);
                    greens[i + 32] = (byte) ((greens[i] & 0xff) / 2);
                    blues[i + 32] = (byte) ((blues[i] & 0xff) / 2);
                }
            }

            // TODO: Bitmask transparency
            // Would it work to double to numbers of colors, and create an indexcolormodel,
            // with alpha, where all colors above the original color is all transparent?
            // This is a waste of time and space, of course...
            int transparent = header.transparentIndex();
            int bitplanes = header.bitplanes() == 25 ? 8 : header.bitplanes();

            model = new IndexColorModel(bitplanes, reds.length, reds, greens, blues, transparent); // https://github.com/haraldk/TwelveMonkeys/issues/15
        }

        return model;
    }
}
