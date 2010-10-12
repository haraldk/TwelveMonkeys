package com.twelvemonkeys.image;

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.IOException;

/**
 * A factory for creating {@link BufferedImage}s backed by memory mapped files.
 * The data buffers will be allocated outside the normal JVM heap, allowing more efficient
 * memory usage for large images.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedImageFactory.java,v 1.0 May 26, 2010 5:07:01 PM haraldk Exp$
 */
public class MappedImageFactory {
    public static BufferedImage createCompatibleMappedImage(int width, int height, int type) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        return createCompatibleMappedImage(width, height, temp.getSampleModel().createCompatibleSampleModel(width, height), temp.getColorModel());
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, GraphicsConfiguration configuration, int transparency) throws IOException {
        // TODO: Should we also use the sample model?
        return createCompatibleMappedImage(width, height, configuration.getColorModel(transparency));
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, ImageTypeSpecifier type) throws IOException {
        return createCompatibleMappedImage(width, height, type.getSampleModel(width, height), type.getColorModel());
    }

    static BufferedImage createCompatibleMappedImage(int width, int height, ColorModel cm) throws IOException {
        return createCompatibleMappedImage(width, height, cm.createCompatibleSampleModel(width, height), cm);
    }

    static BufferedImage createCompatibleMappedImage(int width, int height, SampleModel sm, ColorModel cm) throws IOException {
        DataBuffer buffer = MappedFileBuffer.create(sm.getTransferType(), width * height * sm.getNumDataElements(), 1);
//        DataBuffer buffer = DirectBuffer.create(sm.getTransferType(), width * height * sm.getNumDataElements(), 1);

        // TODO: Figure out if it's better to use SunWritableRaster when available? -- Haven't seen any improvements yet
//        return new BufferedImage(cm, new SunWritableRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
        return new BufferedImage(cm, new GenericWritableRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
    }
}
