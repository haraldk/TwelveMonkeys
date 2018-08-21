package com.twelvemonkeys.imageio.plugins.pict;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * QuickDrawComposite
 */
interface QuickDrawComposite extends Composite {

    QuickDrawComposite NotSrcXor = new NotSrcXor();
    QuickDrawComposite AddMax = new AddMax();
    QuickDrawComposite AddMin = new AddMin();

    class NotSrcXor implements QuickDrawComposite {

        // TODO: Src can probably be any color model that can be encoded in PICT, dst is always RGB/TYPE_INT
        public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, RenderingHints hints) {
            {
                if (!srcColorModel.getColorSpace().isCS_sRGB() || !dstColorModel.getColorSpace().isCS_sRGB()) {
                    throw new IllegalArgumentException("Only sRGB supported");
                }
            }

            return new CompositeContext() {
                public void dispose() {
                }

                public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                    int width = min(src.getWidth(), dstIn.getWidth());

                    // We always work in RGB, using DataBuffer.TYPE_INT transfer type.
                    int[] srcData = null;
                    int[] dstData = null;
                    int[] resData = new int[width - src.getMinX()];

                    for (int y = src.getMinY(); y < src.getHeight(); y++) {
                        srcData = (int[]) src.getDataElements(src.getMinX(), y, width, 1, srcData);
                        dstData = (int[]) dstIn.getDataElements(src.getMinX(), y, width, 1, dstData);

                        for (int x = src.getMinX(); x < width; x++) {
                            // TODO: Decide how to handle alpha (if at all)
                            resData[x] = 0xff000000 | ((~srcData[x] ^ dstData[x])) & 0xffffff;
                        }

                        dstOut.setDataElements(src.getMinX(), y, width, 1, resData);
                    }
                }
            };
        }
    }

    class AddMax implements QuickDrawComposite {
        // TODO: Src can probably be any color model that can be encoded in PICT, dst is always RGB/TYPE_INT
        public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, RenderingHints hints) {
            {
                if (!srcColorModel.getColorSpace().isCS_sRGB() || !dstColorModel.getColorSpace().isCS_sRGB()) {
                    throw new IllegalArgumentException("Only sRGB supported");
                }
            }

            return new CompositeContext() {
                public void dispose() {
                }

                public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                    int width = min(src.getWidth(), dstIn.getWidth());

                    // We always work in RGB, using DataBuffer.TYPE_INT transfer type.
                    int[] srcData = null;
                    int[] dstData = null;

                    int[] resData = new int[width - src.getMinX()];

                    for (int y = src.getMinY(); y < src.getHeight(); y++) {
                        srcData = (int[]) src.getDataElements(src.getMinX(), y, width, 1, srcData);
                        dstData = (int[]) dstIn.getDataElements(src.getMinX(), y, width, 1, dstData);

                        for (int x = src.getMinX(); x < width; x++) {
                            int sAlpha = (srcData[x] >>> 24) & 0xFF;
                            int sRed = sAlpha * ((srcData[x] >> 16) & 0xFF) / 0xFF;
                            int sGreen = sAlpha * ((srcData[x] >> 8) & 0xFF) / 0xFF;
                            int sBlue = sAlpha * ((srcData[x]) & 0xFF) / 0xFF;

                            int dAlpha = (dstData[x] >>> 24) & 0xFF;
                            int dRed = dAlpha * ((dstData[x] >> 16) & 0xFF) / 0xFF;
                            int dGreen = dAlpha * ((dstData[x] >> 8) & 0xFF) / 0xFF;
                            int dBlue = dAlpha * ((dstData[x]) & 0xFF) / 0xFF;

                            resData[x] = (max(sAlpha, dAlpha) << 24)
                                         | (max(sRed, dRed) << 16)
                                         | (max(sGreen, dGreen) << 8)
                                         | (max(sBlue, dBlue));
                        }

                        dstOut.setDataElements(src.getMinX(), y, width, 1, resData);
                    }
                }
            };
        }
    }

    class AddMin implements QuickDrawComposite {
        // TODO: Src can probably be any color model that can be encoded in PICT, dst is always RGB/TYPE_INT
        public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, RenderingHints hints) {
            {
                if (!srcColorModel.getColorSpace().isCS_sRGB() || !dstColorModel.getColorSpace().isCS_sRGB()) {
                    throw new IllegalArgumentException("Only sRGB supported");
                }
            }

            return new CompositeContext() {
                public void dispose() {
                }

                public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                    int width = min(src.getWidth(), dstIn.getWidth());

                    // We always work in RGB, using DataBuffer.TYPE_INT transfer type.
                    int[] srcData = null;
                    int[] dstData = null;
                    int[] resData = new int[width - src.getMinX()];

                    for (int y = src.getMinY(); y < src.getHeight(); y++) {
                        srcData = (int[]) src.getDataElements(src.getMinX(), y, width, 1, srcData);
                        dstData = (int[]) dstIn.getDataElements(src.getMinX(), y, width, 1, dstData);

                        for (int x = src.getMinX(); x < width; x++) {
                            int sAlpha = (srcData[x] >>> 24) & 0xFF;
                            int sRed = sAlpha * ((srcData[x] >> 16) & 0xFF) / 0xFF;
                            int sGreen = sAlpha * ((srcData[x] >> 8) & 0xFF) / 0xFF;
                            int sBlue = sAlpha * ((srcData[x]) & 0xFF) / 0xFF;

                            int dAlpha = (dstData[x] >>> 24) & 0xFF;
                            int dRed = dAlpha * ((dstData[x] >> 16) & 0xFF) / 0xFF;
                            int dGreen = dAlpha * ((dstData[x] >> 8) & 0xFF) / 0xFF;
                            int dBlue = dAlpha * ((dstData[x]) & 0xFF) / 0xFF;

                            resData[x] = (min(sAlpha, dAlpha) << 24)
                                         | (min(sRed, dRed) << 16)
                                         | (min(sGreen, dGreen) << 8)
                                         | (min(sBlue, dBlue));
                        }

                        dstOut.setDataElements(src.getMinX(), y, width, 1, resData);
                    }
                }
            };
        }
    }
}
