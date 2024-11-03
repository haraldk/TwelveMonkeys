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
 * Abstract test class for {@link Set} methods and contracts.
 * <p>
 * Since {@link Set} doesn't stipulate much new behavior that isn't already
 * found in {@link Collection}, this class basically just adds tests for
 * {@link Set#equals} and {@link Set#hashCode()} along with an updated
 * {@link #verifyAll()} that ensures elements do not appear more than once in the
 * set.
 * <p>
 * To use, subclass and override the {@link #makeEmptySet()}
 * method.  You may have to override other protected methods if your
 * set is not modifiable, or if your set restricts what kinds of
 * elements may be added; see {@link CollectionAbstractTest} for more details.
 *
 * @since Commons Collections 3.0
 * @version $Revision: #2 $ $Date: 2008/07/15 $
 *
 * @author Paul Jack
 */
public abstract class SetAbstractTest extends CollectionAbstractTest {

    //-----------------------------------------------------------------------
    /**
     * Provides additional verifications for sets.
     */
    public void verifyAll() {
        super.verifyAll();

        assertEquals(confirmed, collection, "Sets should be equal");
        assertEquals(confirmed.hashCode(), collection.hashCode(), "Sets should have equal hashCodes");
        Collection set = makeConfirmedCollection();
        Iterator iterator = collection.iterator();
        while (iterator.hasNext()) {
            assertTrue(set.add(iterator.next()), "Set.iterator should only return unique elements");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Set equals method is defined.
     */
    public boolean isEqualsCheckable() {
        return true;
    }

    /**
     * Returns an empty Set for use in modification testing.
     *
     * @return a confirmed empty collection
     */
    public Collection makeConfirmedCollection() {
        return new HashSet();
    }

    /**
     * Returns a full Set for use in modification testing.
     *
     * @return a confirmed full collection
     */
    public Collection makeConfirmedFullCollection() {
        Collection set = makeConfirmedCollection();
        set.addAll(Arrays.asList(getFullElements()));
        return set;
    }

    /**
     * Makes an empty set.  The returned set should have no elements.
     *
     * @return an empty set
     */
    public abstract Set makeEmptySet();

    /**
     * Makes a full set by first creating an empty set and then adding
     * all the elements returned by {@link #getFullElements()}.
     *
     * Override if your set does not support the add operation.
     *
     * @return a full set
     */
    public Set makeFullSet() {
        Set set = makeEmptySet();
        set.addAll(Arrays.asList(getFullElements()));
        return set;
    }

    /**
     * Makes an empty collection by invoking {@link #makeEmptySet()}.
     *
     * @return an empty collection
     */
    public final Collection makeCollection() {
        return makeEmptySet();
    }

    /**
     * Makes a full collection by invoking {@link #makeFullSet()}.
     *
     * @return a full collection
     */
    @Override
    public final Collection makeFullCollection() {
        return makeFullSet();
    }

    //-----------------------------------------------------------------------
    /**
     * Return the {@link CollectionAbstractTest#collection} fixture, but cast as a Set.
     */
    public Set getSet() {
        return (Set)collection;
    }

    /**
     * Return the {@link CollectionAbstractTest#confirmed} fixture, but cast as a Set.
     */
    public Set getConfirmedSet() {
        return (Set)confirmed;
    }

    //-----------------------------------------------------------------------
    /**
     * Tests {@link Set#equals(Object)}.
     */
    @Test
    public void testSetEquals() {
        resetEmpty();
        assertEquals(getSet(), getConfirmedSet(), "Empty sets should be equal");
        verifyAll();

        Collection set2 = makeConfirmedCollection();
        set2.add("foo");
        assertTrue(!getSet().equals(set2), "Empty set shouldn't equal nonempty set");

        resetFull();
        assertEquals(getSet(), getConfirmedSet(), "Full sets should be equal");
        verifyAll();

        set2.clear();
        set2.addAll(Arrays.asList(getOtherElements()));
        assertTrue(!getSet().equals(set2), "Sets with different contents shouldn't be equal");
    }

    /**
     * Tests {@link Set#hashCode()}.
     */
    @Test
    public void testSetHashCode() {
        resetEmpty();
        assertEquals(getSet().hashCode(), getConfirmedSet().hashCode(), "Empty sets have equal hashCodes");

        resetFull();
        assertEquals(getSet().hashCode(), getConfirmedSet().hashCode(), "Equal sets have equal hashCodes");
    }
}
