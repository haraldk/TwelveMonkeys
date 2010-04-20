package com.twelvemonkeys.servlet;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;

import javax.servlet.ServletResponse;

/**
 * ServletResponseAbsrtactTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletResponseAbsrtactTestCase.java#1 $
 */
public abstract class ServletResponseAbsrtactTestCase extends ObjectAbstractTestCase {
    protected Object makeObject() {
        return makeServletResponse();
    }

    protected abstract ServletResponse makeServletResponse();

    // TODO: Implement
}
