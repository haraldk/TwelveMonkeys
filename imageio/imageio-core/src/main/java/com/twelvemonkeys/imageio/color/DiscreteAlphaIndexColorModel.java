/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.color;

import java.awt.*;
import java.awt.image.*;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * This class represents a hybrid between an {@link IndexColorModel} and a {@link ComponentColorModel},
 * having both a color map and a full, discrete alpha channel and/or one or more "extra" channels.
 * The color map entries are assumed to be fully opaque and should have no transparent index.
 * <p>
 * ColorSpace will always be the default sRGB color space (as with {@code IndexColorModel}).
 * <p>
 * Component order is always I, A, X<sub>1</sub>, X<sub>2</sub>... X<sub>n</sub>,
 * where I is a palette index, A is the alpha value and X<sub>n</sub> are extra samples (ignored for display).
 *
 * @see IndexColorModel
 * @see ComponentColorModel
 */
// TODO: ExtraSamplesIndexColorModel might be a better name?
// TODO: Allow specifying which channel is the transparency mask?
public final class DiscreteAlphaIndexColorModel extends ColorModel {
    // Our IndexColorModel delegate
    private final IndexColorModel icm;

    private final int samples;
    private final boolean hasAlpha;

    /**
     * Creates a {@code DiscreteAlphaIndexColorModel}, delegating color map look-ups
     * to the given {@code IndexColorModel}.
     *
     * @param icm The {@code IndexColorModel} delegate. Color map entries are assumed to be
     *            fully opaque, any transparency or transparent index will be ignored.
     */
    public DiscreteAlphaIndexColorModel(final IndexColorModel icm) {
        this(icm, 1, true);
    }

    /**
     * Creates a {@code DiscreteAlphaIndexColorModel}, delegating color map look-ups
     * to the given {@code IndexColorModel}.
     *
     * @param icm The {@code IndexColorModel} delegate. Color map entries are assumed to be
     *            fully opaque, any transparency or transparent index will be ignored.
     * @param extraSamples the number of extra samples in the color model.
     * @param hasAlpha {@code true} if the extra samples contains alpha, otherwise {@code false}.
     */
    public DiscreteAlphaIndexColorModel(final IndexColorModel icm, int extraSamples, boolean hasAlpha) {
        super(
                notNull(icm, "IndexColorModel").getPixelSize() * (1 + extraSamples),
                new int[] {icm.getPixelSize(), icm.getPixelSize(), icm.getPixelSize(), icm.getPixelSize()},
                icm.getColorSpace(), hasAlpha, false, hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
                icm.getTransferType()
        );

        this.icm = icm;
        this.samples = 1 + extraSamples;
        this.hasAlpha = hasAlpha;
    }

    @Override
    public int getNumComponents() {
        return samples;
    }

    @Override
    public final int getRed(final int pixel) {
        return icm.getRed(pixel);
    }

    @Override
    public final int getGreen(final int pixel) {
        return icm.getGreen(pixel);
    }

    @Override
    public final int getBlue(final int pixel) {
        return icm.getBlue(pixel);
    }

    @Override
    public final int getAlpha(final int pixel) {
        return hasAlpha ? (int) ((((float) pixel) / ((1 << getComponentSize(3))-1)) * 255.0f + 0.5f) : 0xff;
    }

    private int getSample(final Object inData, final int index) {
        int pixel;

        switch (transferType) {
            case DataBuffer.TYPE_BYTE:
                byte bdata[] = (byte[]) inData;
                pixel = bdata[index] & 0xff;
                break;
            case DataBuffer.TYPE_USHORT:
                short sdata[] = (short[]) inData;
                pixel = sdata[index] & 0xffff;
                break;
            case DataBuffer.TYPE_INT:
                int idata[] = (int[]) inData;
                pixel = idata[index];
                break;
            default:
                throw new UnsupportedOperationException("This method has not been implemented for transferType " + transferType);
        }

        return pixel;
    }

    @Override
    public final int getRed(final Object inData) {
        return getRed(getSample(inData, 0));
    }

    @Override
    public final int getGreen(final Object inData) {
        return getGreen(getSample(inData, 0));
    }

    @Override
    public final int getBlue(final Object inData) {
        return getBlue(getSample(inData, 0));
    }

    @Override
    public final int getAlpha(final Object inData) {
        return hasAlpha ? getAlpha(getSample(inData, 1)) : 0xff;
    }

    @Override
    public final SampleModel createCompatibleSampleModel(final int w, final int h) {
        return new PixelInterleavedSampleModel(transferType, w, h, samples, w * samples, createOffsets(samples));
    }

    private int[] createOffsets(int samples) {
        int[] offsets = new int[samples];

        for (int i = 0; i < samples; i++) {
            offsets[i] = i;
        }

        return offsets;
    }

    @Override
    public final boolean isCompatibleSampleModel(final SampleModel sm) {
        return sm instanceof PixelInterleavedSampleModel && sm.getNumBands() == samples;
    }

    @Override
    public final WritableRaster createCompatibleWritableRaster(final int w, final int h) {
        return Raster.createWritableRaster(createCompatibleSampleModel(w, h), new Point(0, 0));
    }

    @Override
    public final boolean isCompatibleRaster(final Raster raster) {
        int size = raster.getSampleModel().getSampleSize(0);
        return ((raster.getTransferType() == transferType) &&
                (raster.getNumBands() == samples) && ((1 << size) >= icm.getMapSize()));
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || obj != null && getClass() == obj.getClass() && icm.equals(((DiscreteAlphaIndexColorModel) obj).icm);
    }

    public String toString() {
        return "DiscreteAlphaIndexColorModel: #pixelBits = " + pixel_bits
                + " numComponents = " + getNumComponents()
                + " color space = " + getColorSpace()
                + " transparency = " + getTransparency()
                + " has alpha = " + hasAlpha()
                + " isAlphaPre = " + isAlphaPremultiplied();
    }
}
