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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
 * <!--
 * <DT>{@code cropUniform}</DT>
 * <DD>boolean, wether or not uniform scalnig should be used. Default is
 * {@code true}.
 * <DT>{@code cropUnits}</DT>
 * <DD>string, one of {@code PIXELS}, {@code PERCENT}.
 * {@code PIXELS} is default. -->
 *
 *
 * </DL>
 *
 * @example
 * JPEG:
 * &lt;IMG src="/scale/test.jpg?image=http://www.iconmedialab.com/images/random/home_image_12.jpg&width=500&uniform=true"&gt;
 *
 * PNG:
 * &lt;IMG src="/scale/test.png?cache=false&image=http://www.iconmedialab.com/images/random/home_image_12.jpg&width=50&units=PERCENT"&gt;
 *
 * @todo Correct rounding errors, resulting in black borders when rotating 90
 *       degrees, and one of width or height is odd length...
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: RotateFilter.java#1 $
 */

public class RotateFilter extends ImageFilter {
    /** {@code angle}*/
    protected final static String PARAM_ANGLE = "angle";
    /** {@code angleUnits (RADIANS|DEGREES)}*/
    protected final static String PARAM_ANGLE_UNITS = "angleUnits";
    /** {@code crop}*/
    protected final static String PARAM_CROP = "rotateCrop";
    /** {@code bgcolor}*/
    protected final static String PARAM_BGCOLOR = "rotateBgcolor";

    /** {@code degrees}*/
    private final static String ANGLE_DEGREES = "degrees";
    /** {@code radians}*/
    //private final static String ANGLE_RADIANS = "radians";

    /**
     * Reads the image from the requested URL, rotates it, and returns
     * it in the
     * Servlet stream. See above for details on parameters.
     */

    protected RenderedImage doFilter(BufferedImage pImage, ServletRequest pRequest, ImageServletResponse pResponse) {
        // Get angle
        double ang = getAngle(pRequest);

        // Get bounds
        Rectangle2D rect = getBounds(pRequest, pImage, ang);
        int width = (int) rect.getWidth();
        int height = (int) rect.getHeight();

        // Create result image
        BufferedImage res = ImageUtil.createTransparent(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = res.createGraphics();

        // Get background color and clear
        String str = pRequest.getParameter(PARAM_BGCOLOR);
        if (!StringUtil.isEmpty(str)) {
            Color bgcolor = StringUtil.toColor(str);
            g.setBackground(bgcolor);
            g.clearRect(0, 0, width, height);
        }

        // Set mHints (why do I always get jagged edgdes?)
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        hints.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC));

        g.setRenderingHints(hints);

        // Rotate around center
        AffineTransform at = AffineTransform
                .getRotateInstance(ang, width / 2.0, height / 2.0);

        // Move to center
        at.translate(width / 2.0 - pImage.getWidth() / 2.0,
                     height / 2.0 - pImage.getHeight() / 2.0);

        // Draw it, centered
        g.drawImage(pImage, at, null);

        return res;
    }

    /**
     * Gets the angle of rotation.
     */

    private double getAngle(ServletRequest pReq) {
        double angle = 0.0;
        String str = pReq.getParameter(PARAM_ANGLE);
        if (!StringUtil.isEmpty(str)) {
            angle = Double.parseDouble(str);

            // Convert to radians, if needed
            str = pReq.getParameter(PARAM_ANGLE_UNITS);
            if (!StringUtil.isEmpty(str)
                    && ANGLE_DEGREES.equalsIgnoreCase(str)) {
                angle = Math.toRadians(angle);
            }
        }

        return angle;
    }

    /**
     * Get the bounding rectangle of the rotated image.
     */

    private Rectangle2D getBounds(ServletRequest pReq, BufferedImage pImage,
                                  double pAng) {
        // Get dimensions of original image
        int width = pImage.getWidth(); // loads the image
        int height = pImage.getHeight();

        // Test if we want to crop image (default)
        // if true
        //  - find the largest bounding box INSIDE the rotated image,
        //    that matches the original proportions (nearest 90deg)
        //    (scale up to fit dimensions?)
        // else
        //  - find the smallest bounding box OUTSIDE the rotated image.
        //    - that matches the original proportions (nearest 90deg) ?
        //      (scale down to fit dimensions?)
        AffineTransform at =
                AffineTransform.getRotateInstance(pAng, width / 2.0, height / 2.0);

        Rectangle2D orig = new Rectangle(width, height);
        Shape rotated = at.createTransformedShape(orig);

        if (ServletUtil.getBooleanParameter(pReq, PARAM_CROP, false)) {
            // TODO: Inside box
            return rotated.getBounds2D();
        }
        else {
            return rotated.getBounds2D();
        }
    }
}

