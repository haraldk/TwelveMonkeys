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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map implementation with size limit, that keeps its entries in LRU
 * (least recently used) order, also known as <em>access-order</em>.
 * When the size limit is reached, the least recently accessed mappings are
 * removed. The number of mappings to be removed from the map, is
 * controlled by the trim factor.
 * <p>
 * <ul>
 *  <li>Default size limit is 1000 elements.
 *      See {@link #setMaxSize(int)}/{@link #getMaxSize()}.</li>
 *  <li>Default trim factor is 1% ({@code 0.01f}).
 *      See {@link #setTrimFactor(float)}/{@link #getTrimFactor()}.</li>
 * </ul>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/LRUHashMap.java#1 $
 */
public class LRUHashMap<K, V> extends LinkedHashMap<K, V> implements ExpiringMap<K, V> {

    private int maxSize = 1000;
    private float trimFactor = 0.01f;

    /**
     * Creates an LRUHashMap with default max size (1000 entries).
     *
     * <small>This is constructor is here to comply with the reccomendations for
     * "standard" constructors in the {@code Map} interface.</small>
     *
     * @see #LRUHashMap(int)
     */
    public LRUHashMap() {
        super(16, .75f, true);
    }

    /**
     * Creates an LRUHashMap with the given max size.
     *
     * @param pMaxSize size limit
     */
    public LRUHashMap(int pMaxSize) {
        super(16, .75f, true);
        setMaxSize(pMaxSize);
    }

    /**
     * Creates an LRUHashMap with initial mappings from the given map,
     * and default max size (1000 entries).
     *
     * <small>This is constructor is here to comply with the reccomendations for
     * "standard" constructors in the {@code Map} interface.</small>
     *
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     *
     * @see #LRUHashMap(java.util.Map, int)
     */
    public LRUHashMap(Map<? extends K, ? extends V> pContents) {
        super(16, .75f, true);
        putAll(pContents);
    }

    /**
     * Creates an LRUHashMap with initial mappings from the given map,
     * and the given max size.
     *
     * @param pContents the map whose mappings are to be placed in this map.
     * May be {@code null}.
     * @param pMaxSize size limit
     */
    public LRUHashMap(Map<? extends K, ? extends V> pContents, int pMaxSize) {
        super(16, .75f, true);
        setMaxSize(pMaxSize);
        putAll(pContents);
    }

    /**
     * Returns the maximum number of mappings in this map.
     *
     * @return the size limit
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum number of elements in this map.
     *
     * If the current size is greater than the new max size, the map will be
     * trimmed to fit the new max size constraint.
     *
     * @see #removeLRU()
     *
     * @param pMaxSize new size limit
     */
    public void setMaxSize(int pMaxSize) {
        if (pMaxSize < 0) {
            throw new IllegalArgumentException("max size must be positive");
        }

        maxSize = pMaxSize;

        while(size() > maxSize) {
            removeLRU();
        }
    }

    /**
     * Returns the current trim factor.
     * <p>
     * The trim factor controls how many percent of the maps current size is
     * reclaimed, when performing an {@code removeLRU} operation.
     * Defaults to 1% ({@code 0.01f}).
     *
     * @return the current trim factor
     */
    public float getTrimFactor() {
        return trimFactor;
    }

    /**
     * Sets the trim factor.
     * <p>
     * The trim factor controls how many percent of the maps current size is
     * reclaimed, when performing an {@code removeLRU} operation.
     * Defaults to 1% ({@code 0.01f}).
     *
     * @param pTrimFactor the new trim factor. Acceptable values are between
     * 0 (inclusive) and 1 (exclusive).
     *
     * @see #removeLRU()
     */
    public void setTrimFactor(float pTrimFactor) {
        if (pTrimFactor < 0f || pTrimFactor >= 1f) {
            throw new IllegalArgumentException("trim factor must be between 0 and 1");
        }

        trimFactor = pTrimFactor;
    }

    /**
     * always returns {@code false}, and instead invokes {@code removeLRU()}
     * if {@code size >= maxSize}.
     */
    protected boolean removeEldestEntry(Map.Entry<K, V> pEldest) {
        // NOTE: As removeLRU() may remove more than one entry, this is better
        // than simply removing the eldest entry.
        if (size() >= maxSize) {
            removeLRU();
        }
        return false;
    }

    /**
     * Default implementation does nothing.
     * May be used by clients as a call-back to notify when mappings expire from
     * the map.
     *
     * @param pRemoved the removed mapping
     */
    public void processRemoved(Map.Entry<K, V> pRemoved) {
    }

    /**
     * Removes the least recently used mapping(s) from this map.
     * <p>
     * How many mappings are removed from the map, is controlled by the
     * trim factor.
     * In any case, at least one mapping will be removed.
     *
     * @see #getTrimFactor()
     */
    public void removeLRU() {
        int removeCount = (int) Math.max((size() * trimFactor), 1);

        Iterator<Map.Entry<K, V>> entries = entrySet().iterator();
        while ((removeCount--) > 0 && entries.hasNext()) {
            entries.next();
            entries.remove();
        }
    }
}
