package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

import java.awt.image.*;

public class SubtractGreenTransform implements Transform {


    private static void addGreenToBlueAndRed(byte[] rgb) {
        rgb[0] = (byte) ((rgb[0] + rgb[1]) & 0xff);
        rgb[2] = (byte) ((rgb[2] + rgb[1]) & 0xff);
    }

    @Override
    public void applyInverse(WritableRaster raster) {
    }
}
