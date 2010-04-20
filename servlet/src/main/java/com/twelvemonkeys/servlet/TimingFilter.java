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

package com.twelvemonkeys.servlet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * TimingFilter class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/TimingFilter.java#1 $
 */
public class TimingFilter extends GenericFilter {

    private String mAttribUsage = null;

    /**
     * Method init
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        mAttribUsage = getFilterName() + ".timerDelta";
    }

    /**
     *
     * @param pRequest
     * @param pResponse
     * @param pChain
     * @throws IOException
     * @throws ServletException
     */
    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain)
            throws IOException, ServletException {
        // Get total usage of earlier filters on same level
        Object usageAttrib = pRequest.getAttribute(mAttribUsage);
        long total = 0;

        if (usageAttrib instanceof Long) {
            // If set, get value, and remove attribute for nested resources
            total = ((Long) usageAttrib).longValue();
            pRequest.removeAttribute(mAttribUsage);
        }

        // Start timing
        long start = System.currentTimeMillis();

        try {
            // Continue chain
            pChain.doFilter(pRequest, pResponse);
        }
        finally {
            // Stop timing
            long end = System.currentTimeMillis();

            // Get time usage of included resources, add to total usage
            usageAttrib = pRequest.getAttribute(mAttribUsage);
            long usage = 0;
            if (usageAttrib instanceof Long) {
                usage = ((Long) usageAttrib).longValue();
            }

            // Get the name of the included resource
            String resourceURI = ServletUtil.getIncludeRequestURI(pRequest);

            // If none, this is probably the parent page itself
            if (resourceURI == null) {
                resourceURI = ((HttpServletRequest) pRequest).getRequestURI();
            }
            long delta = end - start;

            log("Request processing time for resource \"" + resourceURI + "\": " +
                    (delta - usage) + " ms (accumulated: " + delta + " ms).");

            // Store total usage
            total += delta;
            pRequest.setAttribute(mAttribUsage, new Long(total));
        }
    }
}