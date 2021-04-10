package com.twelvemonkeys.imageio.plugins.webp;

import java.awt.*;
import java.awt.image.*;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * RasterUtils
 */
public final class RasterUtils {
    // TODO: Generalize and move to common util package

    private RasterUtils() {}

    public static WritableRaster asByteRaster(final WritableRaster raster, final ColorModel colorModel) {
        switch (raster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                return raster;
            case DataBuffer.TYPE_INT:
                final int bands = colorModel.getNumComponents();
                final DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();

                int w = raster.getWidth();
                int h = raster.getHeight();
                int size = buffer.getSize();

                return new WritableRaster(
                        new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, h, bands, w * bands, createBandOffsets(colorModel)),
                        new DataBuffer(DataBuffer.TYPE_BYTE, size * bands) {
                            // TODO: These masks should probably not be hardcoded
                            final int[] MASKS = {
                                    0xffffff00,
                                    0xffff00ff,
                                    0xff00ffff,
                                    0x00ffffff,
                            };

                            @Override
                            public int getElem(int bank, int i) {
                                int index = i / bands;
                                int shift = (i % bands) * 8;

                                return (buffer.getElem(index) >>> shift) & 0xff;
                            }

                            @Override
                            public void setElem(int bank, int i, int val) {
                                int index = i / bands;
                                int element = i % bands;
                                int shift = element * 8;

                                int value = (buffer.getElem(index) & MASKS[element]) | ((val & 0xff) << shift);
                                buffer.setElem(index, value);
                            }
                        }, new Point()) {};
            default:
                throw new IllegalArgumentException(String.format("Raster type %d not supported", raster.getTransferType()));
        }
    }

    private static int[] createBandOffsets(final ColorModel colorModel) {
        notNull(colorModel, "colorModel");

        if (colorModel instanceof DirectColorModel) {
            DirectColorModel dcm = (DirectColorModel) colorModel;
            int[] masks = dcm.getMasks();
            int[] offs = new int[masks.length];

            for (int i = 0; i < masks.length; i++) {
                int mask = masks[i];
                int off = 0;

                // TODO: FixMe! This only works for standard 8 bit masks (0xFF)
                if (mask != 0) {
                    while ((mask & 0xFF) == 0) {
                        mask >>>= 8;
                        off++;
                    }
                }

                offs[i] = off;
            }

            return offs;
        }

        throw new IllegalArgumentException(String.format("%s not supported", colorModel.getClass().getSimpleName()));
    }
}
