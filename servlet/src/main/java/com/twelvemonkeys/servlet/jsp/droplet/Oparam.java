
package com.twelvemonkeys.servlet.jsp.droplet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.jsp.*;

/**
 * Oparam (Open parameter)
 */
public class Oparam extends Param implements JspFragment {
    /**
     * Creates an Oparam.
     *
     * @param pValue the value of the parameter
     */
    public Oparam(String pValue) {
        super(pValue);
    }

    public void service(PageContext pContext)
            throws ServletException, IOException {
        pContext.getServletContext().log("Service subpage " + pContext.getServletContext().getRealPath(mValue));

        pContext.include(mValue);
    }
}

