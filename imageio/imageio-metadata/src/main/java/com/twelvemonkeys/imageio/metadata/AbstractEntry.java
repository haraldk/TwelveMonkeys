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

package com.twelvemonkeys.imageio.metadata;

import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.CollectionUtil;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * AbstractEntry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractEntry.java,v 1.0 Nov 12, 2009 12:43:13 AM haraldk Exp$
 */
public abstract class AbstractEntry implements Entry {

    private final Object identifier;
    private final Object value; // Entries are immutable, directories can be mutated

    protected AbstractEntry(final Object identifier, final Object value) {
        Validate.notNull(identifier, "identifier");

        this.identifier = identifier;
        this.value = value;
    }

    public final Object getIdentifier() {
        return identifier;
    }

    /**
     * Returns a format-native identifier. 
     * For example {@code "2:00"} for IPTC "Record Version" field, or {@code "0x040c"} for PSD "Thumbnail" resource. 
     * This default implementation simply returns {@code String.valueOf(getIdentifier())}.
     * 
     * @return a format-native identifier.
     */
    protected String getNativeIdentifier() {
        return String.valueOf(getIdentifier());
    }

    /**
     * Returns {@code null}, meaning unknown or undefined.
     *
     * @return {@code null}.
     */
    public String getFieldName() {
        return null;
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        if (valueCount() > 1) {
            if (valueCount() < 16) {
                return arrayToString(value);
            }
            else {
                String first = arrayToString(CollectionUtil.subArray(value, 0, 4));
                String last = arrayToString(CollectionUtil.subArray(value, valueCount() - 4, 4));
                return String.format("%s ... %s (%d)", first.substring(0, first.length() - 1), last.substring(1), valueCount());
            }
        }

        if (value != null && value.getClass().isArray() && Array.getLength(value) == 1) {
            return String.valueOf(Array.get(value, 0));
        }

        return String.valueOf(value);
    }

    private static String arrayToString(final Object value) {
        Class<?> type = value.getClass().getComponentType();

        if (type.isPrimitive()) {
            if (type.equals(boolean.class)) {
                return Arrays.toString((boolean[]) value);
            }
            else if (type.equals(byte.class)) {
                return Arrays.toString((byte[]) value);
            }
            else if (type.equals(char.class)) {
                return new String((char[]) value);
            }
            else if (type.equals(double.class)) {
                return Arrays.toString((double[]) value);
            }
            else if (type.equals(float.class)) {
                return Arrays.toString((float[]) value);
            }
            else if (type.equals(int.class)) {
                return Arrays.toString((int[]) value);
            }
            else if (type.equals(long.class)) {
                return Arrays.toString((long[]) value);
            }
            else if (type.equals(short.class)) {
                return Arrays.toString((short[]) value);
            }
            else {
                // Fall through should never happen
                throw new AssertionError("Unknown type: " + type);
            }
        }
        else {
            return Arrays.toString((Object[]) value);
        }
    }

    public String getTypeName() {
        if (value == null) {
            return null;
        }

        return value.getClass().getSimpleName();
    }

    public int valueCount() {
        // TODO: Collection support?
        if (value != null && value.getClass().isArray()) {
            return Array.getLength(value);
        }

        return 1;
    }

    /// Object

    @Override
    public int hashCode() {
        return identifier.hashCode() + (value != null ? 31 * value.hashCode() : 0);
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (!(pOther instanceof AbstractEntry)) {
            return false;
        }

        AbstractEntry other = (AbstractEntry) pOther;
        
        return identifier.equals(other.identifier) && (
                value == null && other.value == null || value != null && valueEquals(other)
        );
    }

    private boolean valueEquals(final AbstractEntry other) {
        return value.getClass().isArray() ? arrayEquals(value, other.value) : value.equals(other.value);
    }

    static boolean arrayEquals(final Object thisArray, final Object otherArray) {
        // TODO: This is likely a utility method, and should be extracted
        if (thisArray == otherArray) {
            return true;
        }
        if (otherArray == null  || thisArray == null || thisArray.getClass() != otherArray.getClass()) {
            return false;
        }

        Class<?> componentType = thisArray.getClass().getComponentType();

        if (componentType.isPrimitive()) {
            if (thisArray instanceof byte[]) {
                return Arrays.equals((byte[]) thisArray, (byte[]) otherArray);
            }
            if (thisArray instanceof char[]) {
                return Arrays.equals((char[]) thisArray, (char[]) otherArray);
            }
            if (thisArray instanceof short[]) {
                return Arrays.equals((short[]) thisArray, (short[]) otherArray);
            }
            if (thisArray instanceof int[]) {
                return Arrays.equals((int[]) thisArray, (int[]) otherArray);
            }
            if (thisArray instanceof long[]) {
                return Arrays.equals((long[]) thisArray, (long[]) otherArray);
            }
            if (thisArray instanceof boolean[]) {
                return Arrays.equals((boolean[]) thisArray, (boolean[]) otherArray);
            }
            if (thisArray instanceof float[]) {
                return Arrays.equals((float[]) thisArray, (float[]) otherArray);
            }
            if (thisArray instanceof double[]) {
                return Arrays.equals((double[]) thisArray, (double[]) otherArray);
            }

            throw new AssertionError("Unsupported type:" + componentType);
        }

        return Arrays.equals((Object[]) thisArray, (Object[]) otherArray);
    }

    @Override
    public String toString() {
        String name = getFieldName();
        String nameStr = name != null ? String.format("/%s", name) : "";

        String type = getTypeName();
        String typeStr = type != null ? String.format(" (%s)", type) : "";

        return String.format("%s%s: %s%s", getNativeIdentifier(), nameStr, getValueAsString(), typeStr);
    }
}
