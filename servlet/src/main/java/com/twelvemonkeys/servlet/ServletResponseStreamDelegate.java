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

package com.twelvemonkeys.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * A delegate for handling stream support in wrapped servlet responses.
 * <p/>
 * Client code should delegate {@code getOutputStream}, {@code getWriter},
 * {@code flushBuffer} and {@code resetBuffer} methods from the servlet response.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ServletResponseStreamDelegate.java#2 $
 */
public class ServletResponseStreamDelegate {
    private Object out = null;
    protected final ServletResponse response;

    public ServletResponseStreamDelegate(final ServletResponse pResponse) {
        response = notNull(pResponse, "response");
    }

    // NOTE: Intentionally NOT thread safe, as one request/response should be handled by one thread ONLY.
    public final ServletOutputStream getOutputStream() throws IOException {
        if (out == null) {
            OutputStream out = createOutputStream();
            this.out = out instanceof ServletOutputStream ? out : new OutputStreamAdapter(out);
        }
        else if (out instanceof PrintWriter) {
            throw new IllegalStateException("getWriter() already called.");
        }

        return (ServletOutputStream) out;
    }

    // NOTE: Intentionally NOT thread safe, as one request/response should be handled by one thread ONLY.
    public final PrintWriter getWriter() throws IOException {
        if (out == null) {
            // NOTE: getCharacterEncoding may/should not return null
            OutputStream out = createOutputStream();
            String charEncoding = response.getCharacterEncoding();
            this.out = new PrintWriter(charEncoding != null ? new OutputStreamWriter(out, charEncoding) : new OutputStreamWriter(out));
        }
        else if (out instanceof ServletOutputStream) {
            throw new IllegalStateException("getOutputStream() already called.");
        }

        return (PrintWriter) out;
    }

    /**
     * Returns the {@code OutputStream}.
     * Subclasses should override this method to provide a decorated output stream.
     * This method is guaranteed to be invoked only once for a request/response
     * (unless {@code resetBuffer} is invoked).
     * <P/>
     * This implementation simply returns the output stream from the wrapped
     * response.
     *
     * @return the {@code OutputStream} to use for the response
     * @throws IOException if an I/O exception occurs
     */
    protected OutputStream createOutputStream() throws IOException {
        return response.getOutputStream();
    }

    public void flushBuffer() throws IOException {
        if (out instanceof ServletOutputStream) {
            ((ServletOutputStream) out).flush();
        }
        else if (out != null) {
            ((PrintWriter) out).flush();
        }
    }

    public void resetBuffer() {
        out = null;
    }
}
