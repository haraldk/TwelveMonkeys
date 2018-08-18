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

package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * This is a drop-in replacement for {@link java.awt.image.AffineTransformOp}.
 * <p>Currently only a modification on {@link #filter(BufferedImage, BufferedImage)} is done, which does a Graphics2D fallback for the native lib.</p>
 *
 * @author <a href="mailto:mail@schmidor.de">Oliver Schmidtmer</a>
 * @author last modified by $Author$
 * @version $Id$
 */
public class AffineTransformOp implements BufferedImageOp, RasterOp {

    final java.awt.image.AffineTransformOp delegate;

    public static final int TYPE_NEAREST_NEIGHBOR = java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR;

    public static final int TYPE_BILINEAR = java.awt.image.AffineTransformOp.TYPE_BILINEAR;

    public static final int TYPE_BICUBIC = java.awt.image.AffineTransformOp.TYPE_BICUBIC;

    /**
     * @param xform The {@link AffineTransform} to use for the operation.
     * @param hints The {@link RenderingHints} object used to specify the interpolation type for the operation.
     */
    public AffineTransformOp(final AffineTransform xform, final RenderingHints hints) {
        delegate = new java.awt.image.AffineTransformOp(xform, hints);
    }

    /**
     * @param xform             The {@link AffineTransform} to use for the operation.
     * @param interpolationType One of the integer interpolation type constants defined by this class: {@link #TYPE_NEAREST_NEIGHBOR}, {@link #TYPE_BILINEAR}, {@link #TYPE_BICUBIC}.
     */
    public AffineTransformOp(final AffineTransform xform, final int interpolationType) {
        delegate = new java.awt.image.AffineTransformOp(xform, interpolationType);
    }

    @Override
    public BufferedImage filter(final BufferedImage src, BufferedImage dst) {
        try {
            return delegate.filter(src, dst);
        }
        catch (ImagingOpException ex) {
            if (dst == null) {
                dst = createCompatibleDestImage(src, src.getColorModel());
            }

            Graphics2D g2d = null;

            try {
                g2d = dst.createGraphics();
                int interpolationType = delegate.getInterpolationType();

                if (interpolationType > 0) {
                    Object interpolationValue = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

                    switch (interpolationType) {
                        case java.awt.image.AffineTransformOp.TYPE_BILINEAR:
                            interpolationValue = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                            break;
                        case java.awt.image.AffineTransformOp.TYPE_BICUBIC:
                            interpolationValue = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                            break;
                    }

                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationValue);
                }
                else if (getRenderingHints() != null) {
                    g2d.setRenderingHints(getRenderingHints());
                }

                g2d.drawImage(src, delegate.getTransform(), null);

                return dst;
            }
            finally {
                if (g2d != null) {
                    g2d.dispose();
                }
            }
        }
    }

    @Override
    public Rectangle2D getBounds2D(final BufferedImage src) {
        return delegate.getBounds2D(src);
    }

    @Override
    public BufferedImage createCompatibleDestImage(final BufferedImage src, final ColorModel destCM) {
        return delegate.createCompatibleDestImage(src, destCM);
    }

    @Override
    public WritableRaster filter(final Raster src, final WritableRaster dest) {
        return delegate.filter(src, dest);
    }

    @Override
    public Rectangle2D getBounds2D(final Raster src) {
        return delegate.getBounds2D(src);
    }

    @Override
    public WritableRaster createCompatibleDestRaster(final Raster src) {
        return delegate.createCompatibleDestRaster(src);
    }

    @Override
    public Point2D getPoint2D(final Point2D srcPt, final Point2D dstPt) {
        return delegate.getPoint2D(srcPt, dstPt);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return delegate.getRenderingHints();
    }
}
