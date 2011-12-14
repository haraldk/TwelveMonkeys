package com.twelvemonkeys.servlet.jsp.droplet;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

/**
 * Param
 */
public class Param implements JspFragment {

    /** The value member field. */
    protected String value = null;

    /**
     * Creates a Param.
     *
     * @param pValue the value of the parameter
     */
    public Param(String pValue) {
        value = pValue;
    }

    /**
     * Gets the value of the parameter.
     */
    public String getValue() {
        return value;
    }

    /**
     * Services the page fragment. This version simply prints the value of
     * this parameter to teh PageContext's out.
     */
    public void service(PageContext pContext)
            throws ServletException, IOException {
        JspWriter writer = pContext.getOut();
        writer.print(value);
    }
}
