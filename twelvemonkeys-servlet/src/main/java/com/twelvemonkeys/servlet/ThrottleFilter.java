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

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ThrottleFilter, a filter for easing server during heavy load.
 * <!--
 * Renamed from LoadShutoffFilter...
 * Happened to be listening to Xploding Plastix' Shakedown Shutoff at the time..
 * -->
 * Intercepts requests, and returns HTTP response code 503
 * (Service Unavailable), if there are more than a given number of concurrent
 * requests, to avoid large backlogs. The number of concurrent requests and the
 * response messages sent to the user agent, is configurable from the web
 * descriptor.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/ThrottleFilter.java#1 $
 * @see #setMaxConcurrentThreadCount
 * @see #setResponseMessages
 */
public class ThrottleFilter extends GenericFilter {

    /**
     * Minimum free thread count, defaults to {@code 10}
     */
    protected int mMaxConcurrentThreadCount = 10;

    /**
     * The number of running request threads
     */
    private int mRunningThreads = 0;
    private final Object mRunningThreadsLock = new Object();

    /**
     * Default response message sent to user agents, if the request is rejected
     */
    protected final static String DEFUALT_RESPONSE_MESSAGE =
            "Service temporarily unavailable, please try again later.";

    /**
     * Default response content type
     */
    protected static final String DEFAULT_TYPE = "text/html";

    /**
     * The reposne message sent to user agenta, if the request is rejected
     */
    private Map mResponseMessageNames = new HashMap(10);

    /**
     * The reposne message sent to user agents, if the request is rejected
     */
    private String[] mResponseMessageTypes = null;

    /**
     * Cache for response messages
     */
    private Map mResponseCache = new HashMap(10);


    /**
     * Sets the minimum free thread count.
     *
     * @param pMaxConcurrentThreadCount
     */
    public void setMaxConcurrentThreadCount(String pMaxConcurrentThreadCount) {
        if (!StringUtil.isEmpty(pMaxConcurrentThreadCount)) {
            try {
                mMaxConcurrentThreadCount = Integer.parseInt(pMaxConcurrentThreadCount);
            }
            catch (NumberFormatException nfe) {
                // Use default
            }
        }
    }

    /**
     * Sets the response message sent to the user agent, if the request is
     * rejected.
     * <BR/>
     * The format is {@code &lt;mime-type&gt;=&lt;filename&gt;,
     * &lt;mime-type&gt;=&lt;filename&gt;}.
     * <BR/>
     * Example: {@code &lt;text/vnd.wap.wmlgt;=&lt;/errors/503.wml&gt;,
     * &lt;text/html&gt;=&lt;/errors/503.html&gt;}
     *
     * @param pResponseMessages
     */
    public void setResponseMessages(String pResponseMessages) {
        // Split string in type=filename pairs
        String[] mappings = StringUtil.toStringArray(pResponseMessages, ", \r\n\t");
        List types = new ArrayList();

        for (int i = 0; i < mappings.length; i++) {
            // Split pairs on '='
            String[] mapping = StringUtil.toStringArray(mappings[i], "= ");

            // Test for wrong mapping
            if ((mapping == null) || (mapping.length < 2)) {
                log("Error in init param \"responseMessages\": " + pResponseMessages);
                continue;
            }
            types.add(mapping[0]);
            mResponseMessageNames.put(mapping[0], mapping[1]);
        }

        // Create arrays
        mResponseMessageTypes = (String[]) types.toArray(new String[types.size()]);
    }

    /**
     * @param pRequest
     * @param pResponse
     * @param pChain
     * @throws IOException
     * @throws ServletException
     */
    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain)
            throws IOException, ServletException {
        try {
            if (beginRequest()) {
                // Continue request
                pChain.doFilter(pRequest, pResponse);
            }
            else {
                // Send error and end request
                // Get HTTP specific versions
                HttpServletRequest request = (HttpServletRequest) pRequest;
                HttpServletResponse response = (HttpServletResponse) pResponse;

                // Get content type
                String contentType = getContentType(request);

                // Note: This is not the way the spec says you should do it.
                // However, we handle error response this way for preformace reasons.
                // The "correct" way would be to use sendError() and register a servlet
                // that does the content negotiation as errorpage in the web descriptor.
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType(contentType);
                response.getWriter().println(getMessage(contentType));

                // Log warning, as this shouldn't happen too often
                log("Request denied, no more available threads for requestURI=" + request.getRequestURI());
            }
        }
        finally {
            doneRequest();
        }
    }

    /**
     * Marks the beginning of a request
     *
     * @return <CODE>true<CODE> if the request should be handled.
     */
    private boolean beginRequest() {
        synchronized (mRunningThreadsLock) {
            mRunningThreads++;
        }
        return (mRunningThreads <= mMaxConcurrentThreadCount);
    }

    /**
     * Marks the end of the request
     */
    private void doneRequest() {
        synchronized (mRunningThreadsLock) {
            mRunningThreads--;
        }
    }

    /**
     * Gets the content type for the response, suitable for the requesting user agent.
     *
     * @param pRequest
     * @return the content type
     */
    private String getContentType(HttpServletRequest pRequest) {
        if (mResponseMessageTypes != null) {
            String accept = pRequest.getHeader("Accept");

            for (int i = 0; i < mResponseMessageTypes.length; i++) {
                String type = mResponseMessageTypes[i];

                // Note: This is not 100% correct way of doing content negotiation
                // But we just want a compatible result, quick, so this is okay
                if (StringUtil.contains(accept, type)) {
                    return type;
                }
            }
        }

        // If none found, return default
        return DEFAULT_TYPE;
    }

    /**
     * Gets the response message for the given content type.
     *
     * @param pContentType
     * @return the message
     */
    private String getMessage(String pContentType) {

        String fileName = (String) mResponseMessageNames.get(pContentType);

        // Get cached value
        CacheEntry entry = (CacheEntry) mResponseCache.get(fileName);

        if ((entry == null) || entry.isExpired()) {

            // Create and add or replace cached value
            entry = new CacheEntry(readMessage(fileName));
            mResponseCache.put(fileName, entry);
        }

        // Return value
        return (entry.getValue() != null)
                ? (String) entry.getValue()
                : DEFUALT_RESPONSE_MESSAGE;
    }

    /**
     * Reads the response message from a file in the current web app.
     *
     * @param pFileName
     * @return the message
     */
    private String readMessage(String pFileName) {
        try {
            // Read resource from web app
            InputStream is = getServletContext().getResourceAsStream(pFileName);

            if (is != null) {
                return new String(FileUtil.read(is));
            }
            else {
                log("File not found: " + pFileName);
            }
        }
        catch (IOException ioe) {
            log("Error reading file: " + pFileName + " (" + ioe.getMessage() + ")");
        }
        return null;
    }

    /**
     * Keeps track of Cached objects
     */
    private static class CacheEntry {
        private Object mValue;
        private long mTimestamp = -1;

        CacheEntry(Object pValue) {
            mValue = pValue;
            mTimestamp = System.currentTimeMillis();
        }

        Object getValue() {
            return mValue;
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - mTimestamp) > 60000;  // Cache 1 minute
        }
    }
}