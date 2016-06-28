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

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.convert.ConversionException;
import com.twelvemonkeys.util.convert.Converter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * Various servlet related helper methods.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author Eirik Torske
 * @author last modified by $Author: haku $
 * @version $Id: ServletUtil.java#3 $
 */
public final class ServletUtil {

    /**
     * {@code "javax.servlet.include.request_uri"}
     */
    private final static String ATTRIB_INC_REQUEST_URI = "javax.servlet.include.request_uri";

    /**
     * {@code "javax.servlet.include.context_path"}
     */
    private final static String ATTRIB_INC_CONTEXT_PATH = "javax.servlet.include.context_path";

    /**
     * {@code "javax.servlet.include.servlet_path"}
     */
    private final static String ATTRIB_INC_SERVLET_PATH = "javax.servlet.include.servlet_path";

    /**
     * {@code "javax.servlet.include.path_info"}
     */
    private final static String ATTRIB_INC_PATH_INFO = "javax.servlet.include.path_info";

    /**
     * {@code "javax.servlet.include.query_string"}
     */
    private final static String ATTRIB_INC_QUERY_STRING = "javax.servlet.include.query_string";

    /**
     * {@code "javax.servlet.forward.request_uri"}
     */
    private final static String ATTRIB_FWD_REQUEST_URI = "javax.servlet.forward.request_uri";

    /**
     * {@code "javax.servlet.forward.context_path"}
     */
    private final static String ATTRIB_FWD_CONTEXT_PATH = "javax.servlet.forward.context_path";

    /**
     * {@code "javax.servlet.forward.servlet_path"}
     */
    private final static String ATTRIB_FWD_SERVLET_PATH = "javax.servlet.forward.servlet_path";

    /**
     * {@code "javax.servlet.forward.path_info"}
     */
    private final static String ATTRIB_FWD_PATH_INFO = "javax.servlet.forward.path_info";

    /**
     * {@code "javax.servlet.forward.query_string"}
     */
    private final static String ATTRIB_FWD_QUERY_STRING = "javax.servlet.forward.query_string";

    /**
     * Don't create, static methods only
     */
    private ServletUtil() {
    }

    /**
     * Gets the value of the given parameter from the request, or if the
     * parameter is not set, the default value.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter, or the default value, if the
     *         parameter is not set.
     */
    public static String getParameter(final ServletRequest pReq, final String pName, final String pDefault) {
        String str = pReq.getParameter(pName);

        return str != null ? str : pDefault;
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * an Object. If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pType    the type of object (class) to return
     * @param pFormat  the format to use (might be {@code null} in many cases)
     * @param pDefault the default value
     * @return the value of the parameter converted to a boolean, or the
     *         default value, if the parameter is not set.
     * @throws IllegalArgumentException if {@code pDefault} is
     *                                  non-{@code null} and not an instance of {@code pType}
     * @throws NullPointerException     if {@code pReq}, {@code pName} or
     *                                  {@code pType} is {@code null}.
     * @todo Well, it's done. Need some thinking... We probably don't want default if conversion fails...
     * @see Converter#toObject
     */
    static <T> T getParameter(final ServletRequest pReq, final String pName, final Class<T> pType, final String pFormat, final T pDefault) {
        // Test if pDefault is either null or instance of pType
        if (pDefault != null && !pType.isInstance(pDefault)) {
            throw new IllegalArgumentException("default value not instance of " + pType + ": " + pDefault.getClass());
        }

        String str = pReq.getParameter(pName);

        if (str == null) {
            return pDefault;
        }

        try {
            return pType.cast(Converter.getInstance().toObject(str, pType, pFormat));
        }
        catch (ConversionException ce) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a {@code boolean}.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to a {@code boolean}, or the
     *         default value, if the parameter is not set.
     */
    public static boolean getBooleanParameter(final ServletRequest pReq, final String pName, final boolean pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return str != null ? Boolean.valueOf(str) : pDefault;
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * an {@code int}.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to an {@code int}, or the default
     *         value, if the parameter is not set.
     */
    public static int getIntParameter(final ServletRequest pReq, final String pName, final int pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return str != null ? Integer.parseInt(str) : pDefault;
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * an {@code long}.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to an {@code long}, or the default
     *         value, if the parameter is not set.
     */
    public static long getLongParameter(final ServletRequest pReq, final String pName, final long pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return str != null ? Long.parseLong(str) : pDefault;
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a {@code float}.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to a {@code float}, or the default
     *         value, if the parameter is not set.
     */
    public static float getFloatParameter(final ServletRequest pReq, final String pName, final float pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return str != null ? Float.parseFloat(str) : pDefault;
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a {@code double}.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to n {@code double}, or the default
     *         value, if the parameter is not set.
     */
    public static double getDoubleParameter(final ServletRequest pReq, final String pName, final double pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return str != null ? Double.parseDouble(str) : pDefault;
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a {@code Date}.&nbsp;If the parameter is not set or not parseable, the
     * default value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to a {@code Date}, or the
     *         default value, if the parameter is not set.
     * @see com.twelvemonkeys.lang.StringUtil#toDate(String)
     */
    public static long getDateParameter(final ServletRequest pReq, final String pName, final long pDefault) {
        String str = pReq.getParameter(pName);
        try {
            return str != null ? StringUtil.toDate(str).getTime() : pDefault;
        }
        catch (IllegalArgumentException iae) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a Date.&nbsp;If the parameter is not set or not parseable, the
     * default value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pFormat  the date format to use
     * @param pDefault the default value
     * @return the value of the parameter converted to a Date, or the
     *         default value, if the parameter is not set.
     * @see com.twelvemonkeys.lang.StringUtil#toDate(String,String)
     */
    /*
    public static long getDateParameter(ServletRequest pReq, String pName, String pFormat, long pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return ((str != null) ? StringUtil.toDate(str, pFormat).getTime() : pDefault);
        }
        catch (IllegalArgumentException iae) {
            return pDefault;
        }
    }
    */

    /**
     * Builds a full-blown HTTP/HTTPS URL from a
     * {@code javax.servlet.http.HttpServletRequest} object.
     * <p/>
     *
     * @param pRequest The HTTP servlet request object.
     * @return the reproduced URL
     * @deprecated Use {@link javax.servlet.http.HttpServletRequest#getRequestURL()}
     *             instead.
     */
    static StringBuffer buildHTTPURL(final HttpServletRequest pRequest) {
        StringBuffer resultURL = new StringBuffer();

        // Scheme, as in http, https, ftp etc
        String scheme = pRequest.getScheme();
        resultURL.append(scheme);
        resultURL.append("://");
        resultURL.append(pRequest.getServerName());

        // Append port only if not default port
        int port = pRequest.getServerPort();
        if (port > 0 &&
                !(("http".equals(scheme) && port == 80) ||
                        ("https".equals(scheme) && port == 443))) {
            resultURL.append(":");
            resultURL.append(port);
        }

        // Append URI
        resultURL.append(pRequest.getRequestURI());

        // If present, append extra path info
        String pathInfo = pRequest.getPathInfo();
        if (pathInfo != null) {
            resultURL.append(pathInfo);
        }

        return resultURL;
    }

    /**
     * Gets the URI of the resource currently included.
     * The value is read from the request attribute
     * {@code "javax.servlet.include.request_uri"}
     *
     * @param pRequest the servlet request
     * @return the URI of the included resource, or {@code null} if no include
     * @see HttpServletRequest#getRequestURI
     * @since Servlet 2.2
     */
    public static String getIncludeRequestURI(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_INC_REQUEST_URI);
    }

    /**
     * Gets the context path of the resource currently included.
     * The value is read from the request attribute
     * {@code "javax.servlet.include.context_path"}
     *
     * @param pRequest the servlet request
     * @return the context path of the included resource, or {@code null} if no include
     * @see HttpServletRequest#getContextPath
     * @since Servlet 2.2
     */
    public static String getIncludeContextPath(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_INC_CONTEXT_PATH);
    }

    /**
     * Gets the servlet path of the resource currently included.
     * The value is read from the request attribute
     * {@code "javax.servlet.include.servlet_path"}
     *
     * @param pRequest the servlet request
     * @return the servlet path of the included resource, or {@code null} if no include
     * @see HttpServletRequest#getServletPath
     * @since Servlet 2.2
     */
    public static String getIncludeServletPath(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_INC_SERVLET_PATH);
    }

    /**
     * Gets the path info of the resource currently included.
     * The value is read from the request attribute
     * {@code "javax.servlet.include.path_info"}
     *
     * @param pRequest the servlet request
     * @return the path info of the included resource, or {@code null} if no include
     * @see HttpServletRequest#getPathInfo
     * @since Servlet 2.2
     */
    public static String getIncludePathInfo(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_INC_PATH_INFO);
    }

    /**
     * Gets the query string of the resource currently included.
     * The value is read from the request attribute
     * {@code "javax.servlet.include.query_string"}
     *
     * @param pRequest the servlet request
     * @return the query string of the included resource, or {@code null} if no include
     * @see HttpServletRequest#getQueryString
     * @since Servlet 2.2
     */
    public static String getIncludeQueryString(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_INC_QUERY_STRING);
    }

    /**
     * Gets the URI of the resource this request was forwarded from.
     * The value is read from the request attribute
     * {@code "javax.servlet.forward.request_uri"}
     *
     * @param pRequest the servlet request
     * @return the URI of the resource, or {@code null} if not forwarded
     * @see HttpServletRequest#getRequestURI
     * @since Servlet 2.4
     */
    public static String getForwardRequestURI(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_FWD_REQUEST_URI);
    }

    /**
     * Gets the context path of the resource this request was forwarded from.
     * The value is read from the request attribute
     * {@code "javax.servlet.forward.context_path"}
     *
     * @param pRequest the servlet request
     * @return the context path of the resource, or {@code null} if not forwarded
     * @see HttpServletRequest#getContextPath
     * @since Servlet 2.4
     */
    public static String getForwardContextPath(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_FWD_CONTEXT_PATH);
    }

    /**
     * Gets the servlet path of the resource this request was forwarded from.
     * The value is read from the request attribute
     * {@code "javax.servlet.forward.servlet_path"}
     *
     * @param pRequest the servlet request
     * @return the servlet path of the resource, or {@code null} if not forwarded
     * @see HttpServletRequest#getServletPath
     * @since Servlet 2.4
     */
    public static String getForwardServletPath(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_FWD_SERVLET_PATH);
    }

    /**
     * Gets the path info of the resource this request was forwarded from.
     * The value is read from the request attribute
     * {@code "javax.servlet.forward.path_info"}
     *
     * @param pRequest the servlet request
     * @return the path info of the resource, or {@code null} if not forwarded
     * @see HttpServletRequest#getPathInfo
     * @since Servlet 2.4
     */
    public static String getForwardPathInfo(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_FWD_PATH_INFO);
    }

    /**
     * Gets the query string of the resource this request was forwarded from.
     * The value is read from the request attribute
     * {@code "javax.servlet.forward.query_string"}
     *
     * @param pRequest the servlet request
     * @return the query string of the resource, or {@code null} if not forwarded
     * @see HttpServletRequest#getQueryString
     * @since Servlet 2.4
     */
    public static String getForwardQueryString(final ServletRequest pRequest) {
        return (String) pRequest.getAttribute(ATTRIB_FWD_QUERY_STRING);
    }

    /**
     * Gets the name of the servlet or the script that generated the servlet.
     *
     * @param pRequest The HTTP servlet request object.
     * @return the script name.
     * @todo Read the spec, seems to be a mismatch with the Servlet API...
     * @see javax.servlet.http.HttpServletRequest#getServletPath()
     */
    static String getScriptName(final HttpServletRequest pRequest) {
        String requestURI = pRequest.getRequestURI();
        return StringUtil.getLastElement(requestURI, "/");
    }

    /**
     * Gets the request URI relative to the current context path.
     * <p/>
     * As an example: <pre>
     * requestURI = "/webapp/index.jsp"
     * contextPath = "/webapp"
     * </pre>
     * The method will return {@code "/index.jsp"}.
     *
     * @param pRequest the current HTTP request
     * @return the request URI relative to the current context path.
     */
    public static String getContextRelativeURI(final HttpServletRequest pRequest) {
        String context = pRequest.getContextPath();

        if (!StringUtil.isEmpty(context)) { // "" for root context
            return pRequest.getRequestURI().substring(context.length());
        }

        return pRequest.getRequestURI();
    }

    /**
     * Returns a {@code URL} containing the real path for a given virtual
     * path, on URL form.
     * Note that this method will return {@code null} for all the same reasons
     * as {@code ServletContext.getRealPath(java.lang.String)} does.
     *
     * @param pContext the servlet context
     * @param pPath    the virtual path
     * @return a {@code URL} object containing the path, or {@code null}.
     * @throws MalformedURLException if the path refers to a malformed URL
     * @see ServletContext#getRealPath(java.lang.String)
     * @see ServletContext#getResource(java.lang.String)
     */
    public static URL getRealURL(final ServletContext pContext, final String pPath) throws MalformedURLException {
        String realPath = pContext.getRealPath(pPath);

        if (realPath != null) {
            // NOTE: First convert to URI, as of Java 6 File.toURL is deprecated
            return new File(realPath).toURI().toURL();
        }

        return null;
    }

    /**
     * Gets the temp directory for the given {@code ServletContext} (web app).
     *
     * @param pContext the servlet context
     * @return the temp directory
     */
    public static File getTempDir(final ServletContext pContext) {
        return (File) pContext.getAttribute("javax.servlet.context.tempdir");
    }

    /**
     * Gets the unique identifier assigned to this session.
     * The identifier is assigned by the servlet container and is implementation
     * dependent.
     *
     * @param pRequest The HTTP servlet request object.
     * @return the session Id
     */
    public static String getSessionId(final HttpServletRequest pRequest) {
        HttpSession session = pRequest.getSession();

        return (session != null) ? session.getId() : null;
    }

    /**
     * Creates an unmodifiable {@code Map} view of the given
     * {@code ServletConfig}s init-parameters.
     * <small>Note: The returned {@code Map} is optimized for {@code get}
     * operations and iterating over it's {@code keySet}.
     * For other operations it may not perform well.</small>
     *
     * @param pConfig the servlet configuration
     * @return a {@code Map} view of the config
     * @throws IllegalArgumentException if {@code pConfig} is {@code null}
     */
    public static Map<String, String> asMap(final ServletConfig pConfig) {
        return new ServletConfigMapAdapter(pConfig);
    }

    /**
     * Creates an unmodifiable {@code Map} view of the given
     * {@code FilterConfig}s init-parameters.
     * <small>Note: The returned {@code Map} is optimized for {@code get}
     * operations and iterating over it's {@code keySet}.
     * For other operations it may not perform well.</small>
     *
     * @param pConfig the servlet filter configuration
     * @return a {@code Map} view of the config
     * @throws IllegalArgumentException if {@code pConfig} is {@code null}
     */
    public static Map<String, String> asMap(final FilterConfig pConfig) {
        return new ServletConfigMapAdapter(pConfig);
    }

    /**
     * Creates an unmodifiable {@code Map} view of the given
     * {@code ServletContext}s init-parameters.
     * <small>Note: The returned {@code Map} is optimized for {@code get}
     * operations and iterating over it's {@code keySet}.
     * For other operations it may not perform well.</small>
     *
     * @param pContext the servlet context
     * @return a {@code Map} view of the init parameters
     * @throws IllegalArgumentException if {@code pContext} is {@code null}
     */
    public static Map<String, String> initParamsAsMap(final ServletContext pContext) {
        return new ServletConfigMapAdapter(pContext);
    }

    /**
     * Creates an <em>modifiable</em> {@code Map} view of the given
     * {@code ServletContext}s attributes.
     *
     * @param pContext the servlet context
     * @return a {@code Map} view of the attributes
     * @throws IllegalArgumentException if {@code pContext} is {@code null}
     */
    public static Map<String, Object> attributesAsMap(final ServletContext pContext) {
        return new ServletAttributesMapAdapter(pContext);
    }

    /**
     * Creates an <em>modifiable</em> {@code Map} view of the given
     * {@code ServletRequest}s attributes.
     *
     * @param pRequest the servlet request
     * @return a {@code Map} view of the attributes
     * @throws IllegalArgumentException if {@code pContext} is {@code null}
     */
    public static Map<String, Object> attributesAsMap(final ServletRequest pRequest) {
        return new ServletAttributesMapAdapter(pRequest);
    }

    /**
     * Creates an unmodifiable {@code Map} view of the given
     * {@code HttpServletRequest}s request parameters.
     *
     * @param pRequest the request
     * @return a {@code Map} view of the request parameters
     * @throws IllegalArgumentException if {@code pRequest} is {@code null}
     */
    public static Map<String, List<String>> parametersAsMap(final ServletRequest pRequest) {
        return new ServletParametersMapAdapter(pRequest);
    }

    /**
     * Creates an unmodifiable {@code Map} view of the given
     * {@code HttpServletRequest}s request headers.
     *
     * @param pRequest the request
     * @return a {@code Map} view of the request headers
     * @throws IllegalArgumentException if {@code pRequest} is {@code null}
     */
    public static Map<String, List<String>> headersAsMap(final HttpServletRequest pRequest) {
        return new ServletHeadersMapAdapter(pRequest);
    }

    /**
     * Creates a wrapper that implements either {@code ServletResponse} or
     * {@code HttpServletResponse}, depending on the type of
     * {@code pImplementation.getResponse()}.
     *
     * @param pImplementation the servlet response to create a wrapper for
     * @return a {@code ServletResponse} or
     *         {@code HttpServletResponse}, depending on the type of
     *         {@code pImplementation.getResponse()}
     */
    public static ServletResponse createWrapper(final ServletResponseWrapper pImplementation) {
        // TODO: Get all interfaces from implementation
        if (pImplementation.getResponse() instanceof HttpServletResponse) {
            return (HttpServletResponse) Proxy.newProxyInstance(pImplementation.getClass().getClassLoader(),
                    new Class[]{HttpServletResponse.class, ServletResponse.class},
                    new HttpServletResponseHandler(pImplementation));
        }
        return pImplementation;
    }

    /**
     * Creates a wrapper that implements either {@code ServletRequest} or
     * {@code HttpServletRequest}, depending on the type of
     * {@code pImplementation.getRequest()}.
     *
     * @param pImplementation the servlet request to create a wrapper for
     * @return a {@code ServletResponse} or
     *         {@code HttpServletResponse}, depending on the type of
     *         {@code pImplementation.getResponse()}
     */
    public static ServletRequest createWrapper(final ServletRequestWrapper pImplementation) {
        // TODO: Get all interfaces from implementation
        if (pImplementation.getRequest() instanceof HttpServletRequest) {
            return (HttpServletRequest) Proxy.newProxyInstance(pImplementation.getClass().getClassLoader(),
                    new Class[]{HttpServletRequest.class, ServletRequest.class},
                    new HttpServletRequestHandler(pImplementation));
        }
        return pImplementation;
    }

    private static class HttpServletResponseHandler implements InvocationHandler {
        private final ServletResponseWrapper response;

        HttpServletResponseHandler(final ServletResponseWrapper pResponse) {
            response = pResponse;
        }

        public Object invoke(final Object pProxy, final Method pMethod, final Object[] pArgs) throws Throwable {
            try {
                // TODO: Allow partial implementing?
                if (pMethod.getDeclaringClass().isInstance(response)) {
                    return pMethod.invoke(response, pArgs);
                }

                // Method is not implemented in wrapper
                return pMethod.invoke(response.getResponse(), pArgs);
            }
            catch (InvocationTargetException e) {
                // Unwrap, to avoid UndeclaredThrowableException...
                throw e.getTargetException();
            }
        }
    }

    private static class HttpServletRequestHandler implements InvocationHandler {
        private final ServletRequestWrapper request;

        HttpServletRequestHandler(final ServletRequestWrapper pRequest) {
            request = pRequest;
        }

        public Object invoke(final Object pProxy, final Method pMethod, final Object[] pArgs) throws Throwable {
            try {
                // TODO: Allow partial implementing?
                if (pMethod.getDeclaringClass().isInstance(request)) {
                    return pMethod.invoke(request, pArgs);
                }

                // Method is not implemented in wrapper
                return pMethod.invoke(request.getRequest(), pArgs);
            }
            catch (InvocationTargetException e) {
                // Unwrap, to avoid UndeclaredThrowableException...
                throw e.getTargetException();
            }
        }
    }
}

