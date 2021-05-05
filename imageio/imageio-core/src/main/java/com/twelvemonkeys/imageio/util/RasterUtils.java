/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import java.awt.*;
import java.awt.image.*;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * A class containing various raster utility methods.
 */
public final class RasterUtils {

    private RasterUtils() {
    }

    /**
     * Returns a raster with {@code DataBuffer.TYPE_BYTE} transfer type.
     * Works for any raster from a {@code BufferedImage.TYPE_INT_*} image
     *
     * @param raster a {@code Raster} with either transfer type {@code DataBuffer.TYPE_BYTE}
     *               or {@code DataBuffer.TYPE_INT} with `SinglePixelPackedSampleModel`, not {@code null}.
     * @return a raster with {@code DataBuffer.TYPE_BYTE} transfer type.
     * @throws IllegalArgumentException if {@code raster} does not have transfer type {@code DataBuffer.TYPE_BYTE}
     * or {@code DataBuffer.TYPE_INT} with `SinglePixelPackedSampleModel`
     * @throws NullPointerException if {@code raster} is {@code null}.
     */
    public static Raster asByteRaster(final Raster raster) {
        return asByteRaster0(raster);
    }

    /**
     * Returns a writable raster with {@code DataBuffer.TYPE_BYTE} transfer type.
     * Works for any raster from a {@code BufferedImage.TYPE_INT_*} image.
     *
     * @param raster a {@code WritableRaster} with either transfer type {@code DataBuffer.TYPE_BYTE}
     *               or {@code DataBuffer.TYPE_INT} with `SinglePixelPackedSampleModel`, not {@code null}.
     * @return a writable raster with {@code DataBuffer.TYPE_BYTE} transfer type.
     * @throws IllegalArgumentException if {@code raster} does not have transfer type {@code DataBuffer.TYPE_BYTE}
     * or {@code DataBuffer.TYPE_INT} with `SinglePixelPackedSampleModel`
     * @throws NullPointerException if {@code raster} is {@code null}.
     */
    public static WritableRaster asByteRaster(final WritableRaster raster) {
        return (WritableRaster) asByteRaster0(raster);
    }

    private static Raster asByteRaster0(final Raster raster) {
        switch (raster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                return raster;
            case DataBuffer.TYPE_INT:
                SampleModel sampleModel = raster.getSampleModel();

                if (!(sampleModel instanceof SinglePixelPackedSampleModel)) {
                    throw new IllegalArgumentException(String.format("Requires SinglePixelPackedSampleModel, %s not supported", sampleModel.getClass().getSimpleName()));
                }

                final int bands = 4;
                final DataBufferInt buffer = (DataBufferInt) raster.getDataBuffer();

                int w = raster.getWidth();
                int h = raster.getHeight();
                int size = buffer.getSize();

                return new WritableRaster(
                        new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, h, bands, w * bands, createBandOffsets((SinglePixelPackedSampleModel) sampleModel)),
                        new DataBuffer(DataBuffer.TYPE_BYTE, size * bands) {
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
                        }, new Point()) {
                };

            default:
                throw new IllegalArgumentException(String.format("Raster type %d not supported", raster.getTransferType()));
        }
    }

    private static int[] createBandOffsets(final SinglePixelPackedSampleModel sampleModel) {
        notNull(sampleModel, "sampleModel");

        int[] masks = sampleModel.getBitMasks();
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
}
