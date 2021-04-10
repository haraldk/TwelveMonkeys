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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

/**
 * LuminanceToGray.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LuminanceToGray.java,v 1.0 10/04/2021 haraldk Exp$
 */
final class LuminanceToGray implements RasterOp {

    @Override
    public WritableRaster filter(final Raster src, WritableRaster dest) {
        Validate.notNull(src, "src may not be null");
        Validate.isTrue(src != dest, "src and dest raster may not be same");
        Validate.isTrue(src.getNumDataElements() >= 3, src.getNumDataElements(), "luminance raster must have at least 3 data elements: %s");

        if (dest == null) {
            dest = createCompatibleDestRaster(src);
        }

        // If src and dest have alpha component, keep it, otherwise extract luminance only
        int[] bandList = src.getNumBands() > 3 && dest.getNumBands() > 1 ? new int[] {0, 3} : new int[] {0};
        dest.setRect(0, 0, src.createChild(0, 0, src.getWidth(), src.getHeight(), 0, 0, bandList));

        return dest;
    }

    @Override
    public Rectangle2D getBounds2D(final Raster src) {
        return src.getBounds();
    }

    @Override
    public WritableRaster createCompatibleDestRaster(final Raster src) {
        return src.createCompatibleWritableRaster()
                  .createWritableChild(0, 0, src.getWidth(), src.getHeight(), 0, 0, new int[] {0});
    }

    @Override
    public Point2D getPoint2D(final Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double(srcPt.getX(), srcPt.getY());
        }
        else {
            dstPt.setLocation(srcPt);
        }

        return dstPt;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}
