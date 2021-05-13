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

package com.twelvemonkeys.util.regex;

import com.twelvemonkeys.util.AbstractTokenIterator;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * {@code StringTokenizer} replacement, that uses regular expressions to split
 * strings into tokens.
 *
 *@see java.util.regex.Pattern for pattern syntax.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/regex/RegExTokenIterator.java#1 $
 */
public class RegExTokenIterator extends AbstractTokenIterator {
    private final Matcher matcher;
    private boolean next = false;

    /**
     * Creates a {@code RegExTokenIterator}.
     * Default pettern is {@code "\S+"}.
     *
     * @param pString the string to be parsed.
     *
     * @throws IllegalArgumentException if {@code pString} is {@code null}
     */
    public RegExTokenIterator(String pString) {
        this(pString, "\\S+");
    }

    /**
     * Creates a {@code RegExTokenIterator}.
     *
     * @see Pattern for pattern syntax.
     *
     * @param pString the string to be parsed.
     * @param pPattern the pattern
     *
     * @throws PatternSyntaxException if {@code pPattern} is not a valid pattern
     * @throws IllegalArgumentException if any of the arguments are {@code null}
     */
    public RegExTokenIterator(String pString, String pPattern) {
        if (pString == null) {
            throw new IllegalArgumentException("string == null");
        }

        if (pPattern == null) {
            throw new IllegalArgumentException("pattern == null");
        }

        matcher = Pattern.compile(pPattern).matcher(pString);
    }

    /**
     * Resets this iterator.
     *
     */
    public void reset() {
        matcher.reset();
    }

    public boolean hasNext() {
        return next || (next = matcher.find());
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        next = false;
        return matcher.group();
    }
}