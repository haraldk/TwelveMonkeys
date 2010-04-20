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
 * The response also automtically handles writing the image back to the underlying response stream
 * in the preferred format, when the response is flushed.
 * <p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/ImageServletResponseImpl.java#10 $
 *
 */
// TODO: Refactor out HTTP specifcs (if possible).
// TODO: Is it a good ide to throw IIOException?
class ImageServletResponseImpl extends HttpServletResponseWrapper implements ImageServletResponse {

    private final ServletRequest mOriginalRequest;
    private final ServletContext mContext;
    private final ServletResponseStreamDelegate mStreamDelegate;

    private FastByteArrayOutputStream mBufferedOut;

    private RenderedImage mImage;
    private String mOutputContentType;

    private String mOriginalContentType;
    private int mOriginalContentLength = -1;

    /**
     * Creates an {@code ImageServletResponseImpl}.
     *
     * @param pRequest the request
     * @param pResponse the response
     * @param pContext the servlet context
     */
    public ImageServletResponseImpl(final HttpServletRequest pRequest, final HttpServletResponse pResponse, final ServletContext pContext) {
        super(pResponse);
        mOriginalRequest = pRequest;
        mStreamDelegate = new ServletResponseStreamDelegate(pResponse) {
            @Override
            protected OutputStream createOutputStream() throws IOException {
                if (mOriginalContentLength >= 0) {
                    mBufferedOut = new FastByteArrayOutputStream(mOriginalContentLength);
                }
                else {
                    mBufferedOut = new FastByteArrayOutputStream(0);
                }

                return mBufferedOut;
            }
        };
        mContext = pContext;
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

    /**
     * Called by the container, do not invoke.
     *
     * @param pMimeType the content (MIME) type
     */
    public void setContentType(final String pMimeType) {
        // Throw exception is already set
        if (mOriginalContentType != null) {
            throw new IllegalStateException("ContentType already set.");
        }

        mOriginalContentType = pMimeType;
    }

    /**
     * Called by the container. Do not invoke.
     *
     * @return the response's {@code OutputStream}
     * @throws IOException
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return mStreamDelegate.getOutputStream();
    }

    /**
     * Called by the container. Do not invoke.
     *
     * @return the response's {@code PrintWriter}
     * @throws IOException
     */
    public PrintWriter getWriter() throws IOException {
        return mStreamDelegate.getWriter();
    }

    /**
     * Called by the container. Do not invoke.
     *
     * @param pLength the content length
     */
    public void setContentLength(final int pLength) {
        if (mOriginalContentLength != -1) {
            throw new IllegalStateException("ContentLength already set.");
        }

        mOriginalContentLength = pLength;
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
        if (!outputType.equals(mOriginalContentType)) {
            getImage();
        }

        // For known formats that don't support transparency, convert to opaque
        if (("image/jpeg".equals(outputType) || "image/jpg".equals(outputType)
                || "image/bmp".equals(outputType) || "image/x-bmp".equals(outputType)) &&
                mImage.getColorModel().getTransparency() != Transparency.OPAQUE) {
            mImage = ImageUtil.toBuffered(mImage, BufferedImage.TYPE_INT_RGB);
        }

        if (mImage != null) {
            Iterator writers = ImageIO.getImageWritersByMIMEType(outputType);
            if (writers.hasNext()) {
                super.setContentType(outputType);
                OutputStream out = super.getOutputStream();

                ImageWriter writer = (ImageWriter) writers.next();
                try {
                    ImageWriteParam param = writer.getDefaultWriteParam();

                    Float requestQuality = (Float) mOriginalRequest.getAttribute(ImageServletResponse.ATTRIB_OUTPUT_QUALITY);

                    // The default JPEG quality is not good enough, so always apply compression
                    if ((requestQuality != null || "jpeg".equalsIgnoreCase(getFormatNameSafe(writer))) && param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(requestQuality != null ? requestQuality : 0.8f);
                    }

                    ImageOutputStream stream = ImageIO.createImageOutputStream(out);

                    writer.setOutput(stream);
                    try {
                        writer.write(null, new IIOImage(mImage, null, null), param);
                    }
                    finally {
                        stream.close();
                    }
                }
                finally {
                    writer.dispose();
                    out.flush();
                }
            }
            else {
                mContext.log("ERROR: No writer for content-type: " + outputType);
                throw new IIOException("Unable to transcode image: No suitable image writer found (content-type: " + outputType + ").");
            }
        }
        else {
            super.setContentType(mOriginalContentType);
            ServletOutputStream out = super.getOutputStream();
            try {
                mBufferedOut.writeTo(out);
            }
            finally {
                out.flush();
            }
        }
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
        return  mOutputContentType != null ? mOutputContentType : mOriginalContentType;
    }

    public void setOutputContentType(final String pImageFormat) {
        mOutputContentType = pImageFormat;
    }

    /**
     * Sets the image for this response.
     *
     * @param pImage the {@code RenderedImage} that will be written to the
     *        response stream
     */
    public void setImage(final RenderedImage pImage) {
        mImage = pImage;
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
        if (mImage == null) {
            // No content, no image
            if (mBufferedOut == null) {
                return null;
            }

            // Read from the byte buffer
            InputStream byteStream = mBufferedOut.createInputStream();
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

                        // Extract AOI from request
                        Rectangle aoi = extractAOIFromRequest(originalWidth, originalHeight);
                        if (aoi != null) {
                            param.setSourceRegion(aoi);
                            originalWidth = aoi.width;
                            originalHeight = aoi.height;
                        }

                        // If possible, extract size from request
                        Dimension size = extractSizeFromRequest(originalWidth, originalHeight);
                        double readSubSamplingFactor = getReadSubsampleFactorFromRequest();
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

                        // Finally, read the image using the supplied parameter
                        BufferedImage image = reader.read(0, param);

                        // If reader doesn't support dynamic sizing, scale now
                        if (image != null && size != null
                                && (image.getWidth() != size.width || image.getHeight() != size.height)) {

                            int resampleAlgorithm = getResampleAlgorithmFromRequest();
                            // NOTE: Only use createScaled if IndexColorModel,
                            //  as it's more expensive due to color conversion
                            if (image.getColorModel() instanceof IndexColorModel) {
                                image = ImageUtil.createScaled(image, size.width, size.height, resampleAlgorithm);
                            }
                            else {
                                image = ImageUtil.createResampled(image, size.width, size.height, resampleAlgorithm);
                            }
                        }

                        // Fill bgcolor behind image, if transparent
                        extractAndSetBackgroundColor(image);

                        // Set image
                        mImage = image;
                    }
                    finally {
                        reader.dispose();
                    }
                }
                else {
                    mContext.log("ERROR: No suitable image reader found (content-type: " + mOriginalContentType + ").");
                    mContext.log("ERROR: Available formats: " + getFormatsString());

                    throw new IIOException("Unable to transcode image: No suitable image reader found (content-type: " + mOriginalContentType + ").");
                }

                // Free resources, as the image is now either read, or unreadable
                mBufferedOut = null;
            }
            finally {
                if (input != null) {
                    input.close();
                }
            }
        }

        // Image is usually a BufferedImage, but may also be a RenderedImage
        return mImage != null ? ImageUtil.toBuffered(mImage) : null;
    }

    private int getResampleAlgorithmFromRequest() {
        int resampleAlgoithm;

        Object algorithm = mOriginalRequest.getAttribute(ATTRIB_IMAGE_RESAMPLE_ALGORITHM);
        if (algorithm instanceof Integer && ((Integer) algorithm == Image.SCALE_SMOOTH || (Integer) algorithm == Image.SCALE_FAST || (Integer) algorithm == Image.SCALE_DEFAULT)) {
            resampleAlgoithm = (Integer) algorithm;
        }
        else {
            if (algorithm != null) {
                mContext.log("WARN: Illegal image resampling algorithm: " + algorithm);
            }
            resampleAlgoithm = BufferedImage.SCALE_DEFAULT;
        }

        return resampleAlgoithm;
    }

    private double getReadSubsampleFactorFromRequest() {
        double subsampleFactor;

        Object factor = mOriginalRequest.getAttribute(ATTRIB_READ_SUBSAMPLING_FACTOR);
        if (factor instanceof Number && ((Number) factor).doubleValue() >= 1.0) {
            subsampleFactor = ((Number) factor).doubleValue();
        }
        else {
            if (factor != null) {
                mContext.log("WARN: Illegal read subsampling factor: " + factor);
            }
            subsampleFactor = 2.0;
        }

        return subsampleFactor;
    }

    private void extractAndSetBackgroundColor(final BufferedImage pImage) {
        // TODO: bgColor request attribute instead of parameter?
        if (pImage.getColorModel().hasAlpha()) {
            String bgColor = mOriginalRequest.getParameter("bg.color");
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
        if (mOriginalRequest instanceof HttpServletRequest) {
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
                String baseURI = ServletUtil.getContextRelativeURI((HttpServletRequest) mOriginalRequest);

                URL resourceURL = mContext.getResource(baseURI);
                if (resourceURL == null) {
                    resourceURL = ServletUtil.getRealURL(mContext, baseURI);
                }

                if (resourceURL != null) {
                    setBaseURI.invoke(pParam, resourceURL.toExternalForm());
                }
                else {
                    mContext.log("WARN: Resource URL not found for URI: " + baseURI);
                }
            }
            catch (Exception e) {
                mContext.log("WARN: Could not set base URI: ", e);
            }
        }
    }

    private Dimension extractSizeFromRequest(final int pDefaultWidth, final int pDefaultHeight) {
        // TODO: Allow extraction from request parameters
        /*
        int sizeW = ServletUtil.getIntParameter(mOriginalRequest, "size.w", -1);
        int sizeH = ServletUtil.getIntParameter(mOriginalRequest, "size.h", -1);
        boolean sizePercent = ServletUtil.getBooleanParameter(mOriginalRequest, "size.percent", false);
        boolean sizeUniform = ServletUtil.getBooleanParameter(mOriginalRequest, "size.uniform", true);
        */
        Dimension size = (Dimension) mOriginalRequest.getAttribute(ATTRIB_SIZE);
        int sizeW = size != null ? size.width : -1;
        int sizeH = size != null ? size.height : -1;

        Boolean b = (Boolean) mOriginalRequest.getAttribute(ATTRIB_SIZE_PERCENT);
        boolean sizePercent = b != null && b; // default: false

        b = (Boolean) mOriginalRequest.getAttribute(ATTRIB_SIZE_UNIFORM);
        boolean sizeUniform = b == null || b; // default: true

        if (sizeW >= 0 || sizeH >= 0) {
            size = getSize(pDefaultWidth, pDefaultHeight, sizeW, sizeH, sizePercent, sizeUniform);
        }

        return size;
    }

    private Rectangle extractAOIFromRequest(final int pDefaultWidth, final int pDefaultHeight) {
        // TODO: Allow extraction from request parameters
        /*
        int aoiX = ServletUtil.getIntParameter(mOriginalRequest, "aoi.x", -1);
        int aoiY = ServletUtil.getIntParameter(mOriginalRequest, "aoi.y", -1);
        int aoiW = ServletUtil.getIntParameter(mOriginalRequest, "aoi.w", -1);
        int aoiH = ServletUtil.getIntParameter(mOriginalRequest, "aoi.h", -1);
        boolean aoiPercent = ServletUtil.getBooleanParameter(mOriginalRequest, "aoi.percent", false);
        boolean aoiUniform = ServletUtil.getBooleanParameter(mOriginalRequest, "aoi.uniform", false);
        */
        Rectangle aoi = (Rectangle) mOriginalRequest.getAttribute(ATTRIB_AOI);
        int aoiX = aoi != null ? aoi.x : -1;
        int aoiY = aoi != null ? aoi.y : -1;
        int aoiW = aoi != null ? aoi.width : -1;
        int aoiH = aoi != null ? aoi.height : -1;

        Boolean b = (Boolean) mOriginalRequest.getAttribute(ATTRIB_AOI_PERCENT);
        boolean aoiPercent = b != null && b; // default: false

        b = (Boolean) mOriginalRequest.getAttribute(ATTRIB_AOI_UNIFORM);
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
     * @param pUniformScale boolean specifying uniform scale or not
     * @return a Dimension object, with the correct width and heigth
     *         in pixels, for the scaled version of the image.
     */
    protected static Dimension getSize(int pOriginalWidth, int pOriginalHeight,
                                       int pWidth, int pHeight,
                                       boolean pPercent, boolean pUniformScale) {

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
            if (pUniformScale) {
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

    protected static Rectangle getAOI(int pOriginalWidth, int pOriginalHeight,
                                      int pX, int pY, int pWidth, int pHeight,
                                      boolean pPercent, boolean pUniform) {
        // Algoritm:
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
            if (pUniform) {
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