package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

import java.awt.image.*;

public class SubtractGreenTransform implements Transform {


    private static void addGreenToBlueAndRed(byte[] rgb) {
        rgb[0] = (byte) ((rgb[0] + rgb[1]) & 0xff);
        rgb[2] = (byte) ((rgb[2] + rgb[1]) & 0xff);
    }

    @Override
    public void applyInverse(WritableRaster raster) {

        int width = raster.getWidth();
        int height = raster.getHeight();

        byte[] rgba = new byte[4];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.getDataElements(x, y, rgba);
                addGreenToBlueAndRed(rgba);
                raster.setDataElements(x, y, rgba);
            }
        }
    }
}
