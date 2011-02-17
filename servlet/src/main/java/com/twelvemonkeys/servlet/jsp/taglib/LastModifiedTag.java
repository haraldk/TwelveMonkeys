
package com.twelvemonkeys.servlet.jsp.taglib;

import com.twelvemonkeys.util.convert.Converter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.File;
import java.util.Date;

/**
 * Prints the last modified
 */

public class LastModifiedTag extends TagSupport {
    private String fileName = null;
    private String format = null;

    public void setFile(String pFileName) {
        fileName = pFileName;
    }

    public void setFormat(String pFormat) {
        format = pFormat;
    }

    public int doStartTag() throws JspException {
        File file;

        if (fileName != null) {
            file = new File(pageContext.getServletContext().getRealPath(fileName));
        }
        else {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

            // Get the file containing the servlet
            file = new File(pageContext.getServletContext().getRealPath(request.getServletPath()));
        }

        Date lastModified = new Date(file.lastModified());
        Converter conv = Converter.getInstance();

        // Set the last modified value back
        pageContext.setAttribute("lastModified", conv.toString(lastModified, format));

        return Tag.EVAL_BODY_INCLUDE;
    }
}
