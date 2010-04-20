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

package com.twelvemonkeys.servlet.fileupload;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * This class represents an uploaded file.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/fileupload/UploadedFile.java#1 $
 */
public interface UploadedFile {
    /**
     * Returns the length of file, in bytes.
     *
     * @return length of file
     */
    long length();

    /**
     * Returns the original file name (from client).
     *
     * @return original name
     */
    String getName();

    /**
     * Returns the content type of the file.
     *
     * @return the content type
     */
    String getContentType();

    /**
     * Returns the file data, as an {@code InputStream}.
     * The file data may be read from disk, or from an in-memory source,
     * depending on implementation.
     *
     * @return an {@code InputStream} containing the file data
     * @throws IOException
     * @throws RuntimeException
     */
    InputStream getInputStream() throws IOException;

    /**
     * Writes the file data to the given {@code File}.
     * Note that implementations are free to optimize this to a rename
     * operation, if the file is allready cached to disk.
     *
     * @param pFile the {@code File} (file name) to write to.
     * @throws IOException
     * @throws RuntimeException
     */
    void writeTo(File pFile) throws IOException;

    // TODO: void delete()?
}
