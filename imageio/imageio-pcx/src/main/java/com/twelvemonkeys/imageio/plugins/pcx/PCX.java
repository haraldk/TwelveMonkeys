package com.twelvemonkeys.imageio.plugins.pcx;

interface PCX {
    byte MAGIC = 0x0A;

    int HEADER_SIZE = 128;

    byte VERSION_2_5 = 0;
    byte VERSION_2_8_PALETTE = 2;
    byte VERSION_2_8_NO_PALETTE = 3;
    byte VERSION_2_X_WINDOWS = 4;
    byte VERSION_3 = 5;

    /** No compression, channels stored verbatim. */
    byte COMPRESSION_NONE = 0;
    /** Runlength encoed compression,
     * channels are prepended by one offset and length tables (one entry in each per scanline). */
    byte COMPRESSION_RLE = 1;

    /** Color or BW. */
    int PALETTEINFO_COLOR = 1;
    /** Gray. */
    int PALETTEINFO_GRAY = 2;

    /** Magic identifier for VGA palette. */
    byte VGA_PALETTE_MAGIC = 0x0c;
}
