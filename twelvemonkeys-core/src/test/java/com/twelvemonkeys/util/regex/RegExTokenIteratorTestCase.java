package com.twelvemonkeys.util.regex;

import com.twelvemonkeys.util.TokenIterator;
import com.twelvemonkeys.util.TokenIteratorAbstractTestCase;

import java.util.Iterator;

/**
 * StringTokenIteratorTestCase
 * <p/>
 * <!-- To change this template use Options | File Templates. -->
 * <!-- Created by IntelliJ IDEA. -->
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/regex/RegExTokenIteratorTestCase.java#1 $
 */
public class RegExTokenIteratorTestCase extends TokenIteratorAbstractTestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected TokenIterator createTokenIterator(String pString) {
        return new RegExTokenIterator(pString);
    }

    protected TokenIterator createTokenIterator(String pString, String pDelimiters) {
        return new RegExTokenIterator(pString, pDelimiters);
    }

    public void testEmptyDelimiter() {
        // TODO: What is it supposed to match?
        /*
        Iterator iterator = createTokenIterator("", ".*");
        assertTrue("Empty string has no elements", iterator.hasNext());
        iterator.next();
        assertFalse("Empty string has more then one element", iterator.hasNext());
        */
    }

    public void testSingleToken() {
        Iterator iterator = createTokenIterator("A");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    public void testSingleTokenEmptyDelimiter() {
        // TODO: What is it supposed to match?
        /*
        Iterator iterator = createTokenIterator("A", ".*");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
        */
    }

    public void testSingleTokenSingleDelimiter() {
        Iterator iterator = createTokenIterator("A", "[^,]+");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    public void testSingleSeparatorDefaultDelimiter() {
        Iterator iterator = createTokenIterator("A B C D");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("B", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("C", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("D", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    public void testSingleSeparator() {
        Iterator iterator = createTokenIterator("A,B,C", "[^,]+");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("B", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("C", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    public void testMultipleSeparatorDefaultDelimiter() {
        Iterator iterator = createTokenIterator("A B   C\nD\t\t \nE");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("B", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("C", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("D", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("E", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    public void testMultipleSeparator() {
        Iterator iterator = createTokenIterator("A,B,;,C...D, ., ,E", "[^ ,.;:]+");
        assertTrue("String has no elements", iterator.hasNext());
        Object o = iterator.next();
        assertEquals("A", o);
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("B", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("C", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("D", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("E", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }
}
