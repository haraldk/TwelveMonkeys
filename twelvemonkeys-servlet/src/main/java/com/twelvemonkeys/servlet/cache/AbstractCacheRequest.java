package com.twelvemonkeys.servlet.cache;

import java.io.File;
import java.net.URI;

/**
 * AbstractCacheRequest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/AbstractCacheRequest.java#1 $
 */
public abstract class AbstractCacheRequest implements CacheRequest {
    private final URI mRequestURI;
    private final String mMethod;

    protected AbstractCacheRequest(final URI pRequestURI, final String pMethod) {
        if (pRequestURI == null) {
            throw new IllegalArgumentException("request URI == null");
        }
        if (pMethod == null) {
            throw new IllegalArgumentException("method == null");
        }

        mRequestURI = pRequestURI;
        mMethod = pMethod;
    }

    public URI getRequestURI() {
        return mRequestURI;
    }

    public String getMethod() {
        return mMethod;
    }

    // TODO: Consider overriding equals/hashcode

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[URI=").append(mRequestURI)
                .append(", parameters=").append(getParameters())
                .append(", headers=").append(getHeaders())
                .append("]").toString();
    }
}
