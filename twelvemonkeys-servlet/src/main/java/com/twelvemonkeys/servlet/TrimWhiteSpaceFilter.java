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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.FilterOutputStream;

/**
 * Removes extra unneccessary white space from a servlet response.
 * White space is defined as per {@link Character#isWhitespace(char)}.
 * <p/>
 * This filter has no understanding of the content in the reponse, and will
 * remove repeated white space anywhere in the stream. It is intended for
 * removing white space from HTML or XML streams, but this limitation makes it
 * less suited for filtering HTML/XHTML with embedded CSS or JavaScript,
 * in case white space should be significant here. It is strongly reccommended
 * you keep CSS and JavaScript in separate files (this will have the added
 * benefit of further reducing the ammount of data communicated between
 * server and client).
 * <p/>
 * <em>At the moment this filter has no concept of encoding</em>.
 * This means, that if some multi-byte escape sequence contains one or more
 * bytes that <em>individually</em> is treated as a white space, these bytes
 * may be skipped.
 * As <a href="http://en.wikipedia.org/wiki/UTF-8" title="UTF-8">UTF-8</a>
 * guarantees that no bytes are repeated in this way, this filter can safely
 * filter UTF-8.
 * Simple 8 bit character encodings, like the
 * <a href="http://en.wikipedia.org/wiki/ISO/IEC_8859"
 * title="ISO/IEC 8859">ISO/IEC 8859</a> standard, or
 * <a href="http://en.wikipedia.org/wiki/Windows-1252" title="Windows-1252">
 * are always safe.
 * <p/>
 * <b>Configuration</b><br/>
 * To use {@code TrimWhiteSpaceFilter} in your web-application, you simply need
 * to add it to your web descriptor ({@code web.xml}).
 * If using a servlet container that supports the Servlet 2.4 spec, the new
 * {@code dispatcher} element should be used, and set to
 * {@code REQUEST/FORWARD}, to make sure the filter is invoked only once for 
 * requests.
 * If using an older web descriptor, set the {@code init-param}
 * {@code "once-per-request"} to {@code "true"} (this will have the same effect,
 * but might perform slightly worse than the 2.4 version).
 * Please see the examples below.
 * <p/>
 * <b>Servlet 2.4 version, filter section:</b><br/>
 * <pre>
 * &lt;!-- TrimWS Filter Configuration --&gt;
 * &lt;filter&gt;
 *      &lt;filter-name&gt;trimws&lt;/filter-name&gt;
 *      &lt;filter-class&gt;com.twelvemonkeys.servlet.TrimWhiteSpaceFilter&lt;/filter-class&gt;
 *      &lt;!-- auto-flush=true is the default, may be omitted --&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;auto-flush&lt;/param-name&gt;
 *          &lt;param-value&gt;true&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </pre>
 * <b>Filter-mapping section:</b><br/>
 * <pre>
 * &lt;!-- TimWS Filter Mapping --&gt;
 * &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;trimws&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;*.html&lt;/url-pattern&gt;
 *      &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *      &lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;trimws&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;*.jsp&lt;/url-pattern&gt;
 *      &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *      &lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/TrimWhiteSpaceFilter.java#2 $
 */
public class TrimWhiteSpaceFilter extends GenericFilter {

    private boolean mAutoFlush = true;

    @InitParam
    public void setAutoFlush(final boolean pAutoFlush) {
        mAutoFlush = pAutoFlush;
    }

    public void init() throws ServletException {
        super.init();
        log("Automatic flushing is " + (mAutoFlush ? "enabled" : "disabled"));
    }

    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        ServletResponseWrapper wrapped = new TrimWSServletResponseWrapper(pResponse);
        pChain.doFilter(pRequest, ServletUtil.createWrapper(wrapped));
        if (mAutoFlush) {
            wrapped.flushBuffer();
        }
    }

    static final class TrimWSFilterOutputStream extends FilterOutputStream {
        boolean mLastWasWS = true; // Avoids leading WS by init to true

        public TrimWSFilterOutputStream(OutputStream pOut) {
            super(pOut);
        }

        // Override this, in case the wrapped outputstream overrides...
        public final void write(byte pBytes[]) throws IOException {
            write(pBytes, 0, pBytes.length);
        }

        // Override this, in case the wrapped outputstream overrides...
        public final void write(byte pBytes[], int pOff, int pLen) throws IOException {
            if (pBytes == null) {
                throw new NullPointerException("bytes == null");
            }
            else if (pOff < 0 || pLen < 0 || (pOff + pLen > pBytes.length)) {
                throw new IndexOutOfBoundsException("Bytes: " + pBytes.length + " Offset: " + pOff + " Length: " + pLen);
            }

            for (int i = 0; i < pLen ; i++) {
                write(pBytes[pOff + i]);
            }
        }

        public void write(int pByte) throws IOException {
            // TODO: Is this good enough for multi-byte encodings like UTF-16?
            // Consider writing through a Writer that does that for us, and
            // also buffer whitespace, so we write a linefeed every time there's
            // one in the original...

            // According to http://en.wikipedia.org/wiki/UTF-8:
            // "[...] US-ASCII octet values do not appear otherwise in a UTF-8
            // encoded character stream. This provides compatibility with file
            // systems or other software (e.g., the printf() function in
            // C libraries) that parse based on US-ASCII values but are
            // transparent to other values."

            if (!Character.isWhitespace((char) pByte)) {
                // If char is not WS, just store
                super.write(pByte);
                mLastWasWS = false;
            }
            else {
                // TODO: Consider writing only 0x0a (LF) and 0x20 (space)
                // Else, if char is WS, store first, skip the rest
                if (!mLastWasWS) {
                    if (pByte == 0x0d) { // Convert all CR/LF's to 0x0a
                        super.write(0x0a);
                    }
                    else {
                        super.write(pByte);
                    }
                }
                mLastWasWS = true;
            }
        }
    }

    private static class TrimWSStreamDelegate extends ServletResponseStreamDelegate {
        public TrimWSStreamDelegate(ServletResponse pResponse) {
            super(pResponse);
        }

        protected OutputStream createOutputStream() throws IOException {
            return new TrimWSFilterOutputStream(mResponse.getOutputStream());
        }
    }

    static class TrimWSServletResponseWrapper extends ServletResponseWrapper {
        private final ServletResponseStreamDelegate mStreamDelegate = new TrimWSStreamDelegate(getResponse());

        public TrimWSServletResponseWrapper(ServletResponse pResponse) {
            super(pResponse);
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return mStreamDelegate.getOutputStream();
        }

        public PrintWriter getWriter() throws IOException {
            return mStreamDelegate.getWriter();
        }

        public void setContentLength(int pLength) {
            // Will be changed by filter, so don't set.
        }

        @Override
        public void flushBuffer() throws IOException {
            mStreamDelegate.flushBuffer();
        }

        @Override
        public void resetBuffer() {
            mStreamDelegate.resetBuffer();
        }

        // TODO: Consider picking up content-type/encoding, as we can only
        // filter US-ASCII, UTF-8 and other compatible encodings?
    }
}