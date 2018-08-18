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

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * TimeoutMapTest
 * <p/>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/TimeoutMapTestCase.java#2 $
 */
public class TimeoutMapTest extends MapAbstractTest {

    public Map makeEmptyMap() {
        return new TimeoutMap(60 * 60 * 1000);
    }
    /*
     * The basic Map interface lets one associate keys and values:
     */

    /**
     * Method testBasicMap
     */
    @Test
    public void testBasicMap() {

        Map map = new TimeoutMap(60000L);
        Object key = "key";
        Object value = new Integer(3);

        map.put(key, value);
        assertEquals(value, map.get(key));
    }

    /*
     * If there is no value associated with a key,
     * the basic Map will return null for that key:
     */

    /**
     * Method testBasicMapReturnsNullForMissingKey
     */
    @Test
    public void testBasicMapReturnsNullForMissingKey() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.get("key"));
    }

    /*
     * One can also explicitly store a null value for
     * some key:
     */

    /**
     * Method testBasicMapAllowsNull
     */
    @Test
    public void testBasicMapAllowsNull() {

        Map map = new TimeoutMap(60000L);
        Object key = "key";
        Object value = null;

        map.put(key, value);
        assertNull(map.get(key));
    }

    /**
     * Method testBasicMapAllowsMultipleTypes
     */
    @Test
    public void testBasicMapAllowsMultipleTypes() {

        Map map = new TimeoutMap(60000L);

        map.put("key-1", "value-1");
        map.put(new Integer(2), "value-2");
        map.put("key-3", new Integer(3));
        map.put(new Integer(4), new Integer(4));
        map.put(Boolean.FALSE, "");
        assertEquals("value-1", map.get("key-1"));
        assertEquals("value-2", map.get(new Integer(2)));
        assertEquals(new Integer(3), map.get("key-3"));
        assertEquals(new Integer(4), map.get(new Integer(4)));
        assertEquals("", map.get(Boolean.FALSE));
    }

    /**
     * Method testBasicMapStoresOnlyOneValuePerKey
     */
    @Test
    public void testBasicMapStoresOnlyOneValuePerKey() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key", "value-1"));
        assertEquals("value-1", map.get("key"));
        assertEquals("value-1", map.put("key", "value-2"));
        assertEquals("value-2", map.get("key"));
    }

    /**
     * Method testBasicMapValuesView
     */
    @Test
    public void testBasicMapValuesView() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key-1", new Integer(1)));
        assertNull(map.put("key-2", new Integer(2)));
        assertNull(map.put("key-3", new Integer(3)));
        assertNull(map.put("key-4", new Integer(4)));
        assertEquals(4, map.size());

        Collection values = map.values();
        assertEquals(4, values.size());

        Iterator it = values.iterator();

        assertNotNull(it);
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();

            assertNotNull(o);
            assertTrue(o instanceof Integer);
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Method testBasicMapKeySetView
     */
    @Test
    public void testBasicMapKeySetView() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key-1", "value-1"));
        assertNull(map.put("key-2", "value-2"));
        assertNull(map.put("key-3", "value-3"));
        assertNull(map.put("key-4", "value-4"));
        assertEquals(4, map.size());
        Iterator it = map.keySet().iterator();

        assertNotNull(it);
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();

            assertNotNull(o);
            assertTrue(o instanceof String);
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Method testBasicMapEntrySetView
     */
    @Test
    public void testBasicMapEntrySetView() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key-1", new Integer(1)));
        assertNull(map.put("key-2", "value-2"));
        assertNull(map.put("key-3", new Object()));
        assertNull(map.put("key-4", Boolean.FALSE));
        assertEquals(4, map.size());
        Iterator it = map.entrySet().iterator();

        assertNotNull(it);
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();

            assertNotNull(o);
            assertTrue(o instanceof Map.Entry);
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Method testBasicMapValuesView
     */
    @Test
    public void testBasicMapValuesViewRemoval() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key-1", new Integer(1)));
        assertNull(map.put("key-2", new Integer(2)));
        assertNull(map.put("key-3", new Integer(3)));
        assertNull(map.put("key-4", new Integer(4)));
        assertEquals(4, map.size());
        Iterator it = map.values().iterator();

        assertNotNull(it);
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();

            assertNotNull(o);
            assertTrue(o instanceof Integer);
            try {
                it.remove();
            }
            catch (UnsupportedOperationException e) {
                fail("Removal failed");
            }
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Method testBasicMapKeySetView
     */
    @Test
    public void testBasicMapKeySetViewRemoval() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key-1", "value-1"));
        assertNull(map.put("key-2", "value-2"));
        assertNull(map.put("key-3", "value-3"));
        assertNull(map.put("key-4", "value-4"));
        assertEquals(4, map.size());
        Iterator it = map.keySet().iterator();

        assertNotNull(it);
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();

            assertNotNull(o);
            assertTrue(o instanceof String);
            try {
                it.remove();
            }
            catch (UnsupportedOperationException e) {
                fail("Removal failed");
            }
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Method testBasicMapEntrySetView
     */
    @Test
    public void testBasicMapEntrySetViewRemoval() {

        Map map = new TimeoutMap(60000L);

        assertNull(map.put("key-1", new Integer(1)));
        assertNull(map.put("key-2", "value-2"));
        assertNull(map.put("key-3", new Object()));
        assertNull(map.put("key-4", Boolean.FALSE));
        assertEquals(4, map.size());
        Iterator it = map.entrySet().iterator();

        assertNotNull(it);
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();

            assertNotNull(o);
            assertTrue(o instanceof Map.Entry);
            try {
                it.remove();
            }
            catch (UnsupportedOperationException e) {
                fail("Removal failed");
            }
            count++;
        }
        assertEquals(4, count);
    }

    /**
     * Method testBasicMapStoresOnlyOneValuePerKey
     */
    @Test
    public void testTimeoutReturnNull() {

        Map map = new TimeoutMap(100L);

        assertNull(map.put("key", "value-1"));
        assertEquals("value-1", map.get("key"));
        assertNull(map.put("another", "value-2"));
        assertEquals("value-2", map.get("another"));
        synchronized (this) {
            try {
                Thread.sleep(110L);
            }
            catch (InterruptedException e) {
                // Continue, but might break the timeout thing below...
            }
        }

        // Values should now time out
        assertNull(map.get("key"));
        assertNull(map.get("another"));
    }

    /**
     * Method testTimeoutIsEmpty
     */
    @Test
    public void testTimeoutIsEmpty() {

        TimeoutMap map = new TimeoutMap(50L);

        assertNull(map.put("key", "value-1"));
        assertEquals("value-1", map.get("key"));
        assertNull(map.put("another", "value-2"));
        assertEquals("value-2", map.get("another"));
        synchronized (this) {
            try {
                Thread.sleep(100L);
            }
            catch (InterruptedException e) {
                // Continue, but might break the timeout thing below...
            }
        }

        // This for loop should not print anything, if the tests succeed.
        Set set = map.keySet();
        assertEquals(0, set.size());
        for (Iterator iterator = set.iterator(); iterator.hasNext(); System.out.println(iterator.next())) {
            ;
        }
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /**
     * Method testTimeoutWrapIsEmpty
     */
    @Test
    public void testTimeoutWrapIsEmpty() {

        Map map = new TimeoutMap(new LRUMap(2), null, 100L);

        assertNull(map.put("key", "value-1"));
        assertEquals("value-1", map.get("key"));
        assertNull(map.put("another", "value-2"));
        assertEquals("value-2", map.get("another"));
        assertNull(map.put("third", "value-3"));
        assertEquals("value-3", map.get("third"));
        synchronized (this) {
            try {
                Thread.sleep(110L);
            }
            catch (InterruptedException e) {
                // Continue, but might break the timeout thing below...
            }
        }

        // This for loop should not print anything, if the tests succeed.
        Set set = map.keySet();
        assertEquals(0, set.size());
        for (Iterator iterator = set.iterator(); iterator.hasNext(); System.out.println(iterator.next())) {
            ;
        }
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /**
     * Method testTimeoutWrapReturnNull
     */
    @Test
    public void testTimeoutWrapReturnNull() {

        Map map = new TimeoutMap(new LRUMap(), null, 100L);

        assertNull(map.put("key", "value-1"));
        assertEquals("value-1", map.get("key"));
        assertNull(map.put("another", "value-2"));
        assertEquals("value-2", map.get("another"));
        synchronized (this) {
            try {
                Thread.sleep(110L);
            }
            catch (InterruptedException e) {
                // Continue, but might break the timeout thing below...
            }
        }

        // Values should now time out
        assertNull(map.get("key"));
        assertNull(map.get("another"));
    }

    /**
     * Method testWrapMaxSize
     */
    @Test
    public void testWrapMaxSize() {

        LRUMap lru = new LRUMap();

        lru.setMaxSize(2);
        TimeoutMap map = new TimeoutMap(lru, null, 1000L);

        assertNull(map.put("key", "value-1"));
        assertEquals("value-1", map.get("key"));
        assertNull(map.put("another", "value-2"));
        assertEquals("value-2", map.get("another"));
        assertNull(map.put("third", "value-3"));
        assertEquals("value-3", map.get("third"));

        // This value should have expired
        assertNull(map.get("key"));

        // These should be left
        assertEquals("value-2", map.get("another"));
        assertEquals("value-3", map.get("third"));
    }

    /**
     * Method testWrapMapContainingValues
     */
    @Test
    public void testWrapMapContainingValues() {

        Map backing = new TreeMap();

        backing.put("key", "original");
        TimeoutMap map = null;

        try {
            map = new TimeoutMap(backing, backing, 1000L);
            Object value = map.put("key", "value-1");
            assertNotNull(value);  // Should now have value!
            assertEquals("original", value);
        }
        catch (ClassCastException cce) {
            cce.printStackTrace();
            fail("Content not converted to TimedEntries properly!");
        }
        assertEquals("value-1", map.get("key"));
        assertNull(map.put("another", "value-2"));
        assertEquals("value-2", map.get("another"));
        assertNull(map.put("third", "value-3"));
        assertEquals("value-3", map.get("third"));
    }

    @Test
    public void testIteratorRemove() {
        Map map = makeFullMap();

        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            iterator.remove();
        }
        assertEquals(0, map.size());

        map = makeFullMap();

        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            iterator.next();
            iterator.remove();
        }
        assertEquals(0, map.size());
    }

    @Test
    public void testIteratorPredictableNext() {
        TimeoutMap map = (TimeoutMap) makeFullMap();
        map.setExpiryTime(50l);
        assertFalse(map.isEmpty());

        int count = 0;
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            if (count == 0) {
                // NOTE: Only wait fist time, to avoid slooow tests
                synchronized (this) {
                    try {
                        wait(60l);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }

            try {
                Map.Entry entry = (Map.Entry) iterator.next();
                assertNotNull(entry);
                count++;
            }
            catch (NoSuchElementException nse) {
                fail("Elements expire between Interator.hasNext() and Iterator.next()");
            }
        }

        assertTrue("Elements expired too early, test did not run as expected.", count > 0);
        //assertEquals("Elements did not expire as expected.", 1, count);
    }

    @Test
    public void testIteratorPredictableRemove() {
        TimeoutMap map = (TimeoutMap) makeFullMap();
        map.setExpiryTime(50l);
        assertFalse(map.isEmpty());

        int count = 0;
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            if (count == 0) {
                // NOTE: Only wait fist time, to avoid slooow tests
                synchronized (this) {
                    try {
                        wait(60l);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }

            try {
                iterator.remove();
                count++;
            }
            catch (NoSuchElementException nse) {
                fail("Elements expired between Interator.hasNext() and Iterator.remove()");
            }
        }

        assertTrue("Elements expired too early, test did not run as expected.", count > 0);
        //assertEquals("Elements did not expire as expected.", 1, count);
    }

    @Test
    public void testIteratorPredictableNextRemove() {
        TimeoutMap map = (TimeoutMap) makeFullMap();
        map.setExpiryTime(50l);
        assertFalse(map.isEmpty());

        int count = 0;
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            if (count == 0) {
                // NOTE: Only wait fist time, to avoid slooow tests
                synchronized (this) {
                    try {
                        wait(60l);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }

            try {
                Map.Entry entry = (Map.Entry) iterator.next();
                assertNotNull(entry);
            }
            catch (NoSuchElementException nse) {
                fail("Elements expired between Interator.hasNext() and Iterator.next()");
            }

            try {
                iterator.remove();
                count++;
            }
            catch (NoSuchElementException nse) {
                fail("Elements expired between Interator.hasNext() and Iterator.remove()");
            }
        }

        assertTrue("Elements expired too early, test did not run as expected.", count > 0);
        //assertEquals("Elements did not expire as expected.", 1, count);
    }

    @Test
    public void testIteratorPredictableRemovedEntry() {
        TimeoutMap map = (TimeoutMap) makeEmptyMap();
        map.setExpiryTime(1000l); // No elements should expire during this test

        map.put("key-1", new Integer(1));
        map.put("key-2", new Integer(2));

        assertFalse(map.isEmpty());

        Object removedKey = null;
        Object otherKey = null;
        Iterator iterator = map.entrySet().iterator();
        assertTrue("Iterator was empty", iterator.hasNext());
        try {
            Map.Entry entry = (Map.Entry) iterator.next();
            assertNotNull(entry);
            removedKey = entry.getKey();
            otherKey = "key-1".equals(removedKey) ? "key-2" : "key-1";
        }
        catch (NoSuchElementException nse) {
            fail("Elements expired between Interator.hasNext() and Iterator.next()");
        }

        try {
            iterator.remove();
        }
        catch (NoSuchElementException nse) {
            fail("Elements expired between Interator.hasNext() and Iterator.remove()");
        }

        assertTrue("Wrong entry removed, keySet().iterator() is broken.", !map.containsKey(removedKey));
        assertTrue("Wrong entry removed, keySet().iterator() is broken.", map.containsKey(otherKey));
    }
}

