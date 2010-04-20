
package com.twelvemonkeys.servlet.jsp.taglib;

import javax.servlet.jsp.JspException;

/**
 *
 *
 * @author Thomas Purcell (CSC Australia)
 *
 * @version 1.0
 */

public abstract class BodyReaderTag extends ExBodyTagSupport {
    /**
     * This is the method called by the JSP engine when the body for a tag
     * has been parsed and is ready for inclusion in this current tag.  This
     * method takes the content as a string and passes it to the {@code
     * processBody} method.
     *
     * @return This method returns the {@code BodyTagSupport.SKIP_BODY}
     *     constant.  This means that the body of the tag will only be
     *     processed the one time.
     * @exception JspException
     */

    public int doAfterBody() throws JspException {
        processBody(bodyContent.getString());
        return SKIP_BODY;
    }

    /**
     * This is the method that child classes must implement.  It takes the
     * body of the tag converted to a String as it's parameter.  The body of
     * the tag will have been interpreted to a String by the JSP engine before
     * this method is called.
     *
     * @param pContent The body for the custom tag converted to a String.
     * @exception JscException
     */

    protected abstract void processBody(String pContent) throws JspException;
}
