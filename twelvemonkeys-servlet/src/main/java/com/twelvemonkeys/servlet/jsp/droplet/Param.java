
package com.twelvemonkeys.servlet.jsp.droplet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.jsp.*;

/**
 * Param
 */
public class Param implements JspFragment {

    /** The value member field. */
    protected String mValue = null;

    /**
     * Creates a Param.
     *
     * @param pValue the value of the parameter
     */
    public Param(String pValue) {
        mValue = pValue;
    }

    /**
     * Gets the value of the parameter.
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Services the page fragment. This version simply prints the value of
     * this parameter to teh PageContext's out.
     */
    public void service(PageContext pContext)
            throws ServletException, IOException {
        JspWriter writer = pContext.getOut();
        writer.print(mValue);
    }
}
