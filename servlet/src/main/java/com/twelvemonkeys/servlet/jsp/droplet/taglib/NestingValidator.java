/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: NestingValidator.java,v $
 * Revision 1.4  2003/08/04 15:26:40  WMHAKUR
 * Code clean-up.
 *
 * Revision 1.3  2002/11/18 14:12:43  WMHAKUR
 * *** empty log message ***
 *
 * Revision 1.2  2002/10/18 14:28:07  WMHAKUR
 * Fixed package error.
 *
 * Revision 1.1  2002/10/18 14:03:09  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet.taglib
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet.taglib;


import java.util.*;

import javax.servlet.jsp.tagext.*;
import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.twelvemonkeys.util.*;

/**
 * A validator that verifies that tags follow
 * proper nesting order.
 * <P>
 * Based on NestingValidator.java,
 * taken from More Servlets and JavaServer Pages
 * from Prentice Hall and Sun Microsystems Press,
 * http://www.moreservlets.com/.
 * &copy; 2002 Marty Hall; may be freely used or adapted.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 *
 */

public class NestingValidator extends TagLibraryValidator {

    private Vector errors = new Vector();

    /**
     *
     */

    public ValidationMessage[] validate(String pPrefix,
                                        String pURI,
                                        PageData pPage) {

        //System.out.println("Validating " + pPrefix + " (" + pURI + ") for "
        //                   + pPage + ".");

        // Pass the parser factory in on the command line with
        // -D to override the use of the Apache parser.

        DefaultHandler handler = new NestingHandler(pPrefix, pURI, this);
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try {
            //            FileUtil.copy(pPage.getInputStream(), System.out);

            SAXParser parser = factory.newSAXParser();
            InputSource source =
                    new InputSource(pPage.getInputStream());

            // Parse, handler will use callback to report errors
            parser.parse(source, handler);


        }
        catch (Exception e) {
            String errorMessage = e.getMessage();

            reportError(errorMessage);
        }

        // Return any errors and exceptions, empty array means okay
        return (ValidationMessage[])
                errors.toArray(new ValidationMessage[errors.size()]);
    }

    /**
     * Callback method for the handler to report errors
     */

    public void reportError(String pMessage) {
        // The first argument to the ValidationMessage
        // constructor can be a tag ID. Since tag IDs
        // are not universally supported, use null for
        // portability. The important part is the second
        // argument: the error message.
        errors.add(new ValidationMessage(null, pMessage));
    }
}

