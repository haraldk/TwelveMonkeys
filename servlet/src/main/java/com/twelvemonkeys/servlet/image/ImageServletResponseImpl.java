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
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.servlet.ServletResponseStreamDelegate;
import com.twelvemonkeys.servlet.ServletUtil;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;

/**
 * This {@link ImageServletResponse} implementation can be used with image
 * requests, to have the image immediately decoded to a {@code BufferedImage}.
 * The image may be optionally subsampled, scaled and/or cropped.
 * The response also automatically handles writing the image back to the underlying response stream
 * in the preferred format, when the response is flushed.
 * <p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/ImageServletResponseImpl.java#10 $
 *
 */
// TODO: Refactor out HTTP specifics (if possible).
// TODO: Is it a good ide to throw IIOException?
// TODO: This implementation has a problem if two filters does scaling, as the second will overwrite the SIZE attribute
// TODO: Allow different scaling algorithm based on input image (use case: IndexColorModel does not scale well using default, smooth may be slow for large images)
class ImageServletResponseImpl extends HttpServletResponseWrapper implements ImageServletResponse {

    private ServletRequest originalRequest;
    private final ServletContext context;
    private final ServletResponseStreamDelegate streamDelegate;

    private FastByteArrayOutputStream bufferedOut;

    private RenderedImage image;
    private String outputContentType;

    private String originalContentType;
    private int originalContentLength = -1;

    /**
     * Creates an {@code ImageServletResponseImpl}.
     *
     * @param pRequest the request
     * @param pResponse the response
     * @param pContext the servlet context
     */
    public ImageServletResponseImpl(final HttpServletRequest pRequest, final HttpServletResponse pResponse, final ServletContext pContext) {
        super(pResponse);
        originalRequest = pRequest;
        streamDelegate = new ServletResponseStreamDelegate(pResponse) {
            @Override
            protected OutputStream createOutputStream() throws IOException {
                if (originalContentLength >= 0) {
                    bufferedOut = new FastByteArrayOutputStream(originalContentLength);
                }
                else {
                    bufferedOut = new FastByteArrayOutputStream(0);
                }

                return bufferedOut;
            }
        };
        context = pContext;
    }

    /**
     * Creates an {@code ImageServletResponseImpl}.
     *
     * @param pRequest the request
     * @param pResponse the response
     * @param pContext the servlet context
     *
     * @throws ClassCastException if {@code pRequest} is not an {@link javax.servlet.http.HttpServletRequest} or
     *         {@code pResponse} is not an {@link javax.servlet.http.HttpServletResponse}.
     */
    public ImageServletResponseImpl(final ServletRequest pRequest, final ServletResponse pResponse, final ServletContext pContext) {
        // Cheat for now...
        this((HttpServletRequest) pRequest, (HttpServletResponse) pResponse, pContext);
    }

    public void setRequest(ServletRequest pRequest) {
        originalRequest = pRequest;
    }

    /**
     * Called by the container, do not invoke.
     *
     * @param pMimeType the content (MIME) type
     */
    public void setContentType(final String pMimeType) {
        // Throw exception is already set
        if (originalContentType != null) {
            throw new IllegalStateException("ContentType already set.");
        }

        originalContentType = pMimeType;
    }

    /**
     * Called by the container. Do not invoke.
     *
     * @return the response's {@code OutputStream}
     * @throws IOException
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return streamDelegate.getOutputStream();
    }

    /**
     * Called by the container. Do not invoke.
     *
     * @return the response's {@code PrintWriter}
     * @throws IOException
     */
    public PrintWriter getWriter() throws IOException {
        return streamDelegate.getWriter();
    }

    /**
     * Called by the container. Do not invoke.
     *
     * @param pLength the content length
     */
    public void setContentLength(final int pLength) {
        if (originalContentLength != -1) {
            throw new IllegalStateException("ContentLength already set.");
        }

        originalContentLength = pLength;
    }

    /**
     * Writes the image to the original {@code ServletOutputStream}.
     * If no format is set in this response, the image is encoded in the same
     * format as the original image.
     *
     * @throws IOException if an I/O exception occurs during writing
     */
    public void flush() throws IOException {
        String outputType = getOutputContentType();

        // Force transcoding, if no other filtering is done
        if (!outputType.equals(originalContentType)) {
            getImage();
        }

        if (image != null) {
            Iterator writers = ImageIO.getImageWritersByMIMEType(outputType);
            if (writers.hasNext()) {
                super.setContentType(outputType);
                OutputStream out = super.getOutputStream();
                try {
                    ImageWriter writer = (ImageWriter) writers.next();
                    try {
                        ImageWriteParam param = writer.getDefaultWriteParam();
    ///////////////////
    // POST-PROCESS
                        // For known formats that don't support transparency, convert to opaque
                        if (isNonAlphaFormat(outputType) && image.getColorModel().getTransparency() != Transparency.OPAQUE) {
                            image = ImageUtil.toBuffered(image, BufferedImage.TYPE_INT_RGB);
                        }

                        Float requestQuality = (Float) originalRequest.getAttribute(ImageServletResponse.ATTRIB_OUTPUT_QUALITY);

                        // The default JPEG quality is not good enough, so always apply compression
                        if ((requestQuality != null || "jpeg".equalsIgnoreCase(getFormatNameSafe(writer))) && param.canWriteCompressed()) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(requestQuality != null ? requestQuality : 0.8f);
                        }
    //////////////////
                        ImageOutputStream stream = ImageIO.createImageOutputStream(out);

                        writer.setOutput(stream);
                        try {
                            writer.write(null, new IIOImage(image, null, null), param);
                        }
                        finally {
                            stream.close();
                        }
                    }
                    finally {
                        writer.dispose();
                    }
                }
                finally {
                    out.flush();
                }
            }
            else {
                context.log("ERROR: No writer for content-type: " + outputType);
                throw new IIOException("Unable to transcode image: No suitable image writer found (content-type: " + outputType + ").");
            }
        }
        else {
            super.setContentType(originalContentType);
            ServletOutputStream out = super.getOutputStream();
            try {
                bufferedOut.writeTo(out);
            }
            finally {
                out.flush();
            }
        }
    }

    private boolean isNonAlphaFormat(String outputType) {
        return "image/jpeg".equals(outputType) || "image/jpg".equals(outputType) ||
                "image/bmp".equals(outputType) || "image/x-bmp".equals(outputType);
    }

    private String getFormatNameSafe(final ImageWriter pWriter) {
        try {
            return pWriter.getOriginatingProvider().getFormatNames()[0];
        }
        catch (RuntimeException e) {
            // NPE, AIOOBE, etc..
            return null;
        }
    }

    public String getOutputContentType() {
        return outputContentType != null ? outputContentType : originalContentType;
    }

    public void setOutputContentType(final String pImageFormat) {
        outputContentType = pImageFormat;
    }

    /**
     * Sets the image for this response.
     *
     * @param pImage the {@code RenderedImage} that will be written to the
     *        response stream
     */
    public void setImage(final RenderedImage pImage) {
        image = pImage;
    }

    /**
     * Gets the decoded image from the response.
     *
     * @return a {@code BufferedImage} or {@code null} if the image could
     * not be read.
     *
     * @throws java.io.IOException if an I/O exception occurs during reading
     */
    public BufferedImage getImage() throws IOException {
        if (image == null) {
            // No content, no image
            if (bufferedOut == null) {
                return null;
            }

            // Read from the byte buffer
            InputStream byteStream = bufferedOut.createInputStream();
            ImageInputStream input = null;
            try {
                input = ImageIO.createImageInputStream(byteStream);
                Iterator readers = ImageIO.getImageReaders(input);
                if (readers.hasNext()) {
                    // Get the correct reader
                    ImageReader reader = (ImageReader) readers.next();
                    try {
                        reader.setInput(input);

                        ImageReadParam param = reader.getDefaultReadParam();

                        // Get default size
                        int originalWidth = reader.getWidth(0);
                        int originalHeight = reader.getHeight(0);
//////////////////
// PRE-PROCESS (prepare): param, size, format?, request, response?
                        // TODO: AOI strategy?
                        // Extract AOI from request
                        Rectangle aoi = extractAOIFromRequest(originalWidth, originalHeight, originalRequest);
                        if (aoi != null) {
                            param.setSourceRegion(aoi);
                            originalWidth = aoi.width;
                            originalHeight = aoi.height;
                        }

                        // TODO: Size and subsampling strategy?
                        // If possible, extract size from request
                        Dimension size = extractSizeFromRequest(originalWidth, originalHeight, originalRequest);
                        double readSubSamplingFactor = getReadSubsampleFactorFromRequest(originalRequest);
                        if (size != null) {
                            //System.out.println("Size: " + size);
                            if (param.canSetSourceRenderSize()) {
                                param.setSourceRenderSize(size);
                            }
                            else {
                                int subX = (int) Math.max(originalWidth / (double) (size.width * readSubSamplingFactor), 1.0);
                                int subY = (int) Math.max(originalHeight / (double) (size.height * readSubSamplingFactor), 1.0);

                                if (subX > 1 || subY > 1) {
                                    param.setSourceSubsampling(subX, subY, subX > 1 ? subX / 2 : 0, subY > 1 ? subY / 2 : 0);
                                }
                            }
                        }

                        // Need base URI for SVG with links/stylesheets etc
                        maybeSetBaseURIFromRequest(param);

/////////////////////

                        // Finally, read the image using the supplied parameter
                        BufferedImage image = reader.read(0, param);

                        // If reader doesn't support dynamic sizing, scale now
                        image = resampleImage(image, size);

                        // Fill bgcolor behind image, if transparent
                        extractAndSetBackgroundColor(image); // TODO: Move to flush/POST-PROCESS

                        // Set image
                        this.image = image;
                    }
                    finally {
                        reader.dispose();
                    }
                }
                else {
                    context.log("ERROR: No suitable image reader found (content-type: " + originalContentType + ").");
                    context.log("ERROR: Available formats: " + getFormatsString());

                    throw new IIOException("Unable to transcode image: No suitable image reader found (content-type: " + originalContentType + ").");
                }

                // Free resources, as the image is now either read, or unreadable
                bufferedOut = null;
            }
            finally {
                if (input != null) {
                    input.close();
                }
            }
        }

        // Image is usually a BufferedImage, but may also be a RenderedImage
        return image != null ? ImageUtil.toBuffered(image) : null;
    }

    private BufferedImage resampleImage(final BufferedImage image, final Dimension size) {
        if (image != null && size != null && (image.getWidth() != size.width || image.getHeight() != size.height)) {
            int resampleAlgorithm = getResampleAlgorithmFromRequest();

            // NOTE: Only use createScaled if IndexColorModel, as it's more expensive due to color conversion
            if (image.getColorModel() instanceof IndexColorModel) {
                return ImageUtil.createScaled(image, size.width, size.height, resampleAlgorithm);
            }
            else {
                return ImageUtil.createResampled(image, size.width, size.height, resampleAlgorithm);
            }
        }
        return image;
    }

    private int getResampleAlgorithmFromRequest() {
        Object algorithm = originalRequest.getAttribute(ATTRIB_IMAGE_RESAMPLE_ALGORITHM);
        if (algorithm instanceof Integer && ((Integer) algorithm == Image.SCALE_SMOOTH || (Integer) algorithm == Image.SCALE_FAST || (Integer) algorithm == Image.SCALE_DEFAULT)) {
            return (Integer) algorithm;
        }
        else {
            if (algorithm != null) {
                context.log("WARN: Illegal image resampling algorithm: " + algorithm);
            }
            return BufferedImage.SCALE_DEFAULT;
        }
    }

    private double getReadSubsampleFactorFromRequest(final ServletRequest pOriginalRequest) {
        double subsampleFactor;

        Object factor = pOriginalRequest.getAttribute(ATTRIB_READ_SUBSAMPLING_FACTOR);
        if (factor instanceof Number && ((Number) factor).doubleValue() >= 1.0) {
            subsampleFactor = ((Number) factor).doubleValue();
        }
        else {
            if (factor != null) {
                context.log("WARN: Illegal read subsampling factor: " + factor);
            }

            subsampleFactor = 2.0;
        }

        return subsampleFactor;
    }

    private void extractAndSetBackgroundColor(final BufferedImage pImage) {
        // TODO: bgColor request attribute instead of parameter?
        if (pImage.getColorModel().hasAlpha()) {
            String bgColor = originalRequest.getParameter("bg.color");
            if (bgColor != null) {
                Color color = StringUtil.toColor(bgColor);

                Graphics2D g = pImage.createGraphics();
                try {
                    g.setColor(color);
                    g.setComposite(AlphaComposite.DstOver);
                    g.fillRect(0, 0, pImage.getWidth(), pImage.getHeight());
                }
                finally {
                    g.dispose();
                }
            }
        }
    }

    private static String getFormatsString() {
        String[] formats = ImageIO.getReaderFormatNames();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < formats.length; i++) {
            String format = formats[i];
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(format);
        }
        return buf.toString();
    }

    private void maybeSetBaseURIFromRequest(final ImageReadParam pParam) {
        if (originalRequest instanceof HttpServletRequest) {
            try {
                // If there's a setBaseURI method, we'll try to use that (uses reflection, to avoid dependency on plugins)
                Method setBaseURI;
                try {
                    setBaseURI = pParam.getClass().getMethod("setBaseURI", String.class);
                }
                catch (NoSuchMethodException ignore) {
                    return;
                }

                // Get URL for resource and set as base
                String baseURI = ServletUtil.getContextRelativeURI((HttpServletRequest) originalRequest);

                URL resourceURL = context.getResource(baseURI);
                if (resourceURL == null) {
                    resourceURL = ServletUtil.getRealURL(context, baseURI);
                }

                if (resourceURL != null) {
                    setBaseURI.invoke(pParam, resourceURL.toExternalForm());
                }
                else {
                    context.log("WARN: Resource URL not found for URI: " + baseURI);
                }
            }
            catch (Exception e) {
                context.log("WARN: Could not set base URI: ", e);
            }
        }
    }

    private Dimension extractSizeFromRequest(final int pDefaultWidth, final int pDefaultHeight, final ServletRequest pOriginalRequest) {
        // TODO: Allow extraction from request parameters
        /*
        int sizeW = ServletUtil.getIntParameter(originalRequest, "size.w", -1);
        int sizeH = ServletUtil.getIntParameter(originalRequest, "size.h", -1);
        boolean sizePercent = ServletUtil.getBooleanParameter(originalRequest, "size.percent", false);
        boolean sizeUniform = ServletUtil.getBooleanParameter(originalRequest, "size.uniform", true);
        */
        Dimension size = (Dimension) pOriginalRequest.getAttribute(ATTRIB_SIZE);
        int sizeW = size != null ? size.width : -1;
        int sizeH = size != null ? size.height : -1;

        Boolean b = (Boolean) pOriginalRequest.getAttribute(ATTRIB_SIZE_PERCENT);
        boolean sizePercent = b != null && b; // default: false

        b = (Boolean) pOriginalRequest.getAttribute(ATTRIB_SIZE_UNIFORM);
        boolean sizeUniform = b == null || b; // default: true

        if (sizeW >= 0 || sizeH >= 0) {
            size = getSize(pDefaultWidth, pDefaultHeight, sizeW, sizeH, sizePercent, sizeUniform);
        }

        return size;
    }

    private Rectangle extractAOIFromRequest(final int pDefaultWidth, final int pDefaultHeight, final ServletRequest pOriginalRequest) {
        // TODO: Allow extraction from request parameters
        /*
        int aoiX = ServletUtil.getIntParameter(originalRequest, "aoi.x", -1);
        int aoiY = ServletUtil.getIntParameter(originalRequest, "aoi.y", -1);
        int aoiW = ServletUtil.getIntParameter(originalRequest, "aoi.w", -1);
        int aoiH = ServletUtil.getIntParameter(originalRequest, "aoi.h", -1);
        boolean aoiPercent = ServletUtil.getBooleanParameter(originalRequest, "aoi.percent", false);
        boolean aoiUniform = ServletUtil.getBooleanParameter(originalRequest, "aoi.uniform", false);
        */
        Rectangle aoi = (Rectangle) pOriginalRequest.getAttribute(ATTRIB_AOI);
        int aoiX = aoi != null ? aoi.x : -1;
        int aoiY = aoi != null ? aoi.y : -1;
        int aoiW = aoi != null ? aoi.width : -1;
        int aoiH = aoi != null ? aoi.height : -1;

        Boolean b = (Boolean) pOriginalRequest.getAttribute(ATTRIB_AOI_PERCENT);
        boolean aoiPercent = b != null && b; // default: false

        b = (Boolean) pOriginalRequest.getAttribute(ATTRIB_AOI_UNIFORM);
        boolean aoiUniform = b != null && b; // default: false

        if (aoiX >= 0 || aoiY >= 0 || aoiW >= 0 || aoiH >= 0) {
            aoi = getAOI(pDefaultWidth, pDefaultHeight, aoiX, aoiY, aoiW, aoiH, aoiPercent, aoiUniform);
            return aoi;
        }

        return null;
    }

    // TODO: Move these to ImageUtil or similar, as they are often used...
    // TODO: Consider separate methods for percent and pixels
    /**
     * Gets the dimensions (height and width) of the scaled image. The
     * dimensions are computed based on the old image's dimensions, the units
     * used for specifying new dimensions and whether or not uniform scaling
     * should be used (se algorithm below).
     *
     * @param pOriginalWidth the original width of the image
     * @param pOriginalHeight the original height of the image
     * @param pWidth        the new width of the image, or -1 if unknown
     * @param pHeight       the new height of the image, or -1 if unknown
     * @param pPercent        the constant specifying units for width and height
     *                      parameter (UNITS_PIXELS or UNITS_PERCENT)
     * @param pUniform boolean specifying uniform scale or not
     * @return a Dimension object, with the correct width and heigth
     *         in pixels, for the scaled version of the image.
     */
    static Dimension getSize(int pOriginalWidth, int pOriginalHeight,
                                       int pWidth, int pHeight,
                                       boolean pPercent, boolean pUniform) {

        // If uniform, make sure width and height are scaled the same amount
        // (use ONLY height or ONLY width).
        //
        // Algorithm:
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

        float ratio;

        if (pPercent) {
            if (pWidth >= 0 && pHeight >= 0) {
                // Non-uniform
                pWidth = Math.round((float) pOriginalWidth * (float) pWidth / 100f);
                pHeight = Math.round((float) pOriginalHeight * (float) pHeight / 100f);
            }
            else if (pWidth >= 0) {
                // Find ratio from pWidth
                ratio = (float) pWidth / 100f;
                pWidth = Math.round((float) pOriginalWidth * ratio);
                pHeight = Math.round((float) pOriginalHeight * ratio);
            }
            else if (pHeight >= 0) {
                // Find ratio from pHeight
                ratio = (float) pHeight / 100f;
                pWidth = Math.round((float) pOriginalWidth * ratio);
                pHeight = Math.round((float) pOriginalHeight * ratio);
            }
            // Else: No scale
        }
        else {
            if (pUniform) {
                if (pWidth >= 0 && pHeight >= 0) {
                    // Compute both ratios
                    ratio = (float) pWidth / (float) pOriginalWidth;
                    float heightRatio = (float) pHeight / (float) pOriginalHeight;

                    // Find the largest ratio, and use that for both
                    if (heightRatio < ratio) {
                        ratio = heightRatio;
                        pWidth = Math.round((float) pOriginalWidth * ratio);
                    }
                    else {
                        pHeight = Math.round((float) pOriginalHeight * ratio);
                    }
                }
                else if (pWidth >= 0) {
                    // Find ratio from pWidth
                    ratio = (float) pWidth / (float) pOriginalWidth;
                    pHeight = Math.round((float) pOriginalHeight * ratio);
                }
                else if (pHeight >= 0) {
                    // Find ratio from pHeight
                    ratio = (float) pHeight / (float) pOriginalHeight;
                    pWidth = Math.round((float) pOriginalWidth * ratio);
                }
                // Else: No scale
            }
        }

        // Default is no scale, just work as a proxy
        if (pWidth < 0) {
            pWidth = pOriginalWidth;
        }
        if (pHeight < 0) {
            pHeight = pOriginalHeight;
        }

        // Create new Dimension object and return
        return new Dimension(pWidth, pHeight);
    }

    static Rectangle getAOI(int pOriginalWidth, int pOriginalHeight,
                                      int pX, int pY, int pWidth, int pHeight,
                                      boolean pPercent, boolean pMaximizeToAspect) {
        // Algorithm:
        // Try to get x and y (default 0,0).
        // Try to get width and height (default width-x, height-y)
        //
        // If percent, get ratio
        //
        // If uniform
        //

        float ratio;

        if (pPercent) {
            if (pWidth >= 0 && pHeight >= 0) {
                // Non-uniform
                pWidth = Math.round((float) pOriginalWidth * (float) pWidth / 100f);
                pHeight = Math.round((float) pOriginalHeight * (float) pHeight / 100f);
            }
            else if (pWidth >= 0) {
                // Find ratio from pWidth
                ratio = (float) pWidth / 100f;
                pWidth = Math.round((float) pOriginalWidth * ratio);
                pHeight = Math.round((float) pOriginalHeight * ratio);
            }
            else if (pHeight >= 0) {
                // Find ratio from pHeight
                ratio = (float) pHeight / 100f;
                pWidth = Math.round((float) pOriginalWidth * ratio);
                pHeight = Math.round((float) pOriginalHeight * ratio);
            }
            // Else: No crop
        }
        else {
            // Uniform
            if (pMaximizeToAspect) {
                if (pWidth >= 0 && pHeight >= 0) {
                    // Compute both ratios
                    ratio = (float) pWidth / (float) pHeight;
                    float originalRatio = (float) pOriginalWidth / (float) pOriginalHeight;
                    if (ratio > originalRatio) {
                        pWidth = pOriginalWidth;
                        pHeight = Math.round((float) pOriginalWidth / ratio);
                    }
                    else {
                        pHeight = pOriginalHeight;
                        pWidth = Math.round((float) pOriginalHeight * ratio);
                    }
                }
                else if (pWidth >= 0) {
                    // Find ratio from pWidth
                    ratio = (float) pWidth / (float) pOriginalWidth;
                    pHeight = Math.round((float) pOriginalHeight * ratio);
                }
                else if (pHeight >= 0) {
                    // Find ratio from pHeight
                    ratio = (float) pHeight / (float) pOriginalHeight;
                    pWidth = Math.round((float) pOriginalWidth * ratio);
                }
                // Else: No crop
            }
        }

        // Not specified, or outside bounds: Use original dimensions
        if (pWidth < 0 || (pX < 0 && pWidth > pOriginalWidth)
                || (pX >= 0 && (pX + pWidth) > pOriginalWidth)) {
            pWidth = (pX >= 0 ? pOriginalWidth - pX : pOriginalWidth);
        }
        if (pHeight < 0 || (pY < 0 && pHeight > pOriginalHeight)
                || (pY >= 0 && (pY + pHeight) > pOriginalHeight)) {
            pHeight = (pY >= 0 ? pOriginalHeight - pY : pOriginalHeight);
        }

        // Center
        if (pX < 0) {
            pX = (pOriginalWidth - pWidth) / 2;
        }
        if (pY < 0) {
            pY = (pOriginalHeight - pHeight) / 2;
        }

//        System.out.println("x: " + pX + " y: " + pY
//                           + " w: " + pWidth + " h " + pHeight);

        return new Rectangle(pX, pY, pWidth, pHeight);
    }
}