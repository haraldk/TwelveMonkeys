package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * ServletCacheRequest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/ServletCacheRequest.java#1 $
 */
public final class ServletCacheRequest extends AbstractCacheRequest {
    private final HttpServletRequest request;

    private Map<String, List<String>> headers;
    private Map<String, List<String>> parameters;

    protected ServletCacheRequest(final HttpServletRequest pRequest) {
        super(URI.create(pRequest.getRequestURI()), pRequest.getMethod());
        request = pRequest;
    }

    public Map<String, List<String>> getHeaders() {
        if (headers == null) {
            headers = ServletUtil.headersAsMap(request);
        }

        return headers;
    }

    public Map<String, List<String>> getParameters() {
        if (parameters == null) {
            parameters = ServletUtil.parametersAsMap(request);
        }

        return parameters;
    }

    public String getServerName() {
        return request.getServerName();
    }

    public int getServerPort() {
        return request.getServerPort();
    }

    HttpServletRequest getRequest() {
        return request;
    }

}
