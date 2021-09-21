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

package com.twelvemonkeys.servlet.image;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.servlet.GenericFilter;

/**
 * Abstract base class for image filters. Automatically decoding and encoding of
 * the image is handled in the {@code doFilterImpl} method.
 *
 * @see #doFilter(java.awt.image.BufferedImage,javax.servlet.ServletRequest,ImageServletResponse)
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ImageFilter.java#2 $
 *
 */
@Deprecated
public abstract class ImageFilter extends GenericFilter {
    // TODO: Take the design back to the drawing board (see ImageServletResponseImpl)
    //      - Allow multiple filters to set size attribute
    //      - Allow a later filter to reset, to get pass-through given certain criteria...
    //      - Or better yet, allow a filter to decide if it wants to decode, based on image metadata on the original image (ie: width/height)

    protected String[] triggerParams = null;

    /**
     * The {@code doFilterImpl} method is called once, or each time a
     * request/response pair is passed through the chain, depending on the
     * {@link #oncePerRequest} member variable.
     *
     * @see #oncePerRequest
     * @see com.twelvemonkeys.servlet.GenericFilter#doFilterImpl(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)  doFilter
     * @see Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)  Filter.doFilter
     *
     * @param pRequest the servlet request
     * @param pResponse the servlet response
     * @param pChain the filter chain
     *
     * @throws IOException
     * @throws ServletException
     */
    protected void doFilterImpl(final ServletRequest pRequest, final ServletResponse pResponse, final FilterChain pChain)
            throws IOException, ServletException {

        //System.out.println("Starting filtering...");
        // Test for trigger params
        if (!trigger(pRequest)) {
            //System.out.println("Passing request on to next in chain (skipping " + getFilterName() + ")...");
            // Pass the request on
            pChain.doFilter(pRequest, pResponse);
        }
        else {
            // If already wrapped, the image will be encoded later in the chain
            // Or, if this is first filter in chain, we must encode when done
            boolean encode = !(pResponse instanceof ImageServletResponse);

            // For images, we do post filtering only and need to wrap the response
            ImageServletResponse imageResponse = createImageServletResponse(pRequest, pResponse);

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
                // Make image available to other filters (avoid unnecessary serializing/deserializing)
                imageResponse.setImage(image);
                //System.out.println("Done.");
            }
            if (encode) {
                //System.out.println("Encoding image...");
                // Encode image to original response
                if (image != null) {
                    // TODO: Be smarter than this...
                    // TODO: Make sure ETag is same, if image content is the same...
                    // Use ETag of original response (or derived from)
                    // Use last modified of original response? Or keep original resource's, don't set at all?
                    // TODO: Why weak ETag?
                    String etag = "W/\"" + Integer.toHexString(hashCode()) + "-" + Integer.toHexString(image.hashCode()) + "\"";
                    // TODO: This breaks for wrapped instances, need to either unwrap or test for HttpSR...
                    ((HttpServletResponse) pResponse).setHeader("ETag", etag);
                    ((HttpServletResponse) pResponse).setDateHeader("Last-Modified", (System.currentTimeMillis() / 1000) * 1000);
                }

                imageResponse.flush();
                //System.out.println("Done encoding.");
            }
        }
        //System.out.println("Filtering done.");
    }

    /**
     * Creates the image servlet response for this response.
     *
     * @param pResponse the original response
     * @param pRequest the original request
     * @return the new response, or {@code pResponse} if the response is already wrapped
     *
     * @see com.twelvemonkeys.servlet.image.ImageServletResponseImpl
     */
    private ImageServletResponse createImageServletResponse(final ServletRequest pRequest, final ServletResponse pResponse) {
        if (pResponse instanceof ImageServletResponseImpl) {
            ImageServletResponseImpl response = (ImageServletResponseImpl) pResponse;
//            response.setRequest(pRequest);
            return response;
        }

        return new ImageServletResponseImpl(pRequest, pResponse, getServletContext());
    }

    /**
     * Tests if the filter should do image filtering/processing.
     * <p>
     * This default implementation uses {@link #triggerParams} to test if:
     * </p>
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
     * @param pRequest the servlet request
     * @return {@code true} if the filter should do image filtering
     */
    protected boolean trigger(final ServletRequest pRequest) {
        // If triggerParams not set, assume always trigger
        if (triggerParams == null) {
            return true;
        }

        // Trigger only for certain request parameters
        for (String triggerParam : triggerParams) {
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
    // TODO: Make it an @InitParam, and make sure we may set String[]/Collection<String> as parameter?
    public void setTriggerParams(final String pTriggerParams) {
        triggerParams = StringUtil.toStringArray(pTriggerParams);
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
