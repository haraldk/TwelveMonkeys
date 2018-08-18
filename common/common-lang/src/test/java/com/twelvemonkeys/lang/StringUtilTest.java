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

package com.twelvemonkeys.lang;

import org.junit.Test;

import java.awt.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * StringUtilTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/lang/StringUtilTestCase.java#1 $
 *
 */
public class StringUtilTest {
    final static Object TEST_OBJECT = new Object();
    final static Integer TEST_INTEGER = 42;
    final static String TEST_STRING = "TheQuickBrownFox"; // No WS!
    final static String TEST_SUB_STRING = TEST_STRING.substring(2, 5);
    final static String TEST_DELIM_STRING = "one,two, three\n four\tfive six";
    final static String[] STRING_ARRAY = {"one", "two", "three", "four", "five", "six"};
    final static String TEST_INT_DELIM_STRING = "1,2, 3\n 4\t5 6";
    final static int[] INT_ARRAY = {1, 2, 3, 4, 5, 6};
    final static String TEST_DOUBLE_DELIM_STRING = "1.4,2.1, 3\n .4\t-5 6e5";
    final static double[] DOUBLE_ARRAY = {1.4, 2.1, 3, .4, -5, 6e5};
    final static String EMPTY_STRING = "";
    final static String WHITESPACE_STRING = " \t \r \n  ";

    @Test
    public void testValueOfObject() {
        assertNotNull(StringUtil.valueOf(TEST_OBJECT));
        assertEquals(StringUtil.valueOf(TEST_OBJECT), TEST_OBJECT.toString());
        assertEquals(StringUtil.valueOf(TEST_INTEGER), TEST_INTEGER.toString());
        assertEquals(StringUtil.valueOf(TEST_STRING), TEST_STRING);
        assertSame(StringUtil.valueOf(TEST_STRING), TEST_STRING);

        assertNull(StringUtil.valueOf(null));
    }

    @Test
    public void testToUpperCase() {
        String str = StringUtil.toUpperCase(TEST_STRING);
        assertNotNull(str);
        assertEquals(TEST_STRING.toUpperCase(), str);

        str = StringUtil.toUpperCase(null);
        assertNull(str);
    }

    @Test
    public void testToLowerCase() {
        String str = StringUtil.toLowerCase(TEST_STRING);
        assertNotNull(str);
        assertEquals(TEST_STRING.toLowerCase(), str);

        str = StringUtil.toLowerCase(null);
        assertNull(str);
    }

    @Test
    public void testIsEmpty() {
        assertTrue(StringUtil.isEmpty((String) null));
        assertTrue(StringUtil.isEmpty(EMPTY_STRING));
        assertTrue(StringUtil.isEmpty(WHITESPACE_STRING));
        assertFalse(StringUtil.isEmpty(TEST_STRING));
    }

    @Test
    public void testIsEmptyArray() {
        assertTrue(StringUtil.isEmpty((String[]) null));
        assertTrue(StringUtil.isEmpty(new String[]{EMPTY_STRING}));
        assertTrue(StringUtil.isEmpty(new String[]{EMPTY_STRING, WHITESPACE_STRING}));
        assertFalse(StringUtil.isEmpty(new String[]{EMPTY_STRING, TEST_STRING}));
        assertFalse(StringUtil.isEmpty(new String[]{WHITESPACE_STRING, TEST_STRING}));
    }

    @Test
    public void testContains() {
        assertTrue(StringUtil.contains(TEST_STRING, TEST_STRING));
        assertTrue(StringUtil.contains(TEST_STRING, TEST_SUB_STRING));
        assertTrue(StringUtil.contains(TEST_STRING, EMPTY_STRING));
        assertFalse(StringUtil.contains(TEST_STRING, WHITESPACE_STRING));
        assertFalse(StringUtil.contains(TEST_SUB_STRING, TEST_STRING));
        assertFalse(StringUtil.contains(EMPTY_STRING, TEST_STRING));
        assertFalse(StringUtil.contains(WHITESPACE_STRING, TEST_STRING));
        assertFalse(StringUtil.contains(null, TEST_STRING));
        assertFalse(StringUtil.contains(null, null));
    }

    @Test
    public void testContainsIgnoreCase() {
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_STRING));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase()));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_SUB_STRING));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase()));
        assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, EMPTY_STRING));
        assertFalse(StringUtil.containsIgnoreCase(TEST_STRING, WHITESPACE_STRING));
        assertFalse(StringUtil.containsIgnoreCase(TEST_SUB_STRING, TEST_STRING));
        assertFalse(StringUtil.containsIgnoreCase(EMPTY_STRING, TEST_STRING));
        assertFalse(StringUtil.containsIgnoreCase(WHITESPACE_STRING, TEST_STRING));
        assertFalse(StringUtil.containsIgnoreCase(null, TEST_STRING));
        assertFalse(StringUtil.containsIgnoreCase(null, null));
    }

    @Test
    public void testContainsChar() {
        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(StringUtil.contains(TEST_STRING, TEST_STRING.charAt(i)));
            assertFalse(StringUtil.contains(EMPTY_STRING, TEST_STRING.charAt(i)));
            assertFalse(StringUtil.contains(WHITESPACE_STRING, TEST_STRING.charAt(i)));
            assertFalse(StringUtil.contains(null, TEST_STRING.charAt(i)));
        }
        for (int i = 0; i < TEST_SUB_STRING.length(); i++) {
            assertTrue(StringUtil.contains(TEST_STRING, TEST_SUB_STRING.charAt(i)));
        }
        for (int i = 0; i < WHITESPACE_STRING.length(); i++) {
            assertFalse(StringUtil.contains(TEST_STRING, WHITESPACE_STRING.charAt(i)));
        }

        // Test all alpha-chars
        for (int i = 'a'; i < 'z'; i++) {
            if (TEST_STRING.indexOf(i) < 0) {
                assertFalse(TEST_STRING + " seems to contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), StringUtil.contains(TEST_STRING, i));
            }
            else {
                assertTrue(TEST_STRING + " seems to not contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), StringUtil.contains(TEST_STRING, i));
            }
        }
    }

    @Test
    public void testContainsIgnoreCaseChar() {
        // Must contain all chars in string
        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_STRING.charAt(i)));
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.charAt(i)));
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.charAt(i)));
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, Character.toUpperCase(TEST_STRING.charAt(i))));
            assertFalse(StringUtil.containsIgnoreCase(EMPTY_STRING, TEST_STRING.charAt(i)));
            assertFalse(StringUtil.containsIgnoreCase(WHITESPACE_STRING, TEST_STRING.charAt(i)));
            assertFalse(StringUtil.containsIgnoreCase(null, TEST_STRING.charAt(i)));
        }
        for (int i = 0; i < TEST_SUB_STRING.length(); i++) {
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_SUB_STRING.charAt(i)));
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING.charAt(i)));
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING.charAt(i)));
            assertTrue(StringUtil.containsIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase().charAt(i)));
        }

        for (int i = 0; i < WHITESPACE_STRING.length(); i++) {
            assertFalse(StringUtil.containsIgnoreCase(TEST_STRING, WHITESPACE_STRING.charAt(i)));
        }

        // Test all alpha-chars
        for (int i = 'a'; i < 'z'; i++) {
            if ((TEST_STRING.indexOf(i) < 0) && (TEST_STRING.indexOf(Character.toUpperCase((char) i)) < 0)) {
                assertFalse(TEST_STRING + " seems to contain '" + (char) i + "', at index " + Math.max(TEST_STRING.indexOf(i), TEST_STRING.indexOf(Character.toUpperCase((char) i))), StringUtil.containsIgnoreCase(TEST_STRING, i));
            }
            else {
                assertTrue(TEST_STRING + " seems to not contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), StringUtil.containsIgnoreCase(TEST_STRING, i));
            }
        }
    }

    @Test
    public void testIndexOfIgnoreCase() {
        assertEquals(0, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING));
        assertEquals(0, StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING));
        assertEquals(0, StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING));
        assertEquals(0, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase()));
        assertEquals(0, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase()));

        for (int i = 1; i < TEST_STRING.length(); i++) {
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.substring(i)));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.substring(i)));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.substring(i)));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase().substring(i)));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase().substring(i)));
        }

        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase()));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toLowerCase()));

        for (int i = 1; i < TEST_STRING.length(); i++) {
            assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING.substring(i), TEST_STRING));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING.substring(i), TEST_STRING));
        }

        assertEquals(-1, StringUtil.indexOfIgnoreCase(null, TEST_STRING));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(null, null));
    }

    @Test
    public void testIndexOfIgnoreCasePos() {
        assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING, 1));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING, 2));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING, 3));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase(), 4));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase(), 5));

        for (int i = 1; i < TEST_STRING.length(); i++) {
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.substring(i), i - 1));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.substring(i), i - 1));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.substring(i), i - 1));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase().substring(i), i - 1));
            assertEquals(i, StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase().substring(i), i - 1));
        }

        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING, 1));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING, 1));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING, 2));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase(), 1));
        assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toLowerCase(), 2));

        assertEquals(-1, StringUtil.indexOfIgnoreCase(null, TEST_STRING, 234));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(null, null, -45));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase()));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase()));

        for (int i = 1; i < TEST_STRING.length(); i++) {
            assertEquals(i, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.substring(i)));
            assertEquals(i, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.substring(i)));
            assertEquals(i, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.substring(i)));
            assertEquals(i, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase().substring(i)));
            assertEquals(i, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase().substring(i)));
        }

        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase()));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toLowerCase()));

        for (int i = 1; i < TEST_STRING.length(); i++) {
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.substring(i), TEST_STRING));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.substring(i), TEST_STRING));
        }

        assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(null, TEST_STRING));
        assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(null, null));

    }

    @Test
    public void testLastIndexOfIgnoreCasePos() {
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING, 1));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING, 2));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING, 3));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase(), 4));
        assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase(), 5));

        for (int i = 1; i < TEST_STRING.length(); i++) {
            assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.substring(0, i), i - 1));
            assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.substring(0, i), i - 1));
            assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.substring(0, i), i - 1));
            assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase().substring(0, i), i - 1));
            assertEquals(0, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.toLowerCase().substring(0, i), i - 1));
        }

        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING, TEST_SUB_STRING.length() + 3));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING, TEST_SUB_STRING.length() + 3));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING, TEST_SUB_STRING.length() + 4));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase(), TEST_SUB_STRING.length() + 3));
        assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toLowerCase(), TEST_SUB_STRING.length() + 4));

        assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(null, TEST_STRING, 234));
        assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(null, null, -45));

    }

    @Test
    public void testIndexOfIgnoreCaseChar() {
        // Must contain all chars in string
        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, Character.toUpperCase(TEST_STRING.charAt(i))));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(EMPTY_STRING, TEST_STRING.charAt(i)));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(WHITESPACE_STRING, TEST_STRING.charAt(i)));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(null, TEST_STRING.charAt(i)));
        }

        for (int i = 0; i < TEST_SUB_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase().charAt(i)));
        }

        for (int i = 0; i < WHITESPACE_STRING.length(); i++) {
            assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING, WHITESPACE_STRING.charAt(i)));
        }

        // Test all alpha-chars
        for (int i = 'a'; i < 'z'; i++) {
            if ((TEST_STRING.indexOf(i) < 0) && (TEST_STRING.indexOf(Character.toUpperCase((char) i)) < 0)) {
                assertEquals(TEST_STRING + " seems to contain '" + (char) i + "', at index " + Math.max(TEST_STRING.indexOf(i), TEST_STRING.indexOf(Character.toUpperCase((char) i))), -1, StringUtil.indexOfIgnoreCase(TEST_STRING, i));
            }
            else {
                assertTrue(TEST_STRING + " seems to not contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), 0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, i));
            }
        }
    }

    @Test
    public void testIndexOfIgnoreCaseCharPos() {
        // Must contain all chars in string
        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, Character.toUpperCase(TEST_STRING.charAt(i)), i));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(EMPTY_STRING, TEST_STRING.charAt(i), i));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(WHITESPACE_STRING, TEST_STRING.charAt(i), i));
            assertEquals(-1, StringUtil.indexOfIgnoreCase(null, TEST_STRING.charAt(i), i));
        }

        for (int i = 0; i < TEST_SUB_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase().charAt(i), i));
        }

        for (int i = 0; i < WHITESPACE_STRING.length(); i++) {
            assertEquals(-1, StringUtil.indexOfIgnoreCase(TEST_STRING, WHITESPACE_STRING.charAt(i), i));
        }

        // Test all alpha-chars
        for (int i = 'a'; i < 'z'; i++) {
            if ((TEST_STRING.indexOf(i) < 0) && (TEST_STRING.indexOf(Character.toUpperCase((char) i)) < 0)) {
                assertEquals(TEST_STRING + " seems to contain '" + (char) i + "', at index " + Math.max(TEST_STRING.indexOf(i), TEST_STRING.indexOf(Character.toUpperCase((char) i))), -1, StringUtil.indexOfIgnoreCase(TEST_STRING, i, 0));
            }
            else {
                assertTrue(TEST_STRING + " seems to not contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), 0 <= StringUtil.indexOfIgnoreCase(TEST_STRING, i, 0));
            }
        }
    }

    @Test
    public void testLastIndexOfIgnoreCaseChar() {
        // Must contain all chars in string
        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, Character.toUpperCase(TEST_STRING.charAt(i))));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(EMPTY_STRING, TEST_STRING.charAt(i)));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(WHITESPACE_STRING, TEST_STRING.charAt(i)));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(null, TEST_STRING.charAt(i)));
        }

        for (int i = 0; i < TEST_SUB_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING.charAt(i)));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase().charAt(i)));
        }

        for (int i = 0; i < WHITESPACE_STRING.length(); i++) {
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, WHITESPACE_STRING.charAt(i)));
        }

        // Test all alpha-chars
        for (int i = 'a'; i < 'z'; i++) {
            if ((TEST_STRING.indexOf(i) < 0) && (TEST_STRING.indexOf(Character.toUpperCase((char) i)) < 0)) {
                assertEquals(TEST_STRING + " seems to contain '" + (char) i + "', at index " + Math.max(TEST_STRING.indexOf(i), TEST_STRING.indexOf(Character.toUpperCase((char) i))), -1, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, i));
            }
            else {
                assertTrue(TEST_STRING + " seems to not contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), 0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, i));
            }
        }
    }

    @Test
    public void testLastIndexOfIgnoreCaseCharPos() {
        // Must contain all chars in string
        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_STRING.charAt(i), i));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, Character.toUpperCase(TEST_STRING.charAt(i)), i));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(EMPTY_STRING, TEST_STRING.charAt(i), i));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(WHITESPACE_STRING, TEST_STRING.charAt(i), i));
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(null, TEST_STRING.charAt(i), i));
        }

        for (int i = 0; i < TEST_SUB_STRING.length(); i++) {
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.charAt(i), TEST_STRING.length()));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toUpperCase(), TEST_SUB_STRING.charAt(i), TEST_STRING.length()));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING.toLowerCase(), TEST_SUB_STRING.charAt(i), TEST_STRING.length()));
            assertTrue(0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, TEST_SUB_STRING.toUpperCase().charAt(i), TEST_STRING.length()));
        }

        for (int i = 0; i < WHITESPACE_STRING.length(); i++) {
            assertEquals(-1, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, WHITESPACE_STRING.charAt(i), TEST_STRING.length()));
        }

        // Test all alpha-chars
        for (int i = 'a'; i < 'z'; i++) {
            if ((TEST_STRING.indexOf(i) < 0) && (TEST_STRING.indexOf(Character.toUpperCase((char) i)) < 0)) {
                assertEquals(TEST_STRING + " seems to contain '" + (char) i + "', at index " + Math.max(TEST_STRING.indexOf(i), TEST_STRING.indexOf(Character.toUpperCase((char) i))), -1, StringUtil.lastIndexOfIgnoreCase(TEST_STRING, i, TEST_STRING.length()));
            }
            else {
                assertTrue(TEST_STRING + " seems to not contain '" + (char) i + "', at index " + TEST_STRING.indexOf(i), 0 <= StringUtil.lastIndexOfIgnoreCase(TEST_STRING, i, TEST_STRING.length()));
            }
        }
    }

    @Test
    public void testLtrim() {
        assertEquals(TEST_STRING, StringUtil.ltrim(TEST_STRING));
        assertEquals(TEST_STRING, StringUtil.ltrim("  " + TEST_STRING));
        assertEquals(TEST_STRING, StringUtil.ltrim(WHITESPACE_STRING + TEST_STRING));
        assertFalse(TEST_STRING.equals(StringUtil.ltrim(TEST_STRING + WHITESPACE_STRING)));
        // TODO: Test is not complete
    }

    @Test
    public void testRtrim() {
        assertEquals(TEST_STRING, StringUtil.rtrim(TEST_STRING));
        assertEquals(TEST_STRING, StringUtil.rtrim(TEST_STRING + "  "));
        assertEquals(TEST_STRING, StringUtil.rtrim(TEST_STRING + WHITESPACE_STRING));
        assertFalse(TEST_STRING.equals(StringUtil.rtrim(WHITESPACE_STRING + TEST_STRING)));
        // TODO: Test is not complete
    }

    @Test
    public void testReplace() {
        assertEquals("", StringUtil.replace(TEST_STRING, TEST_STRING, ""));
        assertEquals("", StringUtil.replace("", "", ""));
        assertEquals("", StringUtil.replace("", "xyzzy", "xyzzy"));
        assertEquals(TEST_STRING, StringUtil.replace(TEST_STRING, "", "xyzzy"));
        assertEquals("aabbdd", StringUtil.replace("aabbccdd", "c", ""));
        assertEquals("aabbccdd", StringUtil.replace("aabbdd", "bd", "bccd"));
        // TODO: Test is not complete
    }

    @Test
    public void testReplaceIgnoreCase() {
        assertEquals("", StringUtil.replaceIgnoreCase(TEST_STRING, TEST_STRING.toUpperCase(), ""));
        assertEquals("", StringUtil.replaceIgnoreCase("", "", ""));
        assertEquals("", StringUtil.replaceIgnoreCase("", "xyzzy", "xyzzy"));
        assertEquals(TEST_STRING, StringUtil.replaceIgnoreCase(TEST_STRING, "", "xyzzy"));
        assertEquals("aabbdd", StringUtil.replaceIgnoreCase("aabbCCdd", "c", ""));
        assertEquals("aabbdd", StringUtil.replaceIgnoreCase("aabbccdd", "C", ""));
        assertEquals("aabbccdd", StringUtil.replaceIgnoreCase("aabbdd", "BD", "bccd"));
        assertEquals("aabbccdd", StringUtil.replaceIgnoreCase("aabBDd", "bd", "bccd"));
        // TODO: Test is not complete
    }

    @Test
    public void testCut() {
        assertEquals(TEST_STRING, StringUtil.cut(TEST_STRING, TEST_STRING.length(), ".."));
        assertEquals("This is a test..", StringUtil.cut("This is a test of how this works", 16, ".."));
        assertEquals("This is a test", StringUtil.cut("This is a test of how this works", 16, null));
        assertEquals("This is a test", StringUtil.cut("This is a test of how this works", 16, ""));
        // TODO: Test is not complete
    }

    @Test
    public void testCaptialize() {
        assertNull(StringUtil.capitalize(null));
        assertEquals(TEST_STRING.toUpperCase(), StringUtil.capitalize(TEST_STRING.toUpperCase()));
        assertTrue(StringUtil.capitalize("abc").charAt(0) == 'A');
    }

    @Test
    public void testCaptializePos() {
        assertNull(StringUtil.capitalize(null, 45));

        // TODO: Should this throw IllegalArgument or StringIndexOutOfBonds?
        assertEquals(TEST_STRING, StringUtil.capitalize(TEST_STRING, TEST_STRING.length() + 45));

        for (int i = 0; i < TEST_STRING.length(); i++) {
            assertTrue(Character.isUpperCase(StringUtil.capitalize(TEST_STRING, i).charAt(i)));
        }
    }

    @Test
    public void testPad() {
        assertEquals(TEST_STRING + "...", StringUtil.pad(TEST_STRING, TEST_STRING.length() + 3, "..", false));
        assertEquals(TEST_STRING, StringUtil.pad(TEST_STRING, 4, ".", false));
        assertEquals(TEST_STRING, StringUtil.pad(TEST_STRING, 4, ".", true));
        assertEquals("..." + TEST_STRING, StringUtil.pad(TEST_STRING, TEST_STRING.length() + 3, "..", true));
    }

    @Test
    public void testToDate() {
        long time = System.currentTimeMillis();
        Date now = new Date(time - time % 60000); // Default format seems to have no seconds..
        Date date = StringUtil.toDate(DateFormat.getInstance().format(now));
        assertNotNull(date);
        assertEquals(now, date);
    }

    @Test
    public void testToDateWithFormatString() {
        Calendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(1976, 2, 16); // Month is 0-based
        Date date = StringUtil.toDate("16.03.1976", "dd.MM.yyyy");
        assertNotNull(date);
        assertEquals(cal.getTime(), date);

        cal.clear();
        cal.set(2004, 4, 13, 23, 51, 3);
        date = StringUtil.toDate("2004-5-13 23:51 (03)", "yyyy-MM-dd hh:mm (ss)");
        assertNotNull(date);
        assertEquals(cal.getTime(), date);

        cal.clear();
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 2);
        cal.set(Calendar.SECOND, 3);
        date = StringUtil.toDate("123", "hms");
        assertNotNull(date);
        assertEquals(cal.getTime(), date);
    }

    @Test
    public void testToDateWithFormat() {
        Calendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(1976, 2, 16); // Month is 0-based
        Date date = StringUtil.toDate("16.03.1976", new SimpleDateFormat("dd.MM.yyyy"));
        assertNotNull(date);
        assertEquals(cal.getTime(), date);

        cal.clear();
        cal.set(2004, 4, 13, 23, 51);
        date = StringUtil.toDate("13.5.04 23:51",
                                 DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("no", "NO")));
        assertNotNull(date);
        assertEquals(cal.getTime(), date);

        cal.clear();
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 2);
        date = StringUtil.toDate("1:02 am",
                                 DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US));
        assertNotNull(date);
        assertEquals(cal.getTime(), date);
    }

    @Test
    public void testToTimestamp() {
        Calendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(1976, 2, 16, 21, 28, 4); // Month is 0-based
        Date date = StringUtil.toTimestamp("1976-03-16 21:28:04");
        assertNotNull(date);
        assertTrue(date instanceof Timestamp);
        assertEquals(cal.getTime(), date);
    }

    @Test
    public void testToStringArray() {
        String[] arr = StringUtil.toStringArray(TEST_DELIM_STRING);
        assertNotNull(arr);
        assertEquals(STRING_ARRAY.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(STRING_ARRAY[i], arr[i]);
        }
    }

    @Test
    public void testToStringArrayDelim() {
        String[] arr = StringUtil.toStringArray("-1---2-3--4-5", "---");
        String[] arr2 = {"1", "2", "3", "4", "5"};
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }

        arr = StringUtil.toStringArray("1, 2, 3; 4 5", ",; ");
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }
    }

    @Test
    public void testToIntArray() {
        int[] arr = StringUtil.toIntArray(TEST_INT_DELIM_STRING);
        assertNotNull(arr);
        assertEquals(INT_ARRAY.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(INT_ARRAY[i], arr[i]);
        }
    }

    @Test
    public void testToIntArrayDelim() {
        int[] arr = StringUtil.toIntArray("-1---2-3--4-5", "---");
        int[] arr2 = {1, 2, 3, 4, 5};
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }

        arr = StringUtil.toIntArray("1, 2, 3; 4 5", ",; ");
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }
    }

    @Test
    public void testToIntArrayDelimBase() {
        int[] arr = StringUtil.toIntArray("-1___2_3__F_a", "___", 16);
        int[] arr2 = {-1, 2, 3, 0xf, 0xa};
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }

        arr = StringUtil.toIntArray("-1, 2, 3; 17 12", ",; ", 8);
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }
    }

    @Test
    public void testToLongArray() {
        long[] arr = StringUtil.toLongArray(TEST_INT_DELIM_STRING);
        assertNotNull(arr);
        assertEquals(INT_ARRAY.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(INT_ARRAY[i], arr[i]);
        }
    }

    @Test
    public void testToLongArrayDelim() {
        long[] arr = StringUtil.toLongArray("-12854928752983___2_3__4_5", "___");
        long[] arr2 = {-12854928752983L, 2, 3, 4, 5};
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }

        arr = StringUtil.toLongArray("-12854928752983, 2, 3; 4 5", ",; ");
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i]);
        }
    }

    @Test
    public void testToDoubleArray() {
        double[] arr = StringUtil.toDoubleArray(TEST_DOUBLE_DELIM_STRING);
        assertNotNull(arr);
        assertEquals(DOUBLE_ARRAY.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(DOUBLE_ARRAY[i], arr[i], 0d);
        }
    }

    @Test
    public void testToDoubleArrayDelim() {
        double[] arr = StringUtil.toDoubleArray("-12854928752983___.2_3__4_5e4", "___");
        double[] arr2 = {-12854928752983L, .2, 3, 4, 5e4};
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i], 0d);
        }

        arr = StringUtil.toDoubleArray("-12854928752983, .2, 3; 4 5E4", ",; ");
        assertNotNull(arr);
        assertEquals(arr2.length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr2[i], arr[i], 0d);
        }
    }

    @Test
    public void testTestToColor() {
        // Test all constants
        assertEquals(Color.black, StringUtil.toColor("black"));
        assertEquals(Color.black, StringUtil.toColor("BLACK"));
        assertEquals(Color.blue, StringUtil.toColor("blue"));
        assertEquals(Color.blue, StringUtil.toColor("BLUE"));
        assertEquals(Color.cyan, StringUtil.toColor("cyan"));
        assertEquals(Color.cyan, StringUtil.toColor("CYAN"));
        assertEquals(Color.darkGray, StringUtil.toColor("darkGray"));
        assertEquals(Color.darkGray, StringUtil.toColor("DARK_GRAY"));
        assertEquals(Color.gray, StringUtil.toColor("gray"));
        assertEquals(Color.gray, StringUtil.toColor("GRAY"));
        assertEquals(Color.green, StringUtil.toColor("green"));
        assertEquals(Color.green, StringUtil.toColor("GREEN"));
        assertEquals(Color.lightGray, StringUtil.toColor("lightGray"));
        assertEquals(Color.lightGray, StringUtil.toColor("LIGHT_GRAY"));
        assertEquals(Color.magenta, StringUtil.toColor("magenta"));
        assertEquals(Color.magenta, StringUtil.toColor("MAGENTA"));
        assertEquals(Color.orange, StringUtil.toColor("orange"));
        assertEquals(Color.orange, StringUtil.toColor("ORANGE"));
        assertEquals(Color.pink, StringUtil.toColor("pink"));
        assertEquals(Color.pink, StringUtil.toColor("PINK"));
        assertEquals(Color.red, StringUtil.toColor("red"));
        assertEquals(Color.red, StringUtil.toColor("RED"));
        assertEquals(Color.white, StringUtil.toColor("white"));
        assertEquals(Color.white, StringUtil.toColor("WHITE"));
        assertEquals(Color.yellow, StringUtil.toColor("yellow"));
        assertEquals(Color.yellow, StringUtil.toColor("YELLOW"));

//        System.out.println(StringUtil.deepToString(Color.yellow));
//        System.out.println(StringUtil.deepToString(Color.pink, true, -1));

        // Test HTML/CSS style color
        for (int i = 0; i < 256; i++) {
            int c = i;
            if (i < 0x10) {
                c = i * 16;
            }
            String colorStr = "#" + Integer.toHexString(i) + Integer.toHexString(i) + Integer.toHexString(i);
            String colorStrAlpha = "#" + Integer.toHexString(i) + Integer.toHexString(i) + Integer.toHexString(i) + Integer.toHexString(i);
            assertEquals(new Color(c, c, c), StringUtil.toColor(colorStr));
            assertEquals(new Color(c, c, c, c), StringUtil.toColor(colorStrAlpha));

        }

        // Test null
        // TODO: Hmmm.. Maybe reconsider this..
        assertNull(StringUtil.toColor(null));

        // Test
        try {
            StringUtil.toColor("illegal-color-value");
            fail("toColor with illegal color value should throw IllegalArgumentException.");
        }
        catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testToColorString() {
        assertEquals("#ff0000", StringUtil.toColorString(Color.red));
        assertEquals("#00ff00", StringUtil.toColorString(Color.green));
        assertEquals("#0000ff", StringUtil.toColorString(Color.blue));
        assertEquals("#101010", StringUtil.toColorString(new Color(0x10, 0x10, 0x10)));

        for (int i = 0; i < 256; i++) {
            String str = (i < 0x10 ? "0" : "") + Integer.toHexString(i);
            assertEquals("#" + str + str + str, StringUtil.toColorString(new Color(i, i, i)));
        }

        // Test null
        // TODO: Hmmm.. Maybe reconsider this..
        assertNull(StringUtil.toColorString(null));
    }

    @Test
    public void testIsNumber() {
        assertTrue(StringUtil.isNumber("0"));
        assertTrue(StringUtil.isNumber("12345"));
        assertTrue(StringUtil.isNumber(TEST_INTEGER.toString()));
        assertTrue(StringUtil.isNumber("1234567890123456789012345678901234567890"));
        assertTrue(StringUtil.isNumber(String.valueOf(Long.MAX_VALUE) + String.valueOf(Long.MAX_VALUE)));
        assertFalse(StringUtil.isNumber("abc"));
        assertFalse(StringUtil.isNumber(TEST_STRING));
    }

    @Test
    public void testIsNumberNegative() {
        assertTrue(StringUtil.isNumber("-12345"));
        assertTrue(StringUtil.isNumber('-' + TEST_INTEGER.toString()));
        assertTrue(StringUtil.isNumber("-1234567890123456789012345678901234567890"));
        assertTrue(StringUtil.isNumber('-' + String.valueOf(Long.MAX_VALUE) + String.valueOf(Long.MAX_VALUE)));
        assertFalse(StringUtil.isNumber("-abc"));
        assertFalse(StringUtil.isNumber('-' + TEST_STRING));
    }

    @Test
    public void testCamelToLispNull() {
        try {
            StringUtil.camelToLisp(null);
            fail("should not accept null");
        }
        catch (IllegalArgumentException iae) {
            assertNotNull(iae.getMessage());
        }
    }

    @Test
    public void testCamelToLispNoConversion() {
        assertEquals("", StringUtil.camelToLisp(""));
        assertEquals("equal", StringUtil.camelToLisp("equal"));
        assertEquals("allready-lisp", StringUtil.camelToLisp("allready-lisp"));
    }

    @Test
    public void testCamelToLispSimple() {
        // Simple tests
        assertEquals("foo-bar", StringUtil.camelToLisp("fooBar"));
    }

    @Test
    public void testCamelToLispCase() {
        // Casing
        assertEquals("my-url", StringUtil.camelToLisp("myURL"));
        assertEquals("another-url", StringUtil.camelToLisp("AnotherURL"));
    }

    @Test
    public void testCamelToLispMulti() {
        // Several words
        assertEquals("http-request-wrapper", StringUtil.camelToLisp("HttpRequestWrapper"));
        String s = StringUtil.camelToLisp("HttpURLConnection");
        assertEquals("http-url-connection", s);
        // Long and short abbre in upper case
        assertEquals("welcome-to-my-world", StringUtil.camelToLisp("WELCOMEToMYWorld"));
    }

    @Test
    public void testCamelToLispLeaveUntouched() {
        // Leave others untouched
        assertEquals("a-slightly-longer-and-more-bumpy-string?.,[]()", StringUtil.camelToLisp("ASlightlyLongerANDMoreBumpyString?.,[]()"));
    }

    @Test
    public void testCamelToLispNumbers() {
        // Numbers
        // TODO: FixMe
        String s = StringUtil.camelToLisp("my45Caliber");
        assertEquals("my-45-caliber", s);
        assertEquals("hello-12345-world-67890", StringUtil.camelToLisp("Hello12345world67890"));
        assertEquals("hello-12345-my-world-67890-this-time", StringUtil.camelToLisp("HELLO12345MyWorld67890thisTime"));
        assertEquals("hello-12345-world-67890-too", StringUtil.camelToLisp("Hello12345WORLD67890too"));
    }

    @Test
    public void testLispToCamelNull() {
        try {
            StringUtil.lispToCamel(null);
            fail("should not accept null");
        }
        catch (IllegalArgumentException iae) {
            assertNotNull(iae.getMessage());
        }
    }

    @Test
    public void testLispToCamelNoConversion() {
        assertEquals("", StringUtil.lispToCamel(""));
        assertEquals("equal", StringUtil.lispToCamel("equal"));
        assertEquals("alreadyCamel", StringUtil.lispToCamel("alreadyCamel"));
    }

    @Test
    public void testLispToCamelSimple() {
        // Simple tests
        assertEquals("fooBar", StringUtil.lispToCamel("foo-bar"));
        assertEquals("myUrl", StringUtil.lispToCamel("my-URL"));
        assertEquals("anotherUrl", StringUtil.lispToCamel("ANOTHER-URL"));
    }

    @Test
    public void testLispToCamelCase() {
        // Casing
        assertEquals("Object", StringUtil.lispToCamel("object", true));
        assertEquals("object", StringUtil.lispToCamel("Object", false));
    }

    @Test
    public void testLispToCamelMulti() {
        // Several words
        assertEquals("HttpRequestWrapper", StringUtil.lispToCamel("http-request-wrapper", true));
    }

    @Test
    public void testLispToCamelLeaveUntouched() {
        // Leave others untouched
        assertEquals("ASlightlyLongerAndMoreBumpyString?.,[]()", StringUtil.lispToCamel("a-slightly-longer-and-more-bumpy-string?.,[]()", true));
    }

    @Test
    public void testLispToCamelNumber() {    
        // Numbers
        assertEquals("my45Caliber", StringUtil.lispToCamel("my-45-caliber"));
    }
}
