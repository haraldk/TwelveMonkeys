package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * A generic writable raster.
 * For use when factory methods from {@link java.awt.image.Raster} can't be used,
 * typically because of custom data buffers.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: GenericWritableRaster.java,v 1.0 Jun 13, 2010 12:27:45 AM haraldk Exp$
 */
class GenericWritableRaster extends WritableRaster {
    public GenericWritableRaster(final SampleModel model, final DataBuffer buffer, final Point origin) {
        super(model, buffer, origin);
    }

    @Override
    public String toString() {
        return String.format(
                "%s: %s width = %s height = %s #Bands = %s xOff = %s yOff = %s %s",
                getClass().getSimpleName(),
                sampleModel,
                getWidth(), getHeight(), getNumBands(),
                sampleModelTranslateX, sampleModelTranslateY,
                dataBuffer
        );
    }
}
