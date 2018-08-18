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
import java.util.Random;

/**
 * This {@code BufferedImageOp/RasterOp} implements basic
 * Floyd-Steinberg error-diffusion algorithm for dithering.
 * <P/>
 * The weights used are 7/16, 3/16, 5/16 and 1/16, distributed like this:
 * <!-- - -
 *  | |x|7|
 *  - - - -
 *  |3|5|1|
 *   - - -->
 * <P/>
 * <TABLE border="1" cellpadding="4" cellspacing="0">
 *  <TR><TD bgcolor="#000000">&nbsp;</TD><TD class="TableHeadingColor"
 *          align="center">X</TD><TD>7/16</TD></TR>
 *  <TR><TD>3/16</TD><TD>5/16</TD><TD>1/16</TD></TR>
 * </TABLE>
 * <P/>
 * See <A href="http://www.awprofessional.com/bookstore/product.asp?isbn=0201848406&rl=1">Computer Graphics (Foley et al.)</a>
 * for more information.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: DiffusionDither.java#1 $
 */
public class DiffusionDither implements BufferedImageOp, RasterOp {

    private static final int FS_SCALE = 1 << 8;
    private static final Random RANDOM = new Random();

    protected final IndexColorModel indexColorModel;
    private boolean alternateScans = true;

    /**
     * Creates a {@code DiffusionDither}, using the given
     * {@code IndexColorModel} for dithering into.
     *
     * @param pICM an IndexColorModel.
     */
    public DiffusionDither(final IndexColorModel pICM) {
        // Store color model
        indexColorModel = pICM;
    }

    /**
     * Creates a {@code DiffusionDither}, with no fixed
     * {@code IndexColorModel}. The color model will be generated for each
     * filtering, unless the destination image already has an
     * {@code IndexColorModel}.
     */
    public DiffusionDither() {
        this(null);
    }

    /**
     * Sets the scan mode. If the parameter is true, error distribution for
     * every even line will be left-to-right, while odd lines will be
     * right-to-left.
     * The default is {@code true}.
     *
     * @param pUse {@code true} if scan mode should be alternating left/right
     */
    public void setAlternateScans(boolean pUse) {
        alternateScans = pUse;
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
            return new BufferedImage(pSource.getWidth(), pSource.getHeight(),
                                     BufferedImage.TYPE_BYTE_INDEXED,
                                     getICM(pSource));
        }
        else if (pDestCM instanceof IndexColorModel) {
            return new BufferedImage(pSource.getWidth(), pSource.getHeight(),
                                     BufferedImage.TYPE_BYTE_INDEXED,
                                     (IndexColorModel) pDestCM);
        }
        else {
            throw new ImageFilterException("Only IndexColorModel allowed.");
        }
    }

    /**
     * Creates a compatible {@code Raster} to dither into.
     * Only {@code IndexColorModel} allowed.
     *
     * @param pSrc the source raster
     *
     * @return a {@code WritableRaster}
     */
    public final WritableRaster createCompatibleDestRaster(Raster pSrc) {
        return createCompatibleDestRaster(pSrc, getICM(pSrc));
    }

    /**
     * Creates a compatible {@code Raster} to dither into.
     *
     * @param pSrc the source raster.
     * @param pIndexColorModel the index color model used to create a {@code Raster}.
     *
     * @return a {@code WritableRaster}
     */
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
     * Converts an int ARGB to int triplet.
     */
    private static int[] toRGBArray(int pARGB, int[] pBuffer) {
        pBuffer[0] = ((pARGB & 0x00ff0000) >> 16);
        pBuffer[1] = ((pARGB & 0x0000ff00) >> 8);
        pBuffer[2] = ((pARGB & 0x000000ff));
        //pBuffer[3] = ((pARGB & 0xff000000) >> 24); // alpha

        return pBuffer;
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
     * @param pDest the destination image
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
     * @param pSource the source raster, assumed to be in sRGB
     * @param pDest the destination raster, may be {@code null}
     *
     * @return the destination raster, or a new raster, if {@code pDest} was
     * {@code null}.
     */
    public final WritableRaster filter(final Raster pSource, WritableRaster pDest) {
        return filter(pSource, pDest, getICM(pSource));
    }

    private IndexColorModel getICM(BufferedImage pSource) {
        return (indexColorModel != null ? indexColorModel : IndexImage.getIndexColorModel(pSource, 256, IndexImage.TRANSPARENCY_BITMASK));
    }
    private IndexColorModel getICM(Raster pSource) {
        return (indexColorModel != null ? indexColorModel : createIndexColorModel(pSource));
    }

    private IndexColorModel createIndexColorModel(Raster pSource) {
        BufferedImage image = new BufferedImage(pSource.getWidth(), pSource.getHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
        image.setData(pSource);
        return IndexImage.getIndexColorModel(image, 256, IndexImage.TRANSPARENCY_BITMASK);
    }

    /**
     * Performs a single-input/single-output dither operation, applying basic
     * Floyd-Steinberg error-diffusion to the image.
     *
     * @param pSource the source raster, assumed to be in sRGB
     * @param pDest the destination raster, may be {@code null}
     * @param pColorModel the indexed color model to use
     *
     * @return the destination raster, or a new raster, if {@code pDest} was
     * {@code null}.
     */
    public final WritableRaster filter(final Raster pSource, WritableRaster pDest, IndexColorModel pColorModel) {
        int width = pSource.getWidth();
        int height = pSource.getHeight();

        // Create destination raster if needed
        if (pDest == null) {
            pDest = createCompatibleDestRaster(pSource, pColorModel);
        }

        // Initialize Floyd-Steinberg error vectors.
        // +2 to handle the previous pixel and next pixel case minimally
        // When reference for column, add 1 to reference as this buffer is
        // offset from actual column position by one to allow FS to not check
        // left/right edge conditions
        int[][] currErr = new int[width + 2][3];
        int[][] nextErr = new int[width + 2][3];

        // Random errors in [-1 .. 1] - for first row
        for (int i = 0; i < width + 2; i++) {
            // Note: This is broken for the strange cases where nextInt returns Integer.MIN_VALUE
            currErr[i][0] = RANDOM.nextInt(FS_SCALE * 2) - FS_SCALE;
            currErr[i][1] = RANDOM.nextInt(FS_SCALE * 2) - FS_SCALE;
            currErr[i][2] = RANDOM.nextInt(FS_SCALE * 2) - FS_SCALE;
        }

        // Temp buffers
        final int[] diff = new int[3]; // No alpha
        final int[] inRGB = new int[4];
        final int[] outRGB = new int[4];
        Object pixel = null;
        boolean forward = true;

        // Loop through image data
        for (int y = 0; y < height; y++) {
            // Clear out next error rows for colour errors
            for (int i = nextErr.length; --i >= 0;) {
                nextErr[i][0] = 0;
                nextErr[i][1] = 0;
                nextErr[i][2] = 0;
            }

            // Set up start column and limit
            int x;
            int limit;
            if (forward) {
                x = 0;
                limit = width;
            }
            else {
                x = width - 1;
                limit = -1;
            }

            // TODO: Use getPixels instead of getPixel for better performance?            

            // Loop over row
            while (true) {
                // Get RGB from original raster
                // DON'T KNOW IF THIS WILL WORK FOR ALL TYPES.
                pSource.getPixel(x, y, inRGB);

                // Get error for this pixel & add error to rgb
                for (int i = 0; i < 3; i++) {
                    // Make a 28.4 FP number, add Error (with fraction),
                    // rounding and truncate to int
                    inRGB[i] = ((inRGB[i] << 4) + currErr[x + 1][i] + 0x08) >> 4;

                    // Clamp
                    if (inRGB[i] > 255) {
                        inRGB[i] = 255;
                    }
                    else if (inRGB[i] < 0) {
                        inRGB[i] = 0;
                    }
                }

                // Get pixel value...
                // It is VERY important that we are using a IndexColorModel that
                // support reverse color lookup for speed.
                pixel = pColorModel.getDataElements(toIntARGB(inRGB), pixel);

                // ...set it...
                pDest.setDataElements(x, y, pixel);

                // ..and get back the closet match
                pDest.getPixel(x, y, outRGB);

                // Convert the value to default sRGB
                // Should work for all transfertypes supported by IndexColorModel
                toRGBArray(pColorModel.getRGB(outRGB[0]), outRGB);

                // Find diff
                diff[0] = inRGB[0] - outRGB[0];
                diff[1] = inRGB[1] - outRGB[1];
                diff[2] = inRGB[2] - outRGB[2];

                // Apply F-S error diffusion
                // Serpentine scan: left-right
                if (forward) {
                    // Row 1 (y)
                    // Update error in this pixel (x + 1)
                    currErr[x + 2][0] += diff[0] * 7;
                    currErr[x + 2][1] += diff[1] * 7;
                    currErr[x + 2][2] += diff[2] * 7;

                    // Row 2 (y + 1)
                    // Update error in this pixel (x - 1)
                    nextErr[x][0] += diff[0] * 3;
                    nextErr[x][1] += diff[1] * 3;
                    nextErr[x][2] += diff[2] * 3;
                    // Update error in this pixel (x)
                    nextErr[x + 1][0] += diff[0] * 5;
                    nextErr[x + 1][1] += diff[1] * 5;
                    nextErr[x + 1][2] += diff[2] * 5;
                    // Update error in this pixel (x + 1)
                    // TODO: Consider calculating this using
                    // error term = error - sum(error terms 1, 2 and 3)
                    // See Computer Graphics (Foley et al.), p. 573
                    nextErr[x + 2][0] += diff[0]; // * 1;
                    nextErr[x + 2][1] += diff[1]; // * 1;
                    nextErr[x + 2][2] += diff[2]; // * 1;

                    // Next
                    x++;

                    // Done?
                    if (x >= limit) {
                        break;
                    }

                }
                else {
                    // Row 1 (y)
                    // Update error in this pixel (x - 1)
                    currErr[x][0] += diff[0] * 7;
                    currErr[x][1] += diff[1] * 7;
                    currErr[x][2] += diff[2] * 7;

                    // Row 2 (y + 1)
                    // Update error in this pixel (x + 1)
                    nextErr[x + 2][0] += diff[0] * 3;
                    nextErr[x + 2][1] += diff[1] * 3;
                    nextErr[x + 2][2] += diff[2] * 3;
                    // Update error in this pixel (x)
                    nextErr[x + 1][0] += diff[0] * 5;
                    nextErr[x + 1][1] += diff[1] * 5;
                    nextErr[x + 1][2] += diff[2] * 5;
                    // Update error in this pixel (x - 1)
                    // TODO: Consider calculating this using
                    // error term = error - sum(error terms 1, 2 and 3)
                    // See Computer Graphics (Foley et al.), p. 573
                    nextErr[x][0] += diff[0]; // * 1;
                    nextErr[x][1] += diff[1]; // * 1;
                    nextErr[x][2] += diff[2]; // * 1;

                    // Previous
                    x--;

                    // Done?
                    if (x <= limit) {
                        break;
                    }
                }
            }

            // Make next error info current for next iteration
            int[][] temperr;
            temperr = currErr;
            currErr = nextErr;
            nextErr = temperr;

            // Toggle direction
            if (alternateScans) {
                forward = !forward;
            }
        }

        return pDest;
    }
}