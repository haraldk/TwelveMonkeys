/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.ServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.lang.reflect.Field;


/**
 * This filter renders a scaled version of an image read from a
 * given URL. The image can be output as a GIF, JPEG or PNG image
 * or similar<!--,
 * with optional caching of the rendered image files-->.
 * <P>
 * <P><HR><P>
 * <p/>
 * <A name="parameters"></A><STRONG>Parameters:</STRONG><BR>
 * <DL>
 * <DT>{@code scaleX}</DT>
 * <DD>integer, the new width of the image.
 * <DT>{@code scaleY}</DT>
 * <DD>integer, the new height of the image.
 * <DT>{@code scaleUniform}</DT>
 * <DD>boolean, wether or not uniform scalnig should be used. Default is
 * {@code true}.
 * <DT>{@code scaleUnits}</DT>
 * <DD>string, one of {@code PIXELS}, {@code PERCENT}.
 * {@code PIXELS} is default.
 * <DT>{@code scaleQuality}</DT>
 * <DD>string, one of {@code SCALE_SMOOTH}, {@code SCALE_FAST},
 * {@code SCALE_REPLICATE}, {@code SCALE_AREA_AVERAGING}.
 * {@code SCALE_DEFAULT} is default (see
 * {@link java.awt.Image#getScaledInstance(int,int,int)}, {@link java.awt.Image}
 * for more details).
 * </DL>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/ScaleFilter.java#1 $
 *
 * @example &lt;IMG src="/scale/test.jpg?scaleX=500&scaleUniform=false"&gt;
 * @example &lt;IMG src="/scale/test.png?scaleY=50&scaleUnits=PERCENT"&gt;
 */
public class ScaleFilter extends ImageFilter {

    /**
     * Width and height are absolute pixels. The default.
     */
    public static final int UNITS_PIXELS = 1;
    /**
     * Width and height are percentage of original width and height.
     */
    public static final int UNITS_PERCENT = 5;
    /**
     * Ahh, good choice!
     */
    //private static final int UNITS_METRIC = 42;
    /**
     * The root of all evil...
     */
    //private static final int UNITS_INCHES = 666;
    /**
     * Unknown units. <!-- Oops, what now? -->
     */
    public static final int UNITS_UNKNOWN = 0;

    /**
     * {@code scaleQuality}
     */
    protected final static String PARAM_SCALE_QUALITY = "scaleQuality";
    /**
     * {@code scaleUnits}
     */
    protected final static String PARAM_SCALE_UNITS = "scaleUnits";
    /**
     * {@code scaleUniform}
     */
    protected final static String PARAM_SCALE_UNIFORM = "scaleUniform";
    /**
     * {@code scaleX}
     */
    protected final static String PARAM_SCALE_X = "scaleX";
    /**
     * {@code scaleY}
     */
    protected final static String PARAM_SCALE_Y = "scaleY";
    /**
     * {@code image}
     */
    protected final static String PARAM_IMAGE = "image";

    /** */
    protected int mDefaultScaleQuality = Image.SCALE_DEFAULT;

    /**
     * Reads the image from the requested URL, scales it, and returns it in the
     * Servlet stream. See above for details on parameters.
     */
    protected RenderedImage doFilter(BufferedImage pImage, ServletRequest pRequest, ImageServletResponse pResponse) {

        // Get quality setting
        // SMOOTH | FAST | REPLICATE | DEFAULT | AREA_AVERAGING
        // See Image (mHints)
        int quality = getQuality(pRequest.getParameter(PARAM_SCALE_QUALITY));

        // Get units, default is pixels
        // PIXELS | PERCENT | METRIC | INCHES
        int units = getUnits(pRequest.getParameter(PARAM_SCALE_UNITS));
        if (units == UNITS_UNKNOWN) {
            log("Unknown units for scale, returning original.");
            return pImage;
        }

        // Use uniform scaling? Default is true
        boolean uniformScale = ServletUtil.getBooleanParameter(pRequest, PARAM_SCALE_UNIFORM, true);

        // Get dimensions
        int width = ServletUtil.getIntParameter(pRequest, PARAM_SCALE_X, -1);
        int height = ServletUtil.getIntParameter(pRequest, PARAM_SCALE_Y, -1);

        // Get dimensions for scaled image
        Dimension dim = getDimensions(pImage, width, height, units, uniformScale);

        width = (int) dim.getWidth();
        height = (int) dim.getHeight();

        // Return scaled instance directly
        return ImageUtil.createScaled(pImage, width, height, quality);
    }

    /**
     * Gets the quality constant for the scaling, from the string argument.
     *
     * @param pQualityStr The string representation of the scale quality
     *                    constant.
     * @return The matching quality constant, or the default quality if none
     *         was found.
     * @see java.awt.Image
     * @see java.awt.Image#getScaledInstance(int,int,int)
     */
    protected int getQuality(String pQualityStr) {
        if (!StringUtil.isEmpty(pQualityStr)) {
            try {
                // Get quality constant from Image using reflection
                Class cl = Image.class;
                Field field = cl.getField(pQualityStr.toUpperCase());

                return field.getInt(null);
            }
            catch (IllegalAccessException ia) {
                log("Unable to get quality.", ia);
            }
            catch (NoSuchFieldException nsf) {
                log("Unable to get quality.", nsf);
            }
        }

        return mDefaultScaleQuality;
    }

    public void setDefaultScaleQuality(String pDefaultScaleQuality) {
        mDefaultScaleQuality = getQuality(pDefaultScaleQuality);
    }

    /**
     * Gets the units constant for the width and height arguments, from the
     * given string argument.
     *
     * @param pUnitStr The string representation of the units constant,
     *                 can be one of "PIXELS" or "PERCENT".
     * @return The mathcing units constant, or UNITS_UNKNOWN if none was found.
     */
    protected int getUnits(String pUnitStr) {
        if (StringUtil.isEmpty(pUnitStr)
                || pUnitStr.equalsIgnoreCase("PIXELS")) {
            return UNITS_PIXELS;
        }
        else if (pUnitStr.equalsIgnoreCase("PERCENT")) {
            return UNITS_PERCENT;
        }
        else {
            return UNITS_UNKNOWN;
        }
    }

    /**
     * Gets the dimensions (height and width) of the scaled image. The
     * dimensions are computed based on the old image's dimensions, the units
     * used for specifying new dimensions and whether or not uniform scaling
     * should be used (se algorithm below).
     *
     * @param pImage        the image to be scaled
     * @param pWidth        the new width of the image, or -1 if unknown
     * @param pHeight       the new height of the image, or -1 if unknown
     * @param pUnits        the constant specifying units for width and height
     *                      parameter (UNITS_PIXELS or UNITS_PERCENT)
     * @param pUniformScale boolean specifying uniform scale or not
     * @return a Dimension object, with the correct width and heigth
     *         in pixels, for the scaled version of the image.
     */
    protected Dimension getDimensions(Image pImage, int pWidth, int pHeight,
                                      int pUnits, boolean pUniformScale) {

        // If uniform, make sure width and height are scaled the same ammount
        // (use ONLY height or ONLY width).
        //
        // Algoritm:
        // if uniform
        //    if newHeight not set
        //       find ratio newWidth / oldWidth
        //       oldHeight *= ratio
        //    else if newWidth not set
        //       find ratio newWidth / oldWidth
        //       oldHeight *= ratio
        //    else
        //       find both ratios and use the smallest one
        //       (this will be the largest version of the image that fits
        //        inside the rectangle given)
        //       (if PERCENT, just use smallest percentage).
        //
        // If units is percent, we only need old height and width

        int oldWidth = ImageUtil.getWidth(pImage);
        int oldHeight = ImageUtil.getHeight(pImage);
        float ratio;

        if (pUnits == UNITS_PERCENT) {
            if (pWidth >= 0 && pHeight >= 0) {
                // Non-uniform
                pWidth = (int) ((float) oldWidth * (float) pWidth / 100f);
                pHeight = (int) ((float) oldHeight * (float) pHeight / 100f);
            }
            else if (pWidth >= 0) {
                // Find ratio from pWidth
                ratio = (float) pWidth / 100f;
                pWidth = (int) ((float) oldWidth * ratio);
                pHeight = (int) ((float) oldHeight * ratio);
            }
            else if (pHeight >= 0) {
                // Find ratio from pHeight
                ratio = (float) pHeight / 100f;
                pWidth = (int) ((float) oldWidth * ratio);
                pHeight = (int) ((float) oldHeight * ratio);
            }
            // Else: No scale
        }
        else if (pUnits == UNITS_PIXELS) {
            if (pUniformScale) {
                if (pWidth >= 0 && pHeight >= 0) {
                    // Compute both ratios
                    ratio = (float) pWidth / (float) oldWidth;
                    float heightRatio = (float) pHeight / (float) oldHeight;

                    // Find the largest ratio, and use that for both
                    if (heightRatio < ratio) {
                        ratio = heightRatio;
                        pWidth = (int) ((float) oldWidth * ratio);
                    }
                    else {
                        pHeight = (int) ((float) oldHeight * ratio);
                    }

                }
                else if (pWidth >= 0) {
                    // Find ratio from pWidth
                    ratio = (float) pWidth / (float) oldWidth;
                    pHeight = (int) ((float) oldHeight * ratio);
                }
                else if (pHeight >= 0) {
                    // Find ratio from pHeight
                    ratio = (float) pHeight / (float) oldHeight;
                    pWidth = (int) ((float) oldWidth * ratio);
                }
                // Else: No scale
            }
        }

        // Default is no scale, just work as a proxy
        if (pWidth < 0) {
            pWidth = oldWidth;
        }
        if (pHeight < 0) {
            pHeight = oldHeight;
        }

        // Create new Dimension object and return
        return new Dimension(pWidth, pHeight);
    }
}
