/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.lang;

import com.twelvemonkeys.util.StringTokenIterator;

import java.awt.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Pattern;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A utility class with some useful string manipulation methods.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <A href="mailto:eirik.torske@twelvemonkeys.com">Eirik Torske</A>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/StringUtil.java#2 $
 * @todo Consistency check: Method names, parameter sequence, Exceptions,
 * return values, null-value handling and parameter names (cosmetics).
 */
public final class StringUtil {

    /**
     * The default delimiter string, used by the {@code toXXXArray()}
     * methods.
     * Its value is {@code ",&nbsp;\t\n\r\f"}.
     * <!-- No, it IS actually ", \t\b\r\f", but &nbsp; looks better in a browser -->
     *
     * @see #toStringArray(String)
     * @see #toIntArray(String)
     * @see #toLongArray(String)
     * @see #toDoubleArray(String)
     */
    public final static String DELIMITER_STRING = ", \t\n\r\f";

    // Avoid constructor showing up in API doc
    private StringUtil() {
    }

    /**
     * Constructs a new {@link String} by decoding the specified sub array of bytes using the specified charset.
     * Replacement for {@link String#String(byte[], int, int, String) new String(byte[], int, int, String)}, that does
     * not throw the checked {@link UnsupportedEncodingException},
     * but instead the unchecked {@link UnsupportedCharsetException} if the character set is not supported.
     *
     * @param pData the bytes to be decoded to characters
     * @param pOffset the index of the first byte to decode
     * @param pLength the number of bytes to decode
     * @param pCharset the name of a supported character set
     * @return a newly created string.
     * @throws UnsupportedCharsetException
     *
     * @see String#String(byte[], int, int, String)
     */
    public static String decode(final byte[] pData, final int pOffset, final int pLength, final String pCharset) {
        try {
            return new String(pData, pOffset, pLength, pCharset);
        }
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedCharsetException(pCharset);
        }
    }

    /**
     * Returns the value of the given {@code Object}, as a {@code String}.
     * Unlike String.valueOf, this method returns {@code null}
     * instead of the {@code String} "null", if {@code null} is given as
     * the argument.
     *
     * @param pObj the Object to find the {@code String} value of.
     * @return the String value of the given object, or {@code null} if the
     *         {@code pObj} == {@code null}.
     * @see String#valueOf(Object)
     * @see String#toString()
     */
    public static String valueOf(Object pObj) {
        return ((pObj != null) ? pObj.toString() : null);
    }

    /**
     * Converts a string to uppercase.
     *
     * @param pString the string to convert
     * @return the string converted to uppercase, or null if the argument was
     *         null.
     */
    public static String toUpperCase(String pString) {
        if (pString != null) {
            return pString.toUpperCase();
        }
        return null;
    }

    /**
     * Converts a string to lowercase.
     *
     * @param pString the string to convert
     * @return the string converted to lowercase, or null if the argument was
     *         null.
     */
    public static String toLowerCase(String pString) {
        if (pString != null) {
            return pString.toLowerCase();
        }
        return null;
    }

    /**
     * Tests if a String is null, or contains nothing but white-space.
     *
     * @param pString The string to test
     * @return true if the string is null or contains only whitespace,
     *         otherwise false.
     */
    public static boolean isEmpty(String pString) {
        return ((pString == null) || (pString.trim().length() == 0));
    }

    /**
     * Tests a string array, to see if all items are null or an empty string.
     *
     * @param pStringArray The string array to check.
     * @return true if the string array is null or only contains string items
     *         that are null or contain only whitespace, otherwise false.
     */
    public static boolean isEmpty(String[] pStringArray) {
        // No elements to test
        if (pStringArray == null) {
            return true;
        }

        // Test all the elements
        for (String string : pStringArray) {
            if (!isEmpty(string)) {
                return false;
            }
        }

        // All elements are empty
        return true;
    }

    /**
     * Tests if a string contains another string.
     *
     * @param pContainer The string to test
     * @param pLookFor   The string to look for
     * @return {@code true} if the container string is contains the string, and
     *         both parameters are non-{@code null}, otherwise {@code false}.
     */
    public static boolean contains(String pContainer, String pLookFor) {
        return ((pContainer != null) && (pLookFor != null) && (pContainer.indexOf(pLookFor) >= 0));
    }

    /**
     * Tests if a string contains another string, ignoring case.
     *
     * @param pContainer The string to test
     * @param pLookFor   The string to look for
     * @return {@code true} if the container string is contains the string, and
     *         both parameters are non-{@code null}, otherwise {@code false}.
     * @see #contains(String,String)
     */
    public static boolean containsIgnoreCase(String pContainer, String pLookFor) {
        return indexOfIgnoreCase(pContainer, pLookFor, 0) >= 0;
    }

    /**
     * Tests if a string contains a specific character.
     *
     * @param pString The string to check.
     * @param pChar   The character to search for.
     * @return true    if the string contains the specific character.
     */
    public static boolean contains(final String pString, final int pChar) {
        return ((pString != null) && (pString.indexOf(pChar) >= 0));
    }

    /**
     * Tests if a string contains a specific character, ignoring case.
     *
     * @param pString The string to check.
     * @param pChar   The character to search for.
     * @return true    if the string contains the specific character.
     */
    public static boolean containsIgnoreCase(String pString, int pChar) {
        return ((pString != null)
                && ((pString.indexOf(Character.toLowerCase((char) pChar)) >= 0)
                || (pString.indexOf(Character.toUpperCase((char) pChar)) >= 0)));

        // NOTE: I don't convert the  string to uppercase, but instead test
        // the string (potentially) two times, as this is more efficient for
        // long strings (in most cases).
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring.
     *
     * @param pString  The string to test
     * @param pLookFor The string to look for
     * @return if the string argument occurs as a substring within this object,
     *         then the index of the first character of the first such substring is
     *         returned; if it does not occur as a substring, -1 is returned.
     * @see String#indexOf(String)
     */
    public static int indexOfIgnoreCase(String pString, String pLookFor) {
        return indexOfIgnoreCase(pString, pLookFor, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * @param pString  The string to test
     * @param pLookFor The string to look for
     * @param pPos     The first index to test
     * @return if the string argument occurs as a substring within this object,
     *         then the index of the first character of the first such substring is
     *         returned; if it does not occur as a substring, -1 is returned.
     * @see String#indexOf(String,int)
     */
    public static int indexOfIgnoreCase(String pString, String pLookFor, int pPos) {
        if ((pString == null) || (pLookFor == null)) {
            return -1;
        }
        if (pLookFor.length() == 0) {
            return pPos;// All strings "contains" the empty string
        }
        if (pLookFor.length() > pString.length()) {
            return -1;// Cannot contain string longer than itself
        }

        // Get first char
        char firstL = Character.toLowerCase(pLookFor.charAt(0));
        char firstU = Character.toUpperCase(pLookFor.charAt(0));
        int indexLower = 0;
        int indexUpper = 0;

        for (int i = pPos; i <= (pString.length() - pLookFor.length()); i++) {

            // Peek for first char
            indexLower = ((indexLower >= 0) && (indexLower <= i))
                    ? pString.indexOf(firstL, i)
                    : indexLower;
            indexUpper = ((indexUpper >= 0) && (indexUpper <= i))
                    ? pString.indexOf(firstU, i)
                    : indexUpper;
            if (indexLower < 0) {
                if (indexUpper < 0) {
                    return -1;// First char not found
                }
                else {
                    i = indexUpper;// Only upper
                }
            }
            else if (indexUpper < 0) {
                i = indexLower;// Only lower
            }
            else {

                // Both found, select first occurence
                i = (indexLower < indexUpper)
                        ? indexLower
                        : indexUpper;
            }

            // Only one?
            if (pLookFor.length() == 1) {
                return i;// The only char found!
            }

            // Test if we still have enough chars
            else if (i > (pString.length() - pLookFor.length())) {
                return -1;
            }

            // Test if last char equals! (regionMatches is expensive)
            else if ((pString.charAt(i + pLookFor.length() - 1) != Character.toLowerCase(pLookFor.charAt(pLookFor.length() - 1)))
                    && (pString.charAt(i + pLookFor.length() - 1) != Character.toUpperCase(pLookFor.charAt(pLookFor.length() - 1)))) {
                continue;// Nope, try next
            }

            // Test from second char, until second-last char
            else if ((pLookFor.length() <= 2) || pString.regionMatches(true, i + 1, pLookFor, 1, pLookFor.length() - 2)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index within this string of the rightmost occurrence of the
     * specified substring. The rightmost empty string "" is considered to
     * occur at the index value {@code pString.length() - 1}.
     *
     * @param pString  The string to test
     * @param pLookFor The string to look for
     * @return If the string argument occurs one or more times as a substring
     *         within this object at a starting index no greater than fromIndex, then
     *         the index of the first character of the last such substring is returned.
     *         If it does not occur as a substring starting at fromIndex or earlier, -1
     *         is returned.
     * @see String#lastIndexOf(String)
     */
    public static int lastIndexOfIgnoreCase(String pString, String pLookFor) {
        return lastIndexOfIgnoreCase(pString, pLookFor, pString != null ? pString.length() - 1 : -1);
    }

    /**
     * Returns the index within this string of the rightmost occurrence of the
     * specified substring. The rightmost empty string "" is considered to
     * occur at the index value {@code pPos}
     *
     * @param pString  The string to test
     * @param pLookFor The string to look for
     * @param pPos     The last index to test
     * @return If the string argument occurs one or more times as a substring
     *         within this object at a starting index no greater than fromIndex, then
     *         the index of the first character of the last such substring is returned.
     *         If it does not occur as a substring starting at fromIndex or earlier, -1
     *         is returned.
     * @see String#lastIndexOf(String,int)
     */
    public static int lastIndexOfIgnoreCase(String pString, String pLookFor, int pPos) {
        if ((pString == null) || (pLookFor == null)) {
            return -1;
        }
        if (pLookFor.length() == 0) {
            return pPos;// All strings "contains" the empty string
        }
        if (pLookFor.length() > pString.length()) {
            return -1;// Cannot contain string longer than itself
        }

        // Get first char
        char firstL = Character.toLowerCase(pLookFor.charAt(0));
        char firstU = Character.toUpperCase(pLookFor.charAt(0));
        int indexLower = pPos;
        int indexUpper = pPos;

        for (int i = pPos; i >= 0; i--) {

            // Peek for first char
            indexLower = ((indexLower >= 0) && (indexLower >= i))
                    ? pString.lastIndexOf(firstL, i)
                    : indexLower;
            indexUpper = ((indexUpper >= 0) && (indexUpper >= i))
                    ? pString.lastIndexOf(firstU, i)
                    : indexUpper;
            if (indexLower < 0) {
                if (indexUpper < 0) {
                    return -1;// First char not found
                }
                else {
                    i = indexUpper;// Only upper
                }
            }
            else if (indexUpper < 0) {
                i = indexLower;// Only lower
            }
            else {

                // Both found, select last occurence
                i = (indexLower > indexUpper)
                        ? indexLower
                        : indexUpper;
            }

            // Only one?
            if (pLookFor.length() == 1) {
                return i;// The only char found!
            }

            // Test if we still have enough chars
            else if (i > (pString.length() - pLookFor.length())) {
                //return -1;
                continue;
            }

            // Test if last char equals! (regionMatches is expensive)
            else
            if ((pString.charAt(i + pLookFor.length() - 1) != Character.toLowerCase(pLookFor.charAt(pLookFor.length() - 1)))
                    && (pString.charAt(i + pLookFor.length() - 1) != Character.toUpperCase(pLookFor.charAt(pLookFor.length() - 1)))) {
                continue;// Nope, try next
            }

            // Test from second char, until second-last char
            else
            if ((pLookFor.length() <= 2) || pString.regionMatches(true, i + 1, pLookFor, 1, pLookFor.length() - 2)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified character.
     *
     * @param pString The string to test
     * @param pChar   The character to look for
     * @return if the string argument occurs as a substring within this object,
     *         then the index of the first character of the first such substring is
     *         returned; if it does not occur as a substring, -1 is returned.
     * @see String#indexOf(int)
     */
    public static int indexOfIgnoreCase(String pString, int pChar) {
        return indexOfIgnoreCase(pString, pChar, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified character, starting at the specified index.
     *
     * @param pString The string to test
     * @param pChar   The character to look for
     * @param pPos    The first index to test
     * @return if the string argument occurs as a substring within this object,
     *         then the index of the first character of the first such substring is
     *         returned; if it does not occur as a substring, -1 is returned.
     * @see String#indexOf(int,int)
     */
    public static int indexOfIgnoreCase(String pString, int pChar, int pPos) {
        if ((pString == null)) {
            return -1;
        }

        // Get first char
        char lower = Character.toLowerCase((char) pChar);
        char upper = Character.toUpperCase((char) pChar);
        int indexLower;
        int indexUpper;

        // Test for char
        indexLower = pString.indexOf(lower, pPos);
        indexUpper = pString.indexOf(upper, pPos);
        if (indexLower < 0) {

            /*      if (indexUpper < 0)
                    return -1; // First char not found
                    else */
            return indexUpper;// Only upper
        }
        else if (indexUpper < 0) {
            return indexLower;// Only lower
        }
        else {

            // Both found, select first occurence
            return (indexLower < indexUpper)
                    ? indexLower
                    : indexUpper;
        }
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified character.
     *
     * @param pString The string to test
     * @param pChar   The character to look for
     * @return if the string argument occurs as a substring within this object,
     *         then the index of the first character of the first such substring is
     *         returned; if it does not occur as a substring, -1 is returned.
     * @see String#lastIndexOf(int)
     */
    public static int lastIndexOfIgnoreCase(String pString, int pChar) {
        return lastIndexOfIgnoreCase(pString, pChar, pString != null ? pString.length() : -1);
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified character, searching backward starting at the specified index.
     *
     * @param pString The string to test
     * @param pChar   The character to look for
     * @param pPos    The last index to test
     * @return if the string argument occurs as a substring within this object,
     *         then the index of the first character of the first such substring is
     *         returned; if it does not occur as a substring, -1 is returned.
     * @see String#lastIndexOf(int,int)
     */
    public static int lastIndexOfIgnoreCase(String pString, int pChar, int pPos) {
        if ((pString == null)) {
            return -1;
        }

        // Get first char
        char lower = Character.toLowerCase((char) pChar);
        char upper = Character.toUpperCase((char) pChar);
        int indexLower;
        int indexUpper;

        // Test for char
        indexLower = pString.lastIndexOf(lower, pPos);
        indexUpper = pString.lastIndexOf(upper, pPos);
        if (indexLower < 0) {

            /*      if (indexUpper < 0)
                    return -1; // First char not found
                    else */
            return indexUpper;// Only upper
        }
        else if (indexUpper < 0) {
            return indexLower;// Only lower
        }
        else {

            // Both found, select last occurence
            return (indexLower > indexUpper)
                    ? indexLower
                    : indexUpper;
        }
    }

    /**
     * Trims the argument string for whitespace on the left side only.
     *
     * @param pString the string to trim
     * @return the string with no whitespace on the left, or {@code null} if
     *         the string argument is {@code null}.
     * @see #rtrim
     * @see String#trim()
     */
    public static String ltrim(String pString) {
        if ((pString == null) || (pString.length() == 0)) {
            return pString;// Null or empty string
        }
        for (int i = 0; i < pString.length(); i++) {
            if (!Character.isWhitespace(pString.charAt(i))) {
                if (i == 0) {
                    return pString;// First char is not whitespace
                }
                else {
                    return pString.substring(i);// Return rest after whitespace
                }
            }
        }

        // If all whitespace, return empty string
        return "";
    }

    /**
     * Trims the argument string for whitespace on the right side only.
     *
     * @param pString the string to trim
     * @return the string with no whitespace on the right, or {@code null} if
     *         the string argument is {@code null}.
     * @see #ltrim
     * @see String#trim()
     */
    public static String rtrim(String pString) {
        if ((pString == null) || (pString.length() == 0)) {
            return pString;// Null or empty string
        }
        for (int i = pString.length(); i > 0; i--) {
            if (!Character.isWhitespace(pString.charAt(i - 1))) {
                if (i == pString.length()) {
                    return pString;// First char is not whitespace
                }
                else {
                    return pString.substring(0, i);// Return before whitespace
                }
            }
        }

        // If all whitespace, return empty string
        return "";
    }

    /**
     * Replaces a substring of a string with another string. All matches are
     * replaced.
     *
     * @param pSource  The source String
     * @param pPattern The pattern to replace
     * @param pReplace The new String to be inserted instead of the
     *                 replace String
     * @return The new String with the pattern replaced
     */
    public static String replace(String pSource, String pPattern, String pReplace) {
        if (pPattern.length() == 0) {
            return pSource;// Special case: No pattern to replace
        }

        int match;
        int offset = 0;
        StringBuilder result = new StringBuilder();

        // Loop string, until last occurence of pattern, and replace
        while ((match = pSource.indexOf(pPattern, offset)) != -1) {
            // Append everything until pattern
            result.append(pSource.substring(offset, match));
            // Append the replace string
            result.append(pReplace);
            offset = match + pPattern.length();
        }

        // Append rest of string and return
        result.append(pSource.substring(offset));

        return result.toString();
    }

    /**
     * Replaces a substring of a string with another string, ignoring case.
     * All matches are replaced.
     *
     * @param pSource  The source String
     * @param pPattern The pattern to replace
     * @param pReplace The new String to be inserted instead of the
     *                 replace String
     * @return The new String with the pattern replaced
     * @see #replace(String,String,String)
     */
    public static String replaceIgnoreCase(String pSource, String pPattern, String pReplace) {
        if (pPattern.length() == 0) {
            return pSource;// Special case: No pattern to replace
        }
        int match;
        int offset = 0;
        StringBuilder result = new StringBuilder();

        while ((match = indexOfIgnoreCase(pSource, pPattern, offset)) != -1) {
            result.append(pSource.substring(offset, match));
            result.append(pReplace);
            offset = match + pPattern.length();
        }
        result.append(pSource.substring(offset));
        return result.toString();
    }

    /**
     * Cuts a string between two words, before a sepcified length, if the
     * string is longer than the maxium lenght. The string is optionally padded
     * with the pad  argument. The method assumes words to be separated by the
     * space character (" ").
     * Note that the maximum length argument is absolute, and will also include
     * the length of the padding.
     *
     * @param pString The string to cut
     * @param pMaxLen The maximum length before cutting
     * @param pPad    The string to append at the end, aftrer cutting
     * @return The cutted string with padding, or the original string, if it
     *         was shorter than the max length.
     * @see #pad(String,int,String,boolean)
     */
    public static String cut(String pString, int pMaxLen, String pPad) {
        if (pString == null) {
            return null;
        }
        if (pPad == null) {
            pPad = "";
        }
        int len = pString.length();

        if (len > pMaxLen) {
            len = pString.lastIndexOf(' ', pMaxLen - pPad.length());
        }
        else {
            return pString;
        }
        return pString.substring(0, len) + pPad;
    }

    /**
     * Makes the Nth letter of a String uppercase. If the index is outside the
     * the length of the argument string, the argument is simply returned.
     *
     * @param pString The string to capitalize
     * @param pIndex  The base-0 index of the char to capitalize.
     * @return The capitalized string, or null, if a null argument was given.
     */
    public static String capitalize(String pString, int pIndex) {
        if (pIndex < 0) {
            throw new IndexOutOfBoundsException("Negative index not allowed: " + pIndex);
        }
        if (pString == null || pString.length() <= pIndex) {
            return pString;
        }

        // This is the fastest method, according to my tests

        // Skip array duplication if allready capitalized
        if (Character.isUpperCase(pString.charAt(pIndex))) {
            return pString;
        }

        // Convert to char array, capitalize and create new String
        char[] charArray = pString.toCharArray();
        charArray[pIndex] = Character.toUpperCase(charArray[pIndex]);
        return new String(charArray);

        /**
         StringBuilder buf = new StringBuilder(pString);
         buf.setCharAt(pIndex, Character.toUpperCase(buf.charAt(pIndex)));
         return buf.toString();
         //*/

        /**
         return pString.substring(0, pIndex)
         +  Character.toUpperCase(pString.charAt(pIndex))
         + pString.substring(pIndex + 1);
         //*/
    }

    /**
     * Makes the first letter of a String uppercase.
     *
     * @param pString The string to capitalize
     * @return The capitalized string, or null, if a null argument was given.
     */
    public static String capitalize(String pString) {
        return capitalize(pString, 0);
    }

    /**
     * Formats a number with leading zeroes, to a specified length.
     *
     * @param pNum The number to format
     * @param pLen The number of digits
     * @return A string containing the formatted number
     * @throws IllegalArgumentException Thrown, if the number contains
     *                                  more digits than allowed by the length argument.
     * @see #pad(String,int,String,boolean)
     * @deprecated Use StringUtil.pad instead!
     */

    /*public*/
    static String formatNumber(long pNum, int pLen) throws IllegalArgumentException {
        StringBuilder result = new StringBuilder();

        if (pNum >= Math.pow(10, pLen)) {
            throw new IllegalArgumentException("The number to format cannot contain more digits than the length argument specifies!");
        }
        for (int i = pLen; i > 1; i--) {
            if (pNum < Math.pow(10, i - 1)) {
                result.append('0');
            }
            else {
                break;
            }
        }
        result.append(pNum);
        return result.toString();
    }

    /**
     * String length check with simple concatenation of selected pad-string.
     * E.g. a zip number from 123 to the correct 0123.
     *
     * @param pSource         The source string.
     * @param pRequiredLength The accurate length of the resulting string.
     * @param pPadString      The string for concatenation.
     * @param pPrepend        The location of fill-ins, prepend (true),
     *                        or append (false)
     * @return a concatenated string.
     * @todo What if source is allready longer than required length?
     * @todo Consistency with cut
     * @see #cut(String,int,String)
     */
    public static String pad(String pSource, int pRequiredLength, String pPadString, boolean pPrepend) {
        if (pPadString == null || pPadString.length() == 0) {
            throw new IllegalArgumentException("Pad string: \"" + pPadString + "\"");
        }

        if (pSource.length() >= pRequiredLength) {
            return pSource;
        }

        // TODO: Benchmark the new version against the old one, to see if it's really faster
        // Rewrite to first create pad
        // - pad += pad; - until length is >= gap
        // then append the pad and cut if too long
        int gap = pRequiredLength - pSource.length();
        StringBuilder result = new StringBuilder(pPadString);
        while (result.length() < gap) {
            result.append(result);
        }

        if (result.length() > gap) {
            result.delete(gap, result.length());
        }

        return pPrepend ? result.append(pSource).toString() : result.insert(0, pSource).toString();

        /*
        StringBuilder result = new StringBuilder(pSource);

        // Concatenation until proper string length
        while (result.length() < pRequiredLength) {
            // Prepend or append
            if (pPrepend) {  // Front
                result.insert(0, pPadString);
            }
            else {         // Back
                result.append(pPadString);
            }
        }

        // Truncate
        if (result.length() > pRequiredLength) {
            if (pPrepend) {
                result.delete(0, result.length() - pRequiredLength);
            }
            else {
                result.delete(pRequiredLength, result.length());
            }
        }
        return result.toString();
        */
    }

    /**
     * Converts the string to a date, using the default date format.
     *
     * @param pString the string to convert
     * @return the date
     * @see DateFormat
     * @see DateFormat#getInstance()
     */
    public static Date toDate(String pString) {
        // Default
        return toDate(pString, DateFormat.getInstance());
    }

    /**
     * Converts the string to a date, using the given format.
     *
     * @param pString the string to convert
     * @param pFormat the date format
     * @return the date
     * @todo cache formats?
     * @see java.text.SimpleDateFormat
     * @see java.text.SimpleDateFormat#SimpleDateFormat(String)
     */
    public static Date toDate(String pString, String pFormat) {
        // Get the format from cache, or create new and insert
        // Return new date
        return toDate(pString, new SimpleDateFormat(pFormat));
    }

    /**
     * Converts the string to a date, using the given format.
     *
     * @param pString the string to convert
     * @param pFormat the date format
     * @return the date
     * @see SimpleDateFormat
     * @see SimpleDateFormat#SimpleDateFormat(String)
     * @see DateFormat
     */
    public static Date toDate(final String pString, final DateFormat pFormat) {
        try {
            synchronized (pFormat) {
                // Parse date using given format
                return pFormat.parse(pString);
            }
        }
        catch (ParseException pe) {
            // Wrap in RuntimeException
            throw new IllegalArgumentException(pe.getMessage());
        }
    }

    /**
     * Converts the string to a jdbc Timestamp, using the standard Timestamp
     * escape format.
     *
     * @param pValue the value
     * @return a new {@code Timestamp}
     * @see java.sql.Timestamp
     * @see java.sql.Timestamp#valueOf(String)
     */
    public static Timestamp toTimestamp(final String pValue) {
        // Parse date using default format
        return Timestamp.valueOf(pValue);
    }

    /**
     * Converts a delimiter separated String to an array of Strings.
     *
     * @param pString     The comma-separated string
     * @param pDelimiters The delimiter string
     * @return a {@code String} array containing the delimiter separated elements
     */
    public static String[] toStringArray(String pString, String pDelimiters) {
        if (isEmpty(pString)) {
            return new String[0];
        }

        StringTokenIterator st = new StringTokenIterator(pString, pDelimiters);
        List<String> v = new ArrayList<String>();

        while (st.hasMoreElements()) {
            v.add(st.nextToken());
        }

        return v.toArray(new String[v.size()]);
    }

    /**
     * Converts a comma-separated String to an array of Strings.
     *
     * @param pString The comma-separated string
     * @return a {@code String} array containing the comma-separated elements
     * @see #toStringArray(String,String)
     */
    public static String[] toStringArray(String pString) {
        return toStringArray(pString, DELIMITER_STRING);
    }

    /**
     * Converts a comma-separated String to an array of ints.
     *
     * @param pString     The comma-separated string
     * @param pDelimiters The delimiter string
     * @param pBase       The radix
     * @return an {@code int} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as an int
     */
    public static int[] toIntArray(String pString, String pDelimiters, int pBase) {
        if (isEmpty(pString)) {
            return new int[0];
        }

        // Some room for improvement here...
        String[] temp = toStringArray(pString, pDelimiters);
        int[] array = new int[temp.length];

        for (int i = 0; i < array.length; i++) {
            array[i] = Integer.parseInt(temp[i], pBase);
        }
        return array;
    }

    /**
     * Converts a comma-separated String to an array of ints.
     *
     * @param pString The comma-separated string
     * @return an {@code int} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as an int
     * @see #toStringArray(String,String)
     * @see #DELIMITER_STRING
     */
    public static int[] toIntArray(String pString) {
        return toIntArray(pString, DELIMITER_STRING, 10);
    }

    /**
     * Converts a comma-separated String to an array of ints.
     *
     * @param pString     The comma-separated string
     * @param pDelimiters The delimiter string
     * @return an {@code int} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as an int
     * @see #toIntArray(String,String)
     */
    public static int[] toIntArray(String pString, String pDelimiters) {
        return toIntArray(pString, pDelimiters, 10);
    }

    /**
     * Converts a comma-separated String to an array of longs.
     *
     * @param pString     The comma-separated string
     * @param pDelimiters The delimiter string
     * @return a {@code long} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as a long
     */
    public static long[] toLongArray(String pString, String pDelimiters) {
        if (isEmpty(pString)) {
            return new long[0];
        }

        // Some room for improvement here...
        String[] temp = toStringArray(pString, pDelimiters);
        long[] array = new long[temp.length];

        for (int i = 0; i < array.length; i++) {
            array[i] = Long.parseLong(temp[i]);
        }
        return array;
    }

    /**
     * Converts a comma-separated String to an array of longs.
     *
     * @param pString The comma-separated string
     * @return a {@code long} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as a long
     * @see #toStringArray(String,String)
     * @see #DELIMITER_STRING
     */
    public static long[] toLongArray(String pString) {
        return toLongArray(pString, DELIMITER_STRING);
    }

    /**
     * Converts a comma-separated String to an array of doubles.
     *
     * @param pString     The comma-separated string
     * @param pDelimiters The delimiter string
     * @return a {@code double} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as a double
     */
    public static double[] toDoubleArray(String pString, String pDelimiters) {
        if (isEmpty(pString)) {
            return new double[0];
        }

        // Some room for improvement here...
        String[] temp = toStringArray(pString, pDelimiters);
        double[] array = new double[temp.length];

        for (int i = 0; i < array.length; i++) {
            array[i] = Double.valueOf(temp[i]);

            // Double.parseDouble() is 1.2...
        }
        return array;
    }

    /**
     * Converts a comma-separated String to an array of doubles.
     *
     * @param pString The comma-separated string
     * @return a {@code double} array
     * @throws NumberFormatException if any of the elements are not parseable
     *                               as a double
     * @see #toDoubleArray(String,String)
     * @see #DELIMITER_STRING
     */
    public static double[] toDoubleArray(String pString) {
        return toDoubleArray(pString, DELIMITER_STRING);
    }

    /**
     * Parses a string to a Color.
     * The argument can be a color constant (static constant from
     * {@link java.awt.Color java.awt.Color}), like {@code black} or
     * {@code red}, or it can be HTML/CSS-style, on the format:
     * <UL>
     * <LI>{@code #RRGGBB}, where RR, GG and BB means two digit
     * hexadecimal for red, green and blue values respectively.</LI>
     * <LI>{@code #AARRGGBB}, as above, with AA as alpha component.</LI>
     * <LI>{@code #RGB}, where R, G and B means one digit
     * hexadecimal for red, green and blue values respectively.</LI>
     * <LI>{@code #ARGB}, as above, with A as alpha component.</LI>
     * </UL>
     *
     * @param pString the string representation of the color
     * @return the {@code Color} object, or {@code null} if the argument
     *         is {@code null}
     * @throws IllegalArgumentException if the string does not map to a color.
     * @see java.awt.Color
     */
    public static Color toColor(String pString) {
        // No string, no color
        if (pString == null) {
            return null;
        }

        // #RRGGBB format
        if (pString.charAt(0) == '#') {
            int r = 0;
            int g = 0;
            int b = 0;
            int a = -1;// alpha

            if (pString.length() >= 7) {
                int idx = 1;

                // AA
                if (pString.length() >= 9) {
                    a = Integer.parseInt(pString.substring(idx, idx + 2), 0x10);
                    idx += 2;
                }
                // RR GG BB
                r = Integer.parseInt(pString.substring(idx, idx + 2), 0x10);
                g = Integer.parseInt(pString.substring(idx + 2, idx + 4), 0x10);
                b = Integer.parseInt(pString.substring(idx + 4, idx + 6), 0x10);

            }
            else if (pString.length() >= 4) {
                int idx = 1;

                // A
                if (pString.length() >= 5) {
                    a = Integer.parseInt(pString.substring(idx, ++idx), 0x10) * 0x10;
                }
                // R G B
                r = Integer.parseInt(pString.substring(idx, ++idx), 0x10) * 0x10;
                g = Integer.parseInt(pString.substring(idx, ++idx), 0x10) * 0x10;
                b = Integer.parseInt(pString.substring(idx, ++idx), 0x10) * 0x10;
            }
            if (a != -1) {
                // With alpha
                return new Color(r, g, b, a);
            }

            // No alpha
            return new Color(r, g, b);
        }

        // Get color by name
        try {
            Class colorClass = Color.class;
            Field field = null;

            // Workaround for stupidity in Color class constant field names
            try {
                field = colorClass.getField(pString);
            }
            catch (Exception e) {
                // Don't care, this is just a workaround...
            }
            if (field == null) {
                // NOTE: The toLowerCase() on the next line will lose darkGray
                // and lightGray...
                field = colorClass.getField(pString.toLowerCase());
            }

            // Only try to get public final fields
            int mod = field.getModifiers();

            if (Modifier.isPublic(mod) && Modifier.isStatic(mod)) {
                return (Color) field.get(null);
            }
        }
        catch (NoSuchFieldException nsfe) {
            // No such color, throw illegal argument?
            throw new IllegalArgumentException("No such color: " + pString);
        }
        catch (SecurityException se) {
            // Can't access field, return null
        }
        catch (IllegalAccessException iae) {
            // Can't happen, as the field must be public static
        }
        catch (IllegalArgumentException iar) {
            // Can't happen, as the field must be static
        }

        // This should never be reached, but you never know... ;-)
        return null;
    }

    /**
     * Creates a HTML/CSS String representation of the given color.
     * The HTML/CSS color format is defined as:
     * <UL>
     * <LI>{@code #RRGGBB}, where RR, GG and BB means two digit
     * hexadecimal for red, green and blue values respectively.</LI>
     * <LI>{@code #AARRGGBB}, as above, with AA as alpha component.</LI>
     * </UL>
     * <p/>
     * Examlples: {@code toColorString(Color.red) == "#ff0000"},
     * {@code toColorString(new Color(0xcc, 0xcc, 0xcc)) == "#cccccc"}.
     *
     * @param pColor the color
     * @return A String representation of the color on HTML/CSS form
     * @todo Consider moving to ImageUtil?
     */
    public static String toColorString(Color pColor) {
        // Not a color...
        if (pColor == null) {
            return null;
        }

        StringBuilder str = new StringBuilder(Integer.toHexString(pColor.getRGB()));

        // Make sure string is 8 chars
        for (int i = str.length(); i < 8; i++) {
            str.insert(0, '0');
        }

        // All opaque is default
        if (str.charAt(0) == 'f' && str.charAt(1) == 'f') {
            str.delete(0, 2);
        }

        // Prepend hash
        return str.insert(0, '#').toString();
    }

    /**
     * Tests a string, to see if it is an number (element of <b>Z</b>).
     * Valid integers are positive natural numbers (1, 2, 3, ...),
     * their negatives (?1, ?2, ?3, ...) and the number zero.
     * <p/>
     * Note that there is no guarantees made, that this number can be
     * represented as either an int or a long.
     *
     * @param pString The string to check.
     * @return true if the String is a natural number.
     */
    public static boolean isNumber(String pString) {
        if (isEmpty(pString)) {
            return false;
        }

        // Special case for first char, may be minus sign ('-')
        char ch = pString.charAt(0);
        if (!(ch == '-' || Character.isDigit(ch))) {
            return false;
        }

        // Test every char
        for (int i = 1; i < pString.length(); i++) {
            if (!Character.isDigit(pString.charAt(i))) {
                return false;
            }
        }

        // All digits must be a natural number
        return true;
    }

    /*
     * This version is benchmarked against toStringArray and found to be
     * increasingly slower, the more elements the string contains.
     * Kept here
     */

    /**
     * Removes all occurences of a specific character in a string.
     * <i>This method is not design for efficiency!</i>
     * <p>
     *
     * @param pSource
     * @param pSubstring
     * @param pPosition
     * @return the modified string.
     */

    /*
      public static String removeChar(String pSourceString, final char pBadChar) {

      char[] sourceCharArray = pSourceString.toCharArray();
      List modifiedCharList = new Vector(sourceCharArray.length, 1);

      // Filter the string
      for (int i = 0; i < sourceCharArray.length; i++) {
      if (sourceCharArray[i] != pBadChar) {
      modifiedCharList.add(new Character(sourceCharArray[i]));
      }
      }

      // Clean the character list
      modifiedCharList = (List) CollectionUtil.purifyCollection((Collection) modifiedCharList);

      // Create new modified String
      char[] modifiedCharArray = new char[modifiedCharList.size()];
      for (int i = 0; i < modifiedCharArray.length; i++) {
      modifiedCharArray[i] = ((Character) modifiedCharList.get(i)).charValue();
      }

      return new String(modifiedCharArray);
      }
    */

    /**
     *
     * <i>This method is not design for efficiency!</i>
     * <p>
     * @param pSourceString The String for modification.
     * @param pBadChars The char array containing the characters to remove from the source string.
     * @return the modified string.
     * @-deprecated Not tested yet!
     *
     */

    /*
      public static String removeChars(String pSourceString, final char[] pBadChars) {

      char[] sourceCharArray = pSourceString.toCharArray();
      List modifiedCharList = new Vector(sourceCharArray.length, 1);

      Map badCharMap = new Hashtable();
      Character dummyChar = new Character('*');
      for (int i = 0; i < pBadChars.length; i++) {
      badCharMap.put(new Character(pBadChars[i]), dummyChar);
      }

      // Filter the string
      for (int i = 0; i < sourceCharArray.length; i++) {
      Character arrayChar = new Character(sourceCharArray[i]);
      if (!badCharMap.containsKey(arrayChar)) {
      modifiedCharList.add(new Character(sourceCharArray[i]));
      }
      }

      // Clean the character list
      modifiedCharList = (List) CollectionUtil.purifyCollection((Collection) modifiedCharList);

      // Create new modified String
      char[] modifiedCharArray = new char[modifiedCharList.size()];
      for (int i = 0; i < modifiedCharArray.length; i++) {
      modifiedCharArray[i] = ((Character) modifiedCharList.get(i)).charValue();
      }

      return new String(modifiedCharArray);

      }
    */

    /**
     * Ensures that a string includes a given substring at a given position.
     * <p/>
     * Extends the string with a given string if it is not already there.
     * E.g an URL "www.vg.no", to "http://www.vg.no".
     *
     * @param pSource    The source string.
     * @param pSubstring The substring to include.
     * @param pPosition  The location of the fill-in, the index starts with 0.
     * @return the string, with the substring at the given location.
     */
    static String ensureIncludesAt(String pSource, String pSubstring, int pPosition) {
        StringBuilder newString = new StringBuilder(pSource);

        try {
            String existingSubstring = pSource.substring(pPosition, pPosition + pSubstring.length());

            if (!existingSubstring.equalsIgnoreCase(pSubstring)) {
                newString.insert(pPosition, pSubstring);
            }
        }
        catch (Exception e) {
            // Do something!?
        }
        return newString.toString();
    }

    /**
     * Ensures that a string does not include a given substring at a given
     * position.
     * <p/>
     * Removes a given substring from a string if it is there.
     * E.g an URL "http://www.vg.no", to "www.vg.no".
     *
     * @param pSource    The source string.
     * @param pSubstring The substring to check and possibly remove.
     * @param pPosition  The location of possible substring removal, the index starts with 0.
     * @return the string, without the substring at the given location.
     */
    static String ensureExcludesAt(String pSource, String pSubstring, int pPosition) {
        StringBuilder newString = new StringBuilder(pSource);

        try {
            String existingString = pSource.substring(pPosition + 1, pPosition + pSubstring.length() + 1);

            if (!existingString.equalsIgnoreCase(pSubstring)) {
                newString.delete(pPosition, pPosition + pSubstring.length());
            }
        }
        catch (Exception e) {
            // Do something!?
        }
        return newString.toString();
    }

    /**
     * Gets the first substring between the given string boundaries.
     * <p/>
     *
     * @param pSource              The source string.
     * @param pBeginBoundaryString The string that marks the beginning.
     * @param pEndBoundaryString   The string that marks the end.
     * @param pOffset              The index to start searching in the source
     *                             string. If it is less than 0, the index will be set to 0.
     * @return the substring demarcated by the given string boundaries or null
     *         if not both string boundaries are found.
     */
    public static String substring(final String pSource, final String pBeginBoundaryString, final String pEndBoundaryString,
                                   final int pOffset) {
        // Check offset
        int offset = (pOffset < 0)
                ? 0
                : pOffset;

        // Find the start index
        int startIndex = pSource.indexOf(pBeginBoundaryString, offset) + pBeginBoundaryString.length();

        if (startIndex < 0) {
            return null;
        }

        // Find the end index
        int endIndex = pSource.indexOf(pEndBoundaryString, startIndex);

        if (endIndex < 0) {
            return null;
        }
        return pSource.substring(startIndex, endIndex);
    }

    /**
     * Removes the first substring demarcated by the given string boundaries.
     * <p/>
     *
     * @param pSource            The source string.
     * @param pBeginBoundaryChar The character that marks the beginning of the
     *                           unwanted substring.
     * @param pEndBoundaryChar   The character that marks the end of the
     *                           unwanted substring.
     * @param pOffset            The index to start searching in the source
     *                           string. If it is less than 0, the index will be set to 0.
     * @return the source string with all the demarcated substrings removed,
     *         included the demarcation characters.
     * @deprecated this method actually removes all demarcated substring.. doesn't it?
     */

    /*public*/
    static String removeSubstring(final String pSource, final char pBeginBoundaryChar, final char pEndBoundaryChar, final int pOffset) {
        StringBuilder filteredString = new StringBuilder();
        boolean insideDemarcatedArea = false;
        char[] charArray = pSource.toCharArray();

        for (char c : charArray) {
            if (!insideDemarcatedArea) {
                if (c == pBeginBoundaryChar) {
                    insideDemarcatedArea = true;
                }
                else {
                    filteredString.append(c);
                }
            }
            else {
                if (c == pEndBoundaryChar) {
                    insideDemarcatedArea = false;
                }
            }
        }
        return filteredString.toString();
    }

    /**
     * Removes all substrings demarcated by the given string boundaries.
     * <p/>
     *
     * @param pSource            The source string.
     * @param pBeginBoundaryChar The character that marks the beginning of the unwanted substring.
     * @param pEndBoundaryChar   The character that marks the end of the unwanted substring.
     * @return the source string with all the demarcated substrings removed, included the demarcation characters.
     */
    /*public*/
    static String removeSubstrings(final String pSource, final char pBeginBoundaryChar, final char pEndBoundaryChar) {
        StringBuilder filteredString = new StringBuilder();
        boolean insideDemarcatedArea = false;
        char[] charArray = pSource.toCharArray();

        for (char c : charArray) {
            if (!insideDemarcatedArea) {
                if (c == pBeginBoundaryChar) {
                    insideDemarcatedArea = true;
                }
                else {
                    filteredString.append(c);
                }
            }
            else {
                if (c == pEndBoundaryChar) {
                    insideDemarcatedArea = false;
                }
            }
        }
        return filteredString.toString();
    }

    /**
     * Gets the first element of a {@code String} containing string elements delimited by a given delimiter.
     * <i>NB - Straightforward implementation!</i>
     * <p/>
     *
     * @param pSource    The source string.
     * @param pDelimiter The delimiter used in the source string.
     * @return The last string element.
     * @todo This method should be re-implemented for more efficient execution.
     */
    public static String getFirstElement(final String pSource, final String pDelimiter) {
        if (pDelimiter == null) {
            throw new IllegalArgumentException("delimiter == null");
        }

        if (StringUtil.isEmpty(pSource)) {
            return pSource;
        }

        int idx = pSource.indexOf(pDelimiter);
        if (idx >= 0) {
            return pSource.substring(0, idx);
        }
        return pSource;
    }

    /**
     * Gets the last element of a {@code String} containing string elements
     * delimited by a given delimiter.
     * <i>NB - Straightforward implementation!</i>
     * <p/>
     *
     * @param pSource    The source string.
     * @param pDelimiter The delimiter used in the source string.
     * @return The last string element.
     */
    public static String getLastElement(final String pSource, final String pDelimiter) {
        if (pDelimiter == null) {
            throw new IllegalArgumentException("delimiter == null");
        }

        if (StringUtil.isEmpty(pSource)) {
            return pSource;
        }
        int idx = pSource.lastIndexOf(pDelimiter);
        if (idx >= 0) {
            return pSource.substring(idx + 1);
        }
        return pSource;
    }

    /**
     * Converts a string array to a string of comma-separated values.
     *
     * @param pStringArray the string array
     * @return A string of comma-separated values
     */
    public static String toCSVString(Object[] pStringArray) {
        return toCSVString(pStringArray, ", ");
    }

    /**
     * Converts a string array to a string separated by the given delimiter.
     *
     * @param pStringArray     the string array
     * @param pDelimiterString the delimiter string
     * @return string of delimiter separated values
     * @throws IllegalArgumentException if {@code pDelimiterString == null}
     */
    public static String toCSVString(Object[] pStringArray, String pDelimiterString) {
        if (pStringArray == null) {
            return "";
        }
        if (pDelimiterString == null) {
            throw new IllegalArgumentException("delimiter == null");
        }

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < pStringArray.length; i++) {
            if (i > 0) {
                buffer.append(pDelimiterString);
            }

            buffer.append(pStringArray[i]);
        }

        return buffer.toString();
    }

    /**
     * @param pObject the object
     * @return a deep string representation of the given object
     */
    public static String deepToString(Object pObject) {
        return deepToString(pObject, false, 1);
    }

    /**
     * @param pObject    the object
     * @param pDepth     the maximum depth
     * @param pForceDeep {@code true} to force deep {@code toString}, even
     *                   if object overrides toString
     * @return a deep string representation of the given object
     * @todo Array handling (print full type and length)
     * @todo Register handlers for specific toDebugString handling? :-)
     */
    public static String deepToString(Object pObject, boolean pForceDeep, int pDepth) {
        // Null is null
        if (pObject == null) {
            return null;
        }

        // Implements toString, use it as-is unless pForceDeep
        if (!pForceDeep && !isIdentityToString(pObject)) {
            return pObject.toString();
        }

        StringBuilder buffer = new StringBuilder();

        if (pObject.getClass().isArray()) {
            // Special array handling
            Class componentClass = pObject.getClass();
            while (componentClass.isArray()) {
                buffer.append('[');
                buffer.append(Array.getLength(pObject));
                buffer.append(']');
                componentClass = componentClass.getComponentType();
            }
            buffer.insert(0, componentClass);
            buffer.append(" {hashCode=");
            buffer.append(Integer.toHexString(pObject.hashCode()));
            buffer.append("}");
        }
        else {
            // Append toString value only if overridden
            if (isIdentityToString(pObject)) {
                buffer.append(" {");
            }
            else {
                buffer.append(" {toString=");
                buffer.append(pObject.toString());
                buffer.append(", ");
            }
            buffer.append("hashCode=");
            buffer.append(Integer.toHexString(pObject.hashCode()));
            // Loop through, and filter out any getters
            Method[] methods = pObject.getClass().getMethods();
            for (Method method : methods) {
                // Filter only public methods
                if (Modifier.isPublic(method.getModifiers())) {
                    String methodName = method.getName();

                    // Find name of property
                    String name = null;
                    if (!methodName.equals("getClass")
                            && methodName.length() > 3 && methodName.startsWith("get")
                            && Character.isUpperCase(methodName.charAt(3))) {
                        name = methodName.substring(3);
                    }
                    else if (methodName.length() > 2 && methodName.startsWith("is")
                            && Character.isUpperCase(methodName.charAt(2))) {
                        name = methodName.substring(2);
                    }

                    if (name != null) {
                        // If lowercase name, convert, else keep case
                        if (name.length() > 1 && Character.isLowerCase(name.charAt(1))) {
                            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                        }

                        Class[] paramTypes = method.getParameterTypes();// involves array copying...
                        boolean hasParams = (paramTypes != null && paramTypes.length > 0);
                        boolean isVoid = Void.TYPE.equals(method.getReturnType());

                        // Filter return type & parameters
                        if (!isVoid && !hasParams) {
                            try {
                                Object value = method.invoke(pObject);
                                buffer.append(", ");
                                buffer.append(name);
                                buffer.append('=');
                                if (pDepth != 0 && value != null && isIdentityToString(value)) {
                                    buffer.append(deepToString(value, pForceDeep, pDepth > 0 ? pDepth - 1 : -1));
                                }
                                else {
                                    buffer.append(value);
                                }
                            }
                            catch (Exception e) {
                                // Next..!
                            }
                        }
                    }
                }
            }
            buffer.append('}');

            // Get toString from original object
            buffer.insert(0, pObject.getClass().getName());
        }

        return buffer.toString();
    }

    /**
     * Tests if the {@code toString} method of the given object is inherited
     * from {@code Object}.
     *
     * @param pObject the object
     * @return {@code true} if toString of class Object
     */
    private static boolean isIdentityToString(Object pObject) {
        try {
            Method toString = pObject.getClass().getMethod("toString");
            if (toString.getDeclaringClass() == Object.class) {
                return true;
            }
        }
        catch (Exception ignore) {
            // Ignore
        }

        return false;
    }

    /**
     * Returns a string on the same format as {@code Object.toString()}.
     *
     * @param pObject the object
     * @return the object as a {@code String} on the format of
     *         {@code Object.toString()}
     */
    public static String identityToString(Object pObject) {
        if (pObject == null) {
            return null;
        }
        else {
            return pObject.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(pObject));
        }
    }

    /**
     * Tells whether or not the given string string matches the given regular
     * expression.
     * <p/>
     * An invocation of this method of the form
     * <tt>matches(<i>str</i>, <i>regex</i>)</tt> yields exactly the
     * same result as the expression
     * <p/>
     * <blockquote><tt> {@link Pattern}.
     * {@link Pattern#matches(String, CharSequence) matches}
     * (<i>regex</i>, <i>str</i>)</tt></blockquote>
     *
     * @param pString the string
     * @param pRegex  the regular expression to which this string is to be matched
     * @return {@code true} if, and only if, this string matches the
     *         given regular expression
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @see Pattern
     * @see String#matches(String)
     */
    public boolean matches(String pString, String pRegex) throws PatternSyntaxException {
        return Pattern.matches(pRegex, pString);
    }

    /**
     * Replaces the first substring of the given string that matches the given
     * regular expression with the given pReplacement.
     * <p/>
     * An invocation of this method of the form
     * <tt>replaceFirst(<i>str</i>, </tt><i>regex</i>, <i>repl</i>)</tt>
     * yields exactly the same result as the expression
     * <p/>
     * <blockquote><tt>
     * {@link Pattern}.{@link Pattern#compile compile}(<i>regex</i>).
     * {@link Pattern#matcher matcher}(<i>str</i>).
     * {@link java.util.regex.Matcher#replaceFirst replaceFirst}(<i>repl</i>)</tt></blockquote>
     *
     * @param pString      the string
     * @param pRegex       the regular expression to which this string is to be matched
     * @param pReplacement the replacement text
     * @return The resulting {@code String}
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @see Pattern
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public String replaceFirst(String pString, String pRegex, String pReplacement) {
        return Pattern.compile(pRegex).matcher(pString).replaceFirst(pReplacement);
    }

    /**
     * Replaces each substring of this string that matches the given
     * regular expression with the given pReplacement.
     * <p/>
     * An invocation of this method of the form
     * <tt>replaceAll(<i>str</i>, <i>pRegex</i>, <i>repl</i><)</tt>
     * yields exactly the same result as the expression
     * <p/>
     * <blockquote><tt>
     * {@link Pattern}.{@link Pattern#compile compile}(<i>pRegex</i>).
     * {@link Pattern#matcher matcher}(</tt><i>str</i>{@code ).
     * {@link java.util.regex.Matcher#replaceAll replaceAll}(}<i>repl</i>{@code )}</blockquote>
     *
     * @param pString      the string
     * @param pRegex       the regular expression to which this string is to be matched
     * @param pReplacement the replacement string
     * @return The resulting {@code String}
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @see Pattern
     * @see String#replaceAll(String,String)
     */
    public String replaceAll(String pString, String pRegex, String pReplacement) {
        return Pattern.compile(pRegex).matcher(pString).replaceAll(pReplacement);
    }

    /**
     * Splits this string around matches of the given regular expression.
     * <p/>
     * The array returned by this method contains each substring of this
     * string that is terminated by another substring that matches the given
     * expression or is terminated by the end of the string.  The substrings in
     * the array are in the order in which they occur in this string.  If the
     * expression does not match any part of the input then the resulting array
     * has just one element, namely this string.
     * <p/>
     * The {@code pLimit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.  If the pLimit <i>n</i> is greater than zero then the pattern
     * will be applied at most <i>n</i>&nbsp;-&nbsp;1 times, the array's
     * length will be no greater than <i>n</i>, and the array's last entry
     * will contain all input beyond the last matched delimiter.  If <i>n</i>
     * is non-positive then the pattern will be applied as many times as
     * possible and the array can have any length.  If <i>n</i> is zero then
     * the pattern will be applied as many times as possible, the array can
     * have any length, and trailing empty strings will be discarded.
     * <p/>
     * An invocation of this method of the form
     * <tt>split(<i>str</i>, <i>regex</i>, <i>n</i>)</tt>
     * yields the same result as the expression
     * <p/>
     * <blockquote>{@link Pattern}.
     * {@link Pattern#compile compile}<tt>(<i>regex</i>).
     * {@link Pattern#split(CharSequence,int) split}(<i>str</i>, <i>n</i>)</tt>
     * </blockquote>
     *
     * @param pString the string
     * @param pRegex  the delimiting regular expression
     * @param pLimit  the result threshold, as described above
     * @return the array of strings computed by splitting this string
     *         around matches of the given regular expression
     * @throws PatternSyntaxException
     *          if the regular expression's syntax is invalid
     * @see Pattern
     * @see String#split(String,int)
     */
    public String[] split(String pString, String pRegex, int pLimit) {
        return Pattern.compile(pRegex).split(pString, pLimit);
    }

    /**
     * Splits this string around matches of the given regular expression.
     * <p/>
     * This method works as if by invoking the two-argument
     * {@link  #split(String,String,int) split} method with the given
     * expression and a limit argument of zero.
     * Trailing empty strings are therefore not included in the resulting array.
     *
     * @param pString the string
     * @param pRegex  the delimiting regular expression
     * @return the array of strings computed by splitting this string
     *         around matches of the given regular expression
     * @throws PatternSyntaxException if the regular expression's syntax is invalid
     * @see Pattern
     * @see String#split(String)
     */
    public String[] split(String pString, String pRegex) {
        return split(pString, pRegex, 0);
    }

    /**
     * Converts the input string
     * from camel-style (Java in-fix) naming convention
     * to Lisp-style naming convention (hyphen delimitted, all lower case).
     * Other characters in the string are left untouched.
     * <p/>
     * Eg.
     * {@code "foo" => "foo"},
     * {@code "fooBar" => "foo-bar"},
     * {@code "myURL" => "my-url"},
     * {@code "HttpRequestWrapper" => "http-request-wrapper"}
     * {@code "HttpURLConnection" => "http-url-connection"}
     * {@code "my45Caliber" => "my-45-caliber"}
     * {@code "allready-lisp" => "allready-lisp"}
     *
     * @param pString the camel-style input string
     * @return the string converted to lisp-style naming convention
     * @throws IllegalArgumentException if {@code pString == null}
     * @see #lispToCamel(String)
     */
    // TODO: RefactorMe!
    public static String camelToLisp(final String pString) {
        if (pString == null) {
            throw new IllegalArgumentException("string == null");
        }
        if (pString.length() == 0) {
            return pString;
        }

        StringBuilder buf = null;
        int lastPos = 0;
        boolean inCharSequence = false;
        boolean inNumberSequence = false;

        // NOTE: Start at index 1, as first letter should never be hyphen
        for (int i = 1; i < pString.length(); i++) {
            char current = pString.charAt(i);
            if (Character.isUpperCase(current)) {
                // Init buffer if necessary
                if (buf == null) {
                    buf = new StringBuilder(pString.length() + 3);// Allow for some growth
                }

                if (inNumberSequence) {
                    // Sequence end
                    inNumberSequence = false;

                    buf.append(pString.substring(lastPos, i));
                    if (current != '-') {
                        buf.append('-');
                    }
                    lastPos = i;
                    continue;
                }

                // Treat multiple uppercase chars as single word
                char previous = pString.charAt(i - 1);
                if (i == lastPos || Character.isUpperCase(previous)) {
                    inCharSequence = true;
                    continue;
                }

                // Append word
                buf.append(pString.substring(lastPos, i).toLowerCase());
                if (previous != '-') {
                    buf.append('-');
                }
                buf.append(Character.toLowerCase(current));

                lastPos = i + 1;
            }
            else if (Character.isDigit(current)) {
                // Init buffer if necessary
                if (buf == null) {
                    buf = new StringBuilder(pString.length() + 3);// Allow for some growth
                }

                if (inCharSequence) {
                    // Sequence end
                    inCharSequence = false;

                    buf.append(pString.substring(lastPos, i).toLowerCase());
                    if (current != '-') {
                        buf.append('-');
                    }
                    lastPos = i;
                    continue;
                }

                // Treat multiple digits as single word
                char previous = pString.charAt(i - 1);
                if (i == lastPos || Character.isDigit(previous)) {
                    inNumberSequence = true;
                    continue;
                }

                // Append word
                buf.append(pString.substring(lastPos, i).toLowerCase());
                if (previous != '-') {
                    buf.append('-');
                }
                buf.append(Character.toLowerCase(current));

                lastPos = i + 1;
            }
            else if (inNumberSequence) {
                // Sequence end
                inNumberSequence = false;

                buf.append(pString.substring(lastPos, i));
                if (current != '-') {
                    buf.append('-');
                }
                lastPos = i;
            }
            else if (inCharSequence) {
                // Sequence end
                inCharSequence = false;

                // NOTE: Special treatment! Last upper case, is first char in
                // next word, not last char in this word
                buf.append(pString.substring(lastPos, i - 1).toLowerCase());
                if (current != '-') {
                    buf.append('-');
                }
                lastPos = i - 1;
            }
        }

        if (buf != null) {
            // Append the rest
            buf.append(pString.substring(lastPos).toLowerCase());
            return buf.toString();
        }
        else {
            return Character.isUpperCase(pString.charAt(0)) ? pString.toLowerCase() : pString;
        }
    }

    /**
     * Converts the input string
     * from Lisp-style naming convention (hyphen delimitted, all lower case)
     * to camel-style (Java in-fix) naming convention.
     * Other characters in the string are left untouched.
     * <p/>
     * Eg.
     * {@code "foo" => "foo"},
     * {@code "foo-bar" => "fooBar"},
     * {@code "http-request-wrapper" => "httpRequestWrapper"}
     * {@code "my-45-caliber" => "my45Caliber"}
     * {@code "allreadyCamel" => "allreadyCamel"}
     * <p/>
     *
     * @param pString the lisp-style input string
     * @return the string converted to camel-style
     * @throws IllegalArgumentException if {@code pString == null}
     * @see #lispToCamel(String,boolean)
     * @see #camelToLisp(String)
     */
    public static String lispToCamel(final String pString) {
        return lispToCamel(pString, false);
    }

    /**
     * Converts the input string
     * from Lisp-style naming convention (hyphen delimitted, all lower case)
     * to camel-style (Java in-fix) naming convention.
     * Other characters in the string are left untouched.
     * <p/>
     * To create a string starting with a lower case letter
     * (like Java variable names, etc),
     * specify the {@code pFirstUpperCase} paramter to be {@code false}.
     * Eg.
     * {@code "foo" => "foo"},
     * {@code "foo-bar" => "fooBar"},
     * {@code "allreadyCamel" => "allreadyCamel"}
     * <p/>
     * To create a string starting with an upper case letter
     * (like Java class name, etc),
     * specify the {@code pFirstUpperCase} paramter to be {@code true}.
     * Eg.
     * {@code "http-request-wrapper" => "HttpRequestWrapper"}
     * {@code "my-45-caliber" => "My45Caliber"}
     * <p/>
     *
     * @param pString         the lisp-style input string
     * @param pFirstUpperCase {@code true} if the first char should be
     *                        upper case
     * @return the string converted to camel-style
     * @throws IllegalArgumentException if {@code pString == null}
     * @see #camelToLisp(String)
     */
    public static String lispToCamel(final String pString, final boolean pFirstUpperCase) {
        if (pString == null) {
            throw new IllegalArgumentException("string == null");
        }
        if (pString.length() == 0) {
            return pString;
        }

        StringBuilder buf = null;
        int lastPos = 0;

        for (int i = 0; i < pString.length(); i++) {
            char current = pString.charAt(i);
            if (current == '-') {

                // Init buffer if necessary
                if (buf == null) {
                    buf = new StringBuilder(pString.length() - 1);// Can't be larger
                }

                // Append with upper case
                if (lastPos != 0 || pFirstUpperCase) {
                    buf.append(Character.toUpperCase(pString.charAt(lastPos)));
                    lastPos++;
                }

                buf.append(pString.substring(lastPos, i).toLowerCase());
                lastPos = i + 1;
            }
        }

        if (buf != null) {
            buf.append(Character.toUpperCase(pString.charAt(lastPos)));
            buf.append(pString.substring(lastPos + 1).toLowerCase());
            return buf.toString();
        }
        else {
            if (pFirstUpperCase && !Character.isUpperCase(pString.charAt(0))) {
                return capitalize(pString, 0);
            }
            else
            if (!pFirstUpperCase && Character.isUpperCase(pString.charAt(0))) {
                return Character.toLowerCase(pString.charAt(0)) + pString.substring(1);
            }

            return pString;
        }
    }

    public static String reverse(final String pString) {
        final char[] chars = new char[pString.length()];
        pString.getChars(0, chars.length, chars, 0);

        for (int i = 0; i < chars.length / 2; i++) {
            char temp = chars[i];
            chars[i] = chars[chars.length - 1 - i];
            chars[chars.length - 1 - i] = temp;
        }

        return new String(chars);
    }
}