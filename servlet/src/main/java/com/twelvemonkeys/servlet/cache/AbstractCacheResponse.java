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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractCacheResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: AbstractCacheResponse.java#1 $
 */
@Deprecated
public abstract class AbstractCacheResponse implements CacheResponse {
    private int status;
    private final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>(); // Insertion order
    private final Map<String, List<String>> readableHeaders = Collections.unmodifiableMap(headers);

    public int getStatus() {
        return status;
    }

    public void setStatus(int pStatusCode) {
        status = pStatusCode;
    }

    public void addHeader(String pHeaderName, String pHeaderValue) {
        setHeader(pHeaderName, pHeaderValue, true);
    }

    public void setHeader(String pHeaderName, String pHeaderValue) {
        setHeader(pHeaderName, pHeaderValue, false);
    }

    private void setHeader(String pHeaderName, String pHeaderValue, boolean pAdd) {
        List<String> values = pAdd ? headers.get(pHeaderName) : null;

        if (values == null) {
            values = new ArrayList<String>();
            headers.put(pHeaderName, values);
        }

        values.add(pHeaderValue);
    }

    public Map<String, List<String>> getHeaders() {
        return readableHeaders;
    }
}
