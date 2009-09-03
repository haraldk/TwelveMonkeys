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
    private final HttpServletRequest mRequest;

    private Map<String, List<String>> mHeaders;
    private Map<String, List<String>> mParameters;

    protected ServletCacheRequest(final HttpServletRequest pRequest) {
        super(URI.create(pRequest.getRequestURI()), pRequest.getMethod());
        mRequest = pRequest;
    }

    public Map<String, List<String>> getHeaders() {
        if (mHeaders == null) {
            mHeaders = ServletUtil.headersAsMap(mRequest);
        }

        return mHeaders;
    }

    public Map<String, List<String>> getParameters() {
        if (mParameters == null) {
            mParameters = ServletUtil.parametersAsMap(mRequest);
        }

        return mParameters;
    }

    public String getServerName() {
        return mRequest.getServerName();
    }

    public int getServerPort() {
        return mRequest.getServerPort();
    }

    HttpServletRequest getRequest() {
        return mRequest;
    }

}
