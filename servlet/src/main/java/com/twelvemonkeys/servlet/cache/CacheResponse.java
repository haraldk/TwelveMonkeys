package com.twelvemonkeys.servlet.cache;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * CacheResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: CacheResponse.java#1 $
 */
public interface CacheResponse {
    OutputStream getOutputStream() throws IOException;

    void setStatus(int pStatusCode);

    int getStatus();

    void addHeader(String pHeaderName, String pHeaderValue);

    void setHeader(String pHeaderName, String pHeaderValue);

    Map<String, List<String>> getHeaders();
}
