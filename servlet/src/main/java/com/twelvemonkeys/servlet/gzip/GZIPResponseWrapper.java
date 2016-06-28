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

package com.twelvemonkeys.servlet.gzip;

import com.twelvemonkeys.servlet.OutputStreamAdapter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

/**
 * GZIPResponseWrapper class description.
 * <p/>
 * Based on ideas and code found in the ONJava article
 * <a href="http://www.onjava.com/pub/a/onjava/2003/11/19/filters.html">Two Servlet Filters Every Web Application Should Have</a>
 * by Jayson Falkner.
 *
 * @author Jayson Falkner
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: GZIPResponseWrapper.java#1 $
 */
public class GZIPResponseWrapper extends HttpServletResponseWrapper {
    // TODO: Remove/update ETags if needed? Read the spec (RFC 2616) on Vary/ETag for caching

    protected ServletOutputStream out;
    protected PrintWriter writer;
    protected GZIPOutputStream gzipOut;
    protected int contentLength = -1;

    public GZIPResponseWrapper(final HttpServletResponse response) {
        super(response);

        response.addHeader("Content-Encoding", "gzip");
        response.addHeader("Vary", "Accept");
    }

    public ServletOutputStream createOutputStream() throws IOException {
        // FIX: Write directly to servlet output stream, for faster responses.
        // Relies on chunked streams, or buffering in the servlet engine.
        if (contentLength >= 0) {
            gzipOut = new GZIPOutputStream(getResponse().getOutputStream(), contentLength);
        }
        else {
            gzipOut = new GZIPOutputStream(getResponse().getOutputStream());
        }

        // Wrap in ServletOutputStream and return
        return new OutputStreamAdapter(gzipOut);
    }

    // TODO: Move this to flushbuffer or something? Hmmm..
    public void flushResponse() throws IOException {
        try {
            // Finish GZIP encodig
            if (gzipOut != null) {
                gzipOut.finish();
            }

            flushBuffer();
        }
        finally {
            // Close stream
            if (writer != null) {
                writer.close();
            }
            else {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        else if (out != null) {
            out.flush();
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called!");
        }

        if (out == null) {
            out = createOutputStream();
        }

        return out;
    }

    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return (writer);
        }

        if (out != null) {
            throw new IllegalStateException("getOutputStream() has already been called!");
        }

        out = createOutputStream();

        // TODO: This is wrong. Should use getCharacterEncoding() or "ISO-8859-1" if getCE returns null.
        writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

        return writer;
    }

    public void setContentLength(int pLength) {
        // NOTE: Do not call super, as we will shrink the size.
        contentLength = pLength;
    }
}
