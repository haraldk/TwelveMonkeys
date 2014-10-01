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

            System.err.printf("background: %d\n", background);
            System.err.printf("cgaMode: %02x\n", (byte3 & 0xff));
            System.err.printf("cgaMode: %d\n", (byte3 & 0x80) >> 7);
            System.err.printf("cgaMode: %d\n", (byte3 & 0x40) >> 6);
            System.err.printf("cgaMode: %d\n", (byte3 & 0x20) >> 5);

            boolean colorBurstEnable = (byte3 & 0x80) == 0;
            boolean paletteValue = (byte3 & 0x40) != 0;
            boolean intensityValue = (byte3 & 0x20) != 0;

            System.err.println("colorBurstEnable: " + colorBurstEnable);
            System.err.println("paletteValue: " + paletteValue);
            System.err.println("intensityValue: " + intensityValue);

            // Set up the fixed part of the palette
            if (colorBurstEnable) {
                if (paletteValue) {
                    if (intensityValue) {
                        cmap[1] = CGA_PALETTE[11];
                        cmap[2] = CGA_PALETTE[13];
                        cmap[3] = CGA_PALETTE[15];
                    } else {
                        cmap[1] = CGA_PALETTE[3];
                        cmap[2] = CGA_PALETTE[5];
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
            } else {
                if (intensityValue) {
                    cmap[1] = CGA_PALETTE[11];
                    cmap[2] = CGA_PALETTE[12];
                    cmap[3] = CGA_PALETTE[15];
                } else {
                    cmap[1] = CGA_PALETTE[4];
                    cmap[2] = CGA_PALETTE[5];
                    cmap[3] = CGA_PALETTE[7];
                }
            }
        }

        return new IndexColorModel(bitsPerPixel, cmap.length, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
    }
}
