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

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@code Map} decorator that makes the mappings in the backing map
 * case insensitive
 * (this is implemented by converting all keys to uppercase),
 *  if the keys used are {@code Strings}. If the keys
 * used are not {@code String}s, it wil work as a normal
 * {@code java.util.Map}.
 *
 * @see java.util.Map
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public class IgnoreCaseMap<V> extends AbstractDecoratedMap<String, V> implements Serializable, Cloneable {

    /**
     * Constructs a new empty {@code Map}.
     * The backing map will be a {@link java.util.HashMap}
     */
    public IgnoreCaseMap() {
        super();
    }

    /**
     * Constructs a new {@code Map} with the same key-value mappings as the
     * given {@code Map}.
     * The backing map will be a {@link java.util.HashMap}
     * <p>
     * NOTE: As the keys in the given map parameter will be converted to
     * uppercase (if they are strings), any duplicate key/value pair where
     * {@code key instanceof String && key.equalsIgnoreCase(otherKey)}
     * is true, will be lost.
     * </p>
     *
     * @param pMap the map whose mappings are to be placed in this map.
     */
    public IgnoreCaseMap(Map<String, ? extends V> pMap) {
        super(pMap);
    }

    /**
     * Constructs a new {@code Map} with the same key-value mappings as the
     * given {@code Map}.
     * <p>
     * NOTE: The backing map is structuraly cahnged, and it should NOT be
     * accessed directly, after the wrapped map is created.
     * </p>
     * <p>
     * NOTE: As the keys in the given map parameter will be converted to
     * uppercase (if they are strings), any duplicate key/value pair where
     * {@code key instanceof String && key.equalsIgnoreCase(otherKey)}
     * is true, will be lost.
     * </p>
     *
     * @param pBacking the backing map of this map. Must be either empty, or
     * the same map as {@code pContents}.
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}
     *
     * @throws IllegalArgumentException if {@code pBacking} is {@code null}
     * @throws IllegalArgumentException if {@code pBacking} differs from
     * {@code pContent} and is not empty.
     */
    public IgnoreCaseMap(Map pBacking, Map<String, ? extends V> pContents) {
        super(pBacking, pContents);
    }

    /**
     * Maps the specified key to the specified value in this map.
     * Note: If the key used is a string, the key will not be case-sensitive.
     *
     * @param pKey   the map key.
     * @param pValue the value.
     * @return the previous value of the specified key in this map,
     *         or null if it did not have one.
     */
    public V put(String pKey, V pValue) {
        String key = (String) toUpper(pKey);
        return unwrap(entries.put(key, new BasicEntry<String, V>(key, pValue)));
    }

    private V unwrap(Entry<String, V> pEntry) {
        return pEntry != null ? pEntry.getValue() : null;
    }

    /**
     * Returns the value to which the specified key is mapped in this
     * map.
     * Note: If the key used is a string, the key will not be case-sensitive.
     *
     * @param pKey a key in the map
     * @return the value to which the key is mapped in this map; null if
     *         the key is not mapped to any value in this map.
     */
    public V get(Object pKey) {
        return unwrap(entries.get(toUpper(pKey)));
    }

    /**
     * Removes the key (and its corresponding value) from this map. This
     * method does nothing if the key is not in the map.
     * Note: If the key used is a string, the key will not be case-sensitive.
     *
     * @param pKey the key that needs to be removed.
     * @return the value to which the key had been mapped in this map,
     *         or null if the key did not have a mapping.
     */
    public V remove(Object pKey) {
        return unwrap(entries.remove(toUpper(pKey)));
    }

    /**
     * Tests if the specified object is a key in this map.
     * Note: If the key used is a string, the key will not be case-sensitive.
     *
     * @param pKey possible key.
     * @return true if and only if the specified object is a key in this
     *         map, as determined by the equals method; false otherwise.
     */
    public boolean containsKey(Object pKey) {
        return entries.containsKey(toUpper(pKey));
    }

    /**
     * Converts the parameter to uppercase, if it's a String.
     */
    protected static Object toUpper(final Object pObject) {
        if (pObject instanceof String) {
            return ((String) pObject).toUpperCase();
        }
        return pObject;
    }

    protected Iterator<Entry<String, V>> newEntryIterator() {
        return (Iterator) entries.entrySet().iterator();
    }

    protected Iterator<String> newKeyIterator() {
        return entries.keySet().iterator();
    }

    protected Iterator<V> newValueIterator() {
        return (Iterator<V>) entries.values().iterator();
    }
}
