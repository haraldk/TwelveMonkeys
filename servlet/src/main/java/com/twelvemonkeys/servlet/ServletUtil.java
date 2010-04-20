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
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;


/**
 * Various servlet related helper methods.
 *
 * @author Harald Kuhr
 * @author Eirik Torske
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/ServletUtil.java#3 $
 */
public final class ServletUtil {

    /**
     * "javax.servlet.include.request_uri"
     */
    private final static String ATTRIB_INC_REQUEST_URI = "javax.servlet.include.request_uri";

    /**
     * "javax.servlet.include.context_path"
     */
    private final static String ATTRIB_INC_CONTEXT_PATH = "javax.servlet.include.context_path";

    /**
     * "javax.servlet.include.servlet_path"
     */
    private final static String ATTRIB_INC_SERVLET_PATH = "javax.servlet.include.servlet_path";

    /**
     * "javax.servlet.include.path_info"
     */
    private final static String ATTRIB_INC_PATH_INFO = "javax.servlet.include.path_info";

    /**
     * "javax.servlet.include.query_string"
     */
    private final static String ATTRIB_INC_QUERY_STRING = "javax.servlet.include.query_string";

    /**
     * "javax.servlet.forward.request_uri"
     */
    private final static String ATTRIB_FWD_REQUEST_URI = "javax.servlet.forward.request_uri";

    /**
     * "javax.servlet.forward.context_path"
     */
    private final static String ATTRIB_FWD_CONTEXT_PATH = "javax.servlet.forward.context_path";

    /**
     * "javax.servlet.forward.servlet_path"
     */
    private final static String ATTRIB_FWD_SERVLET_PATH = "javax.servlet.forward.servlet_path";

    /**
     * "javax.servlet.forward.path_info"
     */
    private final static String ATTRIB_FWD_PATH_INFO = "javax.servlet.forward.path_info";

    /**
     * "javax.servlet.forward.query_string"
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
    public static String getParameter(ServletRequest pReq, String pName, String pDefault) {
        String str = pReq.getParameter(pName);

        return ((str != null) ? str : pDefault);
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
     * @todo Well, it's done. Need some thinking...
     * @see Converter#toObject
     */

    // public static T getParameter<T>(ServletRequest pReq, String pName,
    //                                 String pFormat, T pDefault) {
    static <T> T getParameter(ServletRequest pReq, String pName, Class<T> pType, String pFormat, T pDefault) {
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
     * a boolean. If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to a boolean, or the
     *         default value, if the parameter is not set.
     */
    public static boolean getBooleanParameter(ServletRequest pReq, String pName, boolean pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return ((str != null) ? Boolean.valueOf(str) : pDefault);
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * an int.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to an int, or the default
     *         value, if the parameter is not set.
     */
    public static int getIntParameter(ServletRequest pReq, String pName, int pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return ((str != null) ? Integer.parseInt(str) : pDefault);
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * an long.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to an long, or the default
     *         value, if the parameter is not set.
     */
    public static long getLongParameter(ServletRequest pReq, String pName, long pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return ((str != null) ? Long.parseLong(str) : pDefault);
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a float.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to a float, or the default
     *         value, if the parameter is not set.
     */
    public static float getFloatParameter(ServletRequest pReq, String pName, float pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return ((str != null) ? Float.parseFloat(str) : pDefault);
        }
        catch (NumberFormatException nfe) {
            return pDefault;
        }
    }

    /**
     * Gets the value of the given parameter from the request converted to
     * a double.&nbsp;If the parameter is not set or not parseable, the default
     * value is returned.
     *
     * @param pReq     the servlet request
     * @param pName    the parameter name
     * @param pDefault the default value
     * @return the value of the parameter converted to n double, or the default
     *         value, if the parameter is not set.
     */
    public static double getDoubleParameter(ServletRequest pReq, String pName, double pDefault) {
        String str = pReq.getParameter(pName);

        try {
            return ((str != null) ? Double.parseDouble(str) : pDefault);
        }
        catch (NumberFormatException nfe) {
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
     * @param pDefault the default value
     * @return the value of the parameter converted to a Date, or the
     *         default value, if the parameter is not set.
     * @see com.twelvemonkeys.lang.StringUtil#toDate(String)
     */
    public static long getDateParameter(ServletRequest pReq, String pName, long pDefault) {
        String str = pReq.getParameter(pName);
        try {
            return ((str != null) ? StringUtil.toDate(str).getTime() : pDefault);
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
    static StringBuffer buildHTTPURL(HttpServletRequest pRequest) {
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
    public static String getIncludeRequestURI(ServletRequest pRequest) {
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
    public static String getIncludeContextPath(ServletRequest pRequest) {
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
    public static String getIncludeServletPath(ServletRequest pRequest) {
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
    public static String getIncludePathInfo(ServletRequest pRequest) {
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
    public static String getIncludeQueryString(ServletRequest pRequest) {
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
    public static String getForwardRequestURI(ServletRequest pRequest) {
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
    public static String getForwardContextPath(ServletRequest pRequest) {
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
    public static String getForwardServletPath(ServletRequest pRequest) {
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
    public static String getForwardPathInfo(ServletRequest pRequest) {
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
    public static String getForwardQueryString(ServletRequest pRequest) {
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
    static String getScriptName(HttpServletRequest pRequest) {
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
    public static String getContextRelativeURI(HttpServletRequest pRequest) {
        String context = pRequest.getContextPath();
        if (!StringUtil.isEmpty(context)) { // "" for root context
            return pRequest.getRequestURI().substring(context.length());
        }
        return pRequest.getRequestURI();
    }

    /**
     * Returns a {@code URL} containing the real path for a given virtual
     * path, on URL form.
     * Note that this mehtod will return {@code null} for all the same reasons
     * as {@code ServletContext.getRealPath(java.lang.String)} does.
     *
     * @param pContext the servlet context
     * @param pPath    the virtual path
     * @return a {@code URL} object containing the path, or {@code null}.
     * @throws MalformedURLException if the path refers to a malformed URL
     * @see ServletContext#getRealPath(java.lang.String)
     * @see ServletContext#getResource(java.lang.String)
     */
    public static URL getRealURL(ServletContext pContext, String pPath) throws MalformedURLException {
        String realPath = pContext.getRealPath(pPath);
        if (realPath != null) {
            // NOTE: First convert to URI, as of Java 6 File.toURL is deprecated
            return new File(realPath).toURI().toURL();
        }
        return null;
    }

    /**
     * Gets the temp directory for the given {@code ServletContext} (webapp).
     *
     * @param pContext the servlet context
     * @return the temp directory
     */
    public static File getTempDir(ServletContext pContext) {
        return (File) pContext.getAttribute("javax.servlet.context.tempdir");
    }

    /**
     * Gets the identificator string containing the unique identifier assigned
     * to this session.
     * The identifier is assigned by the servlet container and is implementation
     * dependent.
     *
     * @param pRequest The HTTP servlet request object.
     * @return the session Id
     */
    public static String getSessionId(HttpServletRequest pRequest) {
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
     * @param pConfig the serlvet configuration
     * @return a {@code Map} view of the config
     * @throws IllegalArgumentException if {@code pConfig} is {@code null}
     */
    public static Map<String, String> asMap(ServletConfig pConfig) {
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
    public static Map<String, String> asMap(FilterConfig pConfig) {
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
     * Creates an unmodifiable {@code Map} view of the given
     * {@code HttpServletRequest}s request parameters.
     *
     * @param pRequest the request
     * @return a {@code Map} view of the request parameters
     * @throws IllegalArgumentException if {@code pRequest} is {@code null}
     */
    public static Map<String, List<String>> parametersAsMap(final HttpServletRequest pRequest) {
        return new SerlvetParametersMapAdapter(pRequest);
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
        return new SerlvetHeadersMapAdapter(pRequest);
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


    /**
     * Prints the init parameters in a {@code javax.servlet.ServletConfig}
     * object to a {@code java.io.PrintStream}.
     * <p/>
     *
     * @param pServletConfig The Servlet Config object.
     * @param pPrintStream   The {@code java.io.PrintStream} for flushing
     *                       the results.
     */
    public static void printDebug(final ServletConfig pServletConfig, final PrintStream pPrintStream) {
        Enumeration parameterNames = pServletConfig.getInitParameterNames();

        while (parameterNames.hasMoreElements()) {
            String initParameterName = (String) parameterNames.nextElement();

            pPrintStream.println(initParameterName + ": " + pServletConfig.getInitParameter(initParameterName));
        }
    }

    /**
     * Prints the init parameters in a {@code javax.servlet.ServletConfig}
     * object to {@code System.out}.
     *
     * @param pServletConfig the Servlet Config object.
     */
    public static void printDebug(final ServletConfig pServletConfig) {
        printDebug(pServletConfig, System.out);
    }

    /**
     * Prints the init parameters in a {@code javax.servlet.ServletContext}
     * object to a {@code java.io.PrintStream}.
     *
     * @param pServletContext the Servlet Context object.
     * @param pPrintStream    the {@code java.io.PrintStream} for flushing the
     *                        results.
     */
    public static void printDebug(final ServletContext pServletContext, final PrintStream pPrintStream) {
        Enumeration parameterNames = pServletContext.getInitParameterNames();

        while (parameterNames.hasMoreElements()) {
            String initParameterName = (String) parameterNames.nextElement();

            pPrintStream.println(initParameterName + ": " + pServletContext.getInitParameter(initParameterName));
        }
    }

    /**
     * Prints the init parameters in a {@code javax.servlet.ServletContext}
     * object to {@code System.out}.
     *
     * @param pServletContext The Servlet Context object.
     */
    public static void printDebug(final ServletContext pServletContext) {
        printDebug(pServletContext, System.out);
    }

    /**
     * Prints an excerpt of the residing information in a
     * {@code javax.servlet.http.HttpServletRequest} object to a
     * {@code java.io.PrintStream}.
     *
     * @param pRequest     The HTTP servlet request object.
     * @param pPrintStream The {@code java.io.PrintStream} for flushing
     *                     the results.
     */
    public static void printDebug(final HttpServletRequest pRequest, final PrintStream pPrintStream) {
        String indentation = "   ";
        StringBuilder buffer = new StringBuilder();

        // Returns the name of the authentication scheme used to protect the
        // servlet, for example, "BASIC" or "SSL," or null if the servlet was
        // not protected.
        buffer.append(indentation);
        buffer.append("Authentication scheme: ");
        buffer.append(pRequest.getAuthType());
        buffer.append("\n");

        // Returns the portion of the request URI that indicates the context
        // of the request.
        buffer.append(indentation);
        buffer.append("Context path: ");
        buffer.append(pRequest.getContextPath());
        buffer.append("\n");

        // Returns an enumeration of all the header mNames this request contains.
        buffer.append(indentation);
        buffer.append("Header:");
        buffer.append("\n");
        Enumeration headerNames = pRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerElement = (String) headerNames.nextElement();

            buffer.append(indentation);
            buffer.append(indentation);
            buffer.append(headerElement);
            buffer.append(": ");
            buffer.append(pRequest.getHeader(headerElement));
            buffer.append("\n");
        }

        // Returns the name of the HTTP method with which this request was made,
        // for example, GET, POST, or PUT.
        buffer.append(indentation);
        buffer.append("HTTP method: ");
        buffer.append(pRequest.getMethod());
        buffer.append("\n");

        // Returns any extra path information associated with the URL the client
        // sent when it made this request.
        buffer.append(indentation);
        buffer.append("Extra path information from client: ");
        buffer.append(pRequest.getPathInfo());
        buffer.append("\n");

        // Returns any extra path information after the servlet name but before
        // the query string, and translates it to a real path.
        buffer.append(indentation);
        buffer.append("Extra translated path information from client: ");
        buffer.append(pRequest.getPathTranslated());
        buffer.append("\n");

        // Returns the login of the user making this request, if the user has
        // been authenticated, or null if the user has not been authenticated.
        buffer.append(indentation);
        String userInfo = pRequest.getRemoteUser();

        if (StringUtil.isEmpty(userInfo)) {
            buffer.append("User is not authenticated");
        }
        else {
            buffer.append("User logint: ");
            buffer.append(userInfo);
        }
        buffer.append("\n");

        // Returns the session ID specified by the client.
        buffer.append(indentation);
        buffer.append("Session ID from client: ");
        buffer.append(pRequest.getRequestedSessionId());
        buffer.append("\n");

        // Returns the server name.
        buffer.append(indentation);
        buffer.append("Server name: ");
        buffer.append(pRequest.getServerName());
        buffer.append("\n");

        // Returns the part of this request's URL from the protocol name up
        // to the query string in the first line of the HTTP request.
        buffer.append(indentation);
        buffer.append("Request URI: ").append(pRequest.getRequestURI());
        buffer.append("\n");

        // Returns the path info.
        buffer.append(indentation);
        buffer.append("Path information: ").append(pRequest.getPathInfo());
        buffer.append("\n");

        // Returns the part of this request's URL that calls the servlet.
        buffer.append(indentation);
        buffer.append("Servlet path: ").append(pRequest.getServletPath());
        buffer.append("\n");

        // Returns the query string that is contained in the request URL after
        // the path.
        buffer.append(indentation);
        buffer.append("Query string: ").append(pRequest.getQueryString());
        buffer.append("\n");

        // Returns an enumeration of all the parameters bound to this request.
        buffer.append(indentation);
        buffer.append("Parameters:");
        buffer.append("\n");
        Enumeration parameterNames = pRequest.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();

            buffer.append(indentation);
            buffer.append(indentation);
            buffer.append(parameterName);
            buffer.append(": ");
            buffer.append(pRequest.getParameter(parameterName));
            buffer.append("\n");
        }

        // Returns an enumeration of all the attribute objects bound to this
        // request.
        buffer.append(indentation);
        buffer.append("Attributes:");
        buffer.append("\n");
        Enumeration attributeNames = pRequest.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = (String) attributeNames.nextElement();

            buffer.append(indentation);
            buffer.append(indentation);
            buffer.append(attributeName);
            buffer.append(": ");
            buffer.append(pRequest.getAttribute(attributeName).toString());
            buffer.append("\n");
        }
        pPrintStream.println(buffer.toString());
    }

    /**
     * Prints an excerpt of the residing information in a
     * {@code javax.servlet.http.HttpServletRequest} object to
     * {@code System.out}.
     *
     * @param pRequest The HTTP servlet request object.
     */
    public static void printDebug(final HttpServletRequest pRequest) {
        printDebug(pRequest, System.out);
    }

    /**
     * Prints an excerpt of a {@code javax.servlet.http.HttpSession} object
     * to a {@code java.io.PrintStream}.
     *
     * @param pHttpSession The HTTP Session object.
     * @param pPrintStream The {@code java.io.PrintStream} for flushing
     *                     the results.
     */
    public static void printDebug(final HttpSession pHttpSession, final PrintStream pPrintStream) {
        String indentation = "   ";
        StringBuilder buffer = new StringBuilder();

        if (pHttpSession == null) {
            buffer.append(indentation);
            buffer.append("No session object available");
            buffer.append("\n");
        }
        else {

            // Returns a string containing the unique identifier assigned to
            //this session
            buffer.append(indentation);
            buffer.append("Session ID: ").append(pHttpSession.getId());
            buffer.append("\n");

            // Returns the last time the client sent a request associated with
            // this session, as the number of milliseconds since midnight
            // January 1, 1970 GMT, and marked by the time the container
            // recieved the request
            buffer.append(indentation);
            buffer.append("Last accessed time: ");
            buffer.append(new Date(pHttpSession.getLastAccessedTime()));
            buffer.append("\n");

            // Returns the time when this session was created, measured in
            // milliseconds since midnight January 1, 1970 GMT
            buffer.append(indentation);
            buffer.append("Creation time: ");
            buffer.append(new Date(pHttpSession.getCreationTime()));
            buffer.append("\n");

            // Returns true if the client does not yet know about the session
            // or if the client chooses not to join the session
            buffer.append(indentation);
            buffer.append("New session?: ");
            buffer.append(pHttpSession.isNew());
            buffer.append("\n");

            // Returns the maximum time interval, in seconds, that the servlet
            // container will keep this session open between client accesses
            buffer.append(indentation);
            buffer.append("Max inactive interval: ");
            buffer.append(pHttpSession.getMaxInactiveInterval());
            buffer.append("\n");

            // Returns an enumeration of all the attribute objects bound to
            // this session
            buffer.append(indentation);
            buffer.append("Attributes:");
            buffer.append("\n");
            Enumeration attributeNames = pHttpSession.getAttributeNames();

            while (attributeNames.hasMoreElements()) {
                String attributeName = (String) attributeNames.nextElement();

                buffer.append(indentation);
                buffer.append(indentation);
                buffer.append(attributeName);
                buffer.append(": ");
                buffer.append(pHttpSession.getAttribute(attributeName).toString());
                buffer.append("\n");
            }
        }
        pPrintStream.println(buffer.toString());
    }

    /**
     * Prints an excerpt of a {@code javax.servlet.http.HttpSession}
     * object to {@code System.out}.
     * <p/>
     *
     * @param pHttpSession The HTTP Session object.
     */
    public static void printDebug(final HttpSession pHttpSession) {
        printDebug(pHttpSession, System.out);
    }

    private static class HttpServletResponseHandler implements InvocationHandler {
        private ServletResponse mResponse;
        private HttpServletResponse mHttpResponse;

        HttpServletResponseHandler(ServletResponseWrapper pResponse) {
            mResponse = pResponse;
            mHttpResponse = (HttpServletResponse) pResponse.getResponse();
        }

        public Object invoke(Object pProxy, Method pMethod, Object[] pArgs) throws Throwable {
            try {
                if (pMethod.getDeclaringClass().isInstance(mResponse)) {
                    //System.out.println("Invoking " + pMethod + " on wrapper");
                    return pMethod.invoke(mResponse, pArgs);
                }
                // Method is not implemented in wrapper
                //System.out.println("Invoking " + pMethod + " on wrapped object");
                return pMethod.invoke(mHttpResponse, pArgs);
            }
            catch (InvocationTargetException e) {
                // Unwrap, to avoid UndeclaredThrowableException...
                throw e.getTargetException();
            }
        }
    }

    private static class HttpServletRequestHandler implements InvocationHandler {
        private ServletRequest mRequest;
        private HttpServletRequest mHttpRequest;

        HttpServletRequestHandler(ServletRequestWrapper pRequest) {
            mRequest = pRequest;
            mHttpRequest = (HttpServletRequest) pRequest.getRequest();
        }

        public Object invoke(Object pProxy, Method pMethod, Object[] pArgs) throws Throwable {
            try {
                if (pMethod.getDeclaringClass().isInstance(mRequest)) {
                    //System.out.println("Invoking " + pMethod + " on wrapper");
                    return pMethod.invoke(mRequest, pArgs);
                }
                // Method is not implemented in wrapper
                //System.out.println("Invoking " + pMethod + " on wrapped object");
                return pMethod.invoke(mHttpRequest, pArgs);
            }
            catch (InvocationTargetException e) {
                // Unwrap, to avoid UndeclaredThrowableException...
                throw e.getTargetException();
            }
        }
    }
}

