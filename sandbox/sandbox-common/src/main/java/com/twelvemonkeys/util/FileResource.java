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
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * FileResource class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/FileResource.java#1 $
 */
final class FileResource extends AbstractResource {

    /**
     * Creates a {@code FileResource}.
     *
     * @param pResourceId the resource id
     * @param pFile the file resource
     */
    public FileResource(Object pResourceId, File pFile) {
        super(pResourceId, pFile);
    }

    private File getFile() {
        return (File) wrappedResource;
    }

    public URL asURL() {
        try {
            return getFile().toURL();
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException("The file \"" + getFile().getAbsolutePath()
                    + "\" is not parseable as an URL: " + e.getMessage());
        }
    }

    public InputStream asStream() throws IOException {
        return new FileInputStream(getFile());
    }

    public long lastModified() {
        return getFile().lastModified();
    }
}
