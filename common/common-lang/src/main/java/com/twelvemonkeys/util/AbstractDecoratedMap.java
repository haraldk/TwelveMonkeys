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
import java.util.*;

/**
 * AbstractDecoratedMap
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/AbstractDecoratedMap.java#2 $
 */
// TODO: The generics in this class looks suspicious..
abstract class AbstractDecoratedMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable {
    protected Map<K, Entry<K, V>> entries;
    protected transient volatile int modCount;

    private transient volatile Set<Entry<K, V>> entrySet = null;
    private transient volatile Set<K> keySet = null;
    private transient volatile Collection<V> values = null;

    /**
     * Creates a {@code Map} backed by a {@code HashMap}.
     */
    public AbstractDecoratedMap() {
        this(new HashMap<K, Entry<K, V>>(), null);
    }

    /**
     * Creates a {@code Map} backed by a {@code HashMap}, containing all
     * key/value mappings from the given {@code Map}.
     * <p/>
     * <small>This is constructor is here to comply with the reccomendations for
     * "standard" constructors in the {@code Map} interface.</small>
     *
     * @see #AbstractDecoratedMap(java.util.Map, java.util.Map)
     *
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     */
    public AbstractDecoratedMap(Map<? extends K, ? extends V> pContents) {
        this(new HashMap<K, Entry<K, V>>(), pContents);
    }

    /**
     * Creates a {@code Map} backed by the given backing-{@code Map},
     * containing all key/value mappings from the given contents-{@code Map}.
     * <p/>
     * NOTE: The backing map is structuraly cahnged, and it should NOT be
     * accessed directly, after the wrapped map is created.
     *
     * @param pBacking the backing map of this map. Must be either empty, or
     * the same map as {@code pContents}.
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     *
     * @throws IllegalArgumentException if {@code pBacking} is {@code null}
     *         or if {@code pBacking} differs from {@code pContent} and is not empty.
     */
    public AbstractDecoratedMap(Map<K, Entry<K, V>> pBacking, Map<? extends K, ? extends V> pContents) {
        if (pBacking == null) {
            throw new IllegalArgumentException("backing == null");
        }

        Entry<? extends K, ? extends V>[] entries = null;
        if (pBacking == pContents) {
            // NOTE: Special treatment to avoid ClassCastExceptions
            Set<? extends Entry<? extends K, ? extends V>> es = pContents.entrySet();
            //noinspection unchecked
            entries = new Entry[es.size()];
            entries = es.toArray(entries);
            pContents = null;
            pBacking.clear();
        }
        else if (!pBacking.isEmpty()) {
            throw new IllegalArgumentException("backing must be empty");
        }

        this.entries = pBacking;
        init();

        if (pContents != null) {
            putAll(pContents);
        }
        else if (entries != null) {
            // Reinsert entries, this time wrapped
            for (Entry<? extends K, ? extends V> entry : entries) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Default implementation, does nothing.
     */
    protected void init() {
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
        modCount++;
        init();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean containsKey(Object pKey) {
        return entries.containsKey(pKey);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified pValue.  More formally, returns {@code true} if and only if
     * this map contains at least one mapping to a pValue {@code v} such that
     * {@code (pValue==null ? v==null : pValue.equals(v))}.
     * <p/>
     * This implementation requires time linear in the map size for this
     * operation.
     *
     * @param pValue pValue whose presence in this map is to be tested.
     * @return {@code true} if this map maps one or more keys to the
     *         specified pValue.
     */
    public boolean containsValue(Object pValue) {
        for (V value : values()) {
            if (value == pValue || (value != null && value.equals(pValue))) {
                return true;
            }
        }

        return false;
    }

    public Collection<V> values() {
        Collection<V> values = this.values;
        return values != null ? values : (this.values = new Values());
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    public Set<K> keySet() {
        Set<K> ks = keySet;
        return ks != null ? ks : (keySet = new KeySet());
    }

    /**
     * Returns a shallow copy of this {@code AbstractMap} instance: the keys
     * and values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    protected Object clone() throws CloneNotSupportedException {
        AbstractDecoratedMap map = (AbstractDecoratedMap) super.clone();

        map.values = null;
        map.entrySet = null;
        map.keySet = null;

        // TODO: Implement: Need to clone the backing map...

        return map;
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    protected abstract Iterator<K> newKeyIterator();

    protected abstract Iterator<V> newValueIterator();

    protected abstract Iterator<Entry<K, V>> newEntryIterator();

    // TODO: Implement these (get/put/remove)?
    public abstract V get(Object pKey);

    public abstract V remove(Object pKey);

    public abstract V put(K pKey, V pValue);

    /*protected*/ Entry<K, V> createEntry(K pKey, V pValue) {
        return new BasicEntry<K, V>(pKey, pValue);
    }

    /*protected*/  Entry<K, V> getEntry(K pKey) {
        return entries.get(pKey);
    }

    /**
     * Removes the given entry from the Map.
     *
     * @param pEntry the entry to be removed
     *
     * @return the removed entry, or {@code null} if nothing was removed.
     */
    protected Entry<K, V> removeEntry(Entry<K, V> pEntry) {
        if (pEntry == null) {
            return null;
        }

        // Find candidate entry for this key
        Entry<K, V> candidate = getEntry(pEntry.getKey());
        if (candidate == pEntry || (candidate != null && candidate.equals(pEntry))) {
            // Remove
            remove(pEntry.getKey());
            return pEntry;
        }
        return null;
    }

    protected class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return newValueIterator();
        }

        public int size() {
            return AbstractDecoratedMap.this.size();
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public void clear() {
            AbstractDecoratedMap.this.clear();
        }
    }

    protected class EntrySet extends AbstractSet<Entry<K, V>> {
        public Iterator<Entry<K, V>> iterator() {
            return newEntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry e = (Entry) o;

            //noinspection SuspiciousMethodCalls
            Entry<K, V> candidate = entries.get(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }

            /*
            // NOTE: Extra cautions is taken, to only remove the entry if it
            // equals the entry in the map
            Object key = ((Entry) o).getKey();
            Entry entry = (Entry) entries.get(key);

            // Same entry?
            if (entry != null && entry.equals(o)) {
                return AbstractWrappedMap.this.remove(key) != null;
            }

            return false;
            */

            //noinspection unchecked
            return AbstractDecoratedMap.this.removeEntry((Entry) o) != null;
        }

        public int size() {
            return AbstractDecoratedMap.this.size();
        }

        public void clear() {
            AbstractDecoratedMap.this.clear();
        }
    }

    protected class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return newKeyIterator();
        }
        public int size() {
            return AbstractDecoratedMap.this.size();
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return AbstractDecoratedMap.this.remove(o) != null;
        }
        public void clear() {
            AbstractDecoratedMap.this.clear();
        }
    }

    /**
     * A simple Map.Entry implementaton.
     */
    static class BasicEntry<K, V> implements Entry<K, V>, Serializable {
        K mKey;
        V mValue;

        BasicEntry(K pKey, V pValue) {
            mKey = pKey;
            mValue = pValue;
        }

        /**
         * Default implementation does nothing.
         *
         * @param pMap the map that is accessed
         */
        protected void recordAccess(Map<K, V> pMap) {
        }

        /**
         * Default implementation does nothing.
         * @param pMap the map that is removed from
         */
        protected void recordRemoval(Map<K, V> pMap) {
        }

        public V getValue() {
            return mValue;
        }

        public V setValue(V pValue) {
            V oldValue = mValue;
            mValue = pValue;
            return oldValue;
        }

        public K getKey() {
            return mKey;
        }

        public boolean equals(Object pOther) {
            if (!(pOther instanceof Map.Entry)) {
                return false;
            }

            Map.Entry entry = (Map.Entry) pOther;

            Object k1 = mKey;
            Object k2 = entry.getKey();

            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = mValue;
                Object v2 = entry.getValue();

                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }

            return false;
        }

        public int hashCode() {
            return (mKey == null ? 0 : mKey.hashCode()) ^
                   (mValue == null ? 0 : mValue.hashCode());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }
}