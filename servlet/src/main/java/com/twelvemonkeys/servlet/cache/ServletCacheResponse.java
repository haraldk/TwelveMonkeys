package com.twelvemonkeys.servlet.cache;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ServletCacheResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ServletCacheResponse.java#2 $
 */
public final class ServletCacheResponse extends AbstractCacheResponse {
    private HttpServletResponse response;

    public ServletCacheResponse(HttpServletResponse pResponse) {
        response = pResponse;
    }

    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public void setStatus(int pStatusCode) {
        response.setStatus(pStatusCode);
        super.setStatus(pStatusCode);
    }

    @Override
    public void addHeader(String pHeaderName, String pHeaderValue) {
        response.addHeader(pHeaderName, pHeaderValue);
        super.addHeader(pHeaderName, pHeaderValue);
    }

    @Override
    public void setHeader(String pHeaderName, String pHeaderValue) {
        response.setHeader(pHeaderName, pHeaderValue);
        super.setHeader(pHeaderName, pHeaderValue);
    }

    HttpServletResponse getResponse() {
        return response;
    }
}
