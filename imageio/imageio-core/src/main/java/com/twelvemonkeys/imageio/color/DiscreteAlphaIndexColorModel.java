package com.twelvemonkeys.imageio.color;

import java.awt.*;
import java.awt.image.*;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * This class represents a hybrid between an {@link IndexColorModel} and a {@link ComponentColorModel},
 * having both a color map and a full, discrete alpha channel.
 * The color map entries are assumed to be fully opaque and should have no transparent index.
 * <p>
 * ColorSpace will always be the default sRGB color space (as with {@code IndexColorModel}).
 * <p>
 * Component order is always P, A, where P is a palette index, and A is the alpha value.
 *
 * @see IndexColorModel
 * @see ComponentColorModel
 */
public final class DiscreteAlphaIndexColorModel extends ColorModel {
    // Our IndexColorModel delegate
    private final IndexColorModel icm;

    /**
     * Creates a {@code DiscreteAlphaIndexColorModel}, delegating color map look-ups
     * to the given {@code IndexColorModel}.
     *
     * @param icm The {@code IndexColorModel} delegate. Color map entries are assumed to be
     *            fully opaque, any transparency or transparent index will be ignored.
     */
    public DiscreteAlphaIndexColorModel(final IndexColorModel icm) {
        super(
                notNull(icm, "IndexColorModel").getPixelSize() * 2,
                new int[] {icm.getPixelSize(), icm.getPixelSize(), icm.getPixelSize(), icm.getPixelSize()},
                icm.getColorSpace(), true, false, Transparency.TRANSLUCENT, icm.getTransferType()
        );

        this.icm = icm;
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
        return (int) ((((float) pixel) / ((1 << getComponentSize(3))-1)) * 255.0f + 0.5f);
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
        return getAlpha(getSample(inData, 1));
    }

    @Override
    public final SampleModel createCompatibleSampleModel(final int w, final int h) {
        return new PixelInterleavedSampleModel(transferType, w, h, 2, w * 2, new int[] {0, 1});
    }

    @Override
    public final boolean isCompatibleSampleModel(final SampleModel sm) {
        return sm instanceof PixelInterleavedSampleModel && sm.getNumBands() == 2;
    }

    @Override
    public final WritableRaster createCompatibleWritableRaster(final int w, final int h) {
        return Raster.createWritableRaster(createCompatibleSampleModel(w, h), new Point(0, 0));
    }

    @Override
    public final boolean isCompatibleRaster(final Raster raster) {
        int size = raster.getSampleModel().getSampleSize(0);
        return ((raster.getTransferType() == transferType) &&
                (raster.getNumBands() == 2) && ((1 << size) >= icm.getMapSize()));
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
