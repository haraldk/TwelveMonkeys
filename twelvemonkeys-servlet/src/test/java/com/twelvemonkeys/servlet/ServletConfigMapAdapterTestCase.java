package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.MapAbstractTestCase;

import javax.servlet.*;
import java.util.*;
import java.io.Serializable;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * ServletConfigMapAdapterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletConfigMapAdapterTestCase.java#3 $
 */
public abstract class ServletConfigMapAdapterTestCase extends MapAbstractTestCase {

    public boolean isPutAddSupported() {
        return false;
    }

    public boolean isPutChangeSupported() {
        return false;
    }

    public boolean isRemoveSupported() {
        return false;
    }

    public boolean isSetValueSupported() {
        return false;
    }

    private static class TestConfig implements ServletConfig, FilterConfig, ServletContext, Serializable, Cloneable {
        Map mMap = new HashMap();

        public String getServletName() {
            return "dummy"; // Not needed for this test
        }

        public String getFilterName() {
            return getServletName();
        }

        public String getServletContextName() {
            return getServletName();
        }


        public ServletContext getServletContext() {
            throw new UnsupportedOperationException("Method getSerlvetContext not implemented");
        }

        public String getInitParameter(String s) {
            return (String) mMap.get(s);
        }

        public Enumeration getInitParameterNames() {
            //noinspection unchecked
            return Collections.enumeration(mMap.keySet());
        }

        public ServletContext getContext(String uripath) {
            throw new UnsupportedOperationException("Method getContext not implemented");
        }

        public int getMajorVersion() {
            throw new UnsupportedOperationException("Method getMajorVersion not implemented");
        }

        public int getMinorVersion() {
            throw new UnsupportedOperationException("Method getMinorVersion not implemented");
        }

        public String getMimeType(String file) {
            throw new UnsupportedOperationException("Method getMimeType not implemented");
        }

        public Set getResourcePaths(String path) {
            throw new UnsupportedOperationException("Method getResourcePaths not implemented");
        }

        public URL getResource(String path) throws MalformedURLException {
            throw new UnsupportedOperationException("Method getResource not implemented");
        }

        public InputStream getResourceAsStream(String path) {
            throw new UnsupportedOperationException("Method getResourceAsStream not implemented");
        }

        public RequestDispatcher getRequestDispatcher(String path) {
            throw new UnsupportedOperationException("Method getRequestDispatcher not implemented");
        }

        public RequestDispatcher getNamedDispatcher(String name) {
            throw new UnsupportedOperationException("Method getNamedDispatcher not implemented");
        }

        public Servlet getServlet(String name) throws ServletException {
            throw new UnsupportedOperationException("Method getServlet not implemented");
        }

        public Enumeration getServlets() {
            throw new UnsupportedOperationException("Method getServlets not implemented");
        }

        public Enumeration getServletNames() {
            throw new UnsupportedOperationException("Method getServletNames not implemented");
        }

        public void log(String msg) {
            throw new UnsupportedOperationException("Method log not implemented");
        }

        public void log(Exception exception, String msg) {
            throw new UnsupportedOperationException("Method log not implemented");
        }

        public void log(String message, Throwable throwable) {
            throw new UnsupportedOperationException("Method log not implemented");
        }

        public String getRealPath(String path) {
            throw new UnsupportedOperationException("Method getRealPath not implemented");
        }

        public String getServerInfo() {
            throw new UnsupportedOperationException("Method getServerInfo not implemented");
        }

        public Object getAttribute(String name) {
            throw new UnsupportedOperationException("Method getAttribute not implemented");
        }

        public Enumeration getAttributeNames() {
            throw new UnsupportedOperationException("Method getAttributeNames not implemented");
        }

        public void setAttribute(String name, Object object) {
            throw new UnsupportedOperationException("Method setAttribute not implemented");
        }

        public void removeAttribute(String name) {
            throw new UnsupportedOperationException("Method removeAttribute not implemented");
        }
    }

    public static final class ServletConfigMapTestCase extends ServletConfigMapAdapterTestCase {

        public Map makeEmptyMap() {
            ServletConfig config = new TestConfig();
            return new ServletConfigMapAdapter(config);
        }

        public Map makeFullMap() {
            ServletConfig config = new TestConfig();
            addSampleMappings(((TestConfig) config).mMap);
            return new ServletConfigMapAdapter(config);
        }
    }

    public static final class FilterConfigMapTestCase extends ServletConfigMapAdapterTestCase {

        public Map makeEmptyMap() {
            FilterConfig config = new TestConfig();
            return new ServletConfigMapAdapter(config);
        }

        public Map makeFullMap() {
            FilterConfig config = new TestConfig();
            addSampleMappings(((TestConfig) config).mMap);
            return new ServletConfigMapAdapter(config);
        }
    }

    public static final class ServletContextMapTestCase extends ServletConfigMapAdapterTestCase {

        public Map makeEmptyMap() {
            ServletContext config = new TestConfig();
            return new ServletConfigMapAdapter(config);
        }

        public Map makeFullMap() {
            FilterConfig config = new TestConfig();
            addSampleMappings(((TestConfig) config).mMap);
            return new ServletConfigMapAdapter(config);
        }
    }
}
