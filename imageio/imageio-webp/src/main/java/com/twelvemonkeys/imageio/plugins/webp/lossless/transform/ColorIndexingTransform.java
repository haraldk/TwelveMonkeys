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
