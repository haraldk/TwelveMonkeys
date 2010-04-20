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

import com.twelvemonkeys.lang.MathUtil;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * This servlet is capable of rendereing a text string and output it as an
 * image. The text can be rendered in any given font, size,
 * style or color, into an image, and output it as a GIF, JPEG or PNG image,
 * with optional caching of the rendered image files.
 *
 * <P><HR><P>
 *
 * <A name="parameters"></A><STRONG>Parameters:</STRONG><BR>
 * <DL>
 * <DT>{@code text}</DT>
 * <DD>string, the text string to render.
 * <DT>{@code width}</DT>
 * <DD>integer, the width of the image
 * <DT>{@code height}</DT>
 * <DD>integer, the height of the image
 * <DT>{@code fontFamily}</DT>
 * <DD>string, the name of the font family.
 * Default is {@code "Helvetica"}.
 * <DT>{@code fontSize}</DT>
 * <DD>integer, the size of the font. Default is {@code 12}.
 * <DT>{@code fontStyle}</DT>
 * <DD>string, the tyle of the font. Can be one of the constants
 * {@code plain} (default), {@code bold}, {@code italic} or
 * {@code bolditalic}. Any other will result in {@code plain}.
 * <DT>{@code fgcolor}</DT>
 * <DD>color (HTML form, {@code #RRGGBB}), or color constant from
 * {@link java.awt.Color}, default is {@code "black"}.
 * <DT>{@code bgcolor}</DT>
 * <DD>color (HTML form, {@code #RRGGBB}), or color constant from
 * {@link java.awt.Color}, default is {@code "transparent"}.
 * Note that the hash character ({@code "#"}) used in colors must be
 * escaped as {@code %23} in the query string. See
 * {@link StringUtil#toColor(String)}, <A href="#examples">examples</A>.
 *
 * <!-- inherited from ImageServlet below: -->
 *
 * <DT>{@code cache}</DT>
 * <DD>boolean, {@code true} if you want to cache the result
 * to disk (default).
 *
 * <DT>{@code compression}</DT>
 * <DD>float, the optional compression ratio for the output image. For JPEG
 *  images, the quality is the inverse of the compression ratio. See
 * {@link #JPEG_DEFAULT_COMPRESSION_LEVEL},
 * {@link #PNG_DEFAULT_COMPRESSION_LEVEL}.
 * <DD>Applies to JPEG and PNG images only.
 *
 * <DT>{@code dither}</DT>
 * <DD>enumerated, one of {@code NONE}, {@code DEFAULT} or
 * {@code FS}, if you want to dither the result ({@code DEFAULT} is
 * default).
 * {@code FS} will produce the best results, but it's slower.
 * <DD>Use in conjuction with {@code indexed}, {@code palette}
 * and {@code websafe}.
 * <DD>Applies to GIF and PNG images only.
 *
 * <DT>{@code fileName}</DT>
 * <DD>string, an optional filename. If not set, the path after the servlet
 * ({@link HttpServletRequest#getPathInfo}) will be used for the cache
 * filename. See {@link #getCacheFile(ServletRequest)},
 * {@link #getCacheRoot}.
 *
 * <DT>{@code height}</DT>
 * <DD>integer, the height of the image.
 *
 * <DT>{@code width}</DT>
 * <DD>integer, the width of the image.
 *
 * <DT>{@code indexed}</DT>
 * <DD>integer, the number of colors in the resulting image, or -1 (default).
 * If the value is set and positive, the image will use an
 * {@code IndexColorModel} with
 * the number of colors specified. Otherwise the image will be true color.
 * <DD>Applies to GIF and PNG images only.
 *
 * <DT>{@code palette}</DT>
 * <DD>string, an optional filename. If set, the image will use IndexColorModel
 * with a palette read from the given file.
 * <DD>Applies to GIF and PNG images only.
 *
 * <DT>{@code websafe}</DT>
 * <DD>boolean, {@code true} if you want the result to use the 216 color
 * websafe palette (default is false).
 * <DD>Applies to GIF and PNG images only.
 * </DL>
 *
 * @example
 * &lt;IMG src="/text/test.gif?height=40&width=600
 * &fontFamily=TimesRoman&fontSize=30&fontStyle=italic&fgcolor=%23990033
 * &bgcolor=%23cccccc&text=the%20quick%20brown%20fox%20jumps%20over%20the
 * %20lazy%20dog&cache=false" /&gt;
 *
 * @example
 * &lt;IMG src="/text/test.jpg?height=40&width=600
 * &fontFamily=TimesRoman&fontSize=30&fontStyle=italic&fgcolor=black
 * &bgcolor=%23cccccc&text=the%20quick%20brown%20fox%20jumps%20over%20the
 * %20lazy%20dog&compression=3&cache=false" /&gt;
 *
 * @example
 * &lt;IMG src="/text/test.png?height=40&width=600
 * &fontFamily=TimesRoman&fontSize=30&fontStyle=italic&fgcolor=%23336699
 * &bgcolor=%23cccccc&text=the%20quick%20brown%20fox%20jumps%20over%20the
 * %20lazy%20dog&cache=true" /&gt;
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/TextRenderer.java#2 $
 */

class TextRenderer /*extends ImageServlet implements ImagePainterServlet*/ {
    // TODO: Create something useable out of this piece of old junk.. ;-)
    // It just needs a graphics object to write onto
    // Alternatively, defer, and compute the size needed
    // Or, make it a filter...

    /** {@code "italic"} */
    public final static String FONT_STYLE_ITALIC = "italic";
    /** {@code "plain"} */
    public final static String FONT_STYLE_PLAIN = "plain";
    /** {@code "bold"} */
    public final static String FONT_STYLE_BOLD = "bold";

    /** {@code text} */
    public final static String PARAM_TEXT = "text";
    /** {@code marginLeft} */
    public final static String PARAM_MARGIN_LEFT = "marginLeft";
    /** {@code marginTop} */
    public final static String PARAM_MARGIN_TOP = "marginTop";
    /** {@code fontFamily} */
    public final static String PARAM_FONT_FAMILY = "fontFamily";
    /** {@code fontSize} */
    public final static String PARAM_FONT_SIZE = "fontSize";
    /** {@code fontStyle} */
    public final static String PARAM_FONT_STYLE = "fontStyle";
    /** {@code textRotation} */
    public final static String PARAM_TEXT_ROTATION = "textRotation";
    /** {@code textRotation} */
    public final static String PARAM_TEXT_ROTATION_UNITS = "textRotationUnits";

    /** {@code bgcolor} */
    public final static String PARAM_BGCOLOR = "bgcolor";
    /** {@code fgcolor} */
    public final static String PARAM_FGCOLOR = "fgcolor";

    protected final static String ROTATION_DEGREES = "DEGREES";
    protected final static String ROTATION_RADIANS = "RADIANS";

    /**
     * Creates the TextRender servlet.
     */

    public TextRenderer() {
    }

    /**
     * Renders the text string for this servlet request.
     */
    private void paint(ServletRequest pReq, Graphics2D pRes,
                      int pWidth, int pHeight)
            throws ImageServletException {

        // Get parameters
        String text = pReq.getParameter(PARAM_TEXT);
        String[] lines = StringUtil.toStringArray(text, "\n\r");

        String fontFamily = pReq.getParameter(PARAM_FONT_FAMILY);
        String fontSize = pReq.getParameter(PARAM_FONT_SIZE);
        String fontStyle = pReq.getParameter(PARAM_FONT_STYLE);

        String bgcolor = pReq.getParameter(PARAM_BGCOLOR);
        String fgcolor = pReq.getParameter(PARAM_FGCOLOR);

        // TODO: Make them static..
        pRes.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
        pRes.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        pRes.addRenderingHints(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY));
        //    	pRes.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        //System.out.println(pRes.getBackground());

        // Clear area with bgcolor
        if (!StringUtil.isEmpty(bgcolor)) {
            pRes.setBackground(StringUtil.toColor(bgcolor));
            pRes.clearRect(0, 0, pWidth, pHeight);

            //System.out.println(pRes.getBackground());
        }

        // Create and set font
        Font font = new Font((fontFamily != null ? fontFamily : "Helvetica"),
                             getFontStyle(fontStyle),
                             (fontSize != null ? Integer.parseInt(fontSize)
                              : 12));
        pRes.setFont(font);

        // Set rotation
        double angle = getAngle(pReq);
        pRes.rotate(angle, pWidth / 2.0, pHeight / 2.0);

        // Draw string in fgcolor
        pRes.setColor(fgcolor != null ? StringUtil.toColor(fgcolor)
                      : Color.black);

        float x = ServletUtil.getFloatParameter(pReq, PARAM_MARGIN_LEFT,
                                                Float.MIN_VALUE);
        Rectangle2D[] bounds = new Rectangle2D[lines.length];
        if (x <= Float.MIN_VALUE) {
            // Center
            float longest = 0f;
            for (int i = 0; i < lines.length; i++) {
                bounds[i] = font.getStringBounds(lines[i],
                                                 pRes.getFontRenderContext());
                if (bounds[i].getWidth() > longest) {
                    longest = (float) bounds[i].getWidth();
                }
            }

            //x = (float) ((pWidth - bounds.getWidth()) / 2f);
            x = (float) ((pWidth - longest) / 2f);

            //System.out.println("marginLeft: " + x);
        }
        //else {
        //System.out.println("marginLeft (from param): " + x);
        //}

        float y = ServletUtil.getFloatParameter(pReq, PARAM_MARGIN_TOP,
                                                Float.MIN_VALUE);
        float lineHeight = (float) (bounds[0] != null ? bounds[0].getHeight() :
                font.getStringBounds(lines[0],
                                     pRes.getFontRenderContext()).getHeight());

        if (y <= Float.MIN_VALUE) {
            // Center
            y = (float) ((pHeight - lineHeight) / 2f)
                    - (lineHeight * (lines.length - 2.5f) / 2f);

            //System.out.println("marginTop: " + y);
        }
        else {
            // Todo: Correct for font height?
            y += font.getSize2D();
            //System.out.println("marginTop (from param):" + y);

        }

        //System.out.println("Font size: " + font.getSize2D());
        //System.out.println("Line height: " + lineHeight);

        // Draw
        for (int i = 0; i < lines.length; i++) {
            pRes.drawString(lines[i], x, y + lineHeight * i);
        }
    }

    /**
     * Returns the font style constant.
     * 
     * @param pStyle a string containing either the word {@code "plain"} or one
     * or more of {@code "bold"} and {@code italic}.
     * @return the font style constant as defined in {@link Font}.
     *
     * @see Font#PLAIN
     * @see Font#BOLD
     * @see Font#ITALIC
     */
    private int getFontStyle(String pStyle) {
        if (pStyle == null
                || StringUtil.containsIgnoreCase(pStyle, FONT_STYLE_PLAIN)) {
            return Font.PLAIN;
        }

        // Try to find bold/italic
        int style = Font.PLAIN;
        if (StringUtil.containsIgnoreCase(pStyle, FONT_STYLE_BOLD)) {
            style |= Font.BOLD;
        }
        if (StringUtil.containsIgnoreCase(pStyle, FONT_STYLE_ITALIC)) {
            style |= Font.ITALIC;
        }

        return style;
    }

    /**
     * Gets the angle of rotation from the request.
     *
     * @param pRequest the servlet request to get parameters from
     * @return the angle in radians.
     */
    private double getAngle(ServletRequest pRequest) {
        // Get angle
        double angle =
                ServletUtil.getDoubleParameter(pRequest, PARAM_TEXT_ROTATION, 0.0);

        // Convert to radians, if needed
        String units = pRequest.getParameter(PARAM_TEXT_ROTATION_UNITS);
        if (!StringUtil.isEmpty(units)
                && ROTATION_DEGREES.equalsIgnoreCase(units)) {
            angle = MathUtil.toRadians(angle);
        }

        return angle;
    }

}


