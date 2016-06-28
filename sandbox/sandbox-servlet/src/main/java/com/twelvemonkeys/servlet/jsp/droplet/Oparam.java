package com.twelvemonkeys.servlet.jsp.droplet;

import javax.servlet.ServletException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

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

    public void service(PageContext pContext) throws ServletException, IOException {
        pContext.getServletContext().log("Service subpage " + pContext.getServletContext().getRealPath(value));

        pContext.include(value);
    }
}

