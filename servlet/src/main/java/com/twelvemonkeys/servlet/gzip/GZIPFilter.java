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

package com.twelvemonkeys.servlet.gzip;

import com.twelvemonkeys.servlet.GenericFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A filter to reduce the output size of web resources.
 * <p/>
 * The HTTP protocol supports compression of the content to reduce network
 * bandwidth. The important headers involved, are the {@code Accept-Encoding}
 * request header, and the {@code Content-Encoding} response header.
 * This feature can be used to further reduce the number of bytes transferred
 * over the network, at the cost of some extra processing time at both endpoints.
 * Most modern browsers supports compression in GZIP format, which is fairly
 * efficient in cost/compression ratio.
 * <p/>
 * The filter tests for the presence of an {@code Accept-Encoding} header with a
 * value of {@code "gzip"} (several different encoding header values are
 * possible in one header). If not present, the filter simply passes the
 * request/response pair through, leaving it untouched. If present, the
 * {@code Content-Encoding} header is set, with the value {@code "gzip"},
 * and the response is wrapped.
 * The response output stream is wrapped in a
 * {@link java.util.zip.GZIPOutputStream} which performs the GZIP encoding.
 * For efficiency, the filter does not buffer the response, but writes through
 * the gzipped output stream.  
 * <p/>
 * <b>Configuration</b><br/>
 * To use {@code GZIPFilter} in your web-application, you simply need to add it
 * to your web descriptor ({@code web.xml}). If using a servlet container that
 *  supports the Servlet 2.4 spec, the new {@code dispatcher} element should be
 * used, and set to {@code REQUEST/FORWARD}, to make sure the filter is invoked
 * only once for requests.
 * If using an older web descriptor, set the {@code init-param}
 * {@code "once-per-request"} to {@code "true"} (this will have the same effect,
 * but might perform slightly worse than the 2.4 version).
 * Please see the examples below.
 * <b>Servlet 2.4 version, filter section:</b><br/>
 * <pre>
 * &lt;!-- GZIP Filter Configuration --&gt;
 * &lt;filter&gt;
 *      &lt;filter-name&gt;gzip&lt;/filter-name&gt;
 *      &lt;filter-class&gt;com.twelvemonkeys.servlet.GZIPFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * </pre>
 * <b>Filter-mapping section:</b><br/>
 * <pre>
 * &lt;!-- GZIP Filter Mapping --&gt;
 * &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;gzip&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;*.html&lt;/url-pattern&gt;
 *      &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *      &lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;gzip&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;*.jsp&lt; /url-pattern&gt;
 *      &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *      &lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p/>
 * Based on ideas and code found in the ONJava article
 * <a href="http://www.onjava.com/pub/a/onjava/2003/11/19/filters.html">Two
 * Servlet Filters Every Web Application Should Have</a>
 * by Jayson Falkner.
 * <p/>
 *
 * @author Jayson Falkner
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: GZIPFilter.java#1 $
 */
public class GZIPFilter extends GenericFilter {

    {
        oncePerRequest = true;
    }

    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        // Can only filter HTTP responses
        if (pRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) pRequest;
            HttpServletResponse response = (HttpServletResponse) pResponse;

            // If GZIP is supported, use compression
            String accept = request.getHeader("Accept-Encoding");
            if (accept != null && accept.contains("gzip")) {
                //System.out.println("GZIP supported, compressing.");
                GZIPResponseWrapper wrapped = new GZIPResponseWrapper(response);

                try {
                    pChain.doFilter(pRequest, wrapped);
                }
                finally {
                    wrapped.flushResponse();
                }

                return;
            }
        }

        // Else, continue chain
        pChain.doFilter(pRequest, pResponse);
    }
}
