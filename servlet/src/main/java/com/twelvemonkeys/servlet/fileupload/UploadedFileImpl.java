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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;

/**
 * An {@code UploadedFile} implementation, based on
 * <a href="http://jakarta.apache.org/commons/fileupload/">Jakarta Commons FileUpload</a>.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/fileupload/UploadedFileImpl.java#1 $
 */
class UploadedFileImpl implements UploadedFile {
    private final FileItem mItem;

    public UploadedFileImpl(FileItem pItem) {
        if (pItem == null) {
            throw new IllegalArgumentException("fileitem == null");
        }

        mItem = pItem;
    }

    public String getContentType() {
        return mItem.getContentType();
    }

    public InputStream getInputStream() throws IOException {
        return mItem.getInputStream();
    }

    public String getName() {
        return mItem.getName();
    }

    public long length() {
        return mItem.getSize();
    }

    public void writeTo(File pFile) throws IOException {
        try {
            mItem.write(pFile);
        }
        catch(RuntimeException e) {
            throw e;
        }
        catch (IOException e) {
            throw e;
        }
        catch (FileUploadException e) {
            // We deliberately change this exception to an IOException, as it really is
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
        catch (Exception e) {
            // Should not really happen, ever
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}