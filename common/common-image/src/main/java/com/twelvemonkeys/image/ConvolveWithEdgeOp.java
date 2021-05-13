/*
 * Copyright (c) 2008, Harald Kuhr
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * This class implements a convolution from the source
 * to the destination.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/ConvolveWithEdgeOp.java#1 $
 *
 * @see java.awt.image.ConvolveOp
 */
public class ConvolveWithEdgeOp implements BufferedImageOp, RasterOp {

    /**
     * Alias for {@link ConvolveOp#EDGE_ZERO_FILL}.
     * @see #EDGE_REFLECT
     */
    public static final int EDGE_ZERO_FILL = ConvolveOp.EDGE_ZERO_FILL;
    /**
     * Alias for {@link ConvolveOp#EDGE_NO_OP}.
     * @see #EDGE_REFLECT
     */
    public static final int EDGE_NO_OP = ConvolveOp.EDGE_NO_OP;
    /**
     * Adds a border to the image while convolving. The border will reflect the
     * edges of the original image. This is usually a good default.
     * Note that while this mode typically provides better quality than the
     * standard modes {@code EDGE_ZERO_FILL} and {@code EDGE_NO_OP}, it does so
     * at the expense of higher memory consumption and considerable more computation.
     */
    public static final int EDGE_REFLECT = 2; // as JAI BORDER_REFLECT
    /**
     * Adds a border to the image while convolving. The border will wrap the
     * edges of the original image. This is usually the best choice for tiles.
     * Note that while this mode typically provides better quality than the
     * standard modes {@code EDGE_ZERO_FILL} and {@code EDGE_NO_OP}, it does so
     * at the expense of higher memory consumption and considerable more computation.
     * @see #EDGE_REFLECT
     */
    public static final int EDGE_WRAP = 3; // as JAI BORDER_WRAP

    private final Kernel kernel;
    private final int edgeCondition;

    private final ConvolveOp convolve;

    public ConvolveWithEdgeOp(final Kernel pKernel, final int pEdgeCondition, final RenderingHints pHints) {
        // Create convolution operation
        int edge;

        switch (pEdgeCondition) {
            case EDGE_REFLECT:
            case EDGE_WRAP:
                edge = ConvolveOp.EDGE_NO_OP;
                break;
            default:
                edge = pEdgeCondition;
                break;
        }

        kernel = pKernel;
        edgeCondition = pEdgeCondition;
        convolve = new ConvolveOp(pKernel, edge, pHints);
    }

    public ConvolveWithEdgeOp(final Kernel pKernel) {
        this(pKernel, EDGE_ZERO_FILL, null);
    }

    public BufferedImage filter(BufferedImage pSource, BufferedImage pDestination) {
        if (pSource == null) {
            throw new NullPointerException("source image is null");
        }
        if (pSource == pDestination) {
            throw new IllegalArgumentException("source image cannot be the same as the destination image");
        }

        int borderX = kernel.getWidth() / 2;
        int borderY = kernel.getHeight() / 2;

        BufferedImage original = addBorder(pSource, borderX, borderY);

        // Workaround for what seems to be a Java2D bug:
        // ConvolveOp needs explicit destination image type for some "uncommon"
        // image types. However, TYPE_3BYTE_BGR is what javax.imageio.ImageIO
        // normally returns for color JPEGs... :-/
        BufferedImage destination = pDestination;
        if (original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            destination = ImageUtil.createBuffered(
                    pSource.getWidth(), pSource.getHeight(),
                    pSource.getType(), pSource.getColorModel().getTransparency(),
                    null
            );
        }

        // Do the filtering (if destination is null, a new image will be created)
        destination = convolve.filter(original, destination);

        if (pSource != original) {
            // Remove the border
            destination = destination.getSubimage(borderX, borderY, pSource.getWidth(), pSource.getHeight());
        }

        return destination;
    }

    private BufferedImage addBorder(final BufferedImage pOriginal, final int pBorderX, final int pBorderY) {
        if ((edgeCondition & 2) == 0) {
            return pOriginal;
        }

        // TODO: Might be faster if we could clone raster and stretch it...
        int w = pOriginal.getWidth();
        int h = pOriginal.getHeight();

        ColorModel cm = pOriginal.getColorModel();
        WritableRaster raster = cm.createCompatibleWritableRaster(w + 2 * pBorderX, h + 2 * pBorderY);
        BufferedImage bordered = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        Graphics2D g = bordered.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

            // Draw original in center
            g.drawImage(pOriginal, pBorderX, pBorderY, null);

            // TODO: I guess we need the top/left etc, if the corner pixels are covered by the kernel
            switch (edgeCondition) {
                case EDGE_REFLECT:
                    // Top/left (empty)
                    g.drawImage(pOriginal, pBorderX, 0, pBorderX + w, pBorderY, 0, 0, w, 1, null); // Top/center
                    // Top/right (empty)

                    g.drawImage(pOriginal, -w + pBorderX, pBorderY, pBorderX, h + pBorderY, 0, 0, 1, h, null); // Center/left
                    // Center/center (already drawn)
                    g.drawImage(pOriginal, w + pBorderX, pBorderY, 2 * pBorderX + w, h + pBorderY, w - 1, 0, w, h, null); // Center/right

                    // Bottom/left (empty)
                    g.drawImage(pOriginal, pBorderX, pBorderY + h, pBorderX + w, 2 * pBorderY + h, 0, h - 1, w, h, null); // Bottom/center
                    // Bottom/right (empty)
                    break;
                case EDGE_WRAP:
                    g.drawImage(pOriginal, -w + pBorderX, -h + pBorderY, null); // Top/left
                    g.drawImage(pOriginal, pBorderX, -h + pBorderY, null); // Top/center
                    g.drawImage(pOriginal, w + pBorderX, -h + pBorderY, null); // Top/right

                    g.drawImage(pOriginal, -w + pBorderX, pBorderY, null); // Center/left
                    // Center/center (already drawn)
                    g.drawImage(pOriginal, w + pBorderX, pBorderY, null); // Center/right

                    g.drawImage(pOriginal, -w + pBorderX, h + pBorderY, null); // Bottom/left
                    g.drawImage(pOriginal, pBorderX, h + pBorderY, null); // Bottom/center
                    g.drawImage(pOriginal, w + pBorderX, h + pBorderY, null); // Bottom/right
                    break;
                default:
                    throw new IllegalArgumentException("Illegal edge operation " + edgeCondition);
            }

        }
        finally {
            g.dispose();
        }

        return bordered;
    }

    /**
     * Returns the edge condition.
     * @return the edge condition of this {@code ConvolveOp}.
     * @see #EDGE_NO_OP
     * @see #EDGE_ZERO_FILL
     * @see #EDGE_REFLECT
     * @see #EDGE_WRAP
     */
    public int getEdgeCondition() {
        return edgeCondition;
    }

    public WritableRaster filter(final Raster pSource, final WritableRaster pDestination) {
        return convolve.filter(pSource, pDestination);
    }

    public BufferedImage createCompatibleDestImage(final BufferedImage pSource, final ColorModel pDesinationColorModel) {
        return convolve.createCompatibleDestImage(pSource, pDesinationColorModel);
    }

    public WritableRaster createCompatibleDestRaster(final Raster pSource) {
        return convolve.createCompatibleDestRaster(pSource);
    }

    public Rectangle2D getBounds2D(final BufferedImage pSource) {
        return convolve.getBounds2D(pSource);
    }

    public Rectangle2D getBounds2D(final Raster pSource) {
        return convolve.getBounds2D(pSource);
    }

    public Point2D getPoint2D(final Point2D pSourcePoint, final Point2D pDestinationPoint) {
        return convolve.getPoint2D(pSourcePoint, pDestinationPoint);
    }

    public RenderingHints getRenderingHints() {
        return convolve.getRenderingHints();
    }

    public Kernel getKernel() {
        return convolve.getKernel();
    }

}
