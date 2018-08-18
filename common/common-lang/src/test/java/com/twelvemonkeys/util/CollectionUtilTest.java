/*
 * Copyright (c) 2012, Harald Kuhr
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

import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * CollectionUtilTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CollectionUtilTest.java,v 1.0 24.01.12 17:39 haraldk Exp$
 */
public class CollectionUtilTest {

    private static final Object[] stringObjects = new Object[] {"foo", "bar", "baz"};
    private static final Object[] integerObjects = new Object[] {1, 2, 3};

    @Test
    public void testMergeArraysObject() {
        Object[] merged = (Object[]) CollectionUtil.mergeArrays(stringObjects, integerObjects);
        assertArrayEquals(new Object[] {"foo", "bar", "baz", 1, 2, 3}, merged);
    }

    @Test
    public void testMergeArraysObjectOffset() {
        Object[] merged = (Object[]) CollectionUtil.mergeArrays(stringObjects, 1, 2, integerObjects, 2, 1);
        assertArrayEquals(new Object[] {"bar", "baz", 3}, merged);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectBadOffset() {
        CollectionUtil.mergeArrays(stringObjects, 4, 2, integerObjects, 2, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectBadSecondOffset() {
        CollectionUtil.mergeArrays(stringObjects, 1, 2, integerObjects, 4, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectBadLength() {
        CollectionUtil.mergeArrays(stringObjects, 1, 4, integerObjects, 2, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectBadSecondLength() {
        CollectionUtil.mergeArrays(stringObjects, 1, 2, integerObjects, 2, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectNegativeOffset() {
        CollectionUtil.mergeArrays(stringObjects, -1, 2, integerObjects, 2, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectNegativeSecondOffset() {
        CollectionUtil.mergeArrays(stringObjects, 1, 2, integerObjects, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectNegativeLength() {
        CollectionUtil.mergeArrays(stringObjects, 1, -1, integerObjects, 2, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testMergeArraysObjectNegativeSecondLength() {
        CollectionUtil.mergeArrays(stringObjects, 1, 2, integerObjects, 2, -1);
    }

    @Test
    public void testMergeArraysObjectAssignable() {
        Integer[] integers = {1, 2, 3}; // Integer assignable to Object

        Object[] merged = (Object[]) CollectionUtil.mergeArrays(stringObjects, integers);
        assertArrayEquals(new Object[] {"foo", "bar", "baz", 1, 2, 3}, merged);
    }

    @Test(expected = ArrayStoreException.class)
    public void testMergeArraysObjectIllegalType() {
        String[] strings = {"foo", "bar", "baz"};
        Integer[] integers = {1, 2, 3}; // Integer not assignable to String

        CollectionUtil.mergeArrays(strings, integers);
    }

    @Test(expected = ArrayStoreException.class)
    public void testMergeArraysNativeIllegalType() {
        char[] chars = {'a', 'b', 'c'};
        int[] integers = {1, 2, 3}; // Integer not assignable to String

        CollectionUtil.mergeArrays(chars, integers);

    }

    @Test
    public void testMergeArraysNative() {
        char[] chars = {'a', 'b', 'c'};
        char[] more = {'x', 'y', 'z'};

        char[] merged = (char[]) CollectionUtil.mergeArrays(chars, more);
        assertArrayEquals(new char[] {'a', 'b', 'c', 'x', 'y', 'z'}, merged);
    }

    @Test
    public void testSubArrayObject() {
        String[] strings = CollectionUtil.subArray(new String[] {"foo", "bar", "baz", "xyzzy"}, 1, 2);
        assertArrayEquals(new String[] {"bar", "baz"}, strings);
    }

    @Test
    public void testSubArrayNative() {
        int[] numbers = (int[]) CollectionUtil.subArray(new int[] {1, 2, 3, 4, 5}, 1, 3);
        assertArrayEquals(new int[] {2, 3, 4}, numbers);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnumIteratorNull() {
        CollectionUtil.iterator((Enumeration<Object>) null);
    }
    
    @Test
    public void testEnumIterator() {
        @SuppressWarnings("unchecked")
        Iterator<String> iterator = CollectionUtil.iterator((Enumeration) new StringTokenizer("foo, bar, baz", ", "));

        int count = 0;
        for (Object stringObject : stringObjects) {
            assertTrue(iterator.hasNext());
            assertEquals(stringObject, iterator.next());
            
            try {
                iterator.remove();
                fail("Enumeration has no remove method, iterator.remove() must throw exception");
            }
            catch (UnsupportedOperationException expected) {
            }

            count++;
        }

        assertEquals(3, count);
        assertFalse(iterator.hasNext());
        
        try {
            iterator.next();
            fail("Iterator has more elements than enumeration");
        }
        catch (NoSuchElementException expected) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayIteratorNull() {
        CollectionUtil.iterator((Object[]) null);
    }

    @Test
    public void testArrayIterator() {
        Iterator<String> iterator = CollectionUtil.iterator(new String[] {"foo", "bar", "baz"});

        int count = 0;
        for (Object stringObject : stringObjects) {
            assertTrue(iterator.hasNext());
            assertEquals(stringObject, iterator.next());

            try {
                iterator.remove();
                fail("Array have fixed length, iterator.remove() must throw exception");
            }
            catch (UnsupportedOperationException expected) {
            }

            count++;
        }

        assertEquals(3, count);
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("Iterator has more elements than array");
        }
        catch (NoSuchElementException expected) {
        }
    }

    @Test
    public void testArrayListIterator() {
        assertCorrectListIterator(CollectionUtil.iterator(new String[] {"foo", "bar", "baz"}), stringObjects);
    }

    @Test
    public void testArrayListIteratorRange() {
        assertCorrectListIterator(CollectionUtil.iterator(new String[] {"foo", "bar", "baz", "boo"}, 1, 2), new String[] {"bar", "baz"});
    }

    @Test
    public void testArrayListIteratorSanityCheckArraysAsList() {
        assertCorrectListIterator(Arrays.asList(new String[] {"foo", "bar", "baz"}).listIterator(), stringObjects);
    }

    @Test
    public void testArrayListIteratorSanityCheckArraysAsListRange() {
        // NOTE: sublist(fromInc, toExcl) vs iterator(start, length)
        assertCorrectListIterator(Arrays.asList(new String[] {"foo", "bar", "baz", "boo"}).subList(1, 3).listIterator(0), new String[] {"bar", "baz"}, false, true);
    }

    @Test
    public void testArrayListIteratorSanityCheckArraysList() {
        assertCorrectListIterator(new ArrayList<String>(Arrays.asList(new String[] {"foo", "bar", "baz"})).listIterator(), stringObjects, true, true);
    }

    @Test
    public void testArrayListIteratorSanityCheckArraysListRange() {
        // NOTE: sublist(fromInc, toExcl) vs iterator(start, length)
        assertCorrectListIterator(new ArrayList<String>(Arrays.asList(new String[] {"foo", "bar", "baz", "boo"})).subList(1, 3).listIterator(0), new String[] {"bar", "baz"}, true, true);
    }

    private void assertCorrectListIterator(ListIterator<String> iterator, final Object[] elements) {
        assertCorrectListIterator(iterator, elements, false, false);
    }

    // NOTE: The test is can only test list iterators with a starting index == 0
    private void assertCorrectListIterator(ListIterator<String> iterator, final Object[] elements, boolean skipRemove, boolean skipAdd) {
        // Index is now "before 0"
        assertEquals(-1, iterator.previousIndex());
        assertEquals(0, iterator.nextIndex());

        int count = 0;
        for (Object element : elements) {
            assertTrue("No next element for element '" + element + "' at index: " + count, iterator.hasNext());
            assertEquals(count > 0, iterator.hasPrevious());
            assertEquals(count, iterator.nextIndex());
            assertEquals(count - 1, iterator.previousIndex());
            assertEquals(element, iterator.next());

            count++;

            if (!skipRemove) {
                try {
                    iterator.remove();
                    fail("Array has fixed length, iterator.remove() must throw exception");
                }
                catch (UnsupportedOperationException expected) {
                }

                // Verify cursor not moving
                assertEquals(count, iterator.nextIndex());
                assertEquals(count - 1, iterator.previousIndex());
            }

            // NOTE: AbstractlList.ListItr.add() moves cursor forward, even if backing list's add() throws Exception
            // (coll) AbstractList.ListItr.add might corrupt iterator state if enclosing add throws
            // See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6533203, fixed in Java 7
            if (!skipAdd && !("java.util.AbstractList$ListItr".equals(iterator.getClass().getName()) /* && isJava7OrLater() */)) {
                try {
                    iterator.add("xyzzy");
                    fail("Array has fixed length, iterator.add() must throw exception");
                }
                catch (UnsupportedOperationException expected) {
                }

                // Verify cursor not moving
                assertEquals(count, iterator.nextIndex());
                assertEquals(count - 1, iterator.previousIndex());
            }

            // Set is supported
            iterator.set(String.valueOf(count));
        }

        assertEquals(elements.length, count);
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("Iterator has more elements than array");
        }
        catch (NoSuchElementException expected) {
        }

        // Index should now be "before last"
        assertEquals(elements.length - 1, iterator.previousIndex());
        assertEquals(elements.length, iterator.nextIndex());

        for (int i = count; i > 0; i--) {
            assertTrue("No previous element for element '" + elements[i - 1] + "' at index: " + (i - 1), iterator.hasPrevious());
            assertEquals(i < elements.length, iterator.hasNext());
            assertEquals(i - 1, iterator.previousIndex());
            assertEquals(i, iterator.nextIndex());

            assertEquals(String.valueOf(i), iterator.previous());
        }

        // Index should now be back "before 0"
        assertEquals(-1, iterator.previousIndex());
        assertEquals(0, iterator.nextIndex());
        assertFalse(iterator.hasPrevious());

        try {
            iterator.previous();
            fail("Iterator has more elements than array");
        }
        catch (NoSuchElementException expected) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayIteratorRangeNull() {
        CollectionUtil.iterator(null, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArrayIteratorRangeBadStart() {
        CollectionUtil.iterator(stringObjects, stringObjects.length + 1, 2);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testArrayIteratorRangeBadLength() {
        CollectionUtil.iterator(stringObjects, 1, stringObjects.length);
    }

    @Test
    public void testArrayIteratorRange() {
        Iterator<String> iterator = CollectionUtil.iterator(new String[] {"foo", "bar", "baz", "xyzzy"}, 1, 2);

        for (int i = 1; i < 3; i++) {
            assertTrue(iterator.hasNext());
            assertEquals(stringObjects[i], iterator.next());

            try {
                iterator.remove();
                fail("Array has no remove method, iterator.remove() must throw exception");
            }
            catch (UnsupportedOperationException expected) {
            }
        }

        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("Iterator has more elements than array range");
        }
        catch (NoSuchElementException expected) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReverseOrderNull() {
        CollectionUtil.reverseOrder(null);
    }

    @Test
    public void testReverseOrder() {
        Comparator<String> naturalOrder = new NaturalOrder<String>();
        Comparator<String> reverse = CollectionUtil.reverseOrder(naturalOrder);

        assertNotNull(reverse);

        assertEquals(0, naturalOrder.compare("foo", "foo"));
        assertEquals(0, reverse.compare("foo", "foo"));

        assertTrue(naturalOrder.compare("bar", "baz") < 0);
        assertTrue(reverse.compare("bar", "baz") > 0);

        assertTrue(naturalOrder.compare("baz", "bar") > 0);
        assertTrue(reverse.compare("baz", "bar") < 0);
    }

    @Test
    public void testReverseOrderRandomIntegers() {
        Comparator<Integer> naturalOrder = new NaturalOrder<Integer>();
        Comparator<Integer> reverse = CollectionUtil.reverseOrder(naturalOrder);

        Random random = new Random(243249878l); // Stable "random" sequence

        for (int i = 0; i < 65536; i++) {
            // Verified to be ~ 50/50 lt/gt
            int integer = random.nextInt();
            int integerToo = random.nextInt();

            assertEquals(0, reverse.compare(integer, integer));
            assertEquals(0, reverse.compare(integerToo, integerToo));

            int natural = naturalOrder.compare(integer, integerToo);

            if (natural == 0) {
                // Actually never hits, but eq case is tested above
                assertEquals(0, reverse.compare(integer, integerToo));
            }
            else if (natural < 0) {
                assertTrue(reverse.compare(integer, integerToo) > 0);
            }
            else {
                assertTrue(reverse.compare(integer, integerToo) < 0);
            }
        }
    }

    @Ignore("For development only")
    @Test
    @SuppressWarnings({"UnusedDeclaration"})
    public void testGenerify() {
        List list = Collections.singletonList("foo");
        @SuppressWarnings({"unchecked"})
        Set set = new HashSet(list);

        List<String> strs0 = CollectionUtil.generify(list, String.class);
        List<Object> objs0 = CollectionUtil.generify(list, String.class);
//        List<String> strs01 = CollectionUtil.generify(list, Object.class); // Not okay
        try {
            List<String> strs1 = CollectionUtil.generify(set, String.class); // Not ok, runtime CCE unless set is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        try {
            ArrayList<String> strs01 = CollectionUtil.generify(list, String.class); // Not ok, runtime CCE unless list is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        Set<String> setstr1 = CollectionUtil.generify(set, String.class);
        Set<Object> setobj1 = CollectionUtil.generify(set, String.class);
        try {
            Set<Object> setobj44 = CollectionUtil.generify(list, String.class);  // Not ok, runtime CCE unless list is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        List<String> strs2 = CollectionUtil.<List<String>, String>generify2(list);
        List<Object> objs2 = CollectionUtil.<List<Object>, String>generify2(list);
//        List<String> morestrs = CollectionUtil.<List<Object>, String>generify2(list); // Not ok
        try {
            List<String> strs3 = CollectionUtil.<List<String>, String>generify2(set); // Not ok, runtime CCE unless set is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static class NaturalOrder<T extends Comparable<T>> implements Comparator<T> {
        public int compare(T left, T right) {
            return left.compareTo(right);
        }
    }
}
