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

/**
 * Util class for various reflection-based operations.
 * <p/>
 * <em>NOTE: This class is not considered part of the public API and may be
 * changed without notice</em>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/ReflectUtil.java#1 $
 */
public final class ReflectUtil {

    /** Don't allow instances */
    private ReflectUtil() {}

    /**
     * Returns the primitive type for the given wrapper type.
     *
     * @param pType the wrapper type
     *
     * @return the primitive type
     *
     * @throws IllegalArgumentException if {@code pType} is not a primitive
     * wrapper
     */
    public static Class unwrapType(Class pType) {
        if (pType == Boolean.class) {
            return Boolean.TYPE;
        }
        else if (pType == Byte.class) {
            return Byte.TYPE;
        }
        else if (pType == Character.class) {
            return Character.TYPE;
        }
        else if (pType == Double.class) {
            return Double.TYPE;
        }
        else if (pType == Float.class) {
            return Float.TYPE;
        }
        else if (pType == Integer.class) {
            return Integer.TYPE;
        }
        else if (pType == Long.class) {
            return Long.TYPE;
        }
        else if (pType == Short.class) {
            return Short.TYPE;
        }

        throw new IllegalArgumentException("Not a primitive wrapper: " + pType);
    }

    /**
     * Returns the wrapper type for the given primitive type.
     *
     * @param pType the primitive tpye
     *
     * @return the wrapper type
     *
     * @throws IllegalArgumentException if {@code pType} is not a primitive
     * type
     */
    public static Class wrapType(Class pType) {
        if (pType == Boolean.TYPE) {
            return Boolean.class;
        }
        else if (pType == Byte.TYPE) {
            return Byte.class;
        }
        else if (pType == Character.TYPE) {
            return Character.class;
        }
        else if (pType == Double.TYPE) {
            return Double.class;
        }
        else if (pType == Float.TYPE) {
            return Float.class;
        }
        else if (pType == Integer.TYPE) {
            return Integer.class;
        }
        else if (pType == Long.TYPE) {
            return Long.class;
        }
        else if (pType == Short.TYPE) {
            return Short.class;
        }

        throw new IllegalArgumentException("Not a primitive type: " + pType);
    }

    /**
     * Returns {@code true} if the given type is a primitive wrapper.
     *
     * @param pType
     *
     * @return {@code true} if the given type is a primitive wrapper, otherwise
     * {@code false}
     */
    public static boolean isPrimitiveWrapper(Class pType) {
        return pType == Boolean.class || pType == Byte.class
                || pType == Character.class || pType == Double.class
                || pType == Float.class || pType == Integer.class
                || pType == Long.class || pType == Short.class;
    }
}
