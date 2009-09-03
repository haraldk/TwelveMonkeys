/*
 *  Copyright 2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twelvemonkeys.util;

import java.util.*;

/**
 * Tests LRUMap.
 *
 * @version $Revision: #2 $ $Date: 2008/07/15 $
 *
 * @author James Strachan
 * @author Morgan Delagrange
 * @author Stephen Colebourne
 */
public class LRUMapTestCase extends LinkedMapTestCase {

    public boolean isGetStructuralModify() {
        return true;
    }

    //-----------------------------------------------------------------------
    public Map makeEmptyMap() {
        LRUMap map = new LRUMap();
        return map;
    }

    //-----------------------------------------------------------------------
    public void testRemoveLRU() {
        LRUMap map2 = new LRUMap(3);
        map2.put(new Integer(1),"foo");
        map2.put(new Integer(2),"foo");
        map2.put(new Integer(3),"foo");
        map2.put(new Integer(4),"foo"); // removes 1 since max size exceeded
        map2.removeLRU();  // should be Integer(2)

        assertTrue("Second to last value should exist",map2.get(new Integer(3)).equals("foo"));
        assertTrue("First value inserted should not exist", map2.get(new Integer(1)) == null);
    }

    public void testMultiplePuts() {
        LRUMap map2 = new LRUMap(2);
        map2.put(new Integer(1),"foo");
        map2.put(new Integer(2),"bar");
        map2.put(new Integer(3),"foo");
        map2.put(new Integer(4),"bar");

        assertTrue("last value should exist",map2.get(new Integer(4)).equals("bar"));
        assertTrue("LRU should not exist", map2.get(new Integer(1)) == null);
    }

    /**
     * Confirm that putAll(Map) does not cause the LRUMap
     * to exceed its maxiumum size.
     */
    public void testPutAll() {
        LRUMap map2 = new LRUMap(3);
        map2.put(new Integer(1),"foo");
        map2.put(new Integer(2),"foo");
        map2.put(new Integer(3),"foo");

        HashMap hashMap = new HashMap();
        hashMap.put(new Integer(4),"foo");

        map2.putAll(hashMap);

        assertTrue("max size is 3, but actual size is " + map2.size(),
                   map2.size() == 3);
        assertTrue("map should contain the Integer(4) object",
                   map2.containsKey(new Integer(4)));
    }

    /**
     * Test that the size of the map is reduced immediately
     * when setMaximumSize(int) is called
     */
    public void testSetMaximumSize() {
        LRUMap map = new LRUMap(6);
        map.put("1","1");
        map.put("2","2");
        map.put("3","3");
        map.put("4","4");
        map.put("5","5");
        map.put("6","6");
        map.setMaxSize(3);

        assertTrue("map should have size = 3, but actually = " + map.size(),
                   map.size() == 3);
    }

    public void testGetPromotion() {
        LRUMap map = new LRUMap(3);
        map.put("1","1");
        map.put("2","2");
        map.put("3","3");
        // LRU is now 1 (then 2 then 3)

        // promote 1 to top
        // eviction order is now 2,3,1
        map.get("1");

        // add another value, forcing a remove
        // 2 should be evicted (then 3,1,4)
        map.put("4","4");

        Iterator keyIterator = map.keySet().iterator();
        Object[] keys = new Object[3];
        for (int i = 0; keyIterator.hasNext() ; ++i) {
            keys[i] = keyIterator.next();
        }

        assertTrue("first evicted should be 3, was " + keys[0], keys[0].equals("3"));
        assertTrue("second evicted should be 1, was " + keys[1], keys[1].equals("1"));
        assertTrue("third evicted should be 4, was " + keys[2], keys[2].equals("4"));

    }

    /**
     * You should be able to subclass LRUMap and perform a
     * custom action when items are removed automatically
     * by the LRU algorithm (the removeLRU() method).
     */
    public void testLRUSubclass() {
        LRUCounter counter = new LRUCounter(3);
        // oldest <--> newest
        // 1
        counter.put("1","foo");
        // 1 2
        counter.put("2","foo");
        // 1 2 3
        counter.put("3","foo");
        // 2 3 1
        counter.put("1","foo");
        // 3 1 4 (2 goes out)
        counter.put("4","foo");
        // 1 4 5 (3 goes out)
        counter.put("5","foo");
        // 4 5 2 (1 goes out)
        counter.put("2","foo");
        // 4 2
        counter.remove("5");

        assertTrue("size should be 2, but was " + counter.size(), counter.size() == 2);
        assertTrue("removedCount should be 3 but was " + counter.removedCount,
                   counter.removedCount == 3);

        assertTrue("first removed was '2'",counter.list.get(0).equals("2"));
        assertTrue("second removed was '3'",counter.list.get(1).equals("3"));
        assertTrue("third removed was '1'",counter.list.get(2).equals("1"));

        //assertTrue("oldest key is '4'",counter.get(0).equals("4"));
        //assertTrue("newest key is '2'",counter.get(1).equals("2"));
    }

    private class LRUCounter extends LRUMap {
        int removedCount = 0;
        List list = new ArrayList(3);

        LRUCounter(int i) {
            super(i);
        }

        public void processRemoved(Entry pEntry) {
            ++removedCount;
            list.add(pEntry.getKey());
        }
    }
}

