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
    private String mIncludeTagName = "include";
    private String mParamTagName = "param";
    private String mOpenParamTagName = "oparam";

    //private Stack mParents = new Stack();

    private boolean mInIncludeTag = false;

    private String mNamespacePrefix = null;
    private String mNamespaceURI = null;

    private NestingValidator mValidator = null;

    public NestingHandler(String pNamespacePrefix, String pNameSpaceURI,
                          NestingValidator pValidator) {
        mNamespacePrefix = pNamespacePrefix;
        mNamespaceURI = pNameSpaceURI;

        mValidator = pValidator;
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
        if (namespacePrefix.equals(mNamespacePrefix)) {
            System.out.println("startElement:\nnamespaceURI=" + pNamespaceURI
                               + " namespacePrefix=" + namespacePrefix
                               + " localName=" + localName
                               + " qName=" + pQualifiedName
                               + " attributes=" + pAttributes);
        }
        */
        if (localName.equals(mIncludeTagName)) {
            // include
            //System.out.println("<" + mNamespacePrefix + ":"
            //                   + mIncludeTagName + ">");
            if (mInIncludeTag) {
                mValidator.reportError("Cannot nest " + namespacePrefix + ":"
                                       + mIncludeTagName);
            }
            mInIncludeTag = true;
        }
        else if (localName.equals(mParamTagName)) {
            // param
            //System.out.println("<" + mNamespacePrefix + ":"
            //                   + mParamTagName + "/>");
            if (!mInIncludeTag) {
                mValidator.reportError(mNamespacePrefix + ":"
                                       + mParamTagName
                                       + " can only appear within "
                                       + mNamespacePrefix + ":"
                                       + mIncludeTagName);
            }
        }
        else if (localName.equals(mOpenParamTagName)) {
            // oparam
            //System.out.println("<" + mNamespacePrefix + ":"
            //                   + mOpenParamTagName + ">");
            if (!mInIncludeTag) {
                mValidator.reportError(mNamespacePrefix + ":"
                                       + mOpenParamTagName
                                       + " can only appear within "
                                       + mNamespacePrefix + ":"
                                       + mIncludeTagName);
            }
            mInIncludeTag = false;
        }
        else {
            // Only jsp:text allowed inside include!
            if (mInIncludeTag && !localName.equals("text")) {
                mValidator.reportError(namespacePrefix + ":" + localName
                                       + " can not appear within "
                                       + mNamespacePrefix + ":"
                                       + mIncludeTagName);
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
        if (namespacePrefix.equals(mNamespacePrefix)) {
            System.out.println("endElement:\nnamespaceURI=" + pNamespaceURI
                               + " namespacePrefix=" + namespacePrefix
                               + " localName=" + localName
                               + " qName=" + pQualifiedName);
        }
        */
        if (namespacePrefix.equals(mNamespacePrefix)
                && localName.equals(mIncludeTagName)) {

            //System.out.println("</" + mNamespacePrefix + ":"
            //                   + mIncludeTagName + ">");

            mInIncludeTag = false;
        }
        else if (namespacePrefix.equals(mNamespacePrefix)
                && localName.equals(mOpenParamTagName)) {

            //System.out.println("</" + mNamespacePrefix + ":"
            //                   + mOpenParamTagName + ">");

            mInIncludeTag = true; // assuming no errors before this...
        }
    }

    /**
     * Stupid broken namespace-support "fix"..
     */

    private String getNSPrefixFromURI(String pNamespaceURI) {
        return (pNamespaceURI.equals(mNamespaceURI)
                ? mNamespacePrefix : "");
    }

    private String getNamespacePrefix(String pQualifiedName) {
        return pQualifiedName.substring(0, pQualifiedName.indexOf(':'));
    }

    private String getLocalName(String pQualifiedName) {
        return pQualifiedName.substring(pQualifiedName.indexOf(':') + 1);
    }
}

