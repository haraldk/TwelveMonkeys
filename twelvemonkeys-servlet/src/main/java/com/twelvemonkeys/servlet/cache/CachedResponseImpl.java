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

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.util.LinkedMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CachedResponseImpl
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/CachedResponseImpl.java#4 $
 */
class CachedResponseImpl implements CachedResponse {
    final protected Map<String, List<String>> mHeaders;
    protected int mHeadersSize;
    protected ByteArrayOutputStream mContent = null;
    int mStatus;

    protected CachedResponseImpl() {
        mHeaders = new LinkedMap<String, List<String>>(); // Keep headers in insertion order
    }

    // For use by HTTPCache, when recreating CachedResponses from disk cache
    CachedResponseImpl(final int pStatus, final LinkedMap<String, List<String>> pHeaders, final int pHeaderSize, final byte[] pContent) {
        if (pHeaders == null) {
            throw new IllegalArgumentException("headers == null");
        }
        mStatus = pStatus;
        mHeaders = pHeaders;
        mHeadersSize = pHeaderSize;
        mContent = new FastByteArrayOutputStream(pContent);
    }

    public int getStatus() {
        return mStatus;
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

            // TODO: Replace Last-Modified with X-Cached-At? See CachedEntityImpl, line 50

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
     * Writes the cahced content to the response
     *
     * @param pStream the response stream
     * @throws java.io.IOException
     */
    public void writeContentsTo(final OutputStream pStream) throws IOException {
        if (mContent == null) {
            throw new IOException("Cache is null, no content to write.");
        }

        mContent.writeTo(pStream);
    }

    /**
     * Gets the header names of all headers set in this response.
     *
     * @return an array of {@code String}s
     */
    public String[] getHeaderNames() {
        Set<String> headers = mHeaders.keySet();
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
        List<String> values = mHeaders.get(pHeaderName);
        if (values == null) {
            return null;
        }
        else {
            return values.toArray(new String[values.size()]);
        }
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
        List<String> values = mHeaders.get(pHeaderName);
        return (values != null && values.size() > 0) ? values.get(0) : null;
    }

    public int size() {
        // mContent.size() is exact size in bytes, mHeadersSize is an estimate
        return (mContent != null ? mContent.size() : 0) + mHeadersSize;
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
        return mHeadersSize == pOther.mHeadersSize &&
                (mContent == null ? pOther.mContent == null : mContent.equals(pOther.mContent)) &&
                mHeaders.equals(pOther.mHeaders);
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
        result = mHeaders.hashCode();
        result = 29 * result + mHeadersSize;
        result = 37 * result + (mContent != null ? mContent.hashCode() : 0);
        return result;
    }
}
