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

package com.twelvemonkeys.servlet.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.lang.Validate;

/**
 * CachedResponseImpl
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: CachedResponseImpl.java#4 $
 */
@Deprecated
class CachedResponseImpl implements CachedResponse {
    final protected Map<String, List<String>> headers;
    protected int headersSize;
    protected ByteArrayOutputStream content = null;
    int status;

    protected CachedResponseImpl() {
        headers = new LinkedHashMap<String, List<String>>(); // Keep headers in insertion order
    }

    // For use by HTTPCache, when recreating CachedResponses from disk cache
    CachedResponseImpl(final int pStatus, final LinkedHashMap<String, List<String>> pHeaders, final int pHeaderSize, final byte[] pContent) {
        status = pStatus;
        headers = Validate.notNull(pHeaders, "headers");
        headersSize = pHeaderSize;
        content = new FastByteArrayOutputStream(pContent);
    }

    public int getStatus() {
        return status;
    }

    /**
     * Writes the cached headers to the response
     *
     * @param pResponse the response
     */
    public void writeHeadersTo(final CacheResponse pResponse) {
        String[] headers = getHeaderNames();
        for (String header : headers) {
            // HACK...
            // Strip away internal headers
            if (HTTPCache.HEADER_CACHED_TIME.equals(header)) {
                continue;
            }

            // TODO: Replace Last-Modified with X-Cached-At? See CachedEntityImpl

            String[] headerValues = getHeaderValues(header);

            for (int i = 0; i < headerValues.length; i++) {
                String headerValue = headerValues[i];

                if (i == 0) {
                    pResponse.setHeader(header, headerValue);
                }
                else {
                    pResponse.addHeader(header, headerValue);
                }
            }
        }
    }

    /**
     * Writes the cached content to the response
     *
     * @param pStream the response stream
     * @throws java.io.IOException
     */
    public void writeContentsTo(final OutputStream pStream) throws IOException {
        if (content == null) {
            throw new IOException("Cache is null, no content to write.");
        }

        content.writeTo(pStream);
    }

    /**
     * Gets the header names of all headers set in this response.
     *
     * @return an array of {@code String}s
     */
    public String[] getHeaderNames() {
        Set<String> headers = this.headers.keySet();

        return headers.toArray(new String[headers.size()]);
    }

    /**
     * Gets all header values set for the given header in this response. If the
     * header is not set, {@code null} is returned.
     *
     * @param pHeaderName the header name
     * @return an array of {@code String}s, or {@code null} if there is no
     * such header in this response.
     */
    public String[] getHeaderValues(final String pHeaderName) {
        List<String> values = headers.get(pHeaderName);

        return values == null ? null : values.toArray(new String[values.size()]);
    }

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
    public String getHeaderValue(final String pHeaderName) {
        List<String> values = headers.get(pHeaderName);

        return (values != null && values.size() > 0) ? values.get(0) : null;
    }

    public int size() {
        // content.size() is exact size in bytes, headersSize is an estimate
        return (content != null ? content.size() : 0) + headersSize;
    }

    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }

        if (pOther instanceof CachedResponseImpl) {
            // "Fast"
            return equalsImpl((CachedResponseImpl) pOther);
        }
        else if (pOther instanceof CachedResponse) {
            // Slow
            return equalsGeneric((CachedResponse) pOther);
        }

        return false;
    }

    private boolean equalsImpl(final CachedResponseImpl pOther) {
        return headersSize == pOther.headersSize &&
                (content == null ? pOther.content == null : content.equals(pOther.content)) &&
                headers.equals(pOther.headers);
    }

    private boolean equalsGeneric(final CachedResponse pOther) {
        if (size() != pOther.size()) {
            return false;
        }

        String[] headers = getHeaderNames();
        String[] otherHeaders = pOther.getHeaderNames();
        if (!Arrays.equals(headers, otherHeaders)) {
            return false;
        }

        if (headers != null) {
            for (String header : headers) {
                String[] values = getHeaderValues(header);
                String[] otherValues = pOther.getHeaderValues(header);

                if (!Arrays.equals(values, otherValues)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = headers.hashCode();
        result = 29 * result + headersSize;
        result = 37 * result + (content != null ? content.hashCode() : 0);
        return result;
    }
}
