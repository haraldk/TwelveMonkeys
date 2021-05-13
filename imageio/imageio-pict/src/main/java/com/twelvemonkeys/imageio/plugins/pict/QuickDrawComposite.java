/*
 * Copyright (c) 2018, Harald Kuhr
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
