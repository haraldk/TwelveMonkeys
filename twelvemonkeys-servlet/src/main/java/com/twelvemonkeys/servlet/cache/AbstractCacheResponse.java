package com.twelvemonkeys.servlet.cache;

import java.util.*;

/**
 * AbstractCacheResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/AbstractCacheResponse.java#1 $
 */
public abstract class AbstractCacheResponse implements CacheResponse {
    private int mStatus;
    private final Map<String, List<String>> mHeaders = new LinkedHashMap<String, List<String>>(); // Insertion order
    private final Map<String, List<String>> mReadableHeaders = Collections.unmodifiableMap(mHeaders);

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int pStatusCode) {
        mStatus = pStatusCode;
    }

    public void addHeader(String pHeaderName, String pHeaderValue) {
        setHeader(pHeaderName, pHeaderValue, true);
    }

    public void setHeader(String pHeaderName, String pHeaderValue) {
        setHeader(pHeaderName, pHeaderValue, false);
    }

    private void setHeader(String pHeaderName, String pHeaderValue, boolean pAdd) {
        List<String> values = pAdd ? mHeaders.get(pHeaderName) : null;
        if (values == null) {
            values = new ArrayList<String>();
            mHeaders.put(pHeaderName, values);
        }
        values.add(pHeaderValue);
    }

    public Map<String, List<String>> getHeaders() {
        return mReadableHeaders;
    }
}
