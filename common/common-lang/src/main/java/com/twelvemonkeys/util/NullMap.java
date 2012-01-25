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

package com.twelvemonkeys.util;

import java.util.*;
import java.io.Serializable;

/**
 * An (immutable) empty {@link Map}, that supports all {@code Map} operations
 * without throwing exceptions (in contrast to {@link Collections#EMPTY_MAP}
 * that will throw exceptions on {@code put}/{@code remove}).
 * <p/>
 * NOTE: This is not a general purpose {@code Map} implementation,
 * as the {@code put} and {@code putAll} methods will not modify the map.
 * Instances of this class will always be an empty map.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: com/twelvemonkeys/util/NullMap.java#2 $
 */
public final class NullMap<K, V> implements Map<K, V>, Serializable {
    public final int size() {
        return 0;
    }

    public final void clear() {
    }

    public final boolean isEmpty() {
        return true;
    }

    public final boolean containsKey(Object pKey) {
        return false;
    }

    public final boolean containsValue(Object pValue) {
        return false;
    }

    public final Collection<V> values() {
        return Collections.emptyList();
    }

    public final void putAll(Map pMap) {
    }

    public final Set<Entry<K, V>> entrySet() {
        return Collections.emptySet();
    }

    public final Set<K> keySet() {
        return Collections.emptySet();
    }

    public final V get(Object pKey) {
        return null;
    }

    public final V remove(Object pKey) {
        return null;
    }

    public final V put(Object pKey, Object pValue) {
        return null;
    }

    /**
     * Tests the given object for equality (wether it is also an empty
     * {@code Map}).
     * This is consistent with the standard {@code Map} implementations of the
     * Java Collections Framework.
     *
     * @param pOther the object to compare with
     * @return {@code true} if {@code pOther} is an empty {@code Map},
     * otherwise {@code false}
     */
    public boolean equals(Object pOther) {
        return (pOther instanceof Map) && ((Map) pOther).isEmpty();
    }

    /**
     * Returns the {@code hashCode} of the empty map, {@code 0}.
     * This is consistent with the standard {@code Map} implementations of the
     * Java Collections Framework.
     *
     * @return {@code 0}, always
     */
    public int hashCode() {
        return 0;
    }
}