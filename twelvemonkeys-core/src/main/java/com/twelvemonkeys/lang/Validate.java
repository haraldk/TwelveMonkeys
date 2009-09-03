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

    // Not empty...

    public static <T extends CharSequence> T notEmpty(final T pParameter) {
        return notEmpty(pParameter, null);
    }

    public static <T extends CharSequence> T notEmpty(final T pParameter, final String pParamName) {
        if (pParameter == null || pParameter.length() == 0) {
            throw new IllegalArgumentException(String.format("%s may not be empty", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
        }

        return pParameter;
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
        noNullElements(Arrays.asList(pParameter), pParamName);
        return pParameter;
    }

    public static <T> Collection<T> noNullElements(final Collection<T> pParameter) {
        return noNullElements(pParameter, null);
    }

    public static <T> Collection<T> noNullElements(final Collection<T> pParameter, final String pParamName) {
        for (T element : pParameter) {
            if (element == null) {
                throw new IllegalArgumentException(String.format("%s may not contain null elements", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
            }
        }

        return pParameter;
    }

    public static <K, V> Map<K, V>  noNullElements(final Map<K, V>  pParameter) {
        return noNullElements(pParameter, null);
    }

    public static <K, V> Map<K, V>  noNullElements(final Map<K, V>  pParameter, final String pParamName) {
        for (V element : pParameter.values()) {
            if (element == null) {
                throw new IllegalArgumentException(String.format("%s may not contain null elements", pParamName == null ? UNSPECIFIED_PARAM_NAME : pParamName));
            }
        }

        return pParameter;
    }
}
