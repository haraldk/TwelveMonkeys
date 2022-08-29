package com.twelvemonkeys.imageio.plugins.webp.lossless.huffman;

import com.twelvemonkeys.imageio.plugins.webp.LSBBitReader;

import java.io.IOException;

public class HuffmanCodeGroup {
    /**
     * Used for green, backward reference length and color cache
     */
    public final HuffmanTable mainCode;

    public final HuffmanTable redCode;
    public final HuffmanTable blueCode;
    public final HuffmanTable alphaCode;
    public final HuffmanTable distanceCode;

    public HuffmanCodeGroup(LSBBitReader lsbBitReader, int colorCacheBits) throws IOException {
        mainCode = new HuffmanTable(lsbBitReader, 256 + 24 + (colorCacheBits > 0 ? 1 << colorCacheBits : 0));
        redCode = new HuffmanTable(lsbBitReader, 256);
        blueCode = new HuffmanTable(lsbBitReader, 256);
        alphaCode = new HuffmanTable(lsbBitReader, 256);
        distanceCode = new HuffmanTable(lsbBitReader, 40);
    }
}
