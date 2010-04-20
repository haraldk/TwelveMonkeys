
package com.twelvemonkeys.servlet.jsp.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;

/**
 * This tag truncates all consecutive whitespace in sequence inside its body,
 * to one whitespace character. The first whitespace character in the sequence
 * will be left untouched (except for CR/LF, which will always leave a LF).
 *
 * @author Harald Kuhr
 *
 * @version 1.0
 */

public class TrimWhiteSpaceTag extends ExBodyTagSupport {

    /**
     * doStartTag implementation, simply returns
     * {@code BodyTag.EVAL_BODY_BUFFERED}.
     *
     * @return {@code BodyTag.EVAL_BODY_BUFFERED}
     */

    public int doStartTag() throws JspException {
        return BodyTag.EVAL_BODY_BUFFERED;
    }

    /**
     * doEndTag implementation, truncates all whitespace.
     *
     * @return {@code super.doEndTag()}
     */

    public int doEndTag() throws JspException {
        // Trim
        String trimmed = truncateWS(bodyContent.getString());
        try {
            // Print trimmed content
            //pageContext.getOut().print("<!--TWS-->\n");
            pageContext.getOut().print(trimmed);
            //pageContext.getOut().print("\n<!--/TWS-->");
        }
        catch (IOException ioe) {
            throw new JspException(ioe);
        }

        return super.doEndTag();
    }

    /**
     * Truncates whitespace from the given string.
     *
     * @todo Candidate for StringUtil?
     */

    private static String truncateWS(String pStr) {
        char[] chars = pStr.toCharArray();

        int count = 0;
        boolean lastWasWS = true; // Avoids leading WS
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isWhitespace(chars[i])) {
                // if char is not WS, just store
                chars[count++] = chars[i];
                lastWasWS = false;
            }
            else {
                // else, if char is WS, store first, skip the rest
                if (!lastWasWS) {
                    if (chars[i] == 0x0d) {
                        chars[count++] = 0x0a; //Always new line
                    }
                    else {
                        chars[count++] = chars[i];
                    }
                }
                lastWasWS = true;
            }
        }

        // Return the trucated string
        return new String(chars, 0, count);
    }

}
