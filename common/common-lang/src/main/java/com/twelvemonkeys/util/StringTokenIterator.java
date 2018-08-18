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

import java.util.NoSuchElementException;

/**
 * StringTokenIterator, a drop-in replacement for {@code StringTokenizer}.
 * StringTokenIterator has the following features:
 * <ul>
 * <li>Iterates over a strings, 20-50% faster than {@code StringTokenizer}
 *     (and magnitudes faster than {@code String.split(..)} or
 *     {@code Pattern.split(..)})</li>
 * <li>Implements the {@code Iterator} interface</li>
 * <li>Optionally returns delimiters</li>
 * <li>Optionally returns empty elements</li>
 * <li>Optionally iterates in reverse</li>
 * <li>Resettable</li>
 * </ul>
 *
 * @see java.util.StringTokenizer
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/StringTokenIterator.java#1 $
 */
public class StringTokenIterator extends AbstractTokenIterator {

    private final String string;
    private final char[] delimiters;
    private int position;
    private final int maxPosition;
    private String next;
    private String nextDelimiter;
    private final boolean includeDelimiters;
    private final boolean includeEmpty;
    private final boolean reverse;

    public final static int FORWARD = 1;
    public final static int REVERSE = -1;

    /**
     * Stores the value of the delimiter character with the highest value.
     * It is used to optimize the detection of delimiter characters.
     */
    private final char maxDelimiter;

    /**
     * Creates a StringTokenIterator
     *
     * @param pString the string to be parsed.
     */
    public StringTokenIterator(String pString) {
        this(pString, " \t\n\r\f".toCharArray(), FORWARD, false, false);
    }

    /**
     * Creates a StringTokenIterator
     *
     * @param pString the string to be parsed.
     * @param pDelimiters the delimiters.
     */
    public StringTokenIterator(String pString, String pDelimiters) {
        this(pString, toCharArray(pDelimiters), FORWARD, false, false);
    }

    /**
     * Creates a StringTokenIterator
     *
     * @param pString the string to be parsed.
     * @param pDelimiters the delimiters.
     * @param pDirection iteration direction.
     */
    public StringTokenIterator(String pString, String pDelimiters, int pDirection) {
        this(pString, toCharArray(pDelimiters), pDirection, false, false);
    }

    /**
     * Creates a StringTokenIterator
     *
     * @param pString the string to be parsed.
     * @param pDelimiters the delimiters.
     * @param pIncludeDelimiters flag indicating whether to return delimiters as tokens.
     */
    public StringTokenIterator(String pString, String pDelimiters, boolean pIncludeDelimiters) {
        this(pString, toCharArray(pDelimiters), FORWARD, pIncludeDelimiters, false);
    }

    /**
     * Creates a StringTokenIterator
     *
     * @param pString the string to be parsed.
     * @param pDelimiters the delimiters.
     * @param pDirection iteration direction.
     * @param pIncludeDelimiters flag indicating whether to return delimiters as tokens.
     * @param pIncludeEmpty flag indicating whether to return empty tokens
     *
     */
    public StringTokenIterator(String pString, String pDelimiters, int pDirection,
                               boolean pIncludeDelimiters, boolean pIncludeEmpty) {
        this(pString, toCharArray(pDelimiters), pDirection, pIncludeDelimiters, pIncludeEmpty);
    }

    /**
     * Implementation.
     *
     * @param pString the string to be parsed.
     * @param pDelimiters the delimiters.
     * @param pDirection iteration direction.
     * @param pIncludeDelimiters flag indicating whether to return delimiters as tokens.
     * @param pIncludeEmpty flag indicating whether to return empty tokens
     */
    private StringTokenIterator(String pString, char[] pDelimiters,
                                int pDirection, boolean pIncludeDelimiters, boolean pIncludeEmpty) {
        if (pString == null) {
            throw new IllegalArgumentException("string == null");
        }

        string = pString;
        maxPosition = pString.length();
        delimiters = pDelimiters;
        includeDelimiters = pIncludeDelimiters;
        reverse = (pDirection == REVERSE);
        includeEmpty = pIncludeEmpty;
        maxDelimiter = initMaxDelimiter(pDelimiters);

        reset();
    }

    private static char[] toCharArray(String pDelimiters) {
        if (pDelimiters == null) {
            throw new IllegalArgumentException("delimiters == null");
        }
        return pDelimiters.toCharArray();
    }

    /**
     * Returns the highest char in the delimiter set.
     * @param pDelimiters the delimiter set
     * @return the highest char
     */
    private static char initMaxDelimiter(char[] pDelimiters) {
        if (pDelimiters == null) {
            return 0;
        }

        char max = 0;
        for (char c : pDelimiters) {
            if (max < c) {
                max = c;
            }
        }

        return max;
    }

    /**
     * Resets this iterator.
     *
     */
    public void reset() {
        position = 0;
        next = null;
        nextDelimiter = null;
    }

    /**
     * Returns {@code true} if the iteration has more elements. (In other
     * words, returns {@code true} if {@code next} would return an element
     * rather than throwing an exception.)
     *
     * @return {@code true} if the iterator has more elements.
     */
    public boolean hasNext() {
        return (next != null || fetchNext() != null);
    }

    private String fetchNext() {
        // If next is delimiter, return fast
        if (nextDelimiter != null) {
            next = nextDelimiter;
            nextDelimiter = null;
            return next;
        }

        // If no more chars, return null
        if (position >= maxPosition) {
            return null;
        }

        return reverse ? fetchReverse() : fetchForward();

    }

    private String fetchReverse() {
        // Get previous position
        int prevPos = scanForPrev();

        // Store next string
        next = string.substring(prevPos + 1, maxPosition - position);

        if (includeDelimiters && prevPos >= 0 && prevPos < maxPosition) {
            nextDelimiter = string.substring(prevPos, prevPos + 1);
        }

        position = maxPosition - prevPos;

        // Skip empty
        if (next.length() == 0 && !includeEmpty) {
            return fetchNext();
        }

        return next;
    }

    private String fetchForward() {
        // Get next position
        int nextPos = scanForNext();

        // Store next string
        next = string.substring(position, nextPos);

        if (includeDelimiters && nextPos >= 0 && nextPos < maxPosition) {
            nextDelimiter = string.substring(nextPos, nextPos + 1);
        }

        position = ++nextPos;

        // Skip empty
        if (next.length() == 0 && !includeEmpty) {
            return fetchNext();
        }

        return next;
    }

    private int scanForNext() {
        int position = this.position;

        while (position < maxPosition) {
            // Find next match, using all delimiters
            char c = string.charAt(position);

            if (c <= maxDelimiter) {

                // Find first delimiter match
                for (char delimiter : delimiters) {
                    if (c == delimiter) {
                        return position;// Return if match
                    }
                }
            }

            // Next...
            position++;
        }

        // Return last position, if no match
        return position;
    }

    private int scanForPrev() {
        int position = (maxPosition - 1) - this.position;

        while (position >= 0) {
            // Find next match, using all delimiters
            char c = string.charAt(position);

            if (c <= maxDelimiter) {

                // Find first delimiter match
                for (char delimiter : delimiters) {
                    if (c == delimiter) {
                        return position;// Return if match
                    }
                }
            }

            // Next...
            position--;
        }

        // Return first position, if no match
        return position;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @exception java.util.NoSuchElementException iteration has no more elements.
     */
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String next = this.next;
        this.next = fetchNext();

        return next;
    }

}