
package com.twelvemonkeys.servlet.jsp.taglib;

import java.io.File;
import java.util.Date;

import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.twelvemonkeys.util.convert.*;

/**
 * Prints the last modified
 */

public class LastModifiedTag extends TagSupport {
    private String mFileName = null;
    private String mFormat = null;

    public void setFile(String pFileName) {
        mFileName = pFileName;
    }

    public void setFormat(String pFormat) {
        mFormat = pFormat;
    }

    public int doStartTag() throws JspException {
        File file = null;

        if (mFileName != null) {
            file = new File(pageContext.getServletContext()
                            .getRealPath(mFileName));
        }
        else {
            HttpServletRequest request =
                    (HttpServletRequest) pageContext.getRequest();

            // Get the file containing the servlet
            file = new File(pageContext.getServletContext()
                            .getRealPath(request.getServletPath()));
        }

        Date lastModified = new Date(file.lastModified());
        Converter conv = Converter.getInstance();

        // Set the last modified value back
        pageContext.setAttribute("lastModified",
                                 conv.toString(lastModified, mFormat));

        return Tag.EVAL_BODY_INCLUDE;
    }
}
