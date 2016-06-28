package com.twelvemonkeys.servlet.log4j;

import org.apache.log4j.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

/**
 * Log4JContextWrapper
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: log4j/Log4JContextWrapper.java#1 $
 */
final class Log4JContextWrapper implements ServletContext {
    // TODO: Move to sandbox

    // TODO: This solution sucks...
    // How about starting to create some kind of pluggable decorator system,
    // something along the lines of AOP mixins/interceptor pattern..
    // Probably using a dynamic Proxy, delegating to the mixins and or the
    // wrapped object based on configuration.
    // This way we could simply call ServletUtil.decorate(ServletContext):ServletContext
    // And the context would be decorated with all configured mixins at once,
    // requiring less boilerplate delegation code, and less layers of wrapping
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

    private final ServletContext context;

    private final Logger logger;

    Log4JContextWrapper(ServletContext pContext) {
        context = pContext;

        // TODO: We want a logger per servlet, not per servlet context, right?
        logger = Logger.getLogger(pContext.getServletContextName());

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
        logger.info(pMessage);
    }

    public void log(String pMessage, Throwable pCause) {
        // TODO: Get logger for caller..

        logger.error(pMessage, pCause);
    }

    public Object getAttribute(String pMessage) {
        return context.getAttribute(pMessage);
    }

    public Enumeration getAttributeNames() {
        return context.getAttributeNames();
    }

    public ServletContext getContext(String pMessage) {
        return context.getContext(pMessage);
    }

    public String getInitParameter(String pMessage) {
        return context.getInitParameter(pMessage);
    }

    public Enumeration getInitParameterNames() {
        return context.getInitParameterNames();
    }

    public int getMajorVersion() {
        return context.getMajorVersion();
    }

    public String getMimeType(String pMessage) {
        return context.getMimeType(pMessage);
    }

    public int getMinorVersion() {
        return context.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String pMessage) {
        return context.getNamedDispatcher(pMessage);
    }

    public String getRealPath(String pMessage) {
        return context.getRealPath(pMessage);
    }

    public RequestDispatcher getRequestDispatcher(String pMessage) {
        return context.getRequestDispatcher(pMessage);
    }

    public URL getResource(String pMessage) throws MalformedURLException {
        return context.getResource(pMessage);
    }

    public InputStream getResourceAsStream(String pMessage) {
        return context.getResourceAsStream(pMessage);
    }

    public Set getResourcePaths(String pMessage) {
        return context.getResourcePaths(pMessage);
    }

    public String getServerInfo() {
        return context.getServerInfo();
    }

    public Servlet getServlet(String pMessage) throws ServletException {
        //noinspection deprecation
        return context.getServlet(pMessage);
    }

    public String getServletContextName() {
        return context.getServletContextName();
    }

    public Enumeration getServletNames() {
        //noinspection deprecation
        return context.getServletNames();
    }

    public Enumeration getServlets() {
        //noinspection deprecation
        return context.getServlets();
    }

    public void removeAttribute(String pMessage) {
        context.removeAttribute(pMessage);
    }

    public void setAttribute(String pMessage, Object pExtension) {
        context.setAttribute(pMessage, pExtension);
    }
}
