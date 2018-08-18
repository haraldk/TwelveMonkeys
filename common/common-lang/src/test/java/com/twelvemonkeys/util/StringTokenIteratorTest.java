/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util;


import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * StringTokenIteratorTestCase
 * <p/>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/StringTokenIteratorTestCase.java#1 $
 */
public class StringTokenIteratorTest extends TokenIteratorAbstractTest {

    protected TokenIterator createTokenIterator(String pString) {
        return new StringTokenIterator(pString);
    }

    protected TokenIterator createTokenIterator(String pString, String pDelimiters) {
        return new StringTokenIterator(pString, pDelimiters);
    }

    @Test
    public void testEmptyDelimiter() {
        Iterator iterator = createTokenIterator("", "");
        assertFalse("Empty string has elements", iterator.hasNext());
    }

    @Test
    public void testSingleToken() {
        Iterator iterator = createTokenIterator("A");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    @Test
    public void testSingleTokenEmptyDelimiter() {
        Iterator iterator = createTokenIterator("A", "");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    @Test
    public void testSingleTokenSingleDelimiter() {
        Iterator iterator = createTokenIterator("A", ",");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    @Test
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

    @Test
    public void testSingleSeparator() {
        Iterator iterator = createTokenIterator("A,B,C", ",");
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("A", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("B", iterator.next());
        assertTrue("String has no elements", iterator.hasNext());
        assertEquals("C", iterator.next());
        assertFalse("String has more than one element", iterator.hasNext());
    }

    @Test
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

    @Test
    public void testMultipleSeparator() {
        Iterator iterator = createTokenIterator("A,B,;,C...D, ., ,E", " ,.;:");
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
}
