/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: ParamTag.java,v $
 * Revision 1.2  2003/10/06 14:26:00  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.1  2002/10/18 14:03:09  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet.taglib
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet.taglib;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.twelvemonkeys.servlet.jsp.droplet.*;
import com.twelvemonkeys.servlet.jsp.taglib.*;

/**
 * Parameter tag that emulates ATG Dynamo JHTML behaviour for JSP.
 *
 * @author Thomas Purcell (CSC Australia)
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 *
 */

public class ParamTag extends ExTagSupport {

    /**
     * This is the name of the parameter to be inserted into the {@code
     * PageContext.REQUEST_SCOPE} scope.
     */

    private String mParameterName;

    /**
     * This is the value for the parameter to be inserted into the {@code
     * PageContext.REQUEST_SCOPE} scope.
     */

    private Object mParameterValue;

    /**
     * This method allows the JSP page to set the name for the parameter by
     * using the {@code name} tag attribute.
     *
     * @param pName The name for the parameter to insert into the {@code
     *     PageContext.REQUEST_SCOPE} scope.
     */

    public void setName(String pName) {
        mParameterName = pName;
    }

    /**
     * This method allows the JSP page to set the value for hte parameter by
     * using the {@code value} tag attribute.
     *
     * @param pValue The value for the parameter to insert into the <code>
     *     PageContext.REQUEST_SCOPE</page> scope.
     */

    public void setValue(String pValue) {
        mParameterValue = new Param(pValue);
    }

    /**
     * Ensure that the tag implemented by this class is enclosed by an {@code
     * IncludeTag}.  If the tag is not enclosed by an
     * {@code IncludeTag} then a {@code JspException} is thrown.
     *
     * @return If this tag is enclosed within an {@code IncludeTag}, then
     *     the default return value from this method is the {@code
     *     TagSupport.SKIP_BODY} value.
     * @exception JspException
     */

    public int doStartTag() throws JspException {
        //checkEnclosedInIncludeTag();

        addParameter();

        return SKIP_BODY;
    }

    /**
     * This is the method responsible for actually testing that the tag
     * implemented by this class is enclosed within an {@code IncludeTag}.
     *
     * @exception JspException
     */
    /*
    protected void checkEnclosedInIncludeTag() throws JspException {
        Tag parentTag = getParent();

        if ((parentTag != null) && (parentTag instanceof IncludeTag)) {
            return;
        }

        String msg = "A class that extends EnclosedIncludeBodyReaderTag " +
                     "is not enclosed within an IncludeTag.";
        log(msg);
        throw new JspException(msg);
    }
    */

    /**
     * This method adds the parameter whose name and value were passed to this
     * object via the tag attributes to the parent {@code Include} tag.
     */

    private void addParameter() {
        IncludeTag includeTag = (IncludeTag) getParent();

        includeTag.addParameter(mParameterName, mParameterValue);
    }

    /**
     * This method cleans up the member variables for this tag in preparation
     * for being used again.  This method is called when the tag finishes it's
     * current call with in the page but could be called upon again within this
     * same page.  This method is also called in the release stage of the tag
     * life cycle just in case a JspException was thrown during the tag
     * execution.
     */

    protected void clearServiceState() {
        mParameterName = null;
        mParameterValue = null;
    }
}
