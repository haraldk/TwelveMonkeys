package com.twelvemonkeys.servlet.log4j;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Set;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 * Log4JContextWrapper
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/log4j/Log4JContextWrapper.java#1 $
 */
final class Log4JContextWrapper implements ServletContext {

    // TODO: This solution sucks...
    // How about starting to create some kind of pluggable decorator system,
    // something along the lines of AOP mixins/interceptor pattern..
    // Probably using a dynamic Proxy, delegating to the mixins and or the
    // wrapped object based on configuration.
    // This way we could simply call ServletUtil.decorate(ServletContext):ServletContext
    // And the context would be decorated with all configured mixins at once,
    // requiring less bolierplate delegation code, and less layers of wrapping
    // (alternatively we could decorate the Servlet/FilterConfig objects).
    // See the ServletUtil.createWrapper methods for some hints..


    // Something like this:
    public static ServletContext wrap(final ServletContext pContext, final Object[] pDelegates, final ClassLoader pLoader) {
        ClassLoader cl = pLoader != null ? pLoader : Thread.currentThread().getContextClassLoader();

        // TODO: Create a "static" mapping between methods in the ServletContext
        // and the corresponding delegate

        // TODO: Resolve super-invokations, to delegate to next delegate in
        // chain, and finally invoke pContext  

        return (ServletContext) Proxy.newProxyInstance(cl, new Class[] {ServletContext.class}, new InvocationHandler() {
            public Object invoke(Object pProxy, Method pMethod, Object[] pArgs) throws Throwable {
                // TODO: Test if any of the delegates should receive, if so invoke

                // Else, invoke on original object
                return pMethod.invoke(pContext, pArgs);
            }
        });
    }

    private final ServletContext mContext;

    private final Logger mLogger;

    Log4JContextWrapper(ServletContext pContext) {
        mContext = pContext;

        // TODO: We want a logger per servlet, not per servlet context, right?
        mLogger = Logger.getLogger(pContext.getServletContextName());

        // TODO: Automatic init/config of Log4J using context parameter for log4j.xml?
        // See Log4JInit.java

        // TODO: Automatic config of properties in the context wrapper?
    }

    public final void log(final Exception pException, final String pMessage) {
        log(pMessage, pException);
    }

    // TODO: Add more logging methods to interface info/warn/error?
    // TODO: Implement these mehtods in GenericFilter/GenericServlet?

    public void log(String pMessage) {
        // TODO: Get logger for caller..
        // Should be possible using some stack peek hack, but that's slow...
        // Find a good way...
        // Maybe just pass it into the constuctor, and have one wrapper per servlet
        mLogger.info(pMessage);
    }

    public void log(String pMessage, Throwable pCause) {
        // TODO: Get logger for caller..

        mLogger.error(pMessage, pCause);
    }

    public Object getAttribute(String pMessage) {
        return mContext.getAttribute(pMessage);
    }

    public Enumeration getAttributeNames() {
        return mContext.getAttributeNames();
    }

    public ServletContext getContext(String pMessage) {
        return mContext.getContext(pMessage);
    }

    public String getInitParameter(String pMessage) {
        return mContext.getInitParameter(pMessage);
    }

    public Enumeration getInitParameterNames() {
        return mContext.getInitParameterNames();
    }

    public int getMajorVersion() {
        return mContext.getMajorVersion();
    }

    public String getMimeType(String pMessage) {
        return mContext.getMimeType(pMessage);
    }

    public int getMinorVersion() {
        return mContext.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String pMessage) {
        return mContext.getNamedDispatcher(pMessage);
    }

    public String getRealPath(String pMessage) {
        return mContext.getRealPath(pMessage);
    }

    public RequestDispatcher getRequestDispatcher(String pMessage) {
        return mContext.getRequestDispatcher(pMessage);
    }

    public URL getResource(String pMessage) throws MalformedURLException {
        return mContext.getResource(pMessage);
    }

    public InputStream getResourceAsStream(String pMessage) {
        return mContext.getResourceAsStream(pMessage);
    }

    public Set getResourcePaths(String pMessage) {
        return mContext.getResourcePaths(pMessage);
    }

    public String getServerInfo() {
        return mContext.getServerInfo();
    }

    public Servlet getServlet(String pMessage) throws ServletException {
        //noinspection deprecation
        return mContext.getServlet(pMessage);
    }

    public String getServletContextName() {
        return mContext.getServletContextName();
    }

    public Enumeration getServletNames() {
        //noinspection deprecation
        return mContext.getServletNames();
    }

    public Enumeration getServlets() {
        //noinspection deprecation
        return mContext.getServlets();
    }

    public void removeAttribute(String pMessage) {
        mContext.removeAttribute(pMessage);
    }

    public void setAttribute(String pMessage, Object pExtension) {
        mContext.setAttribute(pMessage, pExtension);
    }
}
