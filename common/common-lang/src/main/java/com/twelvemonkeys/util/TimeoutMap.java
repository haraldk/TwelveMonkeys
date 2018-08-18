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
 * A {@code Map} implementation that removes (exipres) its elements after
 * a given period. The map is by default backed by a {@link java.util.HashMap},
 * or can be instantiated with any given {@code Map} as backing.
 * <P/>
 * Notes to consider when using this map:
 * <ul>
 *  <li>Elements may not expire on the exact millisecond as expected.</li>
 *  <li>The value returned by the {@code size()} method  of the map, or any of
 *      its collection views, may not represent
 *      the exact number of entries in the map at any given time.</li>
 *  <li>Elements in this map may expire at any time
 *      (but never between invocations of {@code Iterator.hasNext()}
 *      and {@code Iterator.next()} or {@code Iterator.remove()},
 *      when iterating the collection views).</li>
 * </ul>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/TimeoutMap.java#2 $
 *
 * @todo Consider have this Map extend LinkedMap.. That way the removeExpired
 * method only have to run from the first element, until it finds an element
 * that should not expire, as elements are in insertion order.
 * and next expiry time would be the time of the first element.
 * @todo Consider running the removeExpiredEntries method in a separate (deamon) thread
 * @todo - or document why it is not such a good idea.
 */
public class TimeoutMap<K, V> extends AbstractDecoratedMap<K, V> implements ExpiringMap<K, V>, Serializable, Cloneable {
    /**
     * Expiry time
     */
    protected long expiryTime = 60000L;  // 1 minute

    //////////////////////
    private volatile long nextExpiryTime;
    //////////////////////

    /**
     * Creates a {@code TimeoutMap} with the default expiry time of 1 minute.
     * This {@code TimeoutMap} will be backed by a new {@code HashMap} instance.
     * <p/>
     * <small>This is constructor is here to comply with the reccomendations for
     * "standard" constructors in the {@code Map} interface.</small>
     *
     * @see #TimeoutMap(long)
     */
    public TimeoutMap() {
        super();
    }

    /**
     * Creates a {@code TimeoutMap} containing the same elements as the given map
     * with the default expiry time of 1 minute.
     * This {@code TimeoutMap} will be backed by a new {@code HashMap} instance,
     * and <em>not</em> the map passed in as a paramter.
     * <p/>
     * <small>This is constructor is here to comply with the reccomendations for
     * "standard" constructors in the {@code Map} interface.</small>
     *
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     * @see #TimeoutMap(java.util.Map, Map, long)
     * @see java.util.Map
     */
    public TimeoutMap(Map<? extends K, ? extends V> pContents) {
        super(pContents);
    }

    /**
     * Creates a {@code TimeoutMap} with the given expiry time (milliseconds).
     * This {@code TimeoutMap} will be backed by a new {@code HashMap} instance.
     *
     * @param pExpiryTime the expiry time (time to live) for elements in this map
     */
    public TimeoutMap(long pExpiryTime) {
        this();
        expiryTime = pExpiryTime;
    }

    /**
     * Creates a {@code TimeoutMap} with the given expiry time (milliseconds).
     * This {@code TimeoutMap} will be backed by the given {@code Map}.
     * <P/>
     * <EM>Note that structurally modifying the backing map directly (not
     * through this map or its collection views), is not allowed, and will
     * produce undeterministic exceptions.</EM>
     *
     * @param pBacking the map that will be used as backing.
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     * @param pExpiryTime the expiry time (time to live) for elements in this map
     */
    public TimeoutMap(Map<K, Map.Entry<K, V>> pBacking, Map<? extends K, ? extends V> pContents, long pExpiryTime) {
        super(pBacking, pContents);
        expiryTime = pExpiryTime;
    }

    /**
     * Gets the maximum time any value will be kept in the map, before it expires.
     *
     * @return the expiry time
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Sets the maximum time any value will be kept in the map, before it expires.
     * Removes any items that are older than the specified time.
     *
     * @param pExpiryTime the expiry time (time to live) for elements in this map
     */
    public void setExpiryTime(long pExpiryTime) {
        long oldEexpiryTime = expiryTime;

        expiryTime = pExpiryTime;

        if (expiryTime < oldEexpiryTime) {
            // Expire now
            nextExpiryTime = 0;
            removeExpiredEntries();
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
        removeExpiredEntries();
        return entries.size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return (size() <= 0);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * pKey.
     *
     * @param pKey pKey whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified
     *         pKey.
     */
    public boolean containsKey(Object pKey) {
        removeExpiredEntries();
        return entries.containsKey(pKey);
    }

    /**
     * Returns the value to which this map maps the specified pKey.  Returns
     * {@code null} if the map contains no mapping for this pKey.  A return
     * value of {@code null} does not <i>necessarily</i> indicate that the
     * map contains no mapping for the pKey; it's also possible that the map
     * explicitly maps the pKey to {@code null}.  The {@code containsKey}
     * operation may be used to distinguish these two cases.
     *
     * @param pKey pKey whose associated value is to be returned.
     * @return the value to which this map maps the specified pKey, or
     *         {@code null} if the map contains no mapping for this pKey.
     * @see #containsKey(java.lang.Object)
     */
    public V get(Object pKey) {
        TimedEntry<K, V> entry = (TimedEntry<K, V>) entries.get(pKey);

        if (entry == null) {
            return null;
        }
        else if (entry.isExpired()) {
            //noinspection SuspiciousMethodCalls
            entries.remove(pKey);
            processRemoved(entry);
            return null;
        }
        return entry.getValue();
    }

    /**
     * Associates the specified pValue with the specified pKey in this map
     * (optional operation).  If the map previously contained a mapping for
     * this pKey, the old pValue is replaced.
     *
     * @param pKey   pKey with which the specified pValue is to be associated.
     * @param pValue pValue to be associated with the specified pKey.
     * @return previous pValue associated with specified pKey, or {@code null}
     *         if there was no mapping for pKey.  A {@code null} return can
     *         also indicate that the map previously associated {@code null}
     *         with the specified pKey, if the implementation supports
     *         {@code null} values.
     */
    public V put(K pKey, V pValue) {
        TimedEntry<K, V> entry = (TimedEntry<K, V>) entries.get(pKey);
        V oldValue;

        if (entry == null) {
            oldValue = null;

            entry = createEntry(pKey, pValue);

            entries.put(pKey, entry);
        }
        else {
            oldValue = entry.mValue;
            entry.setValue(pValue);
            entry.recordAccess(this);
        }

        // Need to remove expired objects every now and then
        // We do it in the put method, to avoid resource leaks over time.
        removeExpiredEntries();
        modCount++;

        return oldValue;
    }

    /**
     * Removes the mapping for this pKey from this map if present (optional
     * operation).
     *
     * @param pKey pKey whose mapping is to be removed from the map.
     * @return previous value associated with specified pKey, or {@code null}
     *         if there was no mapping for pKey.  A {@code null} return can
     *         also indicate that the map previously associated {@code null}
     *         with the specified pKey, if the implementation supports
     *         {@code null} values.
     */
    public V remove(Object pKey) {
        TimedEntry<K, V> entry = (TimedEntry<K, V>) entries.remove(pKey);
        return (entry != null) ? entry.getValue() : null;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        entries.clear();  // Finally something straightforward.. :-)
        init();
    }

    /*protected*/ TimedEntry<K, V> createEntry(K pKey, V pValue) {
        return new TimedEntry<K, V>(pKey, pValue);
    }

    /**
     * Removes any expired mappings.
     *
     */
    protected void removeExpiredEntries() {
        // Remove any expired elements
        long now = System.currentTimeMillis();
        if (now > nextExpiryTime) {
            removeExpiredEntriesSynced(now);
        }
    }

    /**
     * Okay, I guess this do resemble DCL...
     *
     * @todo Write some exhausting multi-threaded unit-tests.
     *
     * @param pTime now
     */
    private synchronized void removeExpiredEntriesSynced(long pTime) {
        if (pTime > nextExpiryTime) {
            ////
            long next = Long.MAX_VALUE;
            nextExpiryTime = next; // Avoid multiple runs...
            for (Iterator<Entry<K, V>> iterator = new EntryIterator(); iterator.hasNext();) {
                TimedEntry<K, V> entry = (TimedEntry<K, V>) iterator.next();
                ////
                long expires = entry.expires();
                if (expires < next) {
                    next = expires;
                }
                ////
            }
            ////
            nextExpiryTime = next;
        }
    }

    public Collection<V> values() {
        removeExpiredEntries();
        return super.values();
    }

    public Set<Entry<K, V>> entrySet() {
        removeExpiredEntries();
        return super.entrySet();
    }

    public Set<K> keySet() {
        removeExpiredEntries();
        return super.keySet();
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    protected Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    protected Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    protected Iterator<Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    public void processRemoved(Entry pRemoved) {
    }

    /**
     * Note: Iterating through this iterator will remove any expired values.
     */
    private abstract class TimeoutMapIterator<E> implements Iterator<E> {
        Iterator<Entry<K, Entry<K, V>>> mIterator = entries.entrySet().iterator();
        BasicEntry<K, V> mNext;
        long mNow = System.currentTimeMillis();

        public void remove() {
            mNext = null; // advance
            mIterator.remove();
        }

        public boolean hasNext() {
            if (mNext != null) {
                return true; // Never expires between hasNext and next/remove!
            }

            while (mNext == null && mIterator.hasNext()) {
                Entry<K, Entry<K, V>> entry = mIterator.next();
                TimedEntry<K, V> timed = (TimedEntry<K, V>) entry.getValue();

                if (timed.isExpiredBy(mNow)) {
                    // Remove from map, and continue
                    mIterator.remove();
                    processRemoved(timed);
                }
                else {
                    // Go with this entry
                    mNext = timed;
                    return true;
                }
            }

            return false;
        }

        BasicEntry<K, V> nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            BasicEntry<K, V> entry = mNext;
            mNext = null; // advance
            return entry;
        }
    }

    private class KeyIterator extends TimeoutMapIterator<K> {
        public K next() {
            return nextEntry().mKey;
        }
    }

    private class ValueIterator extends TimeoutMapIterator<V> {
        public V next() {
            return nextEntry().mValue;
        }
    }

    private class EntryIterator extends TimeoutMapIterator<Entry<K, V>> {
        public Entry<K, V> next() {
            return nextEntry();
        }
    }

    /**
     * Keeps track of timed objects
     */
    private class TimedEntry<K, V> extends BasicEntry<K, V> {
        private long mTimestamp;

        TimedEntry(K pKey, V pValue) {
            super(pKey, pValue);
            mTimestamp = System.currentTimeMillis();
        }

        public V setValue(V pValue) {
            mTimestamp = System.currentTimeMillis();
            return super.setValue(pValue);
        }

        final boolean isExpired() {
            return isExpiredBy(System.currentTimeMillis());
        }

        final boolean isExpiredBy(final long pTime) {
            return pTime > expires();
        }

        final long expires() {
            return mTimestamp + expiryTime;
        }
    }
}
