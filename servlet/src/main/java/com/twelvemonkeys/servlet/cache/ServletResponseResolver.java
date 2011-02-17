package com.twelvemonkeys.servlet.cache;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ServletResponseResolver
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/ServletResponseResolver.java#2 $
 */
final class ServletResponseResolver implements ResponseResolver {
    final private ServletCacheRequest request;
    final private ServletCacheResponse response;
    final private FilterChain chain;

    ServletResponseResolver(final ServletCacheRequest pRequest, final ServletCacheResponse pResponse, final FilterChain pChain) {
        request = pRequest;
        response = pResponse;
        chain = pChain;
    }

    public void resolve(final CacheRequest pRequest, final CacheResponse pResponse) throws IOException, CacheException {
        // Need only wrap if pResponse is not response...
        HttpServletResponse response = pResponse == this.response ? this.response.getResponse() : new SerlvetCacheResponseWrapper(this.response.getResponse(), pResponse);

        try {
            chain.doFilter(request.getRequest(), response);
        }
        catch (ServletException e) {
            throw new CacheException(e);
        }
        finally {
            response.flushBuffer();
        }
    }
}
