/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pcx;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

final class CGAColorModel {

    // http://en.wikipedia.org/wiki/Color_Graphics_Adapter#Color_palette
    private static final int[] CGA_PALETTE = {
            // black, blue,     green,    cyan,     red,      magenta,  brown,    light gray
            0x000000, 0x0000aa, 0x00aa00, 0x00aaaa, 0xaa0000, 0xaa00aa, 0xaa5500, 0xaaaaaa,
            // gray,  light b,  light g,  light c,  light r,  light m,  yellow,   white
            0x555555, 0x5555ff, 0x55ff55, 0x55ffff, 0xff5555, 0xff55ff, 0xffff55, 0xffffff
    };

    static IndexColorModel create(final byte[] cgaMode, final int bitsPerPixel) {
        int[] cmap = new int[1 << bitsPerPixel];

        byte byte0 = cgaMode[0];
        int background = (byte0 & 0xf0) >> 4;
        cmap[0] = CGA_PALETTE[background];

        if (bitsPerPixel == 1) {
            // Monochrome
            cmap[1] = CGA_PALETTE[0];
        }
        else {
            // Configured palette
            byte byte3 = cgaMode[3];

            boolean colorBurstEnable = (byte3 & 0x80) != 0;
            boolean paletteValue = (byte3 & 0x40) != 0;
            boolean intensityValue = (byte3 & 0x20) != 0;

            if (PCXImageReader.DEBUG) {
                System.err.println("colorBurstEnable: " + colorBurstEnable);
                System.err.println("paletteValue: " + paletteValue);
                System.err.println("intensityValue: " + intensityValue);
            }

            // Set up the fixed part of the palette
            if (paletteValue) {
                if (intensityValue) {
                    cmap[1] = CGA_PALETTE[11];
                    cmap[2] = colorBurstEnable ? CGA_PALETTE[13] : CGA_PALETTE[12];
                    cmap[3] = CGA_PALETTE[15];
                } else {
                    cmap[1] = CGA_PALETTE[3];
                    cmap[2] = colorBurstEnable ? CGA_PALETTE[5] : CGA_PALETTE[4];
                    cmap[3] = CGA_PALETTE[7];
                }
            } else {
                if (intensityValue) {
                    cmap[1] = CGA_PALETTE[10];
                    cmap[2] = CGA_PALETTE[12];
                    cmap[3] = CGA_PALETTE[14];
                } else {
                    cmap[1] = CGA_PALETTE[2];
                    cmap[2] = CGA_PALETTE[4];
                    cmap[3] = CGA_PALETTE[6];
                }
            }
        }

        return new IndexColorModel(bitsPerPixel, cmap.length, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
    }
}
