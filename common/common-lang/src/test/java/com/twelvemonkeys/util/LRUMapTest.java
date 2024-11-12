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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests LRUMap.
 *
 * @version $Revision: #2 $ $Date: 2008/07/15 $
 *
 * @author James Strachan
 * @author Morgan Delagrange
 * @author Stephen Colebourne
 */
public class LRUMapTest extends LinkedMapTest {

    public boolean isGetStructuralModify() {
        return true;
    }

    //-----------------------------------------------------------------------
    public Map makeEmptyMap() {
        return new LRUMap();
    }

    //-----------------------------------------------------------------------
    @Test
    public void testRemoveLRU() {
        LRUMap<Integer, String> map2 = new LRUMap<Integer, String>(3);
        map2.put(1,"foo");
        map2.put(2,"foo");
        map2.put(3,"foo");
        map2.put(4,"foo"); // removes 1 since max size exceeded
        map2.removeLRU();  // should be Integer(2)

        assertTrue(map2.get(new Integer(3)).equals("foo"), "Second to last value should exist");
        assertTrue(map2.get(new Integer(1)) == null, "First value inserted should not exist");
    }

    @Test
    public void testMultiplePuts() {
        LRUMap<Integer, String> map2 = new LRUMap<Integer, String>(2);
        map2.put(1,"foo");
        map2.put(2,"bar");
        map2.put(3,"foo");
        map2.put(4,"bar");

        assertTrue(map2.get(new Integer(4)).equals("bar"), "last value should exist");
        assertTrue(map2.get(new Integer(1)) == null, "LRU should not exist");
    }

    /**
     * Confirm that putAll(Map) does not cause the LRUMap
     * to exceed its maxiumum size.
     */
    @Test
    public void testPutAll() {
        LRUMap<Integer, String> map2 = new LRUMap<Integer, String>(3);
        map2.put(1,"foo");
        map2.put(2,"foo");
        map2.put(3,"foo");

        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(4,"foo");

        map2.putAll(hashMap);

        assertTrue(map2.size() == 3, "max size is 3, but actual size is " + map2.size());
        assertTrue(map2.containsKey(new Integer(4)), "map should contain the Integer(4) object");
    }

    /**
     * Test that the size of the map is reduced immediately
     * when setMaximumSize(int) is called
     */
    @Test
    public void testSetMaximumSize() {
        LRUMap<String, String> map = new LRUMap<String, String>(6);
        map.put("1","1");
        map.put("2","2");
        map.put("3","3");
        map.put("4","4");
        map.put("5","5");
        map.put("6","6");
        map.setMaxSize(3);

        assertTrue(map.size() == 3, "map should have size = 3, but actually = " + map.size());
    }

    @Test
    public void testGetPromotion() {
        LRUMap<String, String> map = new LRUMap<String, String>(3);
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

        Iterator<String> keyIterator = map.keySet().iterator();
        Object[] keys = new Object[3];
        for (int i = 0; keyIterator.hasNext() ; ++i) {
            keys[i] = keyIterator.next();
        }

        assertTrue(keys[0].equals("3"), "first evicted should be 3, was " + keys[0]);
        assertTrue(keys[1].equals("1"), "second evicted should be 1, was " + keys[1]);
        assertTrue(keys[2].equals("4"), "third evicted should be 4, was " + keys[2]);

    }

    /**
     * You should be able to subclass LRUMap and perform a
     * custom action when items are removed automatically
     * by the LRU algorithm (the removeLRU() method).
     */
    @Test
    public void testLRUSubclass() {
        LRUCounter<String, String> counter = new LRUCounter<String, String>(3);
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

        assertTrue(counter.size() == 2, "size should be 2, but was " + counter.size());
        assertTrue(counter.removedCount == 3, "removedCount should be 3 but was " + counter.removedCount);

        assertTrue(counter.list.get(0).equals("2"), "first removed was '2'");
        assertTrue(counter.list.get(1).equals("3"), "second removed was '3'");
        assertTrue(counter.list.get(2).equals("1"), "third removed was '1'");

        //assertTrue("oldest key is '4'",counter.get(0).equals("4"));
        //assertTrue("newest key is '2'",counter.get(1).equals("2"));
    }

    private class LRUCounter<K, V> extends LRUMap<K, V> {
        int removedCount = 0;
        List<Object> list = new ArrayList<Object>(3);

        LRUCounter(int i) {
            super(i);
        }

        public void processRemoved(Entry pEntry) {
            ++removedCount;
            list.add(pEntry.getKey());
        }
    }
}

