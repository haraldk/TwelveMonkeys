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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests
 * {@link org.apache.commons.collections.SequencedHashMap}.
 * Be sure to use the "labRat" instance whenever possible,
 * so that subclasses will be tested correctly.
 *
 * @version $Revision: #2 $ $Date: 2008/07/15 $
 *
 * @author Morgan Delagrange
 * @author Daniel Rall
 * @author Henning P. Schmiedehausen
 * @author James Strachan
 */
public class LinkedMapTest extends MapAbstractTest {
    /**
     * The instance to experiment on.
     */
    protected LinkedMap labRat;

    @Before
    public void setUp() throws Exception {
        // use makeMap and cast the result to a SeqHashMap
        // so that subclasses of SeqHashMap can share these tests
        labRat = (LinkedMap) makeEmptyMap();
    }

    public Map makeEmptyMap() {
        return new LinkedMap();
    }

    protected Object[] getKeys() {
        return new Object[] { "foo", "baz", "eek" };
    }

    protected Object[] getValues() {
        return new Object[] { "bar", "frob", new Object() };
    }

    @Test
    public void testSequenceMap() throws Throwable {
        Object[] keys = getKeys();
        int expectedSize = keys.length;
        Object[] values = getValues();
        for (int i = 0; i < expectedSize; i++) {
            labRat.put(keys[i], values[i]);
        }

        // Test size().
        assertEquals("size() does not match expected size",
                     expectedSize, labRat.size());

        // Test clone(), iterator(), and get(Object).
        LinkedMap clone = (LinkedMap) labRat.clone();
        assertEquals("Size of clone does not match original",
                     labRat.size(), clone.size());
        Iterator origEntries = labRat.entrySet().iterator();
        Iterator copiedEntries = clone.entrySet().iterator();
        while (origEntries.hasNext()) {
            Map.Entry origEntry = (Map.Entry)origEntries.next();
            Map.Entry copiedEntry = (Map.Entry)copiedEntries.next();
            assertEquals("Cloned key does not match original",
                         origEntry.getKey(), copiedEntry.getKey());
            assertEquals("Cloned value does not match original",
                         origEntry.getValue(), copiedEntry.getValue());
            assertEquals("Cloned entry does not match original",
                         origEntry, copiedEntry);
        }
        assertTrue("iterator() returned different number of elements than keys()",
               !copiedEntries.hasNext());

        // Test sequence()
        /*
        List seq = labRat.sequence();
        assertEquals("sequence() returns more keys than in the Map",
                     expectedSize, seq.size());

        for (int i = 0; i < seq.size(); i++) {
            assertEquals("Key " + i + " is not the same as the key in the Map",
                         keys[i], seq.get(i));
        }
        */
    }

    /*
    public void testYoungest() {
        labRat.put(new Integer(1),"foo");
        labRat.put(new Integer(2),"bar");
        assertTrue("first key is correct",labRat.get(0).equals(new Integer(1)));
        labRat.put(new Integer(1),"boo");
        assertTrue("second key is reassigned to first",labRat.get(0).equals(new Integer(2)));
    }

    public void testYoungestReplaceNullWithValue() {
        labRat.put(new Integer(1),null);
        labRat.put(new Integer(2),"foo");
        assertTrue("first key is correct",labRat.get(0).equals(new Integer(1)));
        labRat.put(new Integer(1),"bar");
        assertTrue("second key is reassigned to first",labRat.get(0).equals(new Integer(2)));
    }

    public void testYoungestReplaceValueWithNull() {
        labRat.put(new Integer(1),"bar");
        labRat.put(new Integer(2),"foo");
        assertTrue("first key is correct",labRat.get(0).equals(new Integer(1)));
        labRat.put(new Integer(1),null);
        assertTrue("second key is reassigned to first",labRat.get(0).equals(new Integer(2)));
    }
    */

    // override TestMap method with more specific tests
    /*
    public void testFullMapSerialization()
    throws IOException, ClassNotFoundException {
        LinkedMap map = (LinkedMap) makeFullMap();

        if (!(map instanceof Serializable)) return;

        byte[] objekt = writeExternalFormToBytes((Serializable) map);
        LinkedMap map2 = (LinkedMap) readExternalFormFromBytes(objekt);

        assertEquals("Both maps are same size",map.size(), getSampleKeys().length);
        assertEquals("Both maps are same size",map2.size(),getSampleKeys().length);

        assertEquals("Both maps have the same first key",
                     map.getFirstKey(),getSampleKeys()[0]);
        assertEquals("Both maps have the same first key",
                     map2.getFirstKey(),getSampleKeys()[0]);
        assertEquals("Both maps have the same last key",
                     map.getLastKey(),getSampleKeys()[getSampleKeys().length - 1]);
        assertEquals("Both maps have the same last key",
                     map2.getLastKey(),getSampleKeys()[getSampleKeys().length - 1]);
    }
    */

    /*
    public void testIndexOf() throws Exception {
        Object[] keys = getKeys();
        int expectedSize = keys.length;
        Object[] values = getValues();
        for (int i = 0; i < expectedSize; i++) {
            labRat.put(keys[i], values[i]);
        }
        // test that the index returned are in the same order that they were
        // placed in the map
        for (int i = 0; i < keys.length; i++) {
            assertEquals("indexOf with existing key failed", i, labRat.indexOf(keys[i]));
        }
        // test non existing key..
        assertEquals("test with non-existing key failed", -1, labRat.indexOf("NonExistingKey"));
    }
    */

    @After
    public void tearDown() throws Exception {
        labRat = null;
    }
}
