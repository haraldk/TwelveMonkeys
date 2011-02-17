/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata;

import com.twelvemonkeys.lang.Validate;

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

    private final Object mIdentifier;
    private final Object mValue; // TODO: Might need to be mutable..

    protected AbstractEntry(final Object pIdentifier, final Object pValue) {
        Validate.notNull(pIdentifier, "identifier");

        mIdentifier = pIdentifier;
        mValue = pValue;
    }

    public final Object getIdentifier() {
        return mIdentifier;
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
        return mValue;
    }

    public String getValueAsString() {
        if (valueCount() > 1) {
            if (valueCount() < 16) {
                Class<?> type = mValue.getClass().getComponentType();

                if (type.isPrimitive()) {
                    if (type.equals(boolean.class)) {
                        return Arrays.toString((boolean[]) mValue);
                    }
                    else if (type.equals(byte.class)) {
                        return Arrays.toString((byte[]) mValue);
                    }
                    else if (type.equals(char.class)) {
                        return new String((char[]) mValue);
                    }
                    else if (type.equals(double.class)) {
                        return Arrays.toString((double[]) mValue);
                    }
                    else if (type.equals(float.class)) {
                        return Arrays.toString((float[]) mValue);
                    }
                    else if (type.equals(int.class)) {
                        return Arrays.toString((int[]) mValue);
                    }
                    else if (type.equals(long.class)) {
                        return Arrays.toString((long[]) mValue);
                    }
                    else if (type.equals(short.class)) {
                        return Arrays.toString((short[]) mValue);
                    }
                    // Fall through should never happen
                }
                else {
                    return Arrays.toString((Object[]) mValue);
                }
            }
            
            return String.valueOf(mValue) + " ("  + valueCount() + ")";
        }

        return String.valueOf(mValue);
    }

    public String getTypeName() {
        if (mValue == null) {
            return null;
        }

        return mValue.getClass().getSimpleName();
    }

    public int valueCount() {
        // TODO: Collection support?
        if (mValue != null && mValue.getClass().isArray()) {
            return Array.getLength(mValue);
        }

        return 1;
    }


    /// Object


    @Override
    public int hashCode() {
        return mIdentifier.hashCode() + 31 * mValue.hashCode();
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
        
        return mIdentifier.equals(other.mIdentifier) && (
                mValue == null && other.mValue == null || mValue != null && mValue.equals(other.mValue)
        );
    }

    @Override
    public String toString() {
        String name = getFieldName();
        String nameStr = name != null ? "/" + name + "" : "";

        String type = getTypeName();
        String typeStr = type != null ? " (" + type + ")" : "";

        return String.format("%s%s: %s%s", getIdentifier(), nameStr, getValueAsString(), typeStr);
    }
}
