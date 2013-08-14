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
 * Generic map and linked list implementation of the {@code Map} interface,
 * with predictable iteration order.
 * <p>
 * Resembles {@code LinkedHashMap} from JDK 1.4+, but is backed by a generic
 * {@code Map}, rather than implementing a particular algoritm.
 * <p>
 * This linked list defines the iteration ordering, which is normally the order
 * in which keys were inserted into the map (<em>insertion-order</em>).
 * Note that insertion order is not affected if a key is <em>re-inserted</em>
 * into the map (a key {@code k} is reinserted into a map {@code m} if
 * {@code m.put(k, v)} is invoked when {@code m.containsKey(k)} would return
 * {@code true} immediately prior to the invocation).
 * <p>
 * A special {@link #LinkedMap(boolean) constructor} is provided to create a
 * linked hash map whose order of iteration is the order in which its entries
 * were last accessed, from least-recently accessed to most-recently
 * (<em>access-order</em>).
 * This kind of map is well-suited to building LRU caches.
 * Invoking the {@code put} or {@code get} method results in an access to the
 * corresponding entry (assuming it exists after the invocation completes).
 * The {@code putAll} method generates one entry access for each mapping in
 * the specified map, in the order that key-value mappings are provided by the
 * specified map's entry set iterator.
 * <em>No other methods generate entry accesses.</em>
 * In particular, operations on collection-views do not affect the order of
 * iteration of the backing map.
 * <p>
 * The {@link #removeEldestEntry(Map.Entry)} method may be overridden to impose
 * a policy for removing stale mappings automatically when new mappings are
 * added to the map.
 *
 * @author inspired by LinkedHashMap from JDK 1.4+, by Josh Bloch
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/LinkedMap.java#1 $
 *
 * @see LinkedHashMap
 * @see LRUMap
 */
public class LinkedMap<K, V> extends AbstractDecoratedMap<K, V> implements Serializable {

    transient LinkedEntry<K, V> head;
    protected final boolean accessOrder;

    /**
     * Creates a {@code LinkedMap} backed by a {@code HashMap}, with default
     * (insertion) order.
     */
    public LinkedMap() {
        this(null, false);
    }

    /**
     * Creates a {@code LinkedMap} backed by a {@code HashMap}, with the
     * given order.
     *
     * @param pAccessOrder if {@code true}, ordering will be "least recently
     * accessed item" to "latest accessed item", otherwise "first inserted item"
     * to "latest inserted item".
     */
    public LinkedMap(boolean pAccessOrder) {
        this(null, pAccessOrder);
    }

    /**
     * Creates a {@code LinkedMap} backed by a {@code HashMap}, with key/value
     * pairs copied from {@code pContents} and default (insertion) order.
     *
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     */
    public LinkedMap(Map<? extends K, ? extends V> pContents) {
        this(pContents, false);
    }

    /**
     * Creates a {@code LinkedMap} backed by a {@code HashMap}, with key/value
     * pairs copied from {@code pContents} and the given order.
     *
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     * @param pAccessOrder if {@code true}, ordering will be "least recently
     * accessed item" to "latest accessed item", otherwise "first inserted item"
     * to "latest inserted item".
     */
    public LinkedMap(Map<? extends K, ? extends V> pContents, boolean pAccessOrder) {
        super(pContents);
        accessOrder = pAccessOrder;
    }

    /**
     * Creates a {@code LinkedMap} backed by the given map, with key/value
     * pairs copied from {@code pContents} and default (insertion) order.
     *
     * @param pBacking the backing map of this map. Must be either empty, or
     * the same map as {@code pContents}.
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     */
    public LinkedMap(Map<K, Entry<K, V>> pBacking, Map<? extends K, ? extends V> pContents) {
        this(pBacking, pContents, false);
    }

    /**
     * Creates a {@code LinkedMap} backed by the given map, with key/value
     * pairs copied from {@code pContents} and the given order.
     *
     * @param pBacking the backing map of this map. Must be either empty, or
     * the same map as {@code pContents}.
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     * @param pAccessOrder if {@code true}, ordering will be "least recently
     * accessed item" to "latest accessed item", otherwise "first inserted item"
     * to "latest inserted item".
     */
    public LinkedMap(Map<K, Entry<K, V>> pBacking, Map<? extends K, ? extends V> pContents, boolean pAccessOrder) {
        super(pBacking, pContents);
        accessOrder = pAccessOrder;
    }

    protected void init() {
        head = new LinkedEntry<K, V>(null, null, null) {
            void addBefore(LinkedEntry pExisting) {
                throw new Error();
            }
            void remove() {
                throw new Error();
            }
            public void recordAccess(Map pMap) {
                throw new Error();
            }
            public void recordRemoval(Map pMap) {
                throw new Error();
            }
            public void recordRemoval() {
                throw new Error();
            }
            public V getValue() {
                throw new Error();
            }
            public V setValue(V pValue) {
                throw new Error();
            }
            public K getKey() {
                throw new Error();
            }
            public String toString() {
                return "head";
            }
        };
        head.previous = head.next = head;
    }

    public boolean containsValue(Object pValue) {
        // Overridden to take advantage of faster iterator
        if (pValue == null) {
            for (LinkedEntry e = head.next; e != head; e = e.next) {
                if (e.mValue == null) {
                    return true;
                }
            }
        } else {
            for (LinkedEntry e = head.next; e != head; e = e.next) {
                if (pValue.equals(e.mValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Iterator<K> newKeyIterator()   {
        return new KeyIterator();
    }

    protected Iterator<V> newValueIterator()   {
        return new ValueIterator();
    }

    protected Iterator<Entry<K, V>> newEntryIterator()   {
        return new EntryIterator();
    }

    private abstract class LinkedMapIterator<E> implements Iterator<E> {
        LinkedEntry<K, V> mNextEntry = head.next;
        LinkedEntry<K, V> mLastReturned = null;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        int mExpectedModCount = modCount;

        public boolean hasNext() {
            return mNextEntry != head;
        }

        public void remove() {
            if (mLastReturned == null) {
                throw new IllegalStateException();
            }

            if (modCount != mExpectedModCount) {
                throw new ConcurrentModificationException();
            }

            LinkedMap.this.remove(mLastReturned.mKey);
            mLastReturned = null;

            mExpectedModCount = modCount;
        }

        LinkedEntry<K, V> nextEntry() {
            if (modCount != mExpectedModCount) {
                throw new ConcurrentModificationException();
            }

            if (mNextEntry == head) {
                throw new NoSuchElementException();
            }

            LinkedEntry<K, V> e = mLastReturned = mNextEntry;
            mNextEntry = e.next;

            return e;
        }
    }

    private class KeyIterator extends LinkedMap<K, V>.LinkedMapIterator<K> {
        public K next() {
            return nextEntry().mKey;
        }
    }

    private class ValueIterator extends LinkedMap<K, V>.LinkedMapIterator<V> {
        public V next() {
            return nextEntry().mValue;
        }
    }

    private class EntryIterator extends LinkedMap<K, V>.LinkedMapIterator<Entry<K, V>> {
        public Entry<K, V> next() {
            return nextEntry();
        }
    }

    public V get(Object pKey) {
        LinkedEntry<K, V> entry = (LinkedEntry<K, V>) entries.get(pKey);

        if (entry != null) {
            entry.recordAccess(this);
            return entry.mValue;
        }

        return null;
    }

    public V remove(Object pKey) {
        LinkedEntry<K, V> entry = (LinkedEntry<K, V>) entries.remove(pKey);

        if (entry != null) {
            entry.remove();
            modCount++;

            return entry.mValue;
        }
        return null;
    }

    public V put(K pKey, V pValue) {
        LinkedEntry<K, V> entry = (LinkedEntry<K, V>) entries.get(pKey);
        V oldValue;

        if (entry == null) {
            oldValue = null;

            // Remove eldest entry if instructed, else grow capacity if appropriate
            LinkedEntry<K, V> eldest = head.next;
            if (removeEldestEntry(eldest)) {
                removeEntry(eldest);
            }

            entry = createEntry(pKey, pValue);
            entry.addBefore(head);

            entries.put(pKey, entry);
        }
        else {
            oldValue = entry.mValue;

            entry.mValue = pValue;
            entry.recordAccess(this);
        }

        modCount++;

        return oldValue;
    }

    /**
     * Creates a new {@code LinkedEntry}.
     *
     * @param pKey the key
     * @param pValue the value
     * @return a new LinkedEntry
     */
    /*protected*/ LinkedEntry<K, V> createEntry(K pKey, V pValue) {
        return new LinkedEntry<K, V>(pKey, pValue, null);
    }

    /**
     * @todo
     *
     * @return a copy of this map, with the same order and same key/value pairs.
     */
    public Object clone() throws CloneNotSupportedException {
        LinkedMap map;

        map = (LinkedMap) super.clone();

        // TODO: The rest of the work is PROBABLY handled by
        // AbstractDecoratedMap, but need to verify that.

        return map;
    }

    /**
     * Returns {@code true} if this map should remove its eldest entry.
     * This method is invoked by {@code put} and {@code putAll} after
     * inserting a new entry into the map.  It provides the implementer
     * with the opportunity to remove the eldest entry each time a new one
     * is added.  This is useful if the map represents a cache: it allows
     * the map to reduce memory consumption by deleting stale entries.
     *
     * <p>Sample use: this override will allow the map to grow up to 100
     * entries and then delete the eldest entry each time a new entry is
     * added, maintaining a steady state of 100 entries.
     * <pre>
     *     private static final int MAX_ENTRIES = 100;
     *
     *     protected boolean removeEldestEntry(Map.Entry eldest) {
     *        return size() > MAX_ENTRIES;
     *     }
     * </pre>
     *
     * <p>This method typically does not modify the map in any way,
     * instead allowing the map to modify itself as directed by its
     * return value.  It <i>is</i> permitted for this method to modify
     * the map directly, but if it does so, it <i>must</i> return
     * {@code false} (indicating that the map should not attempt any
     * further modification).  The effects of returning {@code true}
     * after modifying the map from within this method are unspecified.
     *
     * <p>This implementation merely returns {@code false} (so that this
     * map acts like a normal map - the eldest element is never removed).
     *
     * @param    pEldest The least recently inserted entry in the map, or if
     *           this is an access-ordered map, the least recently accessed
     *           entry.  This is the entry that will be removed it this
     *           method returns {@code true}.  If the map was empty prior
     *           to the {@code put} or {@code putAll} invocation resulting
     *           in this invocation, this will be the entry that was just
     *           inserted; in other words, if the map contains a single
     *           entry, the eldest entry is also the newest.
     * @return   {@code true} if the eldest entry should be removed
     *           from the map; {@code false} if it should be retained.
     */
    protected boolean removeEldestEntry(Entry<K, V> pEldest) {
        return false;
    }

    /**
     * Linked list implementation of {@code Map.Entry}.
     */
    protected static class LinkedEntry<K, V> extends BasicEntry<K, V> implements Serializable {
        LinkedEntry<K, V> previous;
        LinkedEntry<K, V> next;

        LinkedEntry(K pKey, V pValue, LinkedEntry<K, V> pNext) {
            super(pKey, pValue);

            next = pNext;
        }

        /**
         * Adds this entry before the given entry (which must be an existing
         * entry) in the list.
         *
         * @param pExisting the entry to add before
         */
        void addBefore(LinkedEntry<K, V> pExisting) {
            next = pExisting;
            previous = pExisting.previous;

            previous.next = this;
            next.previous = this;
        }

        /**
         * Removes this entry from the linked list.
         */
        void remove() {
            previous.next = next;
            next.previous = previous;
        }

        /**
         * If the entry is part of an access ordered list, moves the entry to
         * the end of the list.
         *
         * @param pMap the map to record access for
         */
        protected void recordAccess(Map<K, V> pMap) {
            LinkedMap<K, V> linkedMap = (LinkedMap<K, V>) pMap;
            if (linkedMap.accessOrder) {
                linkedMap.modCount++;
                remove();
                addBefore(linkedMap.head);
            }
        }

        /**
         * Removes this entry from the linked list.
         *
         * @param pMap the map to record removal from
         */
        protected void recordRemoval(Map<K, V> pMap) {
            // TODO: Is this REALLY correct?
            remove();
        }
    }
}