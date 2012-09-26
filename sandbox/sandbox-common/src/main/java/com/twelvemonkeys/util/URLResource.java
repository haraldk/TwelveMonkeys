/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URLResource class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/URLResource.java#1 $
 */
final class URLResource extends AbstractResource {

    // NOTE: For the time being, we rely on the URL class (and helpers) to do
    //       some smart caching and reuse of connections...
    // TODO: Change the implementation if this is a problem
    private long lastModified = -1;

    /**
     * Creates a {@code URLResource}.
     *
     * @param pResourceId the resource id
     * @param pURL the URL resource
     */
    public URLResource(Object pResourceId, URL pURL) {
        super(pResourceId, pURL);
    }

    private URL getURL() {
        return (URL) wrappedResource;
    }

    public URL asURL() {
        return getURL();
    }

    public InputStream asStream() throws IOException {
        URLConnection connection = getURL().openConnection();
        connection.setAllowUserInteraction(false);
        connection.setUseCaches(true);
        return connection.getInputStream();
    }

    public long lastModified() {
        try {
            URLConnection connection = getURL().openConnection();
            connection.setAllowUserInteraction(false);
            connection.setUseCaches(true);
            connection.setIfModifiedSince(lastModified);

            lastModified = connection.getLastModified();
         }
        catch (IOException ignore) {
        }

        return lastModified;
    }
}
