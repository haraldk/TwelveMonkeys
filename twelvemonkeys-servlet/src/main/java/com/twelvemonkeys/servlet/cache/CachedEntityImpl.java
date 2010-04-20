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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * CachedEntity
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/CachedEntityImpl.java#3 $
 */
class CachedEntityImpl implements CachedEntity {
    private String mCacheURI;
    private HTTPCache mCache;

    CachedEntityImpl(String pCacheURI, HTTPCache pCache) {
        if (pCacheURI == null) {
            throw new IllegalArgumentException("cacheURI == null");
        }

        mCacheURI = pCacheURI;
        mCache = pCache;
    }

    public void render(CacheRequest pRequest, CacheResponse pResponse) throws IOException {
        // Get cached content
        CachedResponse cached = mCache.getContent(mCacheURI, pRequest);

        // Sanity check
        if (cached == null) {
            throw new IllegalStateException("Tried to render non-cached response (cache == null).");
        }

        // If the cached entity is not modified since the date of the browsers
        // version, then simply send a "304 Not Modified" response
        // Otherwise send the full response.

        // TODO: WHY DID I COMMENT OUT THIS LINE AND REPLACE IT WITH THE ONE BELOW??
        //long lastModified = HTTPCache.getDateHeader(cached.getHeaderValue(HTTPCache.HEADER_LAST_MODIFIED));
        long lastModified = HTTPCache.getDateHeader(cached.getHeaderValue(HTTPCache.HEADER_CACHED_TIME));

        // TODO: Consider handling time skews between server "now" and client "now"?
        // NOTE: The If-Modified-Since is probably right according to the server
        // even in a time skew situation, as the client should use either the
        // Date or Last-Modifed dates from the response headers (server generated)
        long ifModifiedSince = -1L;
        try {
            List<String> ifmh = pRequest.getHeaders().get(HTTPCache.HEADER_IF_MODIFIED_SINCE);
            ifModifiedSince = ifmh != null ? HTTPCache.getDateHeader(ifmh.get(0)) : -1L;
            if (ifModifiedSince != -1L) {
                /*
                long serverTime = DateUtil.currentTimeMinute();
                long clientTime = DateUtil.roundToMinute(pRequest.getDateHeader(HTTPCache.HEADER_DATE));

                // Test if time skew is greater than time skew threshold (currently 1 minute)
                if (Math.abs(serverTime - clientTime) > 1) {
                    // TODO: Correct error in ifModifiedSince?
                }
                */

                // System.out.println(" << CachedEntity >> If-Modified-Since present: " + ifModifiedSince + " --> " + NetUtil.formatHTTPDate(ifModifiedSince) + "==" + pRequest.getHeader(HTTPCache.HEADER_IF_MODIFIED_SINCE));
                // System.out.println(" << CachedEntity >> Last-Modified for entity: " + lastModified + " --> " + NetUtil.formatHTTPDate(lastModified));
            }
        }
        catch (IllegalArgumentException e) {
            // Seems to be a bug in FireFox 1.0.2..?!
            mCache.log("Error in date header from user-agent. User-Agent: " + pRequest.getHeaders().get("User-Agent"), e);
        }

        if (lastModified == -1L || (ifModifiedSince < (lastModified / 1000L) * 1000L)) {
            pResponse.setStatus(cached.getStatus());
            cached.writeHeadersTo(pResponse);
            if (isStale(pRequest)) {
                // Add warning header
                // Warning: 110 <server>:<port> Content is stale
                pResponse.addHeader(HTTPCache.HEADER_WARNING, "110 " + getHost(pRequest) + " Content is stale.");
            }

            // NOTE: At the moment we only ever try to cache HEAD and GET requests
            if (!"HEAD".equals(pRequest.getMethod())) {
                cached.writeContentsTo(pResponse.getOutputStream());
            }
        }
        else {
            pResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            // System.out.println(" << CachedEntity >> Not modified: " + toString());
            if (isStale(pRequest)) {
                // Add warning header
                // Warning: 110 <server>:<port> Content is stale
                pResponse.addHeader(HTTPCache.HEADER_WARNING, "110 " + getHost(pRequest) + " Content is stale.");
            }
        }
    }

    /* Utility method to get Host header */
    private static String getHost(CacheRequest pRequest) {
        return pRequest.getServerName() + ":" + pRequest.getServerPort();
    }

    public void capture(CacheRequest pRequest, CachedResponse pResponse) throws IOException {
//        if (!(pResponse instanceof CacheResponseWrapper)) {
//            throw new IllegalArgumentException("Response must be created by CachedEntity.createResponseWrapper()");
//        }
//
//        CacheResponseWrapper response = (CacheResponseWrapper) pResponse;

//        if (response.isCachable()) {
            mCache.registerContent(
                    mCacheURI,
                    pRequest,
                    pResponse instanceof WritableCachedResponse ? ((WritableCachedResponse) pResponse).getCachedResponse() : pResponse
            );
//        }
//        else {
            // Else store that the response for this request is not cachable
//            pRequest.setAttribute(ATTRIB_NOT_CACHEABLE, Boolean.TRUE);

            // TODO: Store this in HTTPCache, for subsequent requests to same resource?
//        }
    }

    public boolean isStale(CacheRequest pRequest) {
        return mCache.isContentStale(mCacheURI, pRequest);
    }

    public WritableCachedResponse createCachedResponse() {
        return new WritableCachedResponseImpl();
    }

    public int hashCode() {
        return (mCacheURI != null ? mCacheURI.hashCode() : 0) + 1397;
    }

    public boolean equals(Object pOther) {
        return pOther instanceof CachedEntityImpl &&
                ((mCacheURI == null && ((CachedEntityImpl) pOther).mCacheURI == null) ||
                  mCacheURI != null && mCacheURI.equals(((CachedEntityImpl) pOther).mCacheURI));
    }

    public String toString() {
        return "CachedEntity[URI=" + mCacheURI + "]";
    }
}