package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.lang.Validate;

import java.net.URI;

/**
 * AbstractCacheRequest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: AbstractCacheRequest.java#1 $
 */
public abstract class AbstractCacheRequest implements CacheRequest {
    private final URI requestURI;
    private final String method;

    protected AbstractCacheRequest(final URI pRequestURI, final String pMethod) {
        requestURI = Validate.notNull(pRequestURI, "requestURI");
        method = Validate.notNull(pMethod, "method");
    }

    public URI getRequestURI() {
        return requestURI;
    }

    public String getMethod() {
        return method;
    }

    // TODO: Consider overriding equals/hashcode

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[URI=").append(requestURI)
                .append(", parameters=").append(getParameters())
                .append(", headers=").append(getHeaders())
                .append("]").toString();
    }
}
