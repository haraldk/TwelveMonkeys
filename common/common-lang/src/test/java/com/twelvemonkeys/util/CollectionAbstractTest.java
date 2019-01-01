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

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Abstract test class for {@link java.util.Collection} methods and contracts.
 * <p>
 * You should create a concrete subclass of this class to test any custom
 * {@link Collection} implementation.  At minimum, you'll have to
 * implement the {@link #makeCollection()} method.  You might want to
 * override some of the additional public methods as well:
 * <p>
 * <b>Element Population Methods</b>
 * <p>
 * Override these if your collection restricts what kind of elements are
 * allowed (for instance, if {@code null} is not permitted):
 * <ul>
 * <li>{@link #getFullElements()}
 * <li>{@link #getOtherElements()}
 * </ul>
 * <p>
 * <b>Supported Operation Methods</b>
 * <p>
 * Override these if your collection doesn't support certain operations:
 * <ul>
 * <li>{@link #isAddSupported()}
 * <li>{@link #isRemoveSupported()}
 * <li>{@link #areEqualElementsDistinguishable()}
 * <li>{@link #isNullSupported()}
 * <li>{@link #isFailFastSupported()}
 * </ul>
 * <p>
 * <b>Fixture Methods</b>
 * <p>
 * Fixtures are used to verify that the the operation results in correct state
 * for the collection.  Basically, the operation is performed against your
 * collection implementation, and an identical operation is performed against a
 * <i>confirmed</i> collection implementation.  A confirmed collection
 * implementation is something like {@code java.util.ArrayList}, which is
 * known to conform exactly to its collection interface's contract.  After the
 * operation takes place on both your collection implementation and the
 * confirmed collection implementation, the two collections are compared to see
 * if their state is identical.  The comparison is usually much more involved
 * than a simple {@code equals} test.  This verification is used to ensure
 * proper modifications are made along with ensuring that the collection does
 * not change when read-only modifications are made.
 * <p>
 * The {@link #collection} field holds an instance of your collection
 * implementation; the {@link #confirmed} field holds an instance of the
 * confirmed collection implementation.  The {@link #resetEmpty()} and
 * {@link #resetFull()} methods set these fields to empty or full collections,
 * so that tests can proceed from a known state.
 * <p>
 * After a modification operation to both {@link #collection} and
 * {@link #confirmed}, the {@link #verifyAll()} method is invoked to compare
 * the results.  You may want to override {@link # verifyAll ()} to perform
 * additional verifications.  For instance, when testing the collection
 * views of a map, {@link AbstractTestMap} would override {@link # verifyAll ()} to make
 * sure the map is changed after the collection view is changed.
 * <p>
 * If you're extending this class directly, you will have to provide
 * implementations for the following:
 * <ul>
 * <li>{@link #makeConfirmedCollection()}
 * <li>{@link #makeConfirmedFullCollection()}
 * </ul>
 * <p>
 * Those methods should provide a confirmed collection implementation
 * that's compatible with your collection implementation.
 * <p>
 * If you're extending {@link AbstractTestList}, {@link SetAbstractTest},
 * or {@link AbstractTestBag}, you probably don't have to worry about the
 * above methods, because those three classes already override the methods
 * to provide standard JDK confirmed collections.<P>
 * <p>
 * <b>Other notes</b>
 * <p>
 * If your {@link Collection} fails one of these tests by design,
 * you may still use this base set of cases.  Simply override the
 * test case (method) your {@link Collection} fails.
 *
 * @version $Revision: #2 $ $Date: 2008/07/15 $
 *
 * @author Rodney Waldhoff
 * @author Paul Jack
 * @author Michael A. Smith
 * @author Neil O'Toole
 * @author Stephen Colebourne
 */
public abstract class CollectionAbstractTest extends ObjectAbstractTest {

    //
    // NOTE:
    //
    // Collection doesn't define any semantics for equals, and recommends you
    // use reference-based default behavior of Object.equals.  (And a test for
    // that already exists in ObjectAbstractTestCase).  Tests for equality of lists, sets
    // and bags will have to be written in test subclasses.  Thus, there is no
    // tests on Collection.equals nor any for Collection.hashCode.
    //


    // These fields are used by reset() and verifyAll(), and any test
    // method that tests a modification.

    /**
     *  A collection instance that will be used for testing.
     */
    public Collection collection;

    /**
     *  Confirmed collection.  This is an instance of a collection that is
     *  confirmed to conform exactly to the java.util.Collection contract.
     *  Modification operations are tested by performing a mod on your
     *  collection, performing the exact same mod on an equivalent confirmed
     *  collection, and then calling verifyAll() to make sure your collection
     *  still matches the confirmed collection.
     */
    public Collection confirmed;

    //-----------------------------------------------------------------------
    /**
     *  Specifies whether equal elements in the collection are, in fact,
     *  distinguishable with information not readily available.  That is, if a
     *  particular value is to be removed from the collection, then there is
     *  one and only one value that can be removed, even if there are other
     *  elements which are equal to it.
     *
     *  <P>In most collection cases, elements are not distinguishable (equal is
     *  equal), thus this method defaults to return false.  In some cases,
     *  however, they are.  For example, the collection returned from the map's
     *  values() collection view are backed by the map, so while there may be
     *  two values that are equal, their associated keys are not.  Since the
     *  keys are distinguishable, the values are.
     *
     *  <P>This flag is used to skip some verifications for iterator.remove()
     *  where it is impossible to perform an equivalent modification on the
     *  confirmed collection because it is not possible to determine which
     *  value in the confirmed collection to actually remove.  Tests that
     *  override the default (i.e. where equal elements are distinguishable),
     *  should provide additional tests on iterator.remove() to make sure the
     *  proper elements are removed when remove() is called on the iterator.
     **/
    public boolean areEqualElementsDistinguishable() {
        return false;
    }

    /**
     *  Returns true if the collections produced by
     *  {@link #makeCollection()} and {@link #makeFullCollection()}
     *  support the {@code add} and {@code addAll}
     *  operations.<P>
     *  Default implementation returns true.  Override if your collection
     *  class does not support add or addAll.
     */
    public boolean isAddSupported() {
        return true;
    }

    /**
     *  Returns true if the collections produced by
     *  {@link #makeCollection()} and {@link #makeFullCollection()}
     *  support the {@code remove}, {@code removeAll},
     *  {@code retainAll}, {@code clear} and
     *  {@code iterator().remove()} methods.
     *  Default implementation returns true.  Override if your collection
     *  class does not support removal operations.
     */
    public boolean isRemoveSupported() {
        return true;
    }

    /**
     * Returns true to indicate that the collection supports holding null.
     * The default implementation returns true;
     */
    public boolean isNullSupported() {
        return true;
    }

    /**
     * Returns true to indicate that the collection supports fail fast iterators.
     * The default implementation returns true;
     */
    public boolean isFailFastSupported() {
        return false;
    }

    /**
     * Returns true to indicate that the collection supports equals() comparisons.
     * This implementation returns false;
     */
    public boolean isEqualsCheckable() {
        return false;
    }

    //-----------------------------------------------------------------------
    /**
     *  Verifies that {@link #collection} and {@link #confirmed} have
     *  identical state.
     */
    public void verifyAll() {
        int confirmedSize = confirmed.size();
        assertEquals("Collection size should match confirmed collection's",
                     confirmedSize, collection.size());
        assertEquals("Collection isEmpty() result should match confirmed " +
                     " collection's",
                     confirmed.isEmpty(), collection.isEmpty());

        // verify the collections are the same by attempting to match each
        // object in the collection and confirmed collection.  To account for
        // duplicates and differing orders, each confirmed element is copied
        // into an array and a flag is maintained for each element to determine
        // whether it has been matched once and only once.  If all elements in
        // the confirmed collection are matched once and only once and there
        // aren't any elements left to be matched in the collection,
        // verification is a success.

        // copy each collection value into an array
        Object[] confirmedValues = new Object[confirmedSize];

        Iterator iter;

        iter = confirmed.iterator();
        int pos = 0;
        while(iter.hasNext()) {
            confirmedValues[pos++] = iter.next();
        }

        // allocate an array of boolean flags for tracking values that have
        // been matched once and only once.
        boolean[] matched = new boolean[confirmedSize];

        // now iterate through the values of the collection and try to match
        // the value with one in the confirmed array.
        iter = collection.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            boolean match = false;
            for(int i = 0; i < confirmedSize; i++) {
                if(matched[i]) {
                    // skip values already matched
                    continue;
                }
                if(o == confirmedValues[i] ||
                   (o != null && o.equals(confirmedValues[i]))) {
                    // values matched
                    matched[i] = true;
                    match = true;
                    break;
                }
            }
            // no match found!
            if(!match) {
                fail("Collection should not contain a value that the " +
                     "confirmed collection does not have: " + o +
                     "\nTest: " + collection + "\nReal: " + confirmed);
            }
        }

        // make sure there aren't any unmatched values
        for(int i = 0; i < confirmedSize; i++) {
            if(!matched[i]) {
                // the collection didn't match all the confirmed values
                fail("Collection should contain all values that are in the confirmed collection" +
                     "\nTest: " + collection + "\nReal: " + confirmed);
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     *  Resets the {@link #collection} and {@link #confirmed} fields to empty
     *  collections.  Invoke this method before performing a modification
     *  test.
     */
    public void resetEmpty() {
        this.collection = makeCollection();
        this.confirmed = makeConfirmedCollection();
    }

    /**
     *  Resets the {@link #collection} and {@link #confirmed} fields to full
     *  collections.  Invoke this method before performing a modification
     *  test.
     */
    public void resetFull() {
        this.collection = makeFullCollection();
        this.confirmed = makeConfirmedFullCollection();
    }

    //-----------------------------------------------------------------------
    /**
     *  Returns a confirmed empty collection.
     *  For instance, an {@link java.util.ArrayList} for lists or a
     *  {@link java.util.HashSet} for sets.
     *
     *  @return a confirmed empty collection
     */
    public abstract Collection makeConfirmedCollection();

    /**
     *  Returns a confirmed full collection.
     *  For instance, an {@link java.util.ArrayList} for lists or a
     *  {@link java.util.HashSet} for sets.  The returned collection
     *  should contain the elements returned by {@link #getFullElements()}.
     *
     *  @return a confirmed full collection
     */
    public abstract Collection makeConfirmedFullCollection();

    /**
     * Return a new, empty {@link Collection} to be used for testing.
     */
    public abstract Collection makeCollection();

    /**
     *  Returns a full collection to be used for testing.  The collection
     *  returned by this method should contain every element returned by
     *  {@link #getFullElements()}.  The default implementation, in fact,
     *  simply invokes {@code addAll} on an empty collection with
     *  the results of {@link #getFullElements()}.  Override this default
     *  if your collection doesn't support addAll.
     */
    public Collection makeFullCollection() {
        Collection c = makeCollection();
        c.addAll(Arrays.asList(getFullElements()));
        return c;
    }

    /**
     *  Returns an empty collection for Object tests.
     */
    public Object makeObject() {
        return makeCollection();
    }

    /**
     * Creates a new Map Entry that is independent of the first and the map.
     */
    public Map.Entry cloneMapEntry(Map.Entry entry) {
        HashMap map = new HashMap();
        map.put(entry.getKey(), entry.getValue());
        return (Map.Entry) map.entrySet().iterator().next();
    }

    //-----------------------------------------------------------------------
    /**
     *  Returns an array of objects that are contained in a collection
     *  produced by {@link #makeFullCollection()}.  Every element in the
     *  returned array <I>must</I> be an element in a full collection.<P>
     *  The default implementation returns a heterogenous array of
     *  objects with some duplicates. null is added if allowed.
     *  Override if you require specific testing elements.  Note that if you
     *  override {@link #makeFullCollection()}, you <I>must</I> override
     *  this method to reflect the contents of a full collection.
     */
    public Object[] getFullElements() {
        if (isNullSupported()) {
            ArrayList list = new ArrayList();
            list.addAll(Arrays.asList(getFullNonNullElements()));
            list.add(4, null);
            return list.toArray();
        } else {
            return (Object[]) getFullNonNullElements().clone();
        }
    }

    /**
     *  Returns an array of elements that are <I>not</I> contained in a
     *  full collection.  Every element in the returned array must
     *  not exist in a collection returned by {@link #makeFullCollection()}.
     *  The default implementation returns a heterogenous array of elements
     *  without null.  Note that some of the tests add these elements
     *  to an empty or full collection, so if your collection restricts
     *  certain kinds of elements, you should override this method.
     */
    public Object[] getOtherElements() {
        return getOtherNonNullElements();
    }

    //-----------------------------------------------------------------------
    /**
     *  Returns a list of elements suitable for return by
     *  {@link #getFullElements()}.  The array returned by this method
     *  does not include null, but does include a variety of objects
     *  of different types.  Override getFullElements to return
     *  the results of this method if your collection does not support
     *  the null element.
     */
    public Object[] getFullNonNullElements() {
        return new Object[] {
            new String(""),
            new String("One"),
            new Integer(2),
            "Three",
            new Integer(4),
            "One",
            new Double(5),
            new Float(6),
            "Seven",
            "Eight",
            new String("Nine"),
            new Integer(10),
            new Short((short)11),
            new Long(12),
            "Thirteen",
            "14",
            "15",
            new Byte((byte)16)
        };
    }

    /**
     *  Returns the default list of objects returned by
     *  {@link #getOtherElements()}.  Includes many objects
     *  of different types.
     */
    public Object[] getOtherNonNullElements() {
        return new Object[] {
            new Integer(0),
            new Float(0),
            new Double(0),
            "Zero",
            new Short((short)0),
            new Byte((byte)0),
            new Long(0),
            new Character('\u0000'),
            "0"
        };
    }

    /**
     *  Returns a list of string elements suitable for return by
     *  {@link #getFullElements()}.  Override getFullElements to return
     *  the results of this method if your collection does not support
     *  heterogenous elements or the null element.
     */
    public Object[] getFullNonNullStringElements() {
        return new Object[] {
            "If","the","dull","substance","of","my","flesh","were","thought",
            "Injurious","distance","could","not","stop","my","way",
        };
    }

    /**
     *  Returns a list of string elements suitable for return by
     *  {@link #getOtherElements()}.  Override getOtherElements to return
     *  the results of this method if your collection does not support
     *  heterogenous elements or the null element.
     */
    public Object[] getOtherNonNullStringElements() {
        return new Object[] {
            "For","then","despite",/* of */"space","I","would","be","brought",
            "From","limits","far","remote","where","thou","dost","stay"
        };
    }

    // Tests
    //-----------------------------------------------------------------------
    /**
     *  Tests {@link Collection#add(Object)}.
     */
    @Test
    public void testCollectionAdd() {
        if (!isAddSupported()) return;

        Object[] elements = getFullElements();
        for (int i = 0; i < elements.length; i++) {
            resetEmpty();
            boolean r = collection.add(elements[i]);
            confirmed.add(elements[i]);
            verifyAll();
            assertTrue("Empty collection changed after add", r);
            assertEquals("Collection size is 1 after first add", 1, collection.size());
        }

        resetEmpty();
        int size = 0;
        for (int i = 0; i < elements.length; i++) {
            boolean r = collection.add(elements[i]);
            confirmed.add(elements[i]);
            verifyAll();
            if (r) size++;
            assertEquals("Collection size should grow after add",
                         size, collection.size());
            assertTrue("Collection should contain added element",
                       collection.contains(elements[i]));
        }
    }


    /**
     *  Tests {@link Collection#addAll(Collection)}.
     */
    @Test
    public void testCollectionAddAll() {
        if (!isAddSupported()) return;

        resetEmpty();
        Object[] elements = getFullElements();
        boolean r = collection.addAll(Arrays.asList(elements));
        confirmed.addAll(Arrays.asList(elements));
        verifyAll();
        assertTrue("Empty collection should change after addAll", r);
        for (int i = 0; i < elements.length; i++) {
            assertTrue("Collection should contain added element",
                       collection.contains(elements[i]));
        }

        resetFull();
        int size = collection.size();
        elements = getOtherElements();
        r = collection.addAll(Arrays.asList(elements));
        confirmed.addAll(Arrays.asList(elements));
        verifyAll();
        assertTrue("Full collection should change after addAll", r);
        for (int i = 0; i < elements.length; i++) {
            assertTrue("Full collection should contain added element",
                       collection.contains(elements[i]));
        }
        assertEquals("Size should increase after addAll",
                     size + elements.length, collection.size());

        resetFull();
        size = collection.size();
        r = collection.addAll(Arrays.asList(getFullElements()));
        confirmed.addAll(Arrays.asList(getFullElements()));
        verifyAll();
        if (r) {
            assertTrue("Size should increase if addAll returns true",
                       size < collection.size());
        } else {
            assertEquals("Size should not change if addAll returns false",
                         size, collection.size());
        }
    }


    /**
     *  If {@link #isAddSupported()} returns false, tests that add operations
     *  raise <code>UnsupportedOperationException.
     */
    @Test
    public void testUnsupportedAdd() {
        if (isAddSupported()) return;

        resetEmpty();
        try {
            collection.add(new Object());
            fail("Emtpy collection should not support add.");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        // make sure things didn't change even if the expected exception was
        // thrown.
        verifyAll();

        try {
            collection.addAll(Arrays.asList(getFullElements()));
            fail("Emtpy collection should not support addAll.");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        // make sure things didn't change even if the expected exception was
        // thrown.
        verifyAll();

        resetFull();
        try {
            collection.add(new Object());
            fail("Full collection should not support add.");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        // make sure things didn't change even if the expected exception was
        // thrown.
        verifyAll();

        try {
            collection.addAll(Arrays.asList(getOtherElements()));
            fail("Full collection should not support addAll.");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        // make sure things didn't change even if the expected exception was
        // thrown.
        verifyAll();
    }


    /**
     *  Test {@link Collection#clear()}.
     */
    @Test
    public void testCollectionClear() {
        if (!isRemoveSupported()) return;

        resetEmpty();
        collection.clear(); // just to make sure it doesn't raise anything
        verifyAll();

        resetFull();
        collection.clear();
        confirmed.clear();
        verifyAll();
    }


    /**
     *  Tests {@link Collection#contains(Object)}.
     */
    public void testCollectionContains() {
        Object[] elements;

        resetEmpty();
        elements = getFullElements();
        for(int i = 0; i < elements.length; i++) {
            assertTrue("Empty collection shouldn't contain element[" + i + "]",
                       !collection.contains(elements[i]));
        }
        // make sure calls to "contains" don't change anything
        verifyAll();

        elements = getOtherElements();
        for(int i = 0; i < elements.length; i++) {
            assertTrue("Empty collection shouldn't contain element[" + i + "]",
                       !collection.contains(elements[i]));
        }
        // make sure calls to "contains" don't change anything
        verifyAll();

        resetFull();
        elements = getFullElements();
        for(int i = 0; i < elements.length; i++) {
            assertTrue("Full collection should contain element[" + i + "]",
                       collection.contains(elements[i]));
        }
        // make sure calls to "contains" don't change anything
        verifyAll();

        resetFull();
        elements = getOtherElements();
        for(int i = 0; i < elements.length; i++) {
            assertTrue("Full collection shouldn't contain element",
                       !collection.contains(elements[i]));
        }
    }


    /**
     *  Tests {@link Collection#containsAll(Collection)}.
     */
    @Test
    public void testCollectionContainsAll() {
        resetEmpty();
        Collection col = new HashSet();
        assertTrue("Every Collection should contain all elements of an " +
                   "empty Collection.", collection.containsAll(col));
        col.addAll(Arrays.asList(getOtherElements()));
        assertTrue("Empty Collection shouldn't contain all elements of " +
                   "a non-empty Collection.", !collection.containsAll(col));
        // make sure calls to "containsAll" don't change anything
        verifyAll();

        resetFull();
        assertTrue("Full collection shouldn't contain other elements",
                   !collection.containsAll(col));

        col.clear();
        col.addAll(Arrays.asList(getFullElements()));
        assertTrue("Full collection should containAll full elements",
                   collection.containsAll(col));
        // make sure calls to "containsAll" don't change anything
        verifyAll();

        int min = (getFullElements().length < 2 ? 0 : 2);
        int max = (getFullElements().length == 1 ? 1 :
                    (getFullElements().length <= 5 ? getFullElements().length - 1 : 5));
        col = Arrays.asList(getFullElements()).subList(min, max);
        assertTrue("Full collection should containAll partial full " +
                   "elements", collection.containsAll(col));
        assertTrue("Full collection should containAll itself",
                   collection.containsAll(collection));
        // make sure calls to "containsAll" don't change anything
        verifyAll();

        col = new ArrayList();
        col.addAll(Arrays.asList(getFullElements()));
        col.addAll(Arrays.asList(getFullElements()));
        assertTrue("Full collection should containAll duplicate full " +
                   "elements", collection.containsAll(col));

        // make sure calls to "containsAll" don't change anything
        verifyAll();
    }

    /**
     *  Tests {@link Collection#isEmpty()}.
     */
    @Test
    public void testCollectionIsEmpty() {
        resetEmpty();
        assertEquals("New Collection should be empty.",
                     true, collection.isEmpty());
        // make sure calls to "isEmpty() don't change anything
        verifyAll();

        resetFull();
        assertEquals("Full collection shouldn't be empty",
                     false, collection.isEmpty());
        // make sure calls to "isEmpty() don't change anything
        verifyAll();
    }


    /**
     *  Tests the read-only functionality of {@link Collection#iterator()}.
     */
    @Test
    public void testCollectionIterator() {
        resetEmpty();
        Iterator it1 = collection.iterator();
        assertEquals("Iterator for empty Collection shouldn't have next.",
                     false, it1.hasNext());
        try {
            it1.next();
            fail("Iterator at end of Collection should throw " +
                 "NoSuchElementException when next is called.");
        } catch(NoSuchElementException e) {
            // expected
        }
        // make sure nothing has changed after non-modification
        verifyAll();

        resetFull();
        it1 = collection.iterator();
        for (int i = 0; i < collection.size(); i++) {
            assertTrue("Iterator for full collection should haveNext",
                       it1.hasNext());
            it1.next();
        }
        assertTrue("Iterator should be finished", !it1.hasNext());

        ArrayList list = new ArrayList();
        it1 = collection.iterator();
        for (int i = 0; i < collection.size(); i++) {
            Object next = it1.next();
            assertTrue("Collection should contain element returned by " +
                       "its iterator", collection.contains(next));
            list.add(next);
        }
        try {
            it1.next();
            fail("iterator.next() should raise NoSuchElementException " +
                 "after it finishes");
        } catch (NoSuchElementException e) {
            // expected
        }
        // make sure nothing has changed after non-modification
        verifyAll();
    }


    /**
     *  Tests removals from {@link Collection#iterator()}.
     */
    @Test
    public void testCollectionIteratorRemove() {
        if (!isRemoveSupported()) return;

        resetEmpty();
        try {
            collection.iterator().remove();
            fail("New iterator.remove should raise IllegalState");
        } catch (IllegalStateException e) {
            // expected
        }
        verifyAll();

        try {
            Iterator iter = collection.iterator();
            iter.hasNext();
            iter.remove();
            fail("New iterator.remove should raise IllegalState " +
                 "even after hasNext");
        } catch (IllegalStateException e) {
            // expected
        }
        verifyAll();

        resetFull();
        int size = collection.size();
        Iterator iter = collection.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            // TreeMap reuses the Map Entry, so the verify below fails
            // Clone it here if necessary
            if (o instanceof Map.Entry) {
                o = cloneMapEntry((Map.Entry) o);
            }
            iter.remove();

            // if the elements aren't distinguishable, we can just remove a
            // matching element from the confirmed collection and verify
            // contents are still the same.  Otherwise, we don't have the
            // ability to distinguish the elements and determine which to
            // remove from the confirmed collection (in which case, we don't
            // verify because we don't know how).
            //
            // see areEqualElementsDistinguishable()
            if(!areEqualElementsDistinguishable()) {
                confirmed.remove(o);
                verifyAll();
            }

            size--;
            assertEquals("Collection should shrink by one after " +
                         "iterator.remove", size, collection.size());
        }
        assertTrue("Collection should be empty after iterator purge",
                   collection.isEmpty());

        resetFull();
        iter = collection.iterator();
        iter.next();
        iter.remove();
        try {
            iter.remove();
            fail("Second iter.remove should raise IllegalState");
        } catch (IllegalStateException e) {
            // expected
        }
    }


    /**
     *  Tests {@link Collection#remove(Object)}.
     */
    @Test
    public void testCollectionRemove() {
        if (!isRemoveSupported()) return;

        resetEmpty();
        Object[] elements = getFullElements();
        for (int i = 0; i < elements.length; i++) {
            assertTrue("Shouldn't remove nonexistent element",
                       !collection.remove(elements[i]));
            verifyAll();
        }

        Object[] other = getOtherElements();

        resetFull();
        for (int i = 0; i < other.length; i++) {
            assertTrue("Shouldn't remove nonexistent other element",
                       !collection.remove(other[i]));
            verifyAll();
        }

        int size = collection.size();
        for (int i = 0; i < elements.length; i++) {
            resetFull();
            assertTrue("Collection should remove extant element: " + elements[i],
                       collection.remove(elements[i]));

            // if the elements aren't distinguishable, we can just remove a
            // matching element from the confirmed collection and verify
            // contents are still the same.  Otherwise, we don't have the
            // ability to distinguish the elements and determine which to
            // remove from the confirmed collection (in which case, we don't
            // verify because we don't know how).
            //
            // see areEqualElementsDistinguishable()
            if(!areEqualElementsDistinguishable()) {
                confirmed.remove(elements[i]);
                verifyAll();
            }

            assertEquals("Collection should shrink after remove",
                         size - 1, collection.size());
        }
    }


    /**
     *  Tests {@link Collection#removeAll(Collection)}.
     */
    @Test
    public void testCollectionRemoveAll() {
        if (!isRemoveSupported()) return;

        resetEmpty();
        assertTrue("Emtpy collection removeAll should return false for " +
                   "empty input",
                   !collection.removeAll(Collections.EMPTY_SET));
        verifyAll();

        assertTrue("Emtpy collection removeAll should return false for " +
                   "nonempty input",
                   !collection.removeAll(new ArrayList(collection)));
        verifyAll();

        resetFull();
        assertTrue("Full collection removeAll should return false for " +
                   "empty input",
                   !collection.removeAll(Collections.EMPTY_SET));
        verifyAll();

        assertTrue("Full collection removeAll should return false for other elements",
                   !collection.removeAll(Arrays.asList(getOtherElements())));
        verifyAll();

        assertTrue("Full collection removeAll should return true for full elements",
                    collection.removeAll(new HashSet(collection)));
        confirmed.removeAll(new HashSet(confirmed));
        verifyAll();

        resetFull();
        int size = collection.size();
        int min = (getFullElements().length < 2 ? 0 : 2);
        int max = (getFullElements().length == 1 ? 1 :
                    (getFullElements().length <= 5 ? getFullElements().length - 1 : 5));
        Collection all = Arrays.asList(getFullElements()).subList(min, max);
        assertTrue("Full collection removeAll should work",
                   collection.removeAll(all));
        confirmed.removeAll(all);
        verifyAll();

        assertTrue("Collection should shrink after removeAll",
                   collection.size() < size);
        Iterator iter = all.iterator();
        while (iter.hasNext()) {
            assertTrue("Collection shouldn't contain removed element",
                       !collection.contains(iter.next()));
        }
    }


    /**
     *  Tests {@link Collection#retainAll(Collection)}.
     */
    @Test
    public void testCollectionRetainAll() {
        if (!isRemoveSupported()) return;

        resetEmpty();
        List elements = Arrays.asList(getFullElements());
        List other = Arrays.asList(getOtherElements());

        assertTrue("Empty retainAll() should return false",
                   !collection.retainAll(Collections.EMPTY_SET));
        verifyAll();

        assertTrue("Empty retainAll() should return false",
                   !collection.retainAll(elements));
        verifyAll();

        resetFull();
        assertTrue("Collection should change from retainAll empty",
                   collection.retainAll(Collections.EMPTY_SET));
        confirmed.retainAll(Collections.EMPTY_SET);
        verifyAll();

        resetFull();
        assertTrue("Collection changed from retainAll other",
                   collection.retainAll(other));
        confirmed.retainAll(other);
        verifyAll();

        resetFull();
        int size = collection.size();
        assertTrue("Collection shouldn't change from retainAll elements",
                   !collection.retainAll(elements));
        verifyAll();
        assertEquals("Collection size shouldn't change", size,
                     collection.size());

        if (getFullElements().length > 1) {
            resetFull();
            size = collection.size();
            int min = (getFullElements().length < 2 ? 0 : 2);
            int max = (getFullElements().length <= 5 ? getFullElements().length - 1 : 5);
            assertTrue("Collection should changed by partial retainAll",
                       collection.retainAll(elements.subList(min, max)));
            confirmed.retainAll(elements.subList(min, max));
            verifyAll();

            Iterator iter = collection.iterator();
            while (iter.hasNext()) {
                assertTrue("Collection only contains retained element",
                           elements.subList(min, max).contains(iter.next()));
            }
        }

        resetFull();
        HashSet set = new HashSet(elements);
        size = collection.size();
        assertTrue("Collection shouldn't change from retainAll without " +
                   "duplicate elements", !collection.retainAll(set));
        verifyAll();
        assertEquals("Collection size didn't change from nonduplicate " +
                     "retainAll", size, collection.size());
    }


    /**
     *  Tests {@link Collection#size()}.
     */
    @Test
    public void testCollectionSize() {
        resetEmpty();
        assertEquals("Size of new Collection is 0.", 0, collection.size());

        resetFull();
        assertTrue("Size of full collection should be greater than zero",
                   collection.size() > 0);
    }


    /**
     *  Tests {@link Collection#toArray()}.
     */
    public void testCollectionToArray() {
        resetEmpty();
        assertEquals("Empty Collection should return empty array for toArray",
                     0, collection.toArray().length);

        resetFull();
        Object[] array = collection.toArray();
        assertEquals("Full collection toArray should be same size as " +
                     "collection", array.length, collection.size());
        Object[] confirmedArray = confirmed.toArray();
        assertEquals("length of array from confirmed collection should " +
                     "match the length of the collection's array",
                     confirmedArray.length, array.length);
        boolean[] matched = new boolean[array.length];

        for (int i = 0; i < array.length; i++) {
            assertTrue("Collection should contain element in toArray",
                       collection.contains(array[i]));

            boolean match = false;
            // find a match in the confirmed array
            for(int j = 0; j < array.length; j++) {
                // skip already matched
                if(matched[j]) continue;
                if(array[i] == confirmedArray[j] ||
                   (array[i] != null && array[i].equals(confirmedArray[j]))) {
                    matched[j] = true;
                    match = true;
                    break;
                }
            }
            if(!match) {
                fail("element " + i + " in returned array should be found " +
                     "in the confirmed collection's array");
            }
        }
        for(int i = 0; i < matched.length; i++) {
            assertEquals("Collection should return all its elements in " +
                         "toArray", true, matched[i]);
        }
    }


    /**
     *  Tests {@link Collection#toArray(Object[])}.
     */
    @Test
    public void testCollectionToArray2() {
        resetEmpty();
        Object[] a = new Object[] { new Object(), null, null };
        Object[] array = collection.toArray(a);
        assertArrayEquals("Given array shouldn't shrink", array, a);
        assertEquals("Last element should be set to null", a[0], null);
        verifyAll();

        resetFull();
        try {
            array = collection.toArray(new Void[0]);
            fail("toArray(new Void[0]) should raise ArrayStore");
        } catch (ArrayStoreException e) {
            // expected
        }
        verifyAll();

        try {
            array = collection.toArray((Object[])null);
            fail("toArray(null) should raise NPE");
        } catch (NullPointerException e) {
            // expected
        }
        verifyAll();

        array = collection.toArray(new Object[0]);
        a = collection.toArray();
        assertEquals("toArrays should be equal",
                     Arrays.asList(array), Arrays.asList(a));

        // Figure out if they're all the same class
        // TODO: It'd be nicer to detect a common superclass
        HashSet classes = new HashSet();
        for (int i = 0; i < array.length; i++) {
            classes.add((array[i] == null) ? null : array[i].getClass());
        }
        if (classes.size() > 1) return;

        Class cl = (Class)classes.iterator().next();
        if (Map.Entry.class.isAssignableFrom(cl)) {  // check needed for protective cases like Predicated/Unmod map entrySet
            cl = Map.Entry.class;
        }
        a = (Object[])Array.newInstance(cl, 0);
        array = collection.toArray(a);
        assertEquals("toArray(Object[]) should return correct array type",
                     a.getClass(), array.getClass());
        assertEquals("type-specific toArrays should be equal",
                     Arrays.asList(array),
                     Arrays.asList(collection.toArray()));
        verifyAll();
    }


    /**
     *  Tests {@code toString} on a collection.
     */
    @Test
    public void testCollectionToString() {
        resetEmpty();
        assertTrue("toString shouldn't return null",
                   collection.toString() != null);

        resetFull();
        assertTrue("toString shouldn't return null",
                   collection.toString() != null);
    }


    /**
     *  If isRemoveSupported() returns false, tests to see that remove
     *  operations raise an UnsupportedOperationException.
     */
    @Test
    public void testUnsupportedRemove() {
        if (isRemoveSupported()) return;

        resetEmpty();
        try {
            collection.clear();
            fail("clear should raise UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        verifyAll();

        try {
            collection.remove(null);
            fail("remove should raise UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        verifyAll();

        try {
            collection.removeAll(null);
            fail("removeAll should raise UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        verifyAll();

        try {
            collection.retainAll(null);
            fail("removeAll should raise UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        verifyAll();

        resetFull();
        try {
            Iterator iterator = collection.iterator();
            iterator.next();
            iterator.remove();
            fail("iterator.remove should raise UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        verifyAll();

    }


    /**
     *  Tests that the collection's iterator is fail-fast.
     */
    @Test
    public void testCollectionIteratorFailFast() {
        if (!isFailFastSupported()) return;

        if (isAddSupported()) {
            resetFull();
            try {
                Iterator iter = collection.iterator();
                Object o = getOtherElements()[0];
                collection.add(o);
                confirmed.add(o);
                iter.next();
                fail("next after add should raise ConcurrentModification");
            } catch (ConcurrentModificationException e) {
                // expected
            }
            verifyAll();

            resetFull();
            try {
                Iterator iter = collection.iterator();
                collection.addAll(Arrays.asList(getOtherElements()));
                confirmed.addAll(Arrays.asList(getOtherElements()));
                iter.next();
                fail("next after addAll should raise ConcurrentModification");
            } catch (ConcurrentModificationException e) {
                // expected
            }
            verifyAll();
        }

        if (!isRemoveSupported()) return;

        resetFull();
        try {
            Iterator iter = collection.iterator();
            collection.clear();
            iter.next();
            fail("next after clear should raise ConcurrentModification");
        } catch (ConcurrentModificationException e) {
            // expected
        } catch (NoSuchElementException e) {
            // (also legal given spec)
        }

        resetFull();
        try {
            Iterator iter = collection.iterator();
            collection.remove(getFullElements()[0]);
            iter.next();
            fail("next after remove should raise ConcurrentModification");
        } catch (ConcurrentModificationException e) {
            // expected
        }

        resetFull();
        try {
            Iterator iter = collection.iterator();
            List sublist = Arrays.asList(getFullElements()).subList(2,5);
            collection.removeAll(sublist);
            iter.next();
            fail("next after removeAll should raise ConcurrentModification");
        } catch (ConcurrentModificationException e) {
            // expected
        }

        resetFull();
        try {
            Iterator iter = collection.iterator();
            List sublist = Arrays.asList(getFullElements()).subList(2,5);
            collection.retainAll(sublist);
            iter.next();
            fail("next after retainAll should raise ConcurrentModification");
        } catch (ConcurrentModificationException e) {
            // expected
        }
    }

    /*
    @Test
    public void testSerializeDeserializeThenCompare() throws Exception {
        Object obj = makeCollection();
        if (obj instanceof Serializable && isTestSerialization()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buffer);
            out.writeObject(obj);
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
            Object dest = in.readObject();
            in.close();
            if (isEqualsCheckable()) {
                assertEquals("obj != deserialize(serialize(obj)) - EMPTY Collection", obj, dest);
            }
        }
        obj = makeFullCollection();
        if (obj instanceof Serializable && isTestSerialization()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buffer);
            out.writeObject(obj);
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
            Object dest = in.readObject();
            in.close();
            if (isEqualsCheckable()) {
                assertEquals("obj != deserialize(serialize(obj)) - FULL Collection", obj, dest);
            }
        }
    }
    */

}
