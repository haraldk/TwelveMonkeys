/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: ValueOfTag.java,v $
 * Revision 1.2  2003/10/06 14:26:14  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.1  2002/10/18 14:03:52  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet.taglib
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet.taglib;

import java.io.*;

import javax.servlet.*;
import javax.servlet.jsp.*;

import com.twelvemonkeys.servlet.jsp.droplet.*;
import com.twelvemonkeys.servlet.jsp.taglib.*;

/**
 * ValueOf tag that emulates ATG Dynamo JHTML behaviour for JSP.
 *
 * @author Thomas Purcell (CSC Australia)
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 */
public class ValueOfTag extends ExTagSupport {

    /**
     * This is the name of the parameter whose value is to be inserted into
     * the current JSP page.  This value will be set via the {@code name}
     * attribute.
     */
    private String mParameterName;

    /**
     * This is the value of the parameter read from the {@code
     * PageContext.REQUEST_SCOPE} scope.  If the parameter doesn't exist,
     * then this will be null.
     */
    private Object mParameterValue;

    /**
     * This method is called as part of the initialisation phase of the tag
     * life cycle.  It sets the parameter name to be read from the {@code
     * PageContext.REQUEST_SCOPE} scope.
     *
     * @param pName The name of the parameter to be read from the {@code
     *     PageContext.REQUEST_SCOPE} scope.
     */
    public void setName(String pName) {
        mParameterName = pName;
    }

    /**
     * This method is called as part of the initialisation phase of the tag
     * life cycle.  It sets the parameter name to be read from the {@code
     * PageContext.REQUEST_SCOPE} scope. This is just a synonym for
     * setName, to be more like ATG Dynamo.
     *
     * @param pName The name of the parameter to be read from the {@code
     *     PageContext.REQUEST_SCOPE} scope.
     */
    public void setParam(String pName) {
        mParameterName = pName;
    }

    /**
     * This method looks in the session scope for the session-scoped attribute
     * whose name matches the {@code name} tag attribute for this tag.
     * If it finds it, then it replaces this tag with the value for the
     * session-scoped attribute.  If it fails to find the session-scoped
     * attribute, it displays the body for this tag.
     *
     * @return If the session-scoped attribute is found, then this method will
     *     return {@code TagSupport.SKIP_BODY}, otherwise it will return
     *     {@code TagSupport.EVAL_BODY_INCLUDE}.
     * @exception JspException
     *
     */
    public int doStartTag() throws JspException {
        try {
            if (parameterExists()) {
                if (mParameterValue instanceof JspFragment) {
                    // OPARAM or PARAM
                    ((JspFragment) mParameterValue).service(pageContext);
                    /*
                    log("Service subpage " + pageContext.getServletContext().getRealPath(((Oparam) mParameterValue).getName()));

                    pageContext.include(((Oparam) mParameterValue).getName());
                    */
                }
                else {
                    // Normal JSP parameter value
                    JspWriter writer = pageContext.getOut();
                    writer.print(mParameterValue);
                }

                return SKIP_BODY;
            }
            else {
                return EVAL_BODY_INCLUDE;
            }
        }
        catch (ServletException se) {
            log(se.getMessage(), se);
            throw new JspException(se);
        }
        catch (IOException ioe) {
            String msg = "Caught an IOException in ValueOfTag.doStartTag()\n"
                    + ioe.toString();
            log(msg, ioe);
            throw new JspException(msg);
        }
    }

    /**
     * This method is used to determine whether the parameter whose name is
     * stored in {@code mParameterName} exists within the {@code
     * PageContext.REQUEST_SCOPE} scope.  If the parameter does exist,
     * then this method will return {@code true}, otherwise it returns
     * {@code false}.  This method has the side affect of loading the
     * parameter value into {@code mParameterValue} if the parameter
     * does exist.
     *
     * @return {@code true} if the parameter whose name is in {@code
     *     mParameterName} exists in the {@code PageContext.REQUEST_SCOPE
     *     } scope, {@code false} otherwise.
     */
    private boolean parameterExists() {
        mParameterValue = pageContext.getAttribute(mParameterName, PageContext.REQUEST_SCOPE);

        // -- Harald K 20020726
        if (mParameterValue == null) {
            mParameterValue = pageContext.getRequest().getParameter(mParameterName);
        }

        return (mParameterValue != null);
    }
}
