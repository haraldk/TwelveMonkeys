package com.twelvemonkeys.servlet.cache;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * CacheRequest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: CacheRequest.java#1 $
 */
public interface CacheRequest {
    URI getRequestURI();

    String getMethod();

    Map<String, List<String>> getHeaders();

    Map<String, List<String>> getParameters();

    String getServerName();

    int getServerPort();
}
