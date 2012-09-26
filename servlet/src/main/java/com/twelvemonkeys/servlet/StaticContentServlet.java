/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.servlet;

import com.twelvemonkeys.io.FileUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A minimal servlet that can serve static files. Also from outside the web application.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: StaticContentServlet.java,v 1.0 12.12.11 15:09 haraldk Exp$
 *
 * @see #setRoot(java.io.File)
 */
public final class StaticContentServlet extends HttpServlet {

    private File root;

    /**
     * Configures the file system {@code root} for this servlet.
     * If {@code root} is a directory, files will be served relative to the directory.
     * If {@code root} is a file, only this file may be served
     *
     * @param root the file system root.
     */
    @InitParam(name = "root")
    public void setRoot(final File root) {
        this.root = root;
    }

    @Override
    public void init() throws ServletException {
        if (root == null) {
            throw new ServletConfigException("File system root not configured, check 'root' init-param");
        }
        else if (!root.exists()) {
            throw new ServletConfigException(
                    String.format("File system root '%s' does not exist, check 'root' init-param", root.getAbsolutePath())
            );
        }

        log(String.format("Serving %s '%s'", root.isDirectory() ? "files from directory" : "single file", root.getAbsolutePath()));
    }

    @Override
    protected long getLastModified(final HttpServletRequest request) {
        File file = findFileForRequest(request);

        return file.exists() ? file.lastModified() : -1L;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        File file = findFileForRequest(request);

        if (file.isFile() && file.canRead()) {
            // Normal file, all ok
            response.setStatus(HttpServletResponse.SC_OK);
            String contentType = getServletContext().getMimeType(file.getName());
            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            if (file.length() <= Integer.MAX_VALUE) {
                response.setContentLength((int) file.length());
            }
            else {
                response.setHeader("Content-Length", String.valueOf(file.length()));
            }
            response.setDateHeader("Last-Modified", file.lastModified());

            InputStream in = new FileInputStream(file);

            try {
                FileUtil.copy(in, response.getOutputStream());
            }
            finally {
                in.close();
            }
        }
        else {
            if (file.exists()) {
                // Attempted directory listing or non-readable file
                response.sendError(HttpServletResponse.SC_FORBIDDEN, request.getRequestURI());
            }
            else {
                // No such file
                response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURI());
            }
        }
    }

    private File findFileForRequest(final HttpServletRequest request) {
        String relativePath = request.getPathInfo();

        return relativePath != null ? new File(root, relativePath) : root;
    }
}
