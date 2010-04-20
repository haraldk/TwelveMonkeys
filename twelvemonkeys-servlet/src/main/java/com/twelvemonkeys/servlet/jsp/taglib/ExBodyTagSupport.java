/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: ExBodyTagSupport.java,v $
 * Revision 1.3  2003/10/06 14:24:57  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.2  2002/11/18 22:10:27  WMHAKUR
 * *** empty log message ***
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.taglib;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * This is the class that should be extended by all jsp pages that do use their
 * body. It contains a lot of helper methods for simplifying common tasks.
 *
 * @author Thomas Purcell (CSC Australia)
 * @author Harald Kuhr
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/jsp/taglib/ExBodyTagSupport.java#1 $
 */

public class ExBodyTagSupport extends BodyTagSupport implements ExTag {
    /**
     * writeHtml ensures that the text being outputted appears as it was
     * entered.  This prevents users from hacking the system by entering
     * html or jsp code into an entry form where that value will be displayed
     * later in the site.
     *
     * @param pOut The JspWriter to write the output to.
     * @param pHtml The original html to filter and output to the user.
     * @throws IOException If the user clicks Stop in the browser, or their
     *     browser crashes, then the JspWriter will throw an IOException when
     *     the jsp tries to write to it.
     */

    public void writeHtml(JspWriter pOut, String pHtml) throws IOException {
        StringTokenizer parser = new StringTokenizer(pHtml, "<>&", true);

        while (parser.hasMoreTokens()) {
            String token = parser.nextToken();

            if (token.equals("<")) {
                pOut.print("&lt;");
            }
            else if (token.equals(">")) {
                pOut.print("&gt;");
            }
            else if (token.equals("&")) {
                pOut.print("&amp;");
            }
            else {
                pOut.print(token);
            }
        }
    }

    /**
     * Log a message to the servlet context.
     *
     * @param pMsg The error message to log.
     */

    public void log(String pMsg) {
        getServletContext().log(pMsg);
    }

    /**
     * Log a message to the servlet context and include the exception that is
     * passed in as the second parameter.
     *
     * @param pMsg The error message to log.
     * @param pException The exception that caused this error message to be
     *     logged.
     */

    public void log(String pMsg, Throwable pException) {
        getServletContext().log(pMsg, pException);
    }

    /**
     * Retrieves the ServletContext object associated with the current
     * PageContext object.
     *
     * @return The ServletContext object associated with the current
     *     PageContext object.
     */

    public ServletContext getServletContext() {
        return pageContext.getServletContext();
    }

    /**
     * Called when the tag has finished running.  Any clean up that needs
     * to be done between calls to this tag but within the same JSP page is
     * called in the {@code clearServiceState()} method call.
     *
     * @exception JspException
     */

    public int doEndTag() throws JspException {
        clearServiceState();
        return super.doEndTag();
    }

    /**
     * Called when a tag's role in the current JSP page is finished.  After
     * the {@code clearProperties()} method is called, the custom tag
     * should be in an identical state as when it was first created.  The
     * {@code clearServiceState()} method is called here just in case an
     * exception was thrown in the custom tag.  If an exception was thrown,
     * then the {@code doEndTag()} method will not have been called and
     * the tag might not have been cleaned up properly.
     */

    public void release() {
        clearServiceState();

        clearProperties();
        super.release();
    }

    /**
     * The default implementation for the {@code clearProperties()}.  Not
     * all tags will need to overload this method call.  By implementing it
     * here, all classes that extend this object are able to call {@code
     * super.clearProperties()}.  So, if the class extends a different
     * tag, or this one, the parent method should always be called.  This
     * method will be called when the tag is to be released.  That is, the
     * tag has finished for the current page and should be returned to it's
     * initial state.
     */

    protected void clearProperties() {
    }

    /**
     * The default implementation for the {@code clearServiceState()}.
     * Not all tags will need to overload this method call.  By implementing it
     * here, all classes that extend this object are able to call {@code
     * super.clearServiceState()}.  So, if the class extends a different
     * tag, or this one, the parent method should always be called.  This
     * method will be called when the tag has finished it's current tag
     * within the page, but may be called upon again in this same JSP page.
     */

    protected void clearServiceState() {
    }

    /**
     * Returns the initialisation parameter from the {@code
     * PageContext.APPLICATION_SCOPE} scope.  These initialisation
     * parameters are defined in the {@code web.xml} configuration file.
     *
     * @param pName The name of the initialisation parameter to return the
     *     value for.
     * @return The value for the parameter whose name was passed in as a
     *     parameter.  If the parameter does not exist, then {@code null}
     *     will be returned.
     */

    public String getInitParameter(String pName) {
        return getInitParameter(pName, PageContext.APPLICATION_SCOPE);
    }

    /**
     * Returns an Enumeration containing all the names for all the
     * initialisation parametes defined in the {@code
     * PageContext.APPLICATION_SCOPE} scope.
     *
     * @return An {@code Enumeration} containing all the names for all the
     *     initialisation parameters.
     */

    public Enumeration getInitParameterNames() {
        return getInitParameterNames(PageContext.APPLICATION_SCOPE);
    }

    /**
     * Returns the initialisation parameter from the scope specified with the
     * name specified.
     *
     * @param pName The name of the initialisation parameter to return the
     *     value for.
     * @param pScope The scope to search for the initialisation parameter
     *     within.
     * @return The value of the parameter found.  If no parameter with the
     *     name specified is found in the scope specified, then {@code null
     *     } is returned.
     */

    public String getInitParameter(String pName, int pScope) {
        switch (pScope) {
            case PageContext.PAGE_SCOPE:
                return getServletConfig().getInitParameter(pName);
            case PageContext.APPLICATION_SCOPE:
                return getServletContext().getInitParameter(pName);
            default:
                throw new IllegalArgumentException("Illegal scope.");
        }
    }

    /**
     * Returns an enumeration containing all the parameters defined in the
     * scope specified by the parameter.
     *
     * @param pScope The scope to return the names of all the parameters
     *     defined within.
     * @return An {@code Enumeration} containing all the names for all the
     *     parameters defined in the scope passed in as a parameter.
     */

    public Enumeration getInitParameterNames(int pScope) {
        switch (pScope) {
            case PageContext.PAGE_SCOPE:
                return getServletConfig().getInitParameterNames();
            case PageContext.APPLICATION_SCOPE:
                return getServletContext().getInitParameterNames();
            default:
                throw new IllegalArgumentException("Illegal scope");
        }
    }

    /**
     * Returns the servlet config associated with the current JSP page request.
     *
     * @return The {@code ServletConfig} associated with the current
     *     request.
     */

    public ServletConfig getServletConfig() {
        return pageContext.getServletConfig();
    }

    /**
     * Gets the context path associated with the current JSP page request.
     * If the request is not a HttpServletRequest, this method will
     * return "/".
     *
     * @return a path relative to the current context's  root, or
     *         {@code "/"} if this is not a HTTP request.
     */

    public String getContextPath() {
        ServletRequest request = pageContext.getRequest();
        if (request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getContextPath();
        }
        return "/";
    }

    /**
     * Gets the resource associated with the given relative path for the
     * current JSP page request.
     * The path may be absolute, or relative to the current context root.
     *
     * @param pPath the path
     *
     * @return a path relative to the current context root
     */

    public InputStream getResourceAsStream(String pPath) {
        //        throws MalformedURLException {
        String path = pPath;

        if (pPath != null && !pPath.startsWith("/")) {
            path = getContextPath() + pPath;
        }

        return pageContext.getServletContext().getResourceAsStream(path);
    }

}
