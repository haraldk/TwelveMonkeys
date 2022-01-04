/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Objects;

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

    private final int componentSize;

    ExtraSamplesColorModel(ColorSpace cs, boolean hasAlpha, boolean isAlphaPremultiplied, int dataType, int extraComponents) {
        super(cs, hasAlpha, isAlphaPremultiplied, Transparency.TRANSLUCENT, dataType);
        Validate.isTrue(extraComponents > 0, "Extra components must be > 0");
        this.numComponents = cs.getNumComponents() + (hasAlpha ? 1 : 0) + extraComponents;
        this.componentSize = getDataTypeSize(dataType);
    }

    @Override
    public int getNumComponents() {
        return numComponents;
    }

    @Override
    public int getComponentSize(int componentIdx) {
        return componentSize;
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

    @Override
    public Object getDataElements(final int rgb, final Object pixel) {
        return super.getDataElements(rgb, pixel == null ? createDataArray() : pixel);
    }

    private Object createDataArray() {
        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
                return new byte[numComponents];
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_USHORT:
                return new short[numComponents];
            case DataBuffer.TYPE_INT:
                return new int[numComponents];
            case DataBuffer.TYPE_FLOAT:
                return new float[numComponents];
            case DataBuffer.TYPE_DOUBLE:
                return new double[numComponents];
        }

        throw new IllegalArgumentException("This method has not been implemented for transferType " + transferType);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ExtraSamplesColorModel that = (ExtraSamplesColorModel) other;

        if (hasAlpha() != that.hasAlpha() ||
                isAlphaPremultiplied() != that.isAlphaPremultiplied() ||
                getPixelSize() != that.getPixelSize() ||
                getTransparency() != that.getTransparency() ||
                numComponents != that.numComponents) {
            return false;
        }

        int[] nBits = getComponentSize();
        int[] nb = that.getComponentSize();

        if ((nBits == null) || (nb == null)) {
            return ((nBits == null) && (nb == null));
        }

        for (int i = 0; i < nBits.length; i++) {
            if (nBits[i] != nb[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), numComponents, componentSize);
    }
}
