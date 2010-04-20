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

import javax.servlet.http.HttpServletRequest;

/**
 * This interface represents an HTTP file upload request, as specified by
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">Form-based File Upload in HTML (RFC1867)</a>.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/fileupload/HttpFileUploadRequest.java#1 $
 */
public interface HttpFileUploadRequest extends HttpServletRequest {
    /**
     * Returns the value of a request parameter as an {@code UploadedFile},
     * or {@code null} if the parameter does not exist.
     * You should only use this method when you are sure the parameter has only
     * one value.
     *
     * @param pName the name of the requested parameter
     * @return a {@code UoploadedFile} or {@code null}
     *
     * @see #getUploadedFiles(String)
     */
    UploadedFile getUploadedFile(String pName);

    /**
     * Returns an array of {@code UploadedFile} objects containing all the
     * values for the given request parameter,
     * or {@code null} if the parameter does not exist.
     *
     * @param pName the name of the requested parameter
     * @return an array of {@code UoploadedFile}s or {@code null}
     */
    UploadedFile[] getUploadedFiles(String pName);
}
