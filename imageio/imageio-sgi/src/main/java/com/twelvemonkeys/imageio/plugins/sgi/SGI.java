package com.twelvemonkeys.imageio.plugins.sgi;

interface SGI {
    short MAGIC = 474; // 0x1da

    /** No compression, channels stored verbatim. */
    byte COMPRESSION_NONE = 0;
    /** Runlength encoed compression,
     * channels are prepended by one offset and length tables (one entry in each per scanline). */
    byte COMPRESSION_RLE = 1;

    /** Only ColorMode NORMAL should be used. */
    int COLORMODE_NORMAL = 0;
    /** Obsolete. */
    int COLORMODE_DITHERED = 1;
    /** Obsolete. */
    int COLORMODE_SCREEN = 2;
    /** Obsolete. */
    int COLORMODE_COLORMAP = 3;
}
