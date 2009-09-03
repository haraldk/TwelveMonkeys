package com.twelvemonkeys.util;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * TokenIteratorAbstractTestCase
 * <p/>
 * <!-- To change this template use Options | File Templates. -->
 * <!-- Created by IntelliJ IDEA. -->
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/TokenIteratorAbstractTestCase.java#1 $
 */
public abstract class TokenIteratorAbstractTestCase extends TestCase {
    protected abstract TokenIterator createTokenIterator(String pString);

    protected abstract TokenIterator createTokenIterator(String pString, String pDelimiters);

    public void testNullString() {
        try {
            createTokenIterator(null);
            fail("Null string parameter not allowed");
        }
        catch (IllegalArgumentException e) {
            // okay!
        }
        catch (Throwable t) {
            fail(t.getMessage());
        }
    }

    public void testNullDelimmiter() {
        try {
            createTokenIterator("", null);
            fail("Null delimiter parameter not allowed");
        }
        catch (IllegalArgumentException e) {
            // okay!
        }
        catch (Throwable t) {
            fail(t.getMessage());
        }
    }

    public void testEmptyString() {
        Iterator iterator = createTokenIterator("");
        assertFalse("Empty string has elements", iterator.hasNext());
    }

}
