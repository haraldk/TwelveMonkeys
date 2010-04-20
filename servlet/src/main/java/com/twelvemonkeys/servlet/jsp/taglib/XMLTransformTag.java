/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: XMLTransformTag.java,v $
 * Revision 1.2  2003/10/06 14:25:43  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.1  2002/11/19 10:50:41  WMHAKUR
 * *** empty log message ***
 *
 */

package com.twelvemonkeys.servlet.jsp.taglib;

import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import com.twelvemonkeys.servlet.jsp.*;

/**
 * This tag performs XSL Transformations (XSLT) on a given XML document or its
 * body content.
 *
 * @author Harald Kuhr
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/jsp/taglib/XMLTransformTag.java#1 $
 */

public class XMLTransformTag extends ExBodyTagSupport {
    private String mDocumentURI = null;
    private String mStylesheetURI = null;

    /**
     * Sets the document attribute for this tag.
     */

    public void setDocumentURI(String pDocumentURI) {
        mDocumentURI = pDocumentURI;
    }

    /**
     * Sets the stylesheet attribute for this tag.
     */

    public void setStylesheetURI(String pStylesheetURI) {
        mStylesheetURI = pStylesheetURI;
    }


    /**
     * doStartTag implementation, that performs XML Transformation on the
     * given document, if any.
     * If the documentURI attribute is set, then the transformation is
     * performed on the document at that location, and
     * {@code Tag.SKIP_BODY} is returned.
     * Otherwise, this method simply returns
     * {@code BodyTag.EVAL_BODY_BUFFERED} and leaves the transformation to
     * the doEndTag.
     *
     * @return {@code Tag.SKIP_BODY} if {@code documentURI} is not
     *         {@code null}, otherwise
     *         {@code BodyTag.EVAL_BODY_BUFFERED}.
     *
     * @todo Is it really a good idea to allow "inline" XML in a JSP?
     */

    public int doStartTag() throws JspException {
        //log("XML: " + mDocumentURI + " XSL: " + mStylesheetURI);

        if (mDocumentURI != null) {
            // If document given, transform and skip body...
            try {
                transform(getSource(mDocumentURI));
            }
            catch (MalformedURLException murle) {
                throw new JspException(murle.getMessage(), murle);
            }
            catch (IOException ioe) {
                throw new JspException(ioe.getMessage(), ioe);
            }

            return Tag.SKIP_BODY;
        }

        // ...else process the body
        return BodyTag.EVAL_BODY_BUFFERED;
    }

    /**
     * doEndTag implementation, that will perform XML Transformation on the
     * body content.
     *
     * @return super.doEndTag()
     */

    public int doEndTag() throws JspException {
        // Get body content (trim is CRUCIAL, as some XML parsers are picky...)
        String body = bodyContent.getString().trim();

        // Do transformation
        transform(new StreamSource(new ByteArrayInputStream(body.getBytes())));

        return super.doEndTag();
    }

    /**
     * Performs the transformation and writes the result to the JSP writer.
     *
     * @param in the source document to transform.
     */

    public void transform(Source pIn) throws JspException {
        try {
            // Create transformer
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(getSource(mStylesheetURI));

            // Store temporary output in a bytearray, as the transformer will
            // usually try to flush the stream (illegal operation from a custom
            // tag).
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            StreamResult out = new StreamResult(os);

            // Perform the transformation
            transformer.transform(pIn, out);

            // Write the result back to the JSP writer
            pageContext.getOut().print(os.toString());
        }
        catch (MalformedURLException murle) {
            throw new JspException(murle.getMessage(), murle);
        }
        catch (IOException ioe) {
            throw new JspException(ioe.getMessage(), ioe);
        }
        catch (TransformerException te) {
            throw new JspException("XSLT Trandformation failed: " + te.getMessage(), te);
        }
    }

    /**
     * Returns a StreamSource object, for the given URI
     */

    private StreamSource getSource(String pURI)
            throws IOException, MalformedURLException {
        if (pURI != null && pURI.indexOf("://") < 0) {
            // If local, get as stream
            return new StreamSource(getResourceAsStream(pURI));
        }

        // ...else, create from URI string
        return new StreamSource(pURI);
    }
}
