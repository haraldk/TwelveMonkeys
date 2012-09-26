package com.twelvemonkeys.servlet.cache;

import java.io.IOException;

/**
 * ResponseResolver
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ResponseResolver.java#2 $
 */
public interface ResponseResolver {
    void resolve(CacheRequest pRequest, CacheResponse pResponse) throws IOException, CacheException;
}
