package com.twelvemonkeys.imageio.plugins.tga;

interface TGA {
    /** Fixed header size: 18.*/
    int HEADER_SIZE = 18;

    /** No color map included. */
    int COLORMAP_NONE = 0;
    /** Color map included. */
    int COLORMAP_PALETTE = 1;

    /** No image data included. */
    int IMAGETYPE_NONE = 0;
    /** Uncompressed, color-mapped images. */
    int IMAGETYPE_COLORMAPPED = 1;
    /** Uncompressed, RGB images. */
    int IMAGETYPE_TRUECOLOR = 2;
    /** Uncompressed, black and white images. */
    int IMAGETYPE_MONOCHROME = 3;
    /** Runlength encoded color-mapped images. */
    int IMAGETYPE_COLORMAPPED_RLE = 9;
    /** Runlength encoded RGB images. */
    int IMAGETYPE_TRUECOLOR_RLE = 10;
    /** Compressed, black and white images. */
    int IMAGETYPE_MONOCHROME_RLE = 11;

    /* From http://www.gamers.org/dEngine/quake3/TGA.txt: */
    /** Compressed color-mapped data, using Huffman, Delta, and runlength encoding. */
    int IMAGETYPE_COLORMAPPED_HUFFMAN = 32;
    /** Compressed color-mapped data, using Huffman, Delta, and runlength encoding.  4-pass quadtree-type process. */
    int IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE = 33;

    /* Only origin lower left and upper left supported. */
    int ORIGIN_LOWER_LEFT = 0;
    int ORIGIN_LOWER_RIGHT = 1;
    int ORIGIN_UPPER_LEFT = 2;
    int ORIGIN_UPPER_RIGHT = 3;

    /* From http://www.gamers.org/dEngine/quake3/TGA.txt: */
    int INTERLEAVED_NON_INTERLEAVED = 0;
    int INTERLEAVED_TWO_WAY = 1;
    int INTERLEAVED_FOUR_WAY = 2;
    // The value 3 is reserved...
}
