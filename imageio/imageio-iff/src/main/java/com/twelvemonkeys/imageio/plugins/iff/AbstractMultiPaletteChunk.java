/*
 * Copyright (c) 2012, Harald Kuhr
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
/*
 * Parts of this code is based on ilbmtoppm.c
 *
 * Copyright (C) 1989 by Jef Poskanzer.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation.  This software is provided "as is" without express or
 * implied warranty.
 *
 * Multipalette-support by Ingo Wilken (Ingo.Wilken@informatik.uni-oldenburg.de)
 */

package com.twelvemonkeys.imageio.plugins.iff;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * AbstractMultiPaletteChunk
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractMultiPaletteChunk.java,v 1.0 30.03.12 15:57 haraldk Exp$
 */
abstract class AbstractMultiPaletteChunk extends IFFChunk implements MultiPalette {
    /* scale factor maxval 15 -> maxval 255 */
    static final int FACTOR_4BIT = 17;

    protected MutableIndexColorModel.PaletteChange[] initialChanges;
    protected MutableIndexColorModel.PaletteChange[][] changes;

    protected int lastRow;
    protected WeakReference<IndexColorModel> originalPalette;
    protected MutableIndexColorModel mutablePalette;

    public AbstractMultiPaletteChunk(int pChunkId, int pChunkLength) {
        super(pChunkId, pChunkLength);
    }

    @Override
    void readChunk(final DataInput pInput) throws IOException {
        if (chunkId == IFF.CHUNK_SHAM) {
            pInput.readUnsignedShort(); // Version, typically 0, skipped
        }

        int rows = chunkLength / 32;    /* sizeof(word) * 16 */

        changes = new MutableIndexColorModel.PaletteChange[rows][];

        for (int row = 0; row < rows; row++) {
            changes[row] = new MutableIndexColorModel.PaletteChange[16];

            for (int i = 0; i < 16; i++) {
                changes[row][i] = new MutableIndexColorModel.PaletteChange();
            }

            for (int i = 0; i < 16; i++ ) {
                int data = pInput.readUnsignedShort();

                changes[row][i].index = i;
                changes[row][i].r = (byte) (((data & 0x0f00) >> 8) * FACTOR_4BIT);
                changes[row][i].g = (byte) (((data & 0x00f0) >> 4) * FACTOR_4BIT);
                changes[row][i].b = (byte) (((data & 0x000f)     ) * FACTOR_4BIT);
            }
        }
    }

    @Override
    void writeChunk(DataOutput pOutput) throws IOException {
        throw new UnsupportedOperationException("Method writeChunk not implemented");
    }


    public ColorModel getColorModel(final IndexColorModel colorModel, final int rowIndex, final boolean laced) {
        if (rowIndex < lastRow || mutablePalette == null || originalPalette != null && originalPalette.get() != colorModel) {
            originalPalette = new WeakReference<IndexColorModel>(colorModel);
            mutablePalette = new MutableIndexColorModel(colorModel);

            if (initialChanges != null) {
                mutablePalette.adjustColorMap(initialChanges);
            }
        }

        for (int i = lastRow + 1; i <= rowIndex; i++) {
            int row;

            if (laced && skipLaced()) {
                if (i % 2 != 0) {
                    continue;
                }

                row = i / 2;
            }
            else {
                row = i;
            }

            if (row < changes.length && changes[row] != null) {
                mutablePalette.adjustColorMap(changes[row]);
            }
        }

        return mutablePalette;
    }

    protected boolean skipLaced() {
        return false;
    }
}
