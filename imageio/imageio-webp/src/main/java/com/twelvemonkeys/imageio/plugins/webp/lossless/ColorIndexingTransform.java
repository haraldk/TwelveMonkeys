/*
 * Copyright (c) 2022, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp.lossless;

import java.awt.image.*;

/**
 * @author Simon Kammermeier
 */
final class ColorIndexingTransform implements Transform {

    private final byte[] colorTable;
    private final byte bits;

    public ColorIndexingTransform(byte[] colorTable, byte bits) {
        this.colorTable = colorTable;
        this.bits = bits;
    }

    @Override
    public void applyInverse(WritableRaster raster) {

        int width = raster.getWidth();
        int height = raster.getHeight();

        byte[] rgba = new byte[4];

        for (int y = 0; y < height; y++) {
            //Reversed so no used elements are overridden (in case of packing)
            for (int x = width - 1; x >= 0; x--) {

                int componentSize = 8 >> bits;
                int packed = 1 << bits;
                int xC = x / packed;
                int componentOffset = componentSize * (x % packed);

                int sample = raster.getSample(xC, y, 1);

                int index = sample >> componentOffset & ((1 << componentSize) - 1);

                //Arraycopy for 4 elements might not be beneficial
                System.arraycopy(colorTable, index * 4, rgba, 0, 4);
                raster.setDataElements(x, y, rgba);

            }
        }
    }
}
