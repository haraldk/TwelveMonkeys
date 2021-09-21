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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ClientCacheRequest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ClientCacheRequest.java#1 $
 */
@Deprecated
public final class ClientCacheRequest extends AbstractCacheRequest {
    private Map<String, List<String>> parameters;
    private Map<String, List<String>> headers;

    public ClientCacheRequest(final URI pRequestURI,final Map<String, List<String>> pParameters, final Map<String, List<String>> pHeaders) {
        super(pRequestURI, "GET"); // TODO: Consider supporting more than get? At least HEAD and OPTIONS...
        parameters = normalizeMap(pParameters);
        headers = normalizeMap(pHeaders);
    }

    private <K, V> Map<K, V> normalizeMap(Map<K, V> pMap) {
        return pMap == null ? Collections.<K, V>emptyMap() : Collections.unmodifiableMap(pMap);
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getServerName() {
        return getRequestURI().getAuthority();
    }

    public int getServerPort() {
        return getRequestURI().getPort();
    }
}
