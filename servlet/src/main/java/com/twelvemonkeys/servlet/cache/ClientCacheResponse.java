package com.twelvemonkeys.servlet.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ClientCacheResponse
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/ClientCacheResponse.java#2 $
 */
public final class ClientCacheResponse extends AbstractCacheResponse {
    // It's quite useless to cache the data either on disk or in memory, as it already is cached in the client's cache...
    // It would be nice if we could bypass that...

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Method getOutputStream not implemented"); // TODO: Implement
    }

    public InputStream getInputStream() {
        throw new UnsupportedOperationException("Method getInputStream not implemented"); // TODO: Implement
    }
}
