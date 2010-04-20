/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: JspFragment.java,v $
 * Revision 1.2  2003/10/06 14:25:36  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.1  2002/10/18 14:02:16  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.jsp.*;

/**
 * Interface for JSP sub pages or page fragments to implement.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 */
public interface JspFragment {

    /**
     * Services a sub page or a page fragment inside another page
     * (or PageContext).
     *
     * @param pContext the PageContext that is used to render the subpage.
     *
     * @throws ServletException if an exception occurs that interferes with the
     *         subpage's normal operation
     * @throws IOException if an input or output exception occurs
     */
    public void service(PageContext pContext)
            throws ServletException, IOException;
}
