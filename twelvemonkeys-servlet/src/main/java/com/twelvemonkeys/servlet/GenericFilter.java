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

import com.twelvemonkeys.lang.BeanUtil;

import javax.servlet.*;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

/**
 * Defines a generic, protocol-independent filter.
 * <P/>
 * {@code GenericFilter} is inspired by {@link GenericServlet}, and
 * implements the {@code Filter} and {@code FilterConfig} interfaces.
 * <P/>
 * {@code GenericFilter} makes writing filters easier. It provides simple
 * versions of the lifecycle methods {@code init} and {@code destroy}
 * and of the methods in the {@code FilterConfig} interface.
 * {@code GenericFilter} also implements the {@code log} methods,
 * declared in the {@code ServletContext} interface.
 * <p/
 * {@code GenericFilter} has an auto-init system, that automatically invokes
 * the method matching the signature {@code void setX(&lt;Type&gt;)},
 * for every init-parameter {@code x}. Both camelCase and lisp-style paramter
 * naming is supported, lisp-style names will be converted to camelCase.
 * Parameter values are automatically converted from string represenation to
 * most basic types, if neccessary.
 * <p/>
 * To write a generic filter, you need only override the abstract
 * {@link #doFilterImpl doFilterImpl} method.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/GenericFilter.java#1 $
 *
 * @see Filter
 * @see FilterConfig
 */
public abstract class GenericFilter implements Filter, FilterConfig, Serializable {

    /**
     * The filter config.
     */
    private transient FilterConfig mFilterConfig = null;

    /**
     * Makes sure the filter runs once per request
     * <p/>
     * see #isRunOnce
     *
     * @see #mOncePerRequest
     *      see #ATTRIB_RUN_ONCE_VALUE
     */
    private final static String ATTRIB_RUN_ONCE_EXT = ".REQUEST_HANDLED";

    /**
     * Makes sure the filter runs once per request.
     * Must be configured through init method, as the filter name is not
     * available before we have a FitlerConfig object.
     * <p/>
     * see #isRunOnce
     *
     * @see #mOncePerRequest
     *      see #ATTRIB_RUN_ONCE_VALUE
     */
    private String mAttribRunOnce = null;

    /**
     * Makes sure the filter runs once per request
     * <p/>
     * see #isRunOnce
     *
     * @see #mOncePerRequest
     *      see #ATTRIB_RUN_ONCE_EXT
     */
    private static final Object ATTRIB_RUN_ONCE_VALUE = new Object();

    /**
     * Indicates if this filter should run once per request ({@code true}),
     * or for each forward/include resource ({@code false}).
     * <p/>
     * Set this variable to true, to make sure the filter runs once per request.
     *
     * <em>NOTE: As of Servlet 2.4, this field
     * should always be left to it's default value ({@code false}).
     * <br/>
     * To run the filter once per request, the {@code filter-mapping} element
     * of the web-descriptor should include a {@code dispatcher} element:
     * <pre>&lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;</pre>
     * </em>
     */
    protected boolean mOncePerRequest = false;

    /**
     * Does nothing.
     */
    public GenericFilter() {}

    /**
     * Called by the web container to indicate to a filter that it is being
     * placed into service.
     * <p/>
     * This implementation stores the {@code FilterConfig} object it
     * receives from the servlet container for later use.
     * Generally, there's no reason to override this method, override the
     * no-argument {@code init} instead. However, <em>if</em> you are
     * overriding this form of the method,
     * always call {@code super.init(config)}.
     * <p/>
     * This implementation will also set all configured key/value pairs, that
     * have a matching setter method annotated with {@link InitParam}.
     *
     * @param pConfig the filter config
     * @throws ServletException if an error occurs during init
     *
     * @see Filter#init
     * @see #init() init
     * @see BeanUtil#configure(Object, java.util.Map, boolean)
     */
    public void init(FilterConfig pConfig) throws ServletException {
        if (pConfig == null) {
            throw new ServletConfigException("filterconfig == null");
        }

        // Store filterconfig
        mFilterConfig = pConfig;

        // Configure this
        try {
            BeanUtil.configure(this, ServletUtil.asMap(pConfig), true);
        }
        catch (InvocationTargetException e) {
            throw new ServletConfigException("Could not configure " + getFilterName(), e.getCause());
        }

        // Create run-once attribute name
        mAttribRunOnce = pConfig.getFilterName() + ATTRIB_RUN_ONCE_EXT;
        log("init (oncePerRequest=" + mOncePerRequest + ", attribRunOnce=" + mAttribRunOnce + ")");
        init();
    }

    /**
     * A convenience method which can be overridden so that there's no need to
     * call {@code super.init(config)}.
     *
     * @see #init(FilterConfig)
     *
     * @throws ServletException if an error occurs during init
     */
    public void init() throws ServletException {}

    /**
     * The {@code doFilter} method of the Filter is called by the container
     * each time a request/response pair is passed through the chain due to a
     * client request for a resource at the end of the chain.
     * <p/>
     * Subclasses <em>should not override this method</em>, but rather the
     * abstract {@link #doFilterImpl doFilterImpl} method.
     *
     * @param pRequest the servlet request
     * @param pResponse the servlet response
     * @param pFilterChain the filter chain
     *
     * @throws IOException
     * @throws ServletException
     *
     * @see Filter#doFilter Filter.doFilter
     * @see #doFilterImpl doFilterImpl
     */
    public final void doFilter(ServletRequest pRequest, ServletResponse pResponse, FilterChain pFilterChain) throws IOException, ServletException {
        // If request filter and allready run, continue chain and return fast
        if (mOncePerRequest && isRunOnce(pRequest)) {
            pFilterChain.doFilter(pRequest, pResponse);
            return;
        }

        // Do real filter
        doFilterImpl(pRequest, pResponse, pFilterChain);
    }

    /**
     * If request is filtered, returns true, otherwise marks request as filtered
     * and returns false.
     * A return value of false, indicates that the filter has not yet run.
     * A return value of true, indicates that the filter has run for this
     * request, and processing should not contine.
     * <P/>
     * Note that the method will mark the request as filtered on first
     * invocation.
     * <p/>
     * see #ATTRIB_RUN_ONCE_EXT
     * see #ATTRIB_RUN_ONCE_VALUE
     *
     * @param pRequest the servlet request
     * @return {@code true} if the request is allready filtered, otherwise
     *         {@code false}.
     */
    private boolean isRunOnce(ServletRequest pRequest) {
        // If request allready filtered, return true (skip)
        if (pRequest.getAttribute(mAttribRunOnce) == ATTRIB_RUN_ONCE_VALUE) {
            return true;
        }

        // Set attribute and return false (continue)
        pRequest.setAttribute(mAttribRunOnce, ATTRIB_RUN_ONCE_VALUE);
        return false;
    }

    /**
     * Invoked once, or each time a request/response pair is passed through the
     * chain, depending on the {@link #mOncePerRequest} member variable.
     *
     * @param pRequest the servlet request
     * @param pResponse the servlet response
     * @param pChain the filter chain
     *
     * @throws IOException if an I/O error occurs
     * @throws ServletException if an exception occurs during the filter process
     *
     * @see #mOncePerRequest
     * @see #doFilter doFilter
     * @see Filter#doFilter Filter.doFilter
     */
    protected abstract void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain)
            throws IOException, ServletException;

    /**
     * Called by the web container to indicate to a filter that it is being
     * taken out of service.
     *
     * @see Filter#destroy
     */
    public void destroy() {
        log("destroy");
        mFilterConfig = null;
    }

    /**
     * Returns the filter-name of this filter as defined in the deployment
     * descriptor.
     *
     * @return the filter-name
     * @see FilterConfig#getFilterName
     */
    public String getFilterName() {
        return mFilterConfig.getFilterName();
    }

    /**
     * Returns a reference to the {@link ServletContext} in which the caller is
     * executing.
     *
     * @return the {@code ServletContext} object, used by the caller to
     *         interact with its servlet container
     * @see FilterConfig#getServletContext
     * @see ServletContext
     */
    public ServletContext getServletContext() {
        // TODO: Create a servlet context wrapper that lets you log to a log4j appender?
        return mFilterConfig.getServletContext();
    }

    /**
     * Returns a {@code String} containing the value of the named
     * initialization parameter, or null if the parameter does not exist.
     *
     * @param pKey a {@code String} specifying the name of the
     *             initialization parameter
     * @return a {@code String} containing the value of the initialization
     *         parameter
     */
    public String getInitParameter(String pKey) {
        return mFilterConfig.getInitParameter(pKey);
    }

    /**
     * Returns the names of the servlet's initialization parameters as an
     * {@code Enumeration} of {@code String} objects, or an empty
     * {@code Enumeration} if the servlet has no initialization parameters.
     *
     * @return an {@code Enumeration} of {@code String} objects
     *         containing the mNames of the servlet's initialization parameters
     */
    public Enumeration getInitParameterNames() {
        return mFilterConfig.getInitParameterNames();
    }

    /**
     * Writes the specified message to a servlet log file, prepended by the
     * filter's name.
     *
     * @param pMessage the log message
     * @see ServletContext#log(String)
     */
    protected void log(String pMessage) {
        getServletContext().log(getFilterName() + ": " + pMessage);
    }

    /**
     * Writes an explanatory message and a stack trace for a given
     * {@code Throwable} to the servlet log file, prepended by the
     * filter's name.
     *
     * @param pMessage the log message
     * @param pThrowable the exception
     * @see ServletContext#log(String,Throwable)
     */
    protected void log(String pMessage, Throwable pThrowable) {
        getServletContext().log(getFilterName() + ": " + pMessage, pThrowable);
    }

    /**
     * Initializes the filter.
     *
     * @param pFilterConfig the filter config
     * @see #init init
     *
     * @deprecated For compatibility only, use {@link #init init} instead.
     */
    public void setFilterConfig(FilterConfig pFilterConfig) {
        try {
            init(pFilterConfig);
        }
        catch (ServletException e) {
            log("Error in init(), see stacktrace for details.", e);
        }
    }

    /**
     * Gets the {@code FilterConfig} for this filter.
     *
     * @return the {@code FilterConfig} for this filter
     * @see FilterConfig
     */
    public FilterConfig getFilterConfig() {
        return mFilterConfig;
    }

    /**
     * Specifies if this filter should run once per request ({@code true}),
     * or for each forward/include resource ({@code false}).
     * Called automatically from the {@code init}-method, with settings
     * from web.xml.
     *
     * @param pOncePerRequest {@code true} if the filter should run only
     *        once per request
     * @see #mOncePerRequest
     */
    @InitParam
    public void setOncePerRequest(boolean pOncePerRequest) {
        mOncePerRequest = pOncePerRequest;
    }
}
