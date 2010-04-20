/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: Droplet.java,v $
 * Revision 1.3  2003/10/06 14:25:19  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.2  2002/10/18 14:12:16  WMHAKUR
 * Now, it even compiles. :-/
 *
 * Revision 1.1  2002/10/18 14:02:16  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

import com.twelvemonkeys.servlet.jsp.droplet.taglib.*;

/**
 * Dynamo Droplet like Servlet.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 *
 */
public abstract class Droplet extends HttpServlet implements JspFragment {

    // Copy doc
    public abstract void service(PageContext pPageContext)
            throws ServletException, IOException;

    /**
     * Services a parameter. Programatically equivalent to the
     * <d:valueof param="pParameter"/> JSP tag.
     */
    public void serviceParameter(String pParameter, PageContext pPageContext)
            throws ServletException, IOException {
        Object param = pPageContext.getRequest().getAttribute(pParameter);

        if (param != null) {
            if (param instanceof Param) {
                ((Param) param).service(pPageContext);
            }
            else {
                pPageContext.getOut().print(param);
            }
        }
        else {
            // Try to get value from parameters
            Object obj = pPageContext.getRequest().getParameter(pParameter);

            // Print parameter or default value
            pPageContext.getOut().print((obj != null) ? obj : "");
        }
    }

    /**
     * "There's no need to override this method." :-)
     */
    final public void service(HttpServletRequest pRequest,
                              HttpServletResponse pResponse)
            throws ServletException, IOException {
        PageContext pageContext =
                (PageContext) pRequest.getAttribute(IncludeTag.PAGE_CONTEXT);

        // TODO: What if pageContext == null
        service(pageContext);
    }
}
