/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: ExTag.java,v $
 * Revision 1.2  2003/10/06 14:25:05  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.1  2002/11/18 22:10:27  WMHAKUR
 * *** empty log message ***
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.taglib;


import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * This interface contains a lot of helper methods for simplifying common
 * taglib related tasks.
 *
 * @author Harald Kuhr
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/jsp/taglib/ExTag.java#1 $
 */

public interface ExTag extends Tag {

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

    public void writeHtml(JspWriter pOut, String pHtml) throws IOException;

    /**
     * Log a message to the servlet context.
     *
     * @param pMsg The error message to log.
     */

    public void log(String pMsg);

    /**
     * Logs a message to the servlet context and include the exception that is
     * passed in as the second parameter.
     *
     * @param pMsg The error message to log.
     * @param pException The exception that caused this error message to be
     *     logged.
     */

    public void log(String pMsg, Throwable pException);

    /**
     * Retrieves the ServletContext object associated with the current
     * PageContext object.
     *
     * @return The ServletContext object associated with the current
     *     PageContext object.
     */

    public ServletContext getServletContext();

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

    public String getInitParameter(String pName);

    /**
     * Returns an Enumeration containing all the names for all the
     * initialisation parametes defined in the {@code
     * PageContext.APPLICATION_SCOPE} scope.
     *
     * @return An {@code Enumeration} containing all the names for all the
     *     initialisation parameters.
     */

    public Enumeration getInitParameterNames();

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

    public String getInitParameter(String pName, int pScope);

    /**
     * Returns an enumeration containing all the parameters defined in the
     * scope specified by the parameter.
     *
     * @param pScope The scope to return the names of all the parameters
     *     defined within.
     * @return An {@code Enumeration} containing all the names for all the
     *     parameters defined in the scope passed in as a parameter.
     */

    public Enumeration getInitParameterNames(int pScope);

    /**
     * Returns the servlet config associated with the current JSP page request.
     *
     * @return The {@code ServletConfig} associated with the current
     *     request.
     */

    public ServletConfig getServletConfig();

    /**
     * Gets the context path associated with the current JSP page request.
     *
     * @return a path relative to the current context's  root.
     */

    public String getContextPath();


    /**
     * Gets the resource associated with the given relative path for the
     * current JSP page request.
     * The path may be absolute, or relative to the current context root.
     *
     * @param pPath the path
     *
     * @return a path relative to the current context root
     */

    public InputStream getResourceAsStream(String pPath);

}
