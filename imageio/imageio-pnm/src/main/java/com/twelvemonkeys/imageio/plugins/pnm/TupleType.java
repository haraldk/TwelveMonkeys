/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;

enum TupleType {
    // Official:
    /** B/W, but uses 1 byte (8 bits) per pixel. Black is zero (opposite of PBM) */
    BLACKANDWHITE(1, 1, PNM.MAX_VAL_1BIT, Transparency.OPAQUE),
    /** B/W + bit mask, uses 2 bytes per pixel. Black is zero (opposite of PBM) */
    BLACKANDWHITE_ALPHA(2, PNM.MAX_VAL_1BIT, PNM.MAX_VAL_1BIT, Transparency.BITMASK),
    /** Grayscale, as PGM. */
    GRAYSCALE(1, 2, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** Grayscale + alpha. YA order. */
    GRAYSCALE_ALPHA(2, 2, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),
    /** RGB color, as PPM. RGB order. */
    RGB(3, 1, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** RGB color + alpha. RGBA order. */
    RGB_ALPHA(4, 1, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),

    // De facto (documented on the interwebs):
    /** CMYK color. CMYK order. */
    CMYK(4, 2, PNM.MAX_VAL_16BIT, Transparency.OPAQUE),
    /** CMYK color + alpha. CMYKA order. */
    CMYK_ALPHA(5, 1, PNM.MAX_VAL_16BIT, Transparency.TRANSLUCENT),

    // Custom for PBM compatibility
    /** 1 bit B/W. White is zero (as PBM) */
    BLACKANDWHITE_WHITE_IS_ZERO(1, 1, PNM.MAX_VAL_1BIT, Transparency.OPAQUE);

    private final int samplesPerPixel;
    private final int minMaxSample;
    private final int maxMaxSample;
    private final int transparency;

    TupleType(int samplesPerPixel, int minMaxSample, int maxMaxSample, int transparency) {
        this.samplesPerPixel = samplesPerPixel;
        this.minMaxSample = minMaxSample;
        this.maxMaxSample = maxMaxSample;
        this.transparency = transparency;
    }

    public int getTransparency() {
        return transparency;
    }

    public int getSamplesPerPixel() {
        return samplesPerPixel;
    }

    public boolean isValidMaxSample(int maxSample) {
        return maxSample >= minMaxSample && maxSample <= maxMaxSample;
    }

    
    static TupleType forPNM(Raster raster) {
        return filterPNM(forPAM(raster));
    }

    static TupleType forPNM(ImageTypeSpecifier type) {
        return filterPNM(forPAM(type));
    }

    private static TupleType filterPNM(TupleType tupleType) {
        if (tupleType == null) {
            return null;
        }

        switch (tupleType) {
            case BLACKANDWHITE:
                return BLACKANDWHITE_WHITE_IS_ZERO;
            case GRAYSCALE:
            case RGB:
                return tupleType;
            default:
                return null;
        }
    }

    static TupleType forPAM(Raster raster) {
        SampleModel sampleModel = raster.getSampleModel();
        switch (sampleModel.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_INT:
                // B/W, Gray or RGB
                int bands = sampleModel.getNumBands();

                if (bands == 1 && sampleModel.getSampleSize(0) == 1) {
                    return TupleType.BLACKANDWHITE;
                }
                else if (bands == 2 && sampleModel.getSampleSize(0) == 1 && sampleModel.getSampleSize(1) == 1) {
                    return TupleType.BLACKANDWHITE_ALPHA;
                }

                // We can only write 8 or 16 bits/band
                if (!(sampleModel.getSampleSize(0) == 8 || sampleModel.getSampleSize(0) == 16)) {
                    return null;
                }
                for (int i = 1; i < bands; i++) {
                    if (sampleModel.getSampleSize(0) != sampleModel.getSampleSize(i)) {
                        return null;
                    }
                }

                if (bands == 1) {
                    return TupleType.GRAYSCALE;
                }
                else if (bands == 2) {
                    return TupleType.GRAYSCALE_ALPHA;
                }
                else if (bands == 3) {
                    return TupleType.RGB;
                }
                else if (bands == 4) {
                    return TupleType.RGB_ALPHA;
                }
                // ...else fall through...
        }

        return null;
    }
    
    static TupleType forPAM(ImageTypeSpecifier type) {
        // Support only 1 bit b/w, 8-16 bit gray and 8-16 bit/sample RGB
        switch (type.getBufferedImageType()) {
            // 1 bit b/w  or b/w + a
            case BufferedImage.TYPE_BYTE_BINARY:
                switch (type.getNumBands()) {
                    case 1:
                        return type.getBitsPerBand(0) == 1 ? TupleType.BLACKANDWHITE : null;
                    case 2:
                        return type.getBitsPerBand(0) == 2 || type.getBitsPerBand(0) == 1 && type.getBitsPerBand(1) == 1 ? TupleType.BLACKANDWHITE_ALPHA : null;
                    default:
                        return null;
                }
                // Gray
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_USHORT_GRAY:
                return TupleType.GRAYSCALE;
            // RGB
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_BGR:
                return TupleType.RGB;
            // RGBA
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return TupleType.RGB_ALPHA;
            default:
                // BYTE, USHORT or INT (packed)
                switch (type.getSampleModel().getTransferType()) {
                    case DataBuffer.TYPE_BYTE:
                    case DataBuffer.TYPE_USHORT:
                    case DataBuffer.TYPE_INT:
                        // Gray or RGB
                        ColorModel colorModel = type.getColorModel();

                        if (!(colorModel instanceof IndexColorModel)) {
                            ColorSpace cs = colorModel.getColorSpace();

                            // We can only write 8 or 16 bits/band
                            int bands = type.getNumBands();
                            if (!(type.getBitsPerBand(0) == 8 || type.getBitsPerBand(0) == 16)) {
                                return null;
                            }
                            for (int i = 1; i < bands; i++) {
                                if (type.getBitsPerBand(0) != type.getBitsPerBand(i)) {
                                    return null;
                                }
                            }

                            if (cs.getType() == ColorSpace.TYPE_GRAY && bands == 1) {
                                return TupleType.GRAYSCALE;
                            }
                            else if (cs.getType() == ColorSpace.TYPE_GRAY && colorModel.hasAlpha() && bands == 2) {
                                return TupleType.GRAYSCALE_ALPHA;
                            }
                            else if (cs.getType() == ColorSpace.TYPE_RGB && bands == 3) {
                                return TupleType.RGB;
                            }
                            else if (cs.getType() == ColorSpace.TYPE_RGB && colorModel.hasAlpha() && bands == 4) {
                                return TupleType.RGB_ALPHA;
                            }
                            else if (cs.getType() == ColorSpace.TYPE_CMYK && bands == 4) {
                                return TupleType.CMYK;
                            }
                            else if (cs.getType() == ColorSpace.TYPE_CMYK && colorModel.hasAlpha() && bands == 5) {
                                return TupleType.CMYK_ALPHA;
                            }
                            // ...else fall through...
                        }
                }
        }

        return null;
    }
}
