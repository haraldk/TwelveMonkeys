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

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Special-purpose map implementation with weak keys and weak values. This is
 * useful for mapping between keys and values that refer to (for example by
 * wrapping) their keys.
 * For more info, see {@link WeakHashMap} on why the
 * values in a {@code WeakHashMap} must never refer strongly to their keys.
 *
 * @see WeakHashMap
 * @see WeakReference
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/WeakWeakMap.java#1 $
 */
@SuppressWarnings({"unchecked"})
public class WeakWeakMap<K, V> extends WeakHashMap<K, V> {
    // TODO: Consider using a backing map and delegate, instead of extending...

    /**
     * Creates a {@code WeakWeakMap} with default initial capacity and load
     * factor.
     *
     * @see java.util.WeakHashMap#WeakHashMap()
     */
    public WeakWeakMap() {
        super();
    }

    /**
     * Creates a {@code WeakWeakMap} with the given initial capacity and
     * default load factor.
     *
     * @param pInitialCapacity the initial capacity
     *
     * @see java.util.WeakHashMap#WeakHashMap(int)
     */
    public WeakWeakMap(int pInitialCapacity) {
        super(pInitialCapacity);
    }

    /**
     * Creates a {@code WeakWeakMap} with the given initial capacity and
     * load factor.
     *
     * @param pInitialCapacity the initial capacity
     * @param pLoadFactor the load factor
     *
     * @see WeakHashMap#WeakHashMap(int, float)
     */
    public WeakWeakMap(int pInitialCapacity, float pLoadFactor) {
        super(pInitialCapacity, pLoadFactor);
    }

    /**
     * Creates a {@code WeakWeakMap} containing the mappings in the given map.
     *
     * @param pMap the map whose mappings are to be placed in this map. 
     *
     * @see WeakHashMap#WeakHashMap(java.util.Map)
     */
    public WeakWeakMap(Map<? extends K, ? extends V> pMap) {
        super(pMap);
    }

    @Override
    public V put(K pKey, V pValue) {
        // NOTE: This is wrong, but we don't really care..
        return super.put(pKey, (V) new WeakReference(pValue));
    }

    @Override
    public V get(Object pKey) {
        WeakReference<V> ref = (WeakReference) super.get(pKey);
        return ref != null ? ref.get() : null;
    }

    @Override
    public V remove(Object pKey) {
        WeakReference<V> ref = (WeakReference) super.remove(pKey);
        return ref != null ? ref.get() : null;
    }

    @Override
    public boolean containsValue(Object pValue) {
        for (final V value : values()) {
            if (pValue == value || (value != null && value.equals(pValue))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> pMap) {
        for (final Map.Entry<? extends K, ? extends V> entry : pMap.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    @SuppressWarnings({"unchecked"})
                    final Iterator<Map.Entry<K, WeakReference<V>>> iterator = (Iterator) WeakWeakMap.super.entrySet().iterator();

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public Map.Entry<K, V> next() {
                        return new Map.Entry<K, V>() {
                            final Map.Entry<K, WeakReference<V>> entry = iterator.next();

                            public K getKey() {
                                return entry.getKey();
                            }

                            public V getValue() {
                                WeakReference<V> ref = entry.getValue();
                                return ref.get();
                            }

                            public V setValue(V pValue) {
                                WeakReference<V> ref = entry.setValue(new WeakReference<V>(pValue));
                                return ref != null ? ref.get() : null;
                            }

                            public boolean equals(Object obj) {
                                return entry.equals(obj);
                            }

                            public int hashCode() {
                                return entry.hashCode();
                            }

                            public String toString() {
                                return entry.toString();
                            }
                        };
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }

            public int size() {
                return WeakWeakMap.this.size();
            }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    @SuppressWarnings({"unchecked"})
                    Iterator<WeakReference<V>> iterator = (Iterator<WeakReference<V>>) WeakWeakMap.super.values().iterator();

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public V next() {
                        WeakReference<V> ref = iterator.next();
                        return ref.get();
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }

            public int size() {
                return WeakWeakMap.this.size();
            }
        };
    }
}
