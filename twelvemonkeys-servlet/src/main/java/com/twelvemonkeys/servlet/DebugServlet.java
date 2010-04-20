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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * DebugServlet class description.
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/DebugServlet.java#1 $
 */
public class DebugServlet extends GenericServlet {
    private long mDateModified;

    public final void service(ServletRequest pRequest, ServletResponse pResponse) throws ServletException, IOException {
        service((HttpServletRequest) pRequest, (HttpServletResponse) pResponse);
    }

    public void init() throws ServletException {
        super.init();
        mDateModified = System.currentTimeMillis();
    }

    public void service(HttpServletRequest pRequest, HttpServletResponse pResponse) throws ServletException, IOException {
        pResponse.setContentType("text/plain");
        // Include these to allow browser caching
        pResponse.setDateHeader("Last-Modified", mDateModified);
        pResponse.setHeader("ETag", getServletName());

        ServletOutputStream out = pResponse.getOutputStream();

        out.println("Remote address: " +  pRequest.getRemoteAddr());
        out.println("Remote host name: " + pRequest.getRemoteHost());
        out.println("Remote user: " + pRequest.getRemoteUser());
        out.println();

        out.println("Request Method: " + pRequest.getMethod());
        out.println("Request Scheme: " + pRequest.getScheme());
        out.println("Request URI: " + pRequest.getRequestURI());
        out.println("Request URL: " + pRequest.getRequestURL().toString());
        out.println("Request PathInfo: " + pRequest.getPathInfo());
        out.println("Request ContentLength: " + pRequest.getContentLength());
        out.println();

        out.println("Request Headers:");
        Enumeration headerNames = pRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            Enumeration headerValues = pRequest.getHeaders(headerName);

            if (headerName != null) {
                while (headerValues.hasMoreElements()) {
                    String value = (String) headerValues.nextElement();
                    out.println("   " + headerName + ": " + value);
                }
            }
        }
        out.println();

        out.println("Request parameters:");
        Enumeration paramNames = pRequest.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = (String) paramNames.nextElement();
            String[] values = pRequest.getParameterValues(name);

            for (String value : values) {
                out.println("   " + name + ": " + value);
            }
        }
        out.println();

        out.println("Request attributes:");
        Enumeration attribNames = pRequest.getAttributeNames();
        while (attribNames.hasMoreElements()) {
            String name = (String) attribNames.nextElement();
            Object value = pRequest.getAttribute(name);
            out.println("   " + name + ": " + value);
        }


        out.flush();
    }
}
