package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import static java.awt.image.DataBuffer.getDataTypeSize;

/**
 * ExtraSamplesColorModel.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ExtraSamplesColorModel.java,v 1.0 19/11/2017 harald.kuhr Exp$
 */
final class ExtraSamplesColorModel extends ComponentColorModel {
    // NOTE: This field shadows the package protected field in the super class.
    // This is just enough to get us past a few tricky situations, but the super class
    // still thinks it has numComponents == cs.getNumComponents() + 1 for most operations
    private final int numComponents;

    ExtraSamplesColorModel(ColorSpace cs, boolean hasAlpha, boolean isAlphaPremultiplied, int dataType, int extraComponents) {
        super(cs, bitsArrayHelper(cs, dataType, extraComponents + (hasAlpha ? 1 : 0)), hasAlpha, isAlphaPremultiplied, Transparency.TRANSLUCENT, dataType);
        Validate.isTrue(extraComponents > 0, "Extra components must be > 0");
        this.numComponents = cs.getNumComponents() + (hasAlpha ? 1 : 0) + extraComponents;
    }

    private static int[] bitsArrayHelper(ColorSpace cs, int dataType, int extraComponents) {
        int numBits = getDataTypeSize(dataType);
        int numComponents = cs.getNumComponents() + extraComponents;
        int[] bits = new int[numComponents];

        for (int i = 0; i < numComponents; i++) {
            bits[i] = numBits;
        }

        return bits;
    }

    @Override
    public int getNumComponents() {
        return numComponents;
    }

    @Override
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof ComponentSampleModel)) {
            return false;
        }

        // Must have the same number of components
        return numComponents == sm.getNumBands() && transferType == sm.getTransferType();
    }

    @Override
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        if (!hasAlpha()) {
            return null;
        }

        int x = raster.getMinX();
        int y = raster.getMinY();
        int[] band = new int[] {getAlphaComponent()};

        return raster.createWritableChild(x, y, raster.getWidth(), raster.getHeight(), x, y, band);
    }

    private int getAlphaComponent() {
        return super.getNumComponents() - 1;
    }
}
