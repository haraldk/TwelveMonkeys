package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

import java.awt.image.*;

public class ColorIndexingTransform implements Transform {

    private final byte[] colorTable;
    private final byte bits;

    public ColorIndexingTransform(byte[] colorTable, byte bits) {
        this.colorTable = colorTable;
        this.bits = bits;
    }

    @Override
    public void applyInverse(WritableRaster raster) {
    }
}
