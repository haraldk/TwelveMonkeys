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

import com.twelvemonkeys.servlet.GenericFilter;
import com.twelvemonkeys.servlet.ServletUtil;
import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * A servlet {@code Filter} for processing HTTP file upload requests, as
 * specified by
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">Form-based File Upload in HTML (RFC1867)</a>.
 *
 * @see HttpFileUploadRequest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/fileupload/FileUploadFilter.java#1 $
 */
public class FileUploadFilter extends GenericFilter {
    private File mUploadDir;
    private long mMaxFileSize = 1024 * 1024; // 1 MByte

    /**
     * This method is called by the server before the filter goes into service,
     * and here it determines the file upload directory.
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        // Get the name of the upload directory.
        String uploadDirParam = getInitParameter("uploadDir");
        if (!StringUtil.isEmpty(uploadDirParam)) {
            try {
                URL uploadDirURL = getServletContext().getResource(uploadDirParam);
                mUploadDir = FileUtil.toFile(uploadDirURL);
            }
            catch (MalformedURLException e) {
                throw new ServletException(e.getMessage(), e);
            }
        }
        if (mUploadDir == null) {
            mUploadDir = ServletUtil.getTempDir(getServletContext());
        }
    }

    /**
     * Sets max filesize allowed for upload.
     * <!-- used by automagic init -->
     *
     * @param pMaxSize
     */
//    public void setMaxFileSize(String pMaxSize) {
//        try {
//            setMaxFileSize(Long.parseLong(pMaxSize));
//        }
//        catch (NumberFormatException e) {
//            log("Error setting maxFileSize, using default: " + mMaxFileSize, e);
//        }
//    }

    /**
     * Sets max filesize allowed for upload.
     *
     * @param pMaxSize
     */
    public void setMaxFileSize(long pMaxSize) {
        log("maxFileSize=" + pMaxSize);
        mMaxFileSize = pMaxSize;
    }

    /**
     * Examines the request content type, and if it is a
     * {@code multipart/*} request, wraps the request with a
     * {@code HttpFileUploadRequest}.
     *
     * @param pRequest The servlet request
     * @param pResponse The servlet response
     * @param pChain The filter chain
     *
     * @throws ServletException
     * @throws IOException
     */
    public void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) pRequest;

        // Get the content type from the request
        String contentType = request.getContentType();

        // If the content type is multipart, wrap
        if (isMultipartFileUpload(contentType)) {
            pRequest = new HttpFileUploadRequestWrapper(request, mUploadDir, mMaxFileSize);
        }

        pChain.doFilter(pRequest, pResponse);
    }

    private boolean isMultipartFileUpload(String pContentType) {
        return pContentType != null && pContentType.startsWith("multipart/");
    }
}
