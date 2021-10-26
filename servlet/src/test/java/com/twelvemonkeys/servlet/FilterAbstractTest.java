/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet;

import com.twelvemonkeys.lang.ObjectAbstractTest;
import org.junit.Test;

import javax.servlet.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * FilterAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/FilterAbstractTestCase.java#1 $
 */
public abstract class FilterAbstractTest extends ObjectAbstractTest {
    protected Object makeObject() {
        return makeFilter();
    }

    protected abstract Filter makeFilter();

    // TODO: Is it a good thing to have an API like this?
    protected FilterConfig makeFilterConfig() {
        return makeFilterConfig(new HashMap());
    }

    protected FilterConfig makeFilterConfig(Map pParams) {
        return new MockFilterConfig(pParams);
    }

    protected ServletRequest makeRequest() {
        return new MockServletRequest();
    }

    protected ServletResponse makeResponse() {
        return new MockServletResponse();
    }

    protected FilterChain makeFilterChain() {
        return new MockFilterChain();
    }

    @Test
    public void testInitNull() {
        Filter filter = makeFilter();

        // The spec seems to be a little unclear on this issue, but anyway,
        // the container should never invoke init(null)...
        try {
            filter.init(null);
            fail("Should throw Exception on init(null)");
        }
        catch (IllegalArgumentException e) {
            // Good
        }
        catch (NullPointerException e) {
            // Bad (but not unreasonable)
        }
        catch (ServletException e) {
            // Hmmm.. The jury is still out.
        }
    }

    @Test
    public void testInit() {
        Filter filter = makeFilter();

        try {
            filter.init(makeFilterConfig());
        }
        catch (ServletException e) {
            assertNotNull(e.getMessage());
        }
        finally {
            filter.destroy();
        }
    }

    @Test
    public void testLifeCycle() throws ServletException {
        Filter filter = makeFilter();

        try {
            filter.init(makeFilterConfig());
        }
        finally {
            filter.destroy();
        }
    }

    @Test
    public void testFilterBasic() throws ServletException, IOException {
        Filter filter = makeFilter();

        try {
            filter.init(makeFilterConfig());

            filter.doFilter(makeRequest(), makeResponse(), makeFilterChain());
        }
        finally {
            filter.destroy();
        }
    }

    @Test
    public void testDestroy() {
        // TODO: Implement
    }

    static class MockFilterConfig implements FilterConfig {
        private final Map<String, String> params;

        MockFilterConfig(Map<String, String> pParams) {
            if (pParams == null) {
                throw new IllegalArgumentException("params == null");
            }
            params = pParams;
        }

        public String getFilterName() {
            return "mock-filter";
        }

        public String getInitParameter(String pName) {
            return params.get(pName);
        }

        public Enumeration getInitParameterNames() {
            return Collections.enumeration(params.keySet());
        }

        public ServletContext getServletContext() {
            return new MockServletContext();
        }

        private static class MockServletContext implements ServletContext {
            private final Map<String, Object> attributes;
            private final Map<String, String> params;

            MockServletContext() {
                attributes = new HashMap<>();
                params = new HashMap<>();
            }

            public Object getAttribute(String s) {
                return attributes.get(s);
            }

            public Enumeration getAttributeNames() {
                return Collections.enumeration(attributes.keySet());
            }

            public ServletContext getContext(String s) {
                return null;  // TODO: Implement
            }

            public String getInitParameter(String s) {
                return params.get(s);
            }

            public Enumeration getInitParameterNames() {
                return Collections.enumeration(params.keySet());
            }

            public int getMajorVersion() {
                return 0;  // TODO: Implement
            }

            public String getMimeType(String s) {
                return null;  // TODO: Implement
            }

            public int getMinorVersion() {
                return 0;  // TODO: Implement
            }

            public RequestDispatcher getNamedDispatcher(String s) {
                return null;  // TODO: Implement
            }

            public String getRealPath(String s) {
                return null;  // TODO: Implement
            }

            public RequestDispatcher getRequestDispatcher(String s) {
                return null;  // TODO: Implement
            }

            public URL getResource(String s) throws MalformedURLException {
                return null;  // TODO: Implement
            }

            public InputStream getResourceAsStream(String s) {
                return null;  // TODO: Implement
            }

            public Set getResourcePaths(String s) {
                return null;  // TODO: Implement
            }

            public String getServerInfo() {
                return null;  // TODO: Implement
            }

            public Servlet getServlet(String s) throws ServletException {
                return null;  // TODO: Implement
            }

            public String getServletContextName() {
                return "mock";
            }

            public Enumeration getServletNames() {
                return null;  // TODO: Implement
            }

            public Enumeration getServlets() {
                return null;  // TODO: Implement
            }

            public void log(Exception exception, String s) {
            }

            public void log(String s) {
            }

            public void log(String s, Throwable throwable) {
            }

            public void removeAttribute(String s) {
                attributes.remove(s);
            }

            public void setAttribute(String s, Object obj) {
                attributes.put(s, obj);
            }
        }
    }

    static class MockServletRequest implements ServletRequest {
        final private Map<String, Object> attributes;

        public MockServletRequest() {
            attributes = new HashMap<String, Object>();
        }

        public Object getAttribute(String pKey) {
            return attributes.get(pKey);
        }

        public Enumeration getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        public String getCharacterEncoding() {
            return null;  // TODO: Implement
        }

        public void setCharacterEncoding(String pMessage) throws UnsupportedEncodingException {
            // TODO: Implement
        }

        public int getContentLength() {
            return 0;  // TODO: Implement
        }

        public String getContentType() {
            return null;  // TODO: Implement
        }

        public ServletInputStream getInputStream() throws IOException {
            return null;  // TODO: Implement
        }

        public String getParameter(String pMessage) {
            return null;  // TODO: Implement
        }

        public Enumeration getParameterNames() {
            return null;  // TODO: Implement
        }

        public String[] getParameterValues(String pMessage) {
            return new String[0];  // TODO: Implement
        }

        public Map getParameterMap() {
            return null;  // TODO: Implement
        }

        public String getProtocol() {
            return null;  // TODO: Implement
        }

        public String getScheme() {
            return null;  // TODO: Implement
        }

        public String getServerName() {
            return null;  // TODO: Implement
        }

        public int getServerPort() {
            return 0;  // TODO: Implement
        }

        public BufferedReader getReader() throws IOException {
            return null;  // TODO: Implement
        }

        public String getRemoteAddr() {
            return null;  // TODO: Implement
        }

        public String getRemoteHost() {
            return null;  // TODO: Implement
        }

        public void setAttribute(String pKey, Object pValue) {
            attributes.put(pKey, pValue);
        }

        public void removeAttribute(String pKey) {
            attributes.remove(pKey);
        }

        public Locale getLocale() {
            return null;  // TODO: Implement
        }

        public Enumeration getLocales() {
            return null;  // TODO: Implement
        }

        public boolean isSecure() {
            return false;  // TODO: Implement
        }

        public RequestDispatcher getRequestDispatcher(String pMessage) {
            return null;  // TODO: Implement
        }

        public String getRealPath(String pMessage) {
            return null;  // TODO: Implement
        }

        public int getRemotePort() {
            throw new UnsupportedOperationException("Method getRemotePort not implemented");// TODO: Implement
        }

        public String getLocalName() {
            throw new UnsupportedOperationException("Method getLocalName not implemented");// TODO: Implement
        }

        public String getLocalAddr() {
            throw new UnsupportedOperationException("Method getLocalAddr not implemented");// TODO: Implement
        }

        public int getLocalPort() {
            throw new UnsupportedOperationException("Method getLocalPort not implemented");// TODO: Implement
        }
    }

    static class MockServletResponse implements ServletResponse {
        public void flushBuffer() throws IOException {
            // TODO: Implement
        }

        public int getBufferSize() {
            return 0;  // TODO: Implement
        }

        public String getCharacterEncoding() {
            return null;  // TODO: Implement
        }

        public String getContentType() {
            throw new UnsupportedOperationException("Method getContentType not implemented");// TODO: Implement
        }

        public Locale getLocale() {
            return null;  // TODO: Implement
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return null;  // TODO: Implement
        }

        public PrintWriter getWriter() throws IOException {
            return null;  // TODO: Implement
        }

        public void setCharacterEncoding(String charset) {
            throw new UnsupportedOperationException("Method setCharacterEncoding not implemented");// TODO: Implement
        }

        public boolean isCommitted() {
            return false;  // TODO: Implement
        }

        public void reset() {
            // TODO: Implement
        }

        public void resetBuffer() {
            // TODO: Implement
        }

        public void setBufferSize(int pLength) {
            // TODO: Implement
        }

        public void setContentLength(int pLength) {
            // TODO: Implement
        }

        public void setContentType(String pMessage) {
            // TODO: Implement
        }

        public void setLocale(Locale pLocale) {
            // TODO: Implement
        }
    }

    static class MockFilterChain implements FilterChain {
        public void doFilter(ServletRequest pRequest, ServletResponse pResponse) throws IOException, ServletException {
            // TODO: Implement
        }
    }
}
