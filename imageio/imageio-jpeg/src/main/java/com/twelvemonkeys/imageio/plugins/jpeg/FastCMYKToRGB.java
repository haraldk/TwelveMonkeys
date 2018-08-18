/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * This class performs a pixel by pixel conversion of the source image, from CMYK to RGB.
 * <p/>
 * The conversion is fast, but performed without any color space conversion.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: FastCMYKToRGB.java,v 1.0 21.02.11 13.22 haraldk Exp$
 * @see ColorConvertOp
 */
class FastCMYKToRGB implements /*BufferedImageOp,*/ RasterOp {
    // TODO: Force dest alpha to match source alpha?

    public FastCMYKToRGB() {
    }

    /**
     * Converts the CMYK source raster to the destination RGB raster.
     *
     * @param src assumed to be 4 byte CMYK
     * @param dest raster, in either 3 byte BGR/BGR, 4 byte ABGR or int RGB/ARGB format, or {@code null}
     * @return {@code dest}, or a new {@link WritableRaster} if {@code dest} is {@code null}.
     * @throws IllegalArgumentException if {@code src} and {@code dest} refer to the same object
     */
    public WritableRaster filter(Raster src, WritableRaster dest) {
        Validate.notNull(src, "src may not be null");
        // TODO: Why not allow same raster, if converting to 4 byte ABGR?
        Validate.isTrue(src != dest, "src and dest raster may not be same");
        Validate.isTrue(src.getTransferType() == DataBuffer.TYPE_BYTE, src, "only TYPE_BYTE rasters supported as src: %s");
        Validate.isTrue(src.getNumDataElements() >= 4, src.getNumDataElements(), "CMYK raster must have at least 4 data elements: %s");

        if (dest == null) {
            dest = createCompatibleDestRaster(src);
        }
        else {
            Validate.isTrue(
                    dest.getTransferType() == DataBuffer.TYPE_BYTE && dest.getNumDataElements() >= 3 ||
                            dest.getTransferType() == DataBuffer.TYPE_INT && dest.getNumDataElements() == 1,
                    src,
                    "only 3 or 4 byte TYPE_BYTE or 1 int TYPE_INT rasters supported as dest: %s"
            );
        }

        final int height = src.getHeight();
        final int width = src.getWidth();

        final byte[] in = new byte[src.getNumDataElements()]; // CMYK

        if (dest.getTransferType() == DataBuffer.TYPE_BYTE) {
            final byte[] out = new byte[dest.getNumDataElements()];

            if (out.length > 3) {
                out[3] = (byte) 0xFF;
            }

            for (int y = dest.getMinY(); y < height; y++) {
                for (int x = dest.getMinX(); x < width; x++) {
                    src.getDataElements(x, y, in);
                    convertCMYKToRGB(in, out);
                    dest.setDataElements(x, y, out);
                }
            }
        }
        else if (dest.getTransferType() == DataBuffer.TYPE_INT) {
            final int[] out = new int[dest.getNumDataElements()];
            final byte[] temp = new byte[3]; // RGB

            // Special case for INT_BGR types, as bit offsets are not handled in setDataElements like for the byte raster case
            int[] bitOffsets;
            SampleModel sm = dest.getSampleModel();
            if (sm instanceof SinglePixelPackedSampleModel) {
                bitOffsets = ((SinglePixelPackedSampleModel) sm).getBitOffsets();
            }
            else {
                bitOffsets = new int[]{0, 8, 16};
            }

            final int alpha = bitOffsets.length > 3 ? 0xFF : 0x00;

            for (int y = dest.getMinY(); y < height; y++) {
                for (int x = dest.getMinX(); x < width; x++) {
                    src.getDataElements(x, y, in);
                    convertCMYKToRGB(in, temp);
                    out[0] = alpha << 24 | (temp[0] & 0xFF) << bitOffsets[0] | (temp[1] & 0xFF) << bitOffsets[1] | (temp[2] & 0xFF) << bitOffsets[2];
                    dest.setDataElements(x, y, out);
                }
            }
        }
        else {
            // This is already tested for
            throw new AssertionError();
        }

        return dest;
    }

    private void convertCMYKToRGB(byte[] cmyk, byte[] rgb) {
        // Adapted from http://www.easyrgb.com/index.php?X=MATH
        final int k = cmyk[3] & 0xFF;
        rgb[0] = (byte) (255 - (((cmyk[0] & 0xFF) * (255 - k) / 255) + k));
        rgb[1] = (byte) (255 - (((cmyk[1] & 0xFF) * (255 - k) / 255) + k));
        rgb[2] = (byte) (255 - (((cmyk[2] & 0xFF) * (255 - k) / 255) + k));
    }

    public Rectangle2D getBounds2D(Raster src) {
        return src.getBounds();
    }

    public WritableRaster createCompatibleDestRaster(final Raster src) {
        // WHAT?? This code no longer work for JRE 7u45+... JRE bug?!
//        Raster child = src.createChild(0, 0, src.getWidth(), src.getHeight(), 0, 0, new int[] {0, 1, 2});
//        return child.createCompatibleWritableRaster(); // Throws an exception complaining about the scanline stride from the verify() method

        // This is a workaround for the above code that no longer works.
        // It wil use 25% more memory, but it seems to work...
        WritableRaster raster = src.createCompatibleWritableRaster();
        return raster.createWritableChild(0, 0, src.getWidth(), src.getHeight(), 0, 0, new int[] {0, 1, 2});
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double(srcPt.getX(), srcPt.getY());
        }
        else {
            dstPt.setLocation(srcPt);
        }

        return dstPt;
    }

    public RenderingHints getRenderingHints() {
        return null;
    }
}
