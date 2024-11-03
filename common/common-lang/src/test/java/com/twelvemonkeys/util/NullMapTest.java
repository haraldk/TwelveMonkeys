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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NOTE: This TestCase is written especially for NullMap, and is full of dirty
 * tricks. A good indication that NullMap is not a good, general-purpose Map
 * implementation...
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/NullMapTestCase.java#2 $
 */
public class NullMapTest extends MapAbstractTest {
    private boolean empty = true;

    public Map makeEmptyMap() {
        return new NullMap();
    }

    public Map makeFullMap() {
        return new NullMap();
    }

    public Map makeConfirmedMap() {
        // Always empty
        return new HashMap();
    }

    public void resetEmpty() {
        empty = true;
        super.resetEmpty();
    }

    public void resetFull() {
        empty = false;
        super.resetFull();
    }

    public void verifyAll() {
        if (empty) {
            super.verifyAll();
        }
    }

    // Overriden, as this map is always empty
    @Test
    @Override
    public void testMapIsEmpty() {
        resetEmpty();
        assertEquals(true, map.isEmpty(),
                "Map.isEmpty() should return true with an empty map");
        verifyAll();
        resetFull();
        assertEquals(true, map.isEmpty(),
                "Map.isEmpty() should return true with a full map");
    }

    // Overriden, as this map is always empty
    @Test
    @Override
    public void testMapSize() {
        resetEmpty();
        assertEquals(0, map.size(),
                "Map.size() should be 0 with an empty map");
        verifyAll();

        resetFull();
        assertEquals(0, map.size(),
                "Map.size() should equal the number of entries in the map");
    }

    @Test
    @Override
    public void testMapContainsKey() {
        Object[] keys = getSampleKeys();

        resetEmpty();
        for (Object key : keys) {
            assertTrue(!map.containsKey(key),"Map must not contain key when map is empty");
        }
        verifyAll();
    }

    @Test
    @Override
    public void testMapContainsValue() {
        Object[] values = getSampleValues();

        resetEmpty();
        for (Object value : values) {
            assertTrue(!map.containsValue(value), "Empty map must not contain value");
        }
        verifyAll();
    }

    @Test
    @Override
    public void testMapEquals() {
        resetEmpty();
        assertTrue(map.equals(confirmed), "Empty maps unequal.");
        verifyAll();
    }

    @Test
    @Override
    public void testMapHashCode() {
        resetEmpty();
        assertTrue(map.hashCode() == confirmed.hashCode(), "Empty maps have different hashCodes.");
    }

    @Test
    @Override
    public void testMapGet() {
        resetEmpty();

        Object[] keys = getSampleKeys();

        for (Object key : keys) {
            assertTrue(map.get(key) == null, "Empty map.get() should return null.");
        }
        verifyAll();
    }

    @Test
    @Override
    public void testMapPut() {
        resetEmpty();
        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();
        Object[] newValues = getNewSampleValues();

        for (int i = 0; i < keys.length; i++) {
            Object o = map.put(keys[i], values[i]);
            //confirmed.put(keys[i], values[i]);
            verifyAll();
            assertTrue(o == null, "First map.put should return null");
        }
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], newValues[i]);
            //confirmed.put(keys[i], newValues[i]);
            verifyAll();
        }
    }

    @Test
    @Override
    public void testMapToString() {
        resetEmpty();
        assertTrue(map.toString() != null,
                "Empty map toString() should not return null");
        verifyAll();
    }

    @Test
    @Override
    public void testMapPutAll() {
        // TODO: Find a menaingful way to test this
    }

    @Test
    @Override
    public void testMapRemove() {
        resetEmpty();

        Object[] keys = getSampleKeys();
        for(int i = 0; i < keys.length; i++) {
            Object o = map.remove(keys[i]);
            assertTrue(o == null, "First map.remove should return null");
        }
        verifyAll();
    }

    //-----------------------------------------------------------------------
    @Test
    @Override
    public void testEntrySetClearChangesMap() {
    }

    @Test
    @Override
    public void testKeySetClearChangesMap() {
    }

    @Test
    @Override
    public void testKeySetRemoveChangesMap() {
    }

    @Test
    @Override
    public void testValuesClearChangesMap() {
    }

    @Test
    @Override
    public void testEntrySetContains1() {
    }

    @Test
    @Override
    public void testEntrySetContains2() {
    }

    @Test
    @Override
    public void testEntrySetContains3() {
    }

    @Test
    @Override
    public void testEntrySetRemove1() {
    }

    @Test
    @Override
    public void testEntrySetRemove2() {
    }

    @Test
    @Override
    public void testEntrySetRemove3() {
    }
}
