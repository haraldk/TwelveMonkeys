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

import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.ServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * This Servlet is able to render a cropped part of an image.
 *
 * <P><HR><P>
 *
 * <A name="parameters"></A><STRONG>Parameters:</STRONG><BR>
 * <DL>
 * <DT>{@code cropX}</DT>
 * <DD>integer, the new left edge of the image.
 * <DT>{@code cropY}</DT>
 * <DD>integer, the new top of the image.
 * <DT>{@code cropWidth}</DT>
 * <DD>integer, the new width of the image.
 * <DT>{@code cropHeight}</DT>
 * <DD>integer, the new height of the image.
 * <DT>{@code cropUniform}</DT>
 * <DD>boolean, wether or not uniform scalnig should be used. Default is
 * {@code true}.
 * <DT>{@code cropUnits}</DT>
 * <DD>string, one of {@code PIXELS}, {@code PERCENT}.
 * {@code PIXELS} is default.
 *
 * <!-- inherited from ScaleImage below: -->
 *
 * <DT>{@code image}</DT>
 * <DD>string, the URL of the image to scale.
 *
 * <DT>{@code scaleX}</DT>
 * <DD>integer, the new width of the image.
 *
 * <DT>{@code scaleY}</DT>
 * <DD>integer, the new height of the image.
 *
 * <DT>{@code scaleUniform}</DT>
 * <DD>boolean, wether or not uniform scalnig should be used. Default is
 * {@code true}.
 *
 * <DT>{@code scaleUnits}</DT>
 * <DD>string, one of {@code PIXELS}, {@code PERCENT}.
 * {@code PIXELS} is default.
 *
 * <DT>{@code scaleQuality}</DT>
 * <DD>string, one of {@code SCALE_SMOOTH}, {@code SCALE_FAST},
 * {@code SCALE_REPLICATE}, {@code SCALE_AREA_AVERAGING}.
 * {@code SCALE_DEFAULT} is default.
 *
 * </DL>
 *
 * @example
 * &lt;IMG src="/crop/test.jpg?image=http://www.iconmedialab.com/images/random/home_image_12.jpg&cropWidth=500&cropUniform=true"&gt;
 *
 * @example
 * &lt;IMG src="/crop/test.png?cache=false&image=http://www.iconmedialab.com/images/random/home_image_12.jpg&cropWidth=50&cropUnits=PERCENT"&gt;
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/CropFilter.java#1 $
 */
public class CropFilter extends ScaleFilter {
    /** {@code cropX}*/
    protected final static String PARAM_CROP_X = "cropX";
    /** {@code cropY}*/
    protected final static String PARAM_CROP_Y = "cropY";
    /** {@code cropWidth}*/
    protected final static String PARAM_CROP_WIDTH = "cropWidth";
    /** {@code cropHeight}*/
    protected final static String PARAM_CROP_HEIGHT = "cropHeight";
    /** {@code cropUniform}*/
    protected final static String PARAM_CROP_UNIFORM = "cropUniform";
    /** {@code cropUnits}*/
    protected final static String PARAM_CROP_UNITS = "cropUnits";

    /**
     * Reads the image from the requested URL, scales it, crops it, and returns
     * it in the
     * Servlet stream. See above for details on parameters.
     */
    protected RenderedImage doFilter(BufferedImage pImage, ServletRequest pRequest, ImageServletResponse pResponse) {
        // Get crop coordinates
        int x = ServletUtil.getIntParameter(pRequest, PARAM_CROP_X, -1);
        int y = ServletUtil.getIntParameter(pRequest, PARAM_CROP_Y, -1);
        int width = ServletUtil.getIntParameter(pRequest, PARAM_CROP_WIDTH, -1);
        int height = ServletUtil.getIntParameter(pRequest, PARAM_CROP_HEIGHT, -1);

        boolean uniform =
                ServletUtil.getBooleanParameter(pRequest, PARAM_CROP_UNIFORM, false);

        int units = getUnits(ServletUtil.getParameter(pRequest, PARAM_CROP_UNITS, null));

        // Get crop bounds
        Rectangle bounds =
                getBounds(x, y, width, height, units, uniform, pImage);

        // Return cropped version
        return pImage.getSubimage((int) bounds.getX(), (int) bounds.getY(),
                                  (int) bounds.getWidth(),
                                  (int) bounds.getHeight());
        //return scaled.getSubimage(x, y, width, height);
    }

    protected Rectangle getBounds(int pX, int pY, int pWidth, int pHeight,
                                  int pUnits, boolean pUniform,
                                  BufferedImage pImg) {
        // Algoritm:
        // Try to get x and y (default 0,0).
        // Try to get width and height (default width-x, height-y)
        //
        // If percent, get ratio
        //
        // If uniform
        //

        int oldWidth = pImg.getWidth();
        int oldHeight = pImg.getHeight();
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
            // Else: No crop
        }
        //else if (UNITS_PIXELS.equalsIgnoreCase(pUnits)) {
        else if (pUnits == UNITS_PIXELS) {
            // Uniform
            if (pUniform) {
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
                // Else: No crop
            }
        }
        // Else: No crop

        // Not specified, or outside bounds: Use original dimensions
        if (pWidth < 0 || (pX < 0 && pWidth > oldWidth)
                || (pX >= 0 && (pX + pWidth) > oldWidth)) {
            pWidth = (pX >= 0 ? oldWidth - pX : oldWidth);
        }
        if (pHeight < 0 || (pY < 0 && pHeight > oldHeight)
                || (pY >= 0 && (pY + pHeight) > oldHeight)) {
            pHeight = (pY >= 0 ? oldHeight - pY : oldHeight);
        }

        // Center
        if (pX < 0) {
            pX = (pImg.getWidth() - pWidth) / 2;
        }
        if (pY < 0) {
            pY = (pImg.getHeight() - pHeight) / 2;
        }

        //System.out.println("x: " + pX + " y: " + pY
        //                   + " w: " + pWidth + " h " + pHeight);

        return new Rectangle(pX, pY, pWidth, pHeight);
    }
}