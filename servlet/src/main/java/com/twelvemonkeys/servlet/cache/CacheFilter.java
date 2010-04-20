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

package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.servlet.GenericFilter;
import com.twelvemonkeys.servlet.ServletConfigException;
import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Filter that provides response caching, for HTTP {@code GET} requests.
 * <p/>
 * Originally based on ideas and code found in the ONJava article
 * <a href="http://www.onjava.com/pub/a/onjava/2003/11/19/filters.html">Two
 * Servlet Filters Every Web Application Should Have</a>
 * by Jayson Falkner.
 *
 * @author Jayson Falkner
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/CacheFilter.java#4 $
 *
 */
public class CacheFilter extends GenericFilter {

    HTTPCache mCache;

    /**
     * Initializes the filter
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {
        FilterConfig config = getFilterConfig();

        // Default don't delete cache files on exit (peristent cache)
        boolean deleteCacheOnExit = "TRUE".equalsIgnoreCase(config.getInitParameter("deleteCacheOnExit"));

        // Default expiry time 10 minutes
        int expiryTime = 10 * 60 * 1000;

        String expiryTimeStr = config.getInitParameter("expiryTime");
        if (!StringUtil.isEmpty(expiryTimeStr)) {
            try {
                expiryTime = Integer.parseInt(expiryTimeStr);
            }
            catch (NumberFormatException e) {
                throw new ServletConfigException("Could not parse expiryTime: " + e.toString(), e);
            }
        }

        // Default max mem cache size 10 MB
        int memCacheSize = 10;

        String memCacheSizeStr = config.getInitParameter("memCacheSize");
        if (!StringUtil.isEmpty(memCacheSizeStr)) {
            try {
                memCacheSize = Integer.parseInt(memCacheSizeStr);
            }
            catch (NumberFormatException e) {
                throw new ServletConfigException("Could not parse memCacheSize: " + e.toString(), e);
            }
        }

        int maxCachedEntites = 10000;

        try {
            mCache = new HTTPCache(
                    getTempFolder(),
                    expiryTime,
                    memCacheSize * 1024 * 1024,
                    maxCachedEntites,
                    deleteCacheOnExit,
                    new ServletContextLoggerAdapter(getFilterName(), getServletContext())
            ) {
                @Override
                protected File getRealFile(CacheRequest pRequest) {
                    String contextRelativeURI = ServletUtil.getContextRelativeURI(((ServletCacheRequest) pRequest).getRequest());

                    String path = getServletContext().getRealPath(contextRelativeURI);

                    if (path != null) {
                        return new File(path);
                    }

                    return null;
                }
            };
            log("Created cache: " + mCache);
        }
        catch (IllegalArgumentException e) {
            throw new ServletConfigException("Could not create cache: " + e.toString(), e);
        }
    }

    private File getTempFolder() {
        File tempRoot = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
        if (tempRoot == null) {
            throw new IllegalStateException("Missing context attribute \"javax.servlet.context.tempdir\"");
        }
        return new File(tempRoot, getFilterName());
    }

    public void destroy() {
        log("Destroying cache: " + mCache);
        mCache = null;
        super.destroy();
    }

    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        // We can only cache HTTP GET/HEAD requests
        if (!(pRequest instanceof HttpServletRequest
                && pResponse instanceof HttpServletResponse
                && isCachable((HttpServletRequest) pRequest))) {
            pChain.doFilter(pRequest, pResponse); // Continue chain
        }
        else {
            ServletCacheRequest cacheRequest = new ServletCacheRequest((HttpServletRequest) pRequest);
            ServletCacheResponse cacheResponse = new ServletCacheResponse((HttpServletResponse) pResponse);
            ServletResponseResolver resolver = new ServletResponseResolver(cacheRequest, cacheResponse, pChain);

            // Render fast
            try {
                mCache.doCached(cacheRequest, cacheResponse, resolver);
            }
            catch (CacheException e) {
                if (e.getCause() instanceof ServletException) {
                    throw (ServletException) e.getCause();
                }
                else {
                    throw new ServletException(e);
                }
            }
            finally {
                pResponse.flushBuffer();
            }
        }
    }

    private boolean isCachable(HttpServletRequest pRequest) {
        // TODO: Get Cache-Control: no-cache/max-age=0 and Pragma: no-cache from REQUEST too?
        return "GET".equals(pRequest.getMethod()) || "HEAD".equals(pRequest.getMethod());
    }

    // TODO: Extract, complete and document this class, might be useful in other cases
    // Maybe add it to the ServletUtil class
    static class ServletContextLoggerAdapter extends Logger {
        private final ServletContext mContext;

        public ServletContextLoggerAdapter(String pName, ServletContext pContext) {
            super(pName, null);
            mContext = pContext;
        }

        @Override
        public void log(Level pLevel, String pMessage) {
            mContext.log(pMessage);
        }

        @Override
        public void log(Level pLevel, String pMessage, Throwable pThrowable) {
            mContext.log(pMessage, pThrowable);
        }
    }
}