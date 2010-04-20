/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: OparamTag.java,v $
 * Revision 1.4  2003/10/06 14:25:53  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.3  2002/11/18 14:12:43  WMHAKUR
 * *** empty log message ***
 *
 * Revision 1.2  2002/11/07 12:20:14  WMHAKUR
 * Updated to reflect changes in com.twelvemonkeys.util.*Util
 *
 * Revision 1.1  2002/10/18 14:03:09  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet.taglib
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet.taglib;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.servlet.jsp.droplet.Oparam;
import com.twelvemonkeys.servlet.jsp.taglib.BodyReaderTag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;
import java.io.File;
import java.io.IOException;


/**
 * Open parameter tag that emulates ATG Dynamo JHTML behaviour for JSP.
 *
 * @author Thomas Purcell (CSC Australia)
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/jsp/droplet/taglib/OparamTag.java#1 $
 */

public class OparamTag extends BodyReaderTag {

    protected final static String COUNTER = "com.twelvemonkeys.servlet.jsp.taglib.OparamTag.counter";


    private File mSubpage = null;

    /**
     * This is the name of the parameter to be inserted into the {@code
     * PageContext.REQUEST_SCOPE} scope.
     */

    private String mParameterName = null;

    private String mLanguage = null;

    private String mPrefix = null;

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

    public void setLanguage(String pLanguage) {
        //System.out.println("setLanguage:"+pLanguage);
        mLanguage = pLanguage;
    }

    public void setPrefix(String pPrefix) {
        //System.out.println("setPrefix:"+pPrefix);
        mPrefix = pPrefix;
    }

    /**
     * Ensure that the tag implemented by this class is enclosed by an {@code
     * IncludeTag}.  If the tag is not enclosed by an
     * {@code IncludeTag} then a {@code JspException} is thrown.
     *
     * @return If this tag is enclosed within an {@code IncludeTag}, then
     *     the default return value from this method is the {@code
     *     TagSupport.EVAL_BODY_TAG} value.
     * @exception JspException
     */

    public int doStartTag() throws JspException {
        //checkEnclosedInIncludeTag(); // Moved to TagLibValidator

        // Get request
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        // Get filename
        mSubpage = createFileNameFromRequest(request);

        // Get include tag, and add to parameters
        IncludeTag includeTag = (IncludeTag) getParent();
        includeTag.addParameter(mParameterName, new Oparam(mSubpage.getName()));

        // if ! subpage.exist || jsp newer than subpage, write new
        File jsp = new File(pageContext.getServletContext()
                            .getRealPath(request.getServletPath()));

        if (!mSubpage.exists() || jsp.lastModified() > mSubpage.lastModified()) {
            return BodyTag.EVAL_BODY_BUFFERED;
        }

        // No need to evaluate body again!
        return Tag.SKIP_BODY;
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
     * This method cleans up the member variables for this tag in preparation
     * for being used again.  This method is called when the tag finishes it's
     * current call with in the page but could be called upon again within this
     * same page.  This method is also called in the release stage of the tag
     * life cycle just in case a JspException was thrown during the tag
     * execution.
     */

    protected void clearServiceState() {
        mParameterName = null;
    }

    /**
     * This is the method responsible for taking the result of the JSP code
     * that forms the body of this tag and inserts it as a parameter into the
     * request scope session.  If any problems occur while loading the body
     * into the session scope then a {@code JspException} will be thrown.
     *
     * @param pContent The body of the tag as a String.
     *
     * @exception JspException
     */

    protected void processBody(String pContent) throws JspException {
        // Okay, we have the content, we need to write it to disk somewhere
        String content = pContent;

        if (!StringUtil.isEmpty(mLanguage)) {
            content = "<%@page language=\"" + mLanguage + "\" %>" + content;
        }

        if (!StringUtil.isEmpty(mPrefix)) {
            content = "<%@taglib uri=\"/twelvemonkeys-common\" prefix=\"" + mPrefix + "\" %>" + content;
        }

        // Write the content of the oparam to disk
        try {
            log("Processing subpage " + mSubpage.getPath());
            FileUtil.write(mSubpage, content.getBytes());

        }
        catch (IOException ioe) {
            throw new JspException(ioe);
        }
    }

    /**
     * Creates a unique filename for each (nested) oparam
     */
    private File createFileNameFromRequest(HttpServletRequest pRequest) {
        //System.out.println("ServletPath" + pRequest.getServletPath());
        String path = pRequest.getServletPath();

        // Find last '/'
        int splitIndex = path.lastIndexOf("/");

        // Split -> path + name
        String name = path.substring(splitIndex + 1);
        path = path.substring(0, splitIndex);

        // Replace special chars in name with '_'
        name = name.replace('.', '_');
        String param = mParameterName.replace('.', '_');
        param = param.replace('/', '_');
        param = param.replace('\\', '_');
        param = param.replace(':', '_');

        // tempfile = realPath(path) + name + "_oparam_" + number + ".jsp"
        int count = getOparamCountFromRequest(pRequest);

        // Hmm.. Would be great, but seems like I can't serve pages from within the temp dir
        //File temp = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
        //return new File(new File(temp, path), name + "_oparam_" + count + "_" + param + ".jsp");

        return new File(new File(pageContext.getServletContext().getRealPath(path)), name + "_oparam_" + count + "_" + param + ".jsp");
    }

    /**
     * Gets the current oparam count for this request
     */
    private int getOparamCountFromRequest(HttpServletRequest pRequest) {
        // Use request.attribute for incrementing oparam counter
        Integer count = (Integer) pRequest.getAttribute(COUNTER);
        if (count == null)
            count = new Integer(0);
        else
            count = new Integer(count.intValue() + 1);

        // ... and set it back
        pRequest.setAttribute(COUNTER, count);

        return count.intValue();
    }

}
