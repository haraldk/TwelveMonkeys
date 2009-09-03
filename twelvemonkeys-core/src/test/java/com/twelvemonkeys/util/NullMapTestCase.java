package com.twelvemonkeys.util;


import java.util.Map;
import java.util.HashMap;

/**
 * NOTE: This TestCase is written especially for NullMap, and is full of dirty
 * tricks. A good indication that NullMap is not a good, general-purpose Map
 * implementation...
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/NullMapTestCase.java#2 $
 */
public class NullMapTestCase extends MapAbstractTestCase {
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
    public void testMapIsEmpty() {
        resetEmpty();
        assertEquals("Map.isEmpty() should return true with an empty map",
                     true, map.isEmpty());
        verifyAll();
        resetFull();
        assertEquals("Map.isEmpty() should return true with a full map",
                     true, map.isEmpty());
    }

    // Overriden, as this map is always empty
    public void testMapSize() {
        resetEmpty();
        assertEquals("Map.size() should be 0 with an empty map",
                     0, map.size());
        verifyAll();

        resetFull();
        assertEquals("Map.size() should equal the number of entries " +
                     "in the map", 0, map.size());
    }

    public void testMapContainsKey() {
        Object[] keys = getSampleKeys();

        resetEmpty();
        for(int i = 0; i < keys.length; i++) {
            assertTrue("Map must not contain key when map is empty",
                       !map.containsKey(keys[i]));
        }
        verifyAll();
    }

    public void testMapContainsValue() {
        Object[] values = getSampleValues();

        resetEmpty();
        for(int i = 0; i < values.length; i++) {
            assertTrue("Empty map must not contain value",
                       !map.containsValue(values[i]));
        }
        verifyAll();
    }

    public void testMapEquals() {
        resetEmpty();
        assertTrue("Empty maps unequal.", map.equals(confirmed));
        verifyAll();
    }

    public void testMapHashCode() {
        resetEmpty();
        assertTrue("Empty maps have different hashCodes.",
                   map.hashCode() == confirmed.hashCode());
    }

    public void testMapGet() {
        resetEmpty();

        Object[] keys = getSampleKeys();

        for (int i = 0; i < keys.length; i++) {
            assertTrue("Empty map.get() should return null.",
                       map.get(keys[i]) == null);
        }
        verifyAll();
    }

    public void testMapPut() {
        resetEmpty();
        Object[] keys = getSampleKeys();
        Object[] values = getSampleValues();
        Object[] newValues = getNewSampleValues();

        for (int i = 0; i < keys.length; i++) {
            Object o = map.put(keys[i], values[i]);
            //confirmed.put(keys[i], values[i]);
            verifyAll();
            assertTrue("First map.put should return null", o == null);
        }
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], newValues[i]);
            //confirmed.put(keys[i], newValues[i]);
            verifyAll();
        }
    }

    public void testMapToString() {
        resetEmpty();
        assertTrue("Empty map toString() should not return null",
                   map.toString() != null);
        verifyAll();
    }

    public void testMapPutAll() {
        // TODO: Find a menaingful way to test this
    }

    public void testMapRemove() {
        resetEmpty();

        Object[] keys = getSampleKeys();
        for(int i = 0; i < keys.length; i++) {
            Object o = map.remove(keys[i]);
            assertTrue("First map.remove should return null", o == null);
        }
        verifyAll();
    }

    //-----------------------------------------------------------------------
    public void testEntrySetClearChangesMap() {
    }

    public void testKeySetClearChangesMap() {
    }

    public void testKeySetRemoveChangesMap() {
    }

    public void testValuesClearChangesMap() {
    }

    public void testEntrySetContains1() {
    }

    public void testEntrySetContains2() {
    }

    public void testEntrySetContains3() {
    }

    public void testEntrySetRemove1() {
    }

    public void testEntrySetRemove2() {
    }

    public void testEntrySetRemove3() {
    }
}
