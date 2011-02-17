/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: NestingHandler.java,v $
 * Revision 1.4  2003/10/06 14:25:44  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.3  2003/08/04 15:26:30  WMHAKUR
 * Code clean-up.
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

import com.twelvemonkeys.lang.StringUtil;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX handler that returns an exception if the nesting of
 * {@code param}, {@code oparam}, {@code droplet} and
 * {@code valueof} is not correct.
 *
 * Based on the NestingHandler.java,
 * taken from More Servlets and JavaServer Pages
 * from Prentice Hall and Sun Microsystems Press,
 * http://www.moreservlets.com/.
 * &copy; 2002 Marty Hall; may be freely used or adapted.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 */
public class NestingHandler extends DefaultHandler {
    private String includeTagName = "include";
    private String paramTagName = "param";
    private String openParamTagName = "oparam";

    //private Stack mParents = new Stack();

    private boolean inIncludeTag = false;

    private String namespacePrefix = null;
    private String namespaceURI = null;

    private NestingValidator validator = null;

    public NestingHandler(String pNamespacePrefix, String pNameSpaceURI,
                          NestingValidator pValidator) {
        namespacePrefix = pNamespacePrefix;
        namespaceURI = pNameSpaceURI;

        validator = pValidator;
    }

    public void startElement(String pNamespaceURI, String pLocalName,
                             String pQualifiedName, Attributes pAttributes)
            throws SAXException {
        String namespacePrefix = !StringUtil.isEmpty(pNamespaceURI)
                ? getNSPrefixFromURI(pNamespaceURI)
                : getNamespacePrefix(pQualifiedName);

        String localName = !StringUtil.isEmpty(pLocalName)
                ? pLocalName : getLocalName(pQualifiedName);
        /*
        if (namespacePrefix.equals(namespacePrefix)) {
            System.out.println("startElement:\nnamespaceURI=" + pNamespaceURI
                               + " namespacePrefix=" + namespacePrefix
                               + " localName=" + localName
                               + " qName=" + pQualifiedName
                               + " attributes=" + pAttributes);
        }
        */
        if (localName.equals(includeTagName)) {
            // include
            //System.out.println("<" + namespacePrefix + ":"
            //                   + includeTagName + ">");
            if (inIncludeTag) {
                validator.reportError("Cannot nest " + namespacePrefix + ":"
                                       + includeTagName);
            }
            inIncludeTag = true;
        }
        else if (localName.equals(paramTagName)) {
            // param
            //System.out.println("<" + namespacePrefix + ":"
            //                   + paramTagName + "/>");
            if (!inIncludeTag) {
                validator.reportError(this.namespacePrefix + ":"
                                       + paramTagName
                                       + " can only appear within "
                                       + this.namespacePrefix + ":"
                                       + includeTagName);
            }
        }
        else if (localName.equals(openParamTagName)) {
            // oparam
            //System.out.println("<" + namespacePrefix + ":"
            //                   + openParamTagName + ">");
            if (!inIncludeTag) {
                validator.reportError(this.namespacePrefix + ":"
                                       + openParamTagName
                                       + " can only appear within "
                                       + this.namespacePrefix + ":"
                                       + includeTagName);
            }
            inIncludeTag = false;
        }
        else {
            // Only jsp:text allowed inside include!
            if (inIncludeTag && !localName.equals("text")) {
                validator.reportError(namespacePrefix + ":" + localName
                                       + " can not appear within "
                                       + this.namespacePrefix + ":"
                                       + includeTagName);
            }
        }
    }

    public void endElement(String pNamespaceURI,
                           String pLocalName,
                           String pQualifiedName)
            throws SAXException {
        String namespacePrefix = !StringUtil.isEmpty(pNamespaceURI)
                ? getNSPrefixFromURI(pNamespaceURI)
                : getNamespacePrefix(pQualifiedName);

        String localName = !StringUtil.isEmpty(pLocalName)
                ? pLocalName : getLocalName(pQualifiedName);
        /*
        if (namespacePrefix.equals(namespacePrefix)) {
            System.out.println("endElement:\nnamespaceURI=" + pNamespaceURI
                               + " namespacePrefix=" + namespacePrefix
                               + " localName=" + localName
                               + " qName=" + pQualifiedName);
        }
        */
        if (namespacePrefix.equals(this.namespacePrefix)
                && localName.equals(includeTagName)) {

            //System.out.println("</" + namespacePrefix + ":"
            //                   + includeTagName + ">");

            inIncludeTag = false;
        }
        else if (namespacePrefix.equals(this.namespacePrefix)
                && localName.equals(openParamTagName)) {

            //System.out.println("</" + namespacePrefix + ":"
            //                   + openParamTagName + ">");

            inIncludeTag = true; // assuming no errors before this...
        }
    }

    /**
     * Stupid broken namespace-support "fix"..
     */

    private String getNSPrefixFromURI(String pNamespaceURI) {
        return (pNamespaceURI.equals(namespaceURI)
                ? namespacePrefix : "");
    }

    private String getNamespacePrefix(String pQualifiedName) {
        return pQualifiedName.substring(0, pQualifiedName.indexOf(':'));
    }

    private String getLocalName(String pQualifiedName) {
        return pQualifiedName.substring(pQualifiedName.indexOf(':') + 1);
    }
}

