package com.twelvemonkeys.imageio.plugins.webp.lossless.huffman;

import java.awt.image.*;

public class HuffmanInfo {
    public Raster huffmanMetaCodes; //Raster allows intuitive lookup by x and y

    public int metaCodeBits;

    public HuffmanCodeGroup[] huffmanGroups;

    public HuffmanInfo(Raster huffmanMetaCodes, int metaCodeBits, HuffmanCodeGroup[] huffmanGroups) {
        this.huffmanMetaCodes = huffmanMetaCodes;
        this.metaCodeBits = metaCodeBits;
        this.huffmanGroups = huffmanGroups;
    }
}