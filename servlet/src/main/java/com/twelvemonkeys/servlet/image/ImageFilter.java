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
import com.twelvemonkeys.servlet.GenericFilter;

import javax.servlet.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * Abstract base class for image filters. Automatically decoding and encoding of
 * the image is handled in the {@code doFilterImpl} method.
 *
 * @see #doFilter(java.awt.image.BufferedImage,javax.servlet.ServletRequest,ImageServletResponse)
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/ImageFilter.java#2 $
 *
 */
public abstract class ImageFilter extends GenericFilter {

    protected String[] mTriggerParams = null;

    /**
     * The {@code doFilterImpl} method is called once, or each time a
     * request/response pair is passed through the chain, depending on the
     * {@link #mOncePerRequest} member variable.
     *
     * @see #mOncePerRequest
     * @see com.twelvemonkeys.servlet.GenericFilter#doFilterImpl doFilter
     * @see Filter#doFilter Filter.doFilter
     *
     * @param pRequest the servlet request
     * @param pResponse the servlet response
     * @param pChain the filter chain
     *
     * @throws IOException
     * @throws ServletException
     */
    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain)
            throws IOException, ServletException {

        //System.out.println("Starting filtering...");
        // Test for trigger params
        if (!trigger(pRequest)) {
            //System.out.println("Passing request on to next in chain (skipping " + getFilterName() + ")...");
            // Pass the request on
            pChain.doFilter(pRequest, pResponse);
        }
        else {
            // For images, we do post filtering only and need to wrap the response
            ImageServletResponse imageResponse;
            boolean encode;
            if (pResponse instanceof ImageServletResponse) {
                //System.out.println("Allready ImageServletResponse");
                imageResponse = (ImageServletResponse) pResponse;
                encode = false; // Allready wrapped, will be encoded later in the chain
            }
            else {
                //System.out.println("Wrapping in ImageServletResponse");
                imageResponse = new ImageServletResponseImpl(pRequest, pResponse, getServletContext());
                encode = true; // This is first filter in chain, must encode when done
            }

            //System.out.println("Passing request on to next in chain...");
            // Pass the request on
            pChain.doFilter(pRequest, imageResponse);

            //System.out.println("Post filtering...");

            // Get image
            //System.out.println("Getting image from ImageServletResponse...");
            // Get the image from the wrapped response
            RenderedImage image = imageResponse.getImage();
            //System.out.println("Got image: " + image);

            // Note: Image will be null if this is a HEAD request, the
            // If-Modified-Since header is present, or similar.
            if (image != null) {
                // Do the image filtering
                //System.out.println("Filtering image (" + getFilterName() + ")...");
                image = doFilter(ImageUtil.toBuffered(image), pRequest, imageResponse);
                //System.out.println("Done filtering.");

                //System.out.println("Making image available...");
                // Make image available to other filters (avoid unnecessary
                // serializing/deserializing)
                imageResponse.setImage(image);
                //System.out.println("Done.");

                if (encode) {
                    //System.out.println("Encoding image...");
                    // Encode image to original repsonse
                    if (image != null) {
                        // TODO: Be smarter than this...
                        // TODO: Make sure ETag is same, if image content is the same...
                        // Use ETag of original response (or derived from)
                        // Use last modified of original response? Or keep original resource's, don't set at all? 
                        // TODO: Why weak ETag?
                        String etag = "W/\"" + Integer.toHexString(hashCode()) + "-" + Integer.toHexString(image.hashCode()) + "\"";
                        ((ImageServletResponseImpl) imageResponse).setHeader("ETag", etag);
                        ((ImageServletResponseImpl) imageResponse).setDateHeader("Last-Modified", (System.currentTimeMillis() / 1000) * 1000);
                        imageResponse.flush();
                    }
                    //System.out.println("Done encoding.");
                }
            }
        }
        //System.out.println("Filtering done.");
    }

    /**
     * Tests if the filter should do image filtering/processing.
     * <P/>
     * This default implementation uses {@link #mTriggerParams} to test if:
     * <dl>
     *  <dt>{@code mTriggerParams == null}</dt>
     *  <dd>{@code return true}</dd>
     *  <dt>{@code mTriggerParams != null}, loop through parameters, and test
     *      if {@code pRequest} contains the parameter. If match</dt>
     *  <dd>{@code return true}</dd>
     *  <dt>Otherwise</dt>
     *  <dd>{@code return false}</dd>
     * </dl>
     *
     *
     * @param pRequest the servlet request
     * @return {@code true} if the filter should do image filtering
     */
    protected boolean trigger(ServletRequest pRequest) {
        // If triggerParams not set, assume always trigger
        if (mTriggerParams == null) {
            return true;
        }

        // Trigger only for certain request parameters
        for (String triggerParam : mTriggerParams) {
            if (pRequest.getParameter(triggerParam) != null) {
                return true;
            }
        }

        // Didn't trigger
        return false;
    }

    /**
     * Sets the trigger parameters.
     * The parameter is supposed to be a comma-separated string of parameter
     * names.
     *
     * @param pTriggerParams a comma-separated string of parameter names.
     */
    public void setTriggerParams(String pTriggerParams) {
        mTriggerParams = StringUtil.toStringArray(pTriggerParams);
    }

    /**
     * Filters the image for this request.
     *
     * @param pImage the image to filter
     * @param pRequest the servlet request
     * @param pResponse the servlet response
     *
     * @return the filtered image
     * @throws java.io.IOException if an I/O error occurs during filtering
     */
    protected abstract RenderedImage doFilter(BufferedImage pImage, ServletRequest pRequest, ImageServletResponse pResponse) throws IOException;
}
