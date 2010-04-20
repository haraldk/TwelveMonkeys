package com.twelvemonkeys.servlet.cache;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ServletCacheResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/ServletCacheResponse.java#2 $
 */
public final class ServletCacheResponse extends AbstractCacheResponse {
    private HttpServletResponse mResponse;

    public ServletCacheResponse(HttpServletResponse pResponse) {
        mResponse = pResponse;
    }

    public OutputStream getOutputStream() throws IOException {
        return mResponse.getOutputStream();
    }

    @Override
    public void setStatus(int pStatusCode) {
        mResponse.setStatus(pStatusCode);
        super.setStatus(pStatusCode);
    }

    @Override
    public void addHeader(String pHeaderName, String pHeaderValue) {
        mResponse.addHeader(pHeaderName, pHeaderValue);
        super.addHeader(pHeaderName, pHeaderValue);
    }

    @Override
    public void setHeader(String pHeaderName, String pHeaderValue) {
        mResponse.setHeader(pHeaderName, pHeaderValue);
        super.setHeader(pHeaderName, pHeaderValue);
    }

    HttpServletResponse getResponse() {
        return mResponse;
    }
}
