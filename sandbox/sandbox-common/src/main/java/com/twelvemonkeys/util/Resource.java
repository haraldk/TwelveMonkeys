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

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Resource class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/Resource.java#1 $
 */
public interface Resource {
    /**
     * Returns the id of this resource.
     *
     * The id might be a {@code URL}, a {@code File} or a string
     * representation of some system resource.
     * It will always be equal to the {@code resourceId} parameter
     * sent to the {@link ResourceMonitor#addResourceChangeListener} method
     * for a given resource.
     *
     * @return the id of this resource
     */
    public Object getId();

    /**
     * Returns the {@code URL} for the resource.
     *
     * @return the URL for the resource
     */
    public URL asURL();

    /**
     * Opens an input stream, that reads from this resource.
     *
     * @return an input stream, that reads from this resource.
     *
     * @throws IOException if an I/O exception occurs
     */
    public InputStream asStream() throws IOException;

    /**
     * Returns the last modified time.
     * Should only be used for comparison.
     *
     * @return the time of the last modification of this resource, or
     * {@code -1} if the last modification time cannot be determined.
     */
    public long lastModified();
}
