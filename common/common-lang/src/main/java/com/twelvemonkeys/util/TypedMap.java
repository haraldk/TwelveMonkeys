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

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.io.Serializable;

/**
 * This {@code Map} implementation guarantees that the values have a type that
 * are compatible with it's key. Different keys may have different types.
 * 
 * @see TypedMap.Key
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/TypedMap.java#1 $
 */
public class TypedMap<K extends TypedMap.Key, V> implements Map<K, V>, Serializable {

    /**
     * The wrapped map
     */
    protected Map<K, V> mEntries;

    /**
     * Creates a {@code TypedMap}.
     * This {@code TypedMap} will be backed by a new {@code HashMap} instance.
     */
    public TypedMap() {
        mEntries = new HashMap<K, V>();
    }

    /**
     * Creates a {@code TypedMap} containing the same elements as the given
     * map.
     * This {@code TypedMap} will be backed by a new {@code HashMap} instance,
     * and <em>not</em> the map passed in as a paramter.
     * <p/>
     * <small>This is constructor is here to comply with the reccomendations for
     * "standard" constructors in the {@code Map} interface.</small>
     *
     * @param pMap the map used to populate this map
     * @throws ClassCastException if all keys in the map are not instances of
     *                            {@code TypedMap.Key}.
     * @see java.util.Map
     * @see #TypedMap(java.util.Map, boolean)
     */
    public TypedMap(Map<? extends K, ? extends V> pMap) {
        this();

        if (pMap != null) {
            putAll(pMap);
        }
    }

    /**
     * Creates a {@code TypedMap}.
     * This {@code TypedMap} will be backed by the given {@code Map}.
     * <P/>
     * Note that structurally modifying the backing map directly (not through
     * this map or its collection views), is not allowed, and will produce
     * undeterministic exceptions.
     *
     * @param pBacking     the map that will be used as backing.
     * @param pUseElements if {@code true}, the elements in the map are
     *                     retained. Otherwise, the map is cleared. For an empty {@code Map} the
     *                     parameter has no effect.
     * @throws ClassCastException if {@code pUseElements} is {@code true}
     *                            all keys in the map are not instances of {@code TypedMap.Key}.
     */
    public TypedMap(Map<? extends K, ? extends V> pBacking, boolean pUseElements) {
        if (pBacking == null) {
            throw new IllegalArgumentException("backing == null");
        }

        // This is safe, as we re-insert all values later
        //noinspection unchecked
        mEntries = (Map<K, V>) pBacking;

        // Re-insert all elements to avoid undeterministic ClassCastExceptions
        if (pUseElements) {
            putAll(pBacking);
        }
        else if (mEntries.size() > 0) {
            mEntries.clear();
        }
    }

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return mEntries.size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return mEntries.isEmpty();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param pKey key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified
     *         key.
     */
    public boolean containsKey(Object pKey) {
        return mEntries.containsKey(pKey);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.  More formally, returns {@code true} if and only if
     * this map contains at least one mapping to a value {@code v} such that
     * {@code (pValue==null ? v==null : pValue.equals(v))}.  This operation
     * will probably require time linear in the map size for most
     * implementations of the {@code Map} interface.
     *
     * @param pValue value whose presence in this map is to be tested.
     * @return {@code true} if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object pValue) {
        return mEntries.containsValue(pValue);
    }

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * {@code null} if the map contains no mapping for this key.  A return
     * value of {@code null} does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to {@code null}.  The {@code containsKey}
     * operation may be used to distinguish these two cases.
     *
     * @param pKey key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or
     *         {@code null} if the map contains no mapping for this key.
     * @see #containsKey(java.lang.Object)
     */
    public V get(Object pKey) {
        return mEntries.get(pKey);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for
     * the key, the old value is replaced.
     *
     * @param pKey   key with which the specified value is to be associated.
     * @param pValue value to be associated with the specified key.
     *
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.  A {@code null} return can
     *         also indicate that the map previously associated {@code null}
     *         with the specified pKey, if the implementation supports
     *         {@code null} values.
     *
     * @throws IllegalArgumentException if the value is not compatible with the
     * key.
     *
     * @see TypedMap.Key
     */
    public V put(K pKey, V pValue) {
        if (!pKey.isCompatibleValue(pValue)) {
            throw new IllegalArgumentException("incompatible value for key");
        }
        return mEntries.put(pKey, pValue);
    }

    /**
     * Removes the mapping for this key from this map if present (optional
     * operation).
     *
     * @param pKey key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.  A {@code null} return can
     *         also indicate that the map previously associated {@code null}
     *         with the specified key, if the implementation supports
     *         {@code null} values.
     */
    public V remove(Object pKey) {
        return mEntries.remove(pKey);
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  These mappings will replace any mappings that
     * this map had for any of the keys currently in the specified map.
     * <P/>
     * Note: If you override this method, make sure you add each element through
     * the put method, to avoid resource leaks and undeterministic class casts.
     *
     * @param pMap Mappings to be stored in this map.
     */
    public void putAll(Map<? extends K, ? extends V> pMap) {
        for (final Entry<? extends K, ? extends V> e : pMap.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes all mappings from this map (optional operation).
     */
    public void clear() {
        mEntries.clear();
    }

    public Collection<V> values() {
        return mEntries.values();
    }

    public Set<Entry<K, V>> entrySet() {
        return mEntries.entrySet();
    }

    public Set<K> keySet() {
        return mEntries.keySet();
    }

    /**
     * Keys for use with {@code TypedMap} must implement this interface.
     *
     * @see #isCompatibleValue(Object)
     */
    public static interface Key {

        /**
         * Tests if the given value is compatible with this {@code Key}.
         * Only compatible values may be passed to the
         * {@code TypedMap.put} method.
         *
         * @param pValue the value to test for compatibility
         * @return {@code true} if compatible, otherwise {@code false}
         */
        boolean isCompatibleValue(Object pValue);
    }

    /**
     * An abstract {@code Key} implementation that allows keys to have
     * meaningful names.
     */
    public static abstract class AbstractKey implements Key, Serializable {
        private final String mStringRep;

        /**
         * Creates a {@code Key} with the given name.
         *
         * @param pName name of this key
         */
        public AbstractKey(String pName) {
            if (pName == null) {
                throw new IllegalArgumentException("name == null");
            }
            mStringRep = getClass().getName() + '[' + pName + ']';
        }

        /**
         * Creates a {@code Key} with no name.
         */
        public AbstractKey() {
            this("null");
        }

        @Override
        public String toString() {
            return mStringRep;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this ||
                    (obj != null && obj.getClass() == getClass() &&
                            mStringRep.equals(((AbstractKey) obj).mStringRep));
        }

        @Override
        public int hashCode() {
            return mStringRep.hashCode();
        }
    }
}
