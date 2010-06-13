package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;

/**
 * MappedImageFactory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedImageFactory.java,v 1.0 May 26, 2010 5:07:01 PM haraldk Exp$
 */
public class MappedImageFactory {

    public static BufferedImage createCompatibleMappedImage(int width, int height, int type) throws IOException {
        return createCompatibleMappedImage(width, height, new BufferedImage(1, 1, type).getColorModel());
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, GraphicsConfiguration configuration, int transparency) throws IOException {
        ColorModel cm = configuration.getColorModel(transparency);
//        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
//        ColorModel cm = new ComponentColorModel(cs, ALPHA, false, ALPHA ? TRANSLUCENT : OPAQUE, DataBuffer.TYPE_BYTE);
        return createCompatibleMappedImage(width, height, cm);
    }

    private static BufferedImage createCompatibleMappedImage(int width, int height, ColorModel cm) throws IOException {
        SampleModel sm = cm.createCompatibleSampleModel(width, height);

//        System.err.println("cm: " + cm);
//        System.err.println("cm.getNumComponents(): " + cm.getNumComponents());
//        System.err.println("cm.getPixelSize(): " + cm.getPixelSize());
//        System.err.println("cm.getComponentSize(): " + Arrays.toString(cm.getComponentSize()));
//        System.err.println("sm.getNumDataElements(): " + sm.getNumDataElements());
//        System.err.println("sm.getNumBands(): " + sm.getNumBands());
//        System.err.println("sm.getSampleSize(): " + Arrays.toString(sm.getSampleSize()));

        DataBuffer buffer = MappedFileBuffer.create(sm.getTransferType(), width * height * sm.getNumDataElements(), 1);
//        DataBuffer buffer = sm.createDataBuffer();

        BufferedImage image = new BufferedImage(cm, new GenericWritableRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
//        BufferedImage image = new BufferedImage(cm, new SunWritableRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
//        BufferedImage image = new BufferedImage(cm, Raster.createWritableRaster(sm, buffer, null), false null);
        return image;
    }
}
