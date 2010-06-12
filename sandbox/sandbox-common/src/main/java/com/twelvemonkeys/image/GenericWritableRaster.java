package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * GenericWritableRaster
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
        return String.format("%s@%x: w = %s h = %s", getClass().getSimpleName(), System.identityHashCode(this), getWidth(), getHeight());
    }
}
