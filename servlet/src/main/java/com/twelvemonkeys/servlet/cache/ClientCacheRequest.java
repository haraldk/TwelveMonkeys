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
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/ClientCacheRequest.java#1 $
 */
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
