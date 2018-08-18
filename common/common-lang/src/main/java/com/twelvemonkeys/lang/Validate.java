/*
 * Copyright (c) 2009, Harald Kuhr
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Kind of like {@code org.apache.commons.lang.Validate}. Just smarter. ;-)
 * <p/>
 * Uses type parameterized return values, thus making it possible to check
 * constructor arguments before
 * they are passed on to {@code super} or {@code this} type constructors.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/Validate.java#1 $
 */
public final class Validate {
    // TODO: Make it possible to throw IllegalStateException instead of IllegalArgumentException?

    private static final String UNSPECIFIED_PARAM_NAME = "method parameter";

    private Validate() {}

    // Not null...

    public static <T> T notNull(final T pParameter) {
        return notNull(pParameter, null);
    }

    public static <T> T notNull(final T pParameter, final String pParamName) {
        if (pParameter == null) {
            throw new IllegalArgumentException(String.format("%s may not be null", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
        }

        return pParameter;
    }

    // Not empty

    public static <T extends CharSequence> T notEmpty(final T pParameter) {
        return notEmpty(pParameter, null);
    }

    public static <T extends CharSequence> T notEmpty(final T pParameter, final String pParamName) {
        if (pParameter == null || pParameter.length() == 0 || isOnlyWhiteSpace(pParameter)) {
            throw new IllegalArgumentException(String.format("%s may not be blank", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
        }

        return pParameter;
    }

    private static <T extends CharSequence> boolean isOnlyWhiteSpace(T pParameter) {
        for (int i = 0; i < pParameter.length(); i++) {
            if (!Character.isWhitespace(pParameter.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static <T> T[] notEmpty(final T[] pParameter) {
        return notEmpty(pParameter, null);
    }

    public static <T> T[] notEmpty(final T[] pParameter, final String pParamName) {
        if (pParameter == null || pParameter.length == 0) {
            throw new IllegalArgumentException(String.format("%s may not be empty", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
        }

        return pParameter;
    }

    public static <T> Collection<T> notEmpty(final Collection<T> pParameter) {
        return notEmpty(pParameter, null);
    }

    public static <T> Collection<T> notEmpty(final Collection<T> pParameter, final String pParamName) {
        if (pParameter == null || pParameter.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s may not be empty", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
        }

        return pParameter;
    }

    public static <K, V> Map<K, V> notEmpty(final Map<K, V> pParameter) {
        return notEmpty(pParameter, null);
    }

    public static <K, V> Map<K, V> notEmpty(final Map<K, V> pParameter, final String pParamName) {
        if (pParameter == null || pParameter.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s may not be empty", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
        }

        return pParameter;
    }

    // No null elements

    public static <T> T[] noNullElements(final T[] pParameter) {
        return noNullElements(pParameter, null);
    }

    public static <T> T[] noNullElements(final T[] pParameter, final String pParamName) {
        noNullElements(pParameter == null ? null : Arrays.asList(pParameter), pParamName);
        return pParameter;
    }

    public static <T> Collection<T> noNullElements(final Collection<T> pParameter) {
        return noNullElements(pParameter, null);
    }

    public static <T> Collection<T> noNullElements(final Collection<T> pParameter, final String pParamName) {
        notNull(pParameter, pParamName);

        for (T element : pParameter) {
            if (element == null) {
                throw new IllegalArgumentException(String.format("%s may not contain null elements", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
            }
        }

        return pParameter;
    }

    public static <K, V> Map<K, V> noNullValues(final Map<K, V> pParameter) {
        return noNullValues(pParameter, null);
    }

    public static <K, V> Map<K, V> noNullValues(final Map<K, V> pParameter, final String pParamName) {
        notNull(pParameter, pParamName);

        for (V value : pParameter.values()) {
            if (value == null) {
                throw new IllegalArgumentException(String.format("%s may not contain null values", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
            }
        }

        return pParameter;
    }

    public static <K, V> Map<K, V> noNullKeys(final Map<K, V> pParameter) {
        return noNullKeys(pParameter, null);
    }

    public static <K, V> Map<K, V> noNullKeys(final Map<K, V> pParameter, final String pParamName) {
        notNull(pParameter, pParamName);

        for (K key : pParameter.keySet()) {
            if (key == null) {
                throw new IllegalArgumentException(String.format("%s may not contain null keys", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
            }
        }

        return pParameter;
    }

    // Is true

    public static boolean isTrue(final boolean pExpression, final String pMessage) {
        return isTrue(pExpression, pExpression, pMessage);
    }

    public static <T> T isTrue(final boolean condition, final T value, final String message) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message == null ? "expression may not be %s" : message, value));
        }

        return value;
    }
}
