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
 * This BufferedImageOp simply copies pixels, converting to a
 * {@code IndexColorModel}.

 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/CopyDither.java#1 $
 *
 */
public class CopyDither implements BufferedImageOp, RasterOp {

    protected IndexColorModel indexColorModel = null;

    /**
     * Creates a {@code CopyDither}, using the given
     * {@code IndexColorModel} for dithering into.
     *
     * @param pICM an IndexColorModel.
     */
    public CopyDither(IndexColorModel pICM) {
        // Store colormodel
        indexColorModel = pICM;
    }

    /**
     * Creates a {@code CopyDither}, with no fixed
     * {@code IndexColorModel}. The colormodel will be generated for each
     * filtering, unless the dest image allready has an
     * {@code IndexColorModel}.
     */
    public CopyDither() {
    }


    /**
     * Creates a compatible {@code BufferedImage} to dither into.
     * Only {@code IndexColorModel} allowed.
     *
     * @return a compatible {@code BufferedImage}
     *
     * @throws ImageFilterException if {@code pDestCM} is not {@code null} or
     * an instance of {@code IndexColorModel}.
     */
    public final BufferedImage createCompatibleDestImage(BufferedImage pSource, ColorModel pDestCM) {
        if (pDestCM == null) {
            return new BufferedImage(pSource.getWidth(), pSource.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, indexColorModel);
        }
        else if (pDestCM instanceof IndexColorModel) {
            return new BufferedImage(pSource.getWidth(), pSource.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, (IndexColorModel) pDestCM);
        }
        else {
            throw new ImageFilterException("Only IndexColorModel allowed.");
        }
    }

    /**
     * Creates a compatible {@code Raster} to dither into.
     * Only {@code IndexColorModel} allowed.
     *
     * @param pSrc
     *
     * @return a {@code WritableRaster}
     */
    public final WritableRaster createCompatibleDestRaster(Raster pSrc) {
        return createCompatibleDestRaster(pSrc, getICM(pSrc));
    }

    public final WritableRaster createCompatibleDestRaster(Raster pSrc, IndexColorModel pIndexColorModel) {
        return pIndexColorModel.createCompatibleWritableRaster(pSrc.getWidth(), pSrc.getHeight());
    }


    /**
     * Returns the bounding box of the filtered destination image.  Since
     * this is not a geometric operation, the bounding box does not
     * change.
     * @param pSrc the {@code BufferedImage} to be filtered
     * @return the bounds of the filtered definition image.
     */
    public final Rectangle2D getBounds2D(BufferedImage pSrc) {
        return getBounds2D(pSrc.getRaster());
    }

    /**
     * Returns the bounding box of the filtered destination Raster.  Since
     * this is not a geometric operation, the bounding box does not
     * change.
     * @param pSrc the {@code Raster} to be filtered
     * @return the bounds of the filtered definition {@code Raster}.
     */
    public final Rectangle2D getBounds2D(Raster pSrc) {
        return pSrc.getBounds();

    }

    /**
     * Returns the location of the destination point given a
     * point in the source.  If {@code dstPt} is not
     * {@code null}, it will be used to hold the return value.
     * Since this is not a geometric operation, the {@code srcPt}
     * will equal the {@code dstPt}.
     * @param pSrcPt a {@code Point2D} that represents a point
     *        in the source image
     * @param pDstPt a {@code Point2D}that represents the location
     *        in the destination
     * @return the {@code Point2D} in the destination that
     *         corresponds to the specified point in the source.
     */
    public final Point2D getPoint2D(Point2D pSrcPt, Point2D pDstPt) {
        // Create new Point, if needed
        if (pDstPt == null) {
            pDstPt = new Point2D.Float();
        }

        // Copy location
        pDstPt.setLocation(pSrcPt.getX(), pSrcPt.getY());

        // Return dest
        return pDstPt;
    }

    /**
     * Returns the rendering mHints for this op.
     * @return the {@code RenderingHints} object associated
     *         with this op.
     */
    public final RenderingHints getRenderingHints() {
        return null;
    }

    /**
     * Converts a int triplet to int ARGB.
     */
    private static int toIntARGB(int[] pRGB) {
        return 0xff000000 // All opaque
                | (pRGB[0] << 16)
                | (pRGB[1] << 8)
                | (pRGB[2]);
        /*
          | ((int) (pRGB[0] << 16) & 0x00ff0000)
          | ((int) (pRGB[1] <<  8) & 0x0000ff00)
          | ((int) (pRGB[2]      ) & 0x000000ff);
        */
    }


    /**
     * Performs a single-input/single-output dither operation, applying basic
     * Floyd-Steinberg error-diffusion to the image.
     *
     * @param pSource the source image
     * @param pDest the destiantion image
     *
     * @return the destination image, or a new image, if {@code pDest} was
     * {@code null}.
     */
    public final BufferedImage filter(BufferedImage pSource, BufferedImage pDest) {
        // Create destination image, if none provided
        if (pDest == null) {
            pDest = createCompatibleDestImage(pSource, getICM(pSource));
        }
        else if (!(pDest.getColorModel() instanceof IndexColorModel)) {
            throw new ImageFilterException("Only IndexColorModel allowed.");
        }

        // Filter rasters
        filter(pSource.getRaster(), pDest.getRaster(), (IndexColorModel) pDest.getColorModel());

        return pDest;
    }

    /**
     * Performs a single-input/single-output dither operation, applying basic
     * Floyd-Steinberg error-diffusion to the image.
     *
     * @param pSource
     * @param pDest
     *
     * @return the destination raster, or a new raster, if {@code pDest} was
     * {@code null}.
     */
    public final WritableRaster filter(final Raster pSource, WritableRaster pDest) {
        return filter(pSource, pDest, getICM(pSource));
    }

    private IndexColorModel getICM(BufferedImage pSource) {
        return (indexColorModel != null ? indexColorModel : IndexImage.getIndexColorModel(pSource, 256, IndexImage.TRANSPARENCY_BITMASK | IndexImage.COLOR_SELECTION_QUALITY));
    }

    private IndexColorModel getICM(Raster pSource) {
        return (indexColorModel != null ? indexColorModel : createIndexColorModel(pSource));
    }

    private IndexColorModel createIndexColorModel(Raster pSource) {
        BufferedImage image = new BufferedImage(pSource.getWidth(), pSource.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.setData(pSource);

        return IndexImage.getIndexColorModel(image, 256, IndexImage.TRANSPARENCY_BITMASK | IndexImage.COLOR_SELECTION_QUALITY);
    }

    /**
     * Performs a single-input/single-output pixel copy operation.
     *
     * @param pSource
     * @param pDest
     * @param pColorModel
     *
     * @return the destination raster, or a new raster, if {@code pDest} was
     * {@code null}.
     */
    public final WritableRaster filter(final Raster pSource, WritableRaster pDest, IndexColorModel pColorModel) {
        int width = pSource.getWidth();
        int height = pSource.getHeight();

        if (pDest == null) {
            pDest = createCompatibleDestRaster(pSource, pColorModel);
        }

        // temp buffers
        final int[] inRGB = new int[4];
        Object pixel = null;

        // TODO: Use getPixels instead of getPixel for better performance?

        // Loop through image data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get rgb from original raster
                // DON'T KNOW IF THIS WILL WORK FOR ALL TYPES..?
                pSource.getPixel(x, y, inRGB);

                // Get pixel value...
                // It is VERY important that we are using an IndexColorModel that
                // support reverse color lookup for speed.
                pixel = pColorModel.getDataElements(toIntARGB(inRGB), pixel);

                // And set it
                pDest.setDataElements(x, y, pixel);
            }
        }

        return pDest;
    }
}

