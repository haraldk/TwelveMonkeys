/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: IncludeTag.java,v $
 * Revision 1.2  2003/10/06 14:25:36  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.1  2002/10/18 14:03:09  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet.taglib
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet.taglib;

import com.twelvemonkeys.servlet.jsp.taglib.ExTagSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Include tag tag that emulates ATG Dynamo Droplet tag JHTML behaviour for
 * JSP.
 *
 * @author Thomas Purcell (CSC Australia)
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 *
 */
public class IncludeTag extends ExTagSupport {
    /**
     * This will contain the names of all the parameters that have been
     * added to the PageContext.REQUEST_SCOPE scope by this tag.
     */
    private ArrayList mParameterNames = null;

    /**
     * If any of the parameters we insert for this tag already exist, then
     * we back up the older parameter in this {@code HashMap} and
     * restore them when the tag is finished.
     */
    private HashMap mOldParameters = null;

    /**
     * This is the URL for the JSP page that the parameters contained in this
     * tag are to be inserted into.
     */
    private String mPage;

    /**
     * The name of the PageContext attribute
     */
    public final static String PAGE_CONTEXT = "com.twelvemonkeys.servlet.jsp.PageContext";

    /**
     * Sets the value for the JSP page to insert the parameters into.  This
     * will be set by the tag attribute within the original JSP page.
     *
     * @param pPage The URL for the JSP page to insert parameters into.
     */
    public void setPage(String pPage) {
        mPage = pPage;
    }

    /**
     * Adds a parameter to the {@code PageContext.REQUEST_SCOPE} scope.
     * If a parameter with the same name as {@code pName} already exists,
     * then the old parameter is first placed in the {@code OldParameters}
     * member variable.  When this tag is finished, the old value will be
     * restored.
     *
     * @param pName The name of the new parameter to be stored in the
     *     {@code PageContext.REQUEST_SCOPE} scope.
     * @param pValue The value for the parmeter to be stored in the {@code
     *     PageContext.REQUEST_SCOPE} scope.
     */
    public void addParameter(String pName, Object pValue) {
        // Check that we haven't already saved this parameter
        if (!mParameterNames.contains(pName)) {
            mParameterNames.add(pName);

            // Now check if this parameter already exists in the page.
            Object obj = getRequest().getAttribute(pName);
            if (obj != null) {
                mOldParameters.put(pName, obj);
            }
        }

        // Finally, insert the parameter in the request scope.
        getRequest().setAttribute(pName, pValue);
    }

    /**
     * This is the method called when the JSP interpreter first hits the tag
     * associated with this class.  This method will firstly determine whether
     * the page referenced by the {@code page} attribute exists.  If the
     * page doesn't exist, this method will throw a {@code JspException}.
     * If the page does exist, this method will hand control over to that JSP
     * page.
     *
     * @exception JspException
     */
    public int doStartTag() throws JspException {
        mOldParameters = new HashMap();
        mParameterNames = new ArrayList();

        return EVAL_BODY_INCLUDE;
    }

    /**
     * This method is called when the JSP page compiler hits the end tag.  By
     * now all the data should have been passed and parameters entered into
     * the {@code PageContext.REQUEST_SCOPE} scope.  This method includes
     * the JSP page whose URL is stored in the {@code mPage} member
     * variable.
     *
     * @exception JspException
     */
    public int doEndTag() throws JspException {
        String msg;

        try {
            Iterator iterator;
            String parameterName;

            // -- Harald K 20020726
            // Include the page, in place
            //getDispatcher().include(getRequest(), getResponse());
            addParameter(PAGE_CONTEXT, pageContext); // Will be cleared later
            pageContext.include(mPage);

            // Remove all the parameters that were added to the request scope
            // for this insert tag.
            iterator = mParameterNames.iterator();

            while (iterator.hasNext()) {
                parameterName = (String) iterator.next();

                getRequest().removeAttribute(parameterName);
            }

            iterator = mOldParameters.keySet().iterator();

            // Restore the parameters we temporarily replaced (if any).
            while (iterator.hasNext()) {
                parameterName = (String) iterator.next();

                getRequest().setAttribute(parameterName, mOldParameters.get(parameterName));
            }

            return super.doEndTag();
        }
        catch (IOException ioe) {
            msg = "Caught an IOException while including " + mPage
                    + "\n" + ioe.toString();
            log(msg, ioe);
            throw new JspException(msg);
        }
        catch (ServletException se) {
            msg = "Caught a ServletException while including " + mPage
                    + "\n" + se.toString();
            log(msg, se);
            throw new JspException(msg);
        }
    }

    /**
     * Free up the member variables that we've used throughout this tag.
     */
    protected void clearServiceState() {
        mOldParameters = null;
        mParameterNames = null;
    }

    /**
     * Returns the request dispatcher for the JSP page whose URL is stored in
     * the {@code mPage} member variable.
     *
     * @return The RequestDispatcher for the JSP page whose URL is stored in
     *     the {@code mPage} member variable.
     */
    /*
    private RequestDispatcher getDispatcher() {
        return getRequest().getRequestDispatcher(mPage);
    }
    */

    /**
     * Returns the HttpServletRequest object for the current user request.
     *
     * @return The HttpServletRequest object for the current user request.
     */
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) pageContext.getRequest();
    }

    /**
     * Returns the HttpServletResponse object for the current user request.
     *
     * @return The HttpServletResponse object for the current user request.
     */
    private HttpServletResponse getResponse() {
        return (HttpServletResponse) pageContext.getResponse();
    }
}
