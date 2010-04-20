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

import java.io.OutputStream;

/**
 * WritableCachedResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/WritableCachedResponse.java#2 $
 */
public interface WritableCachedResponse extends CachedResponse, CacheResponse {
    /**
     * Gets the {@code OutputStream} for this cached response.
     * This allows a client to write to the cached response.
     *
     * @return the {@code OutputStream} for this response.
     */
    OutputStream getOutputStream();

    /**
     * Sets a header key/value pair for this response.
     * Any prior header value for the given header key will be overwritten.
     *
     * @see #addHeader(String, String)
     *
     * @param pName the header name
     * @param pValue the header value
     */
    void setHeader(String pName, String pValue);
    
    /**
     * Adds a header key/value pair for this response.
     * If a value allready exists for the given key, the value will be appended.
     *
     * @see #setHeader(String, String)
     *
     * @param pName the header name
     * @param pValue the header value
     */
    void addHeader(String pName, String pValue);

    /**
     * Returns the final (immutable) {@code CachedResponse} created by this
     * {@code WritableCachedResponse}.
     *
     * @return the {@code CachedResponse}
     */
    CachedResponse getCachedResponse();
}
