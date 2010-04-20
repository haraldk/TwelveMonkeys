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

import java.io.IOException;
import java.io.OutputStream;

/**
 * CachedResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/CachedResponse.java#3 $
 */
interface CachedResponse {
    /**
     * Writes the cached headers to the response
     *
     * @param pResponse the servlet response
     */
    void writeHeadersTo(CacheResponse pResponse);

    /**
     * Writes the cahced content to the response
     *
     * @param pStream the response output stream
     * @throws IOException if an I/O exception occurs during write
     */
    void writeContentsTo(OutputStream pStream) throws IOException;

    int getStatus();

    // TODO: Map<String, List<String>> getHeaders()

    /**
     * Gets the header names of all headers set in this response.
     *
     * @return an array of {@code String}s
     */
    String[] getHeaderNames();

    /**
     * Gets all header values set for the given header in this response. If the
     * header is not set, {@code null} is returned.
     *
     * @param pHeaderName the header name
     * @return an array of {@code String}s, or {@code null} if there is no
     * such header in this response.
     */
    String[] getHeaderValues(String pHeaderName);

    /**
     * Gets the first header value set for the given header in this response.
     * If the header is not set, {@code null} is returned.
     * Useful for headers that don't have multiple values, like
     * {@code "Content-Type"} or {@code "Content-Length"}.
     *
     * @param pHeaderName the header name
     * @return a {@code String}, or {@code null} if there is no
     * such header in this response.
     */
    String getHeaderValue(String pHeaderName);

    /**
     * Returns the size of this cached response in bytes.
     *
     * @return the size
     */
    int size();
}
