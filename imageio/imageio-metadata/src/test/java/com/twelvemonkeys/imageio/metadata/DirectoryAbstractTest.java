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

package com.twelvemonkeys.imageio.metadata;

import com.twelvemonkeys.lang.ObjectAbstractTest;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * DirectoryTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DirectoryTest.java,v 1.0 02.01.12 15:07 haraldk Exp$
 */
public abstract class DirectoryAbstractTest extends ObjectAbstractTest {

    @Override
    protected Object makeObject() {
        return createDirectory(Collections.singleton(createEntry("entry", null)));
    }

    protected abstract Directory createDirectory(Collection<Entry> entries);

    // Override by subclasses that requires special type of entries
    protected Entry createEntry(final String identifier, final Object value) {
        return new TestEntry(identifier, value);
    }

    @Test
    public void testCreateNull() {
        Directory directory = createDirectory(null);

        assertEquals(0, directory.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullEntries() {
        createDirectory(Collections.<Entry>singleton(null));
    }

    @Test
    public void testSingleEntry() {
        Entry entry = createEntry("foo", new Object());
        Directory directory = createDirectory(Collections.singleton(entry));

        assertEquals(1, directory.size());
        assertSame(entry, directory.getEntryById("foo"));
    }

    @Test
    public void testNonExistentEntry() {
        Entry entry = createEntry("foo", new Object());
        Directory directory = createDirectory(Collections.singleton(entry));
        assertNull(directory.getEntryById("bar"));
    }

    @Test
    public void testMultipleEntries() {
        Entry one = createEntry("foo", new Object());
        Entry two = createEntry("bar", new Object());

        Directory directory = createDirectory(Arrays.asList(one, two));

        assertEquals(2, directory.size());

        assertSame(one, directory.getEntryById("foo"));
        assertSame(two, directory.getEntryById("bar"));
    }

    @Test
    public void testEmptyIterator() {
        Directory directory = createDirectory(null);

        assertEquals(0, directory.size());

        assertFalse(directory.iterator().hasNext());
    }

    @Test
    public void testSingleIterator() {
        Entry one = createEntry("foo", new Object());

        Directory directory = createDirectory(Arrays.asList(one));
        
        int count = 0;
        for (Entry entry : directory) {
            assertSame(one, entry);
            count++;
        }
        
        assertEquals(1, count);
    }

    @Test
    public void testIteratorMutability() {
        Entry one = createEntry("foo", new Object());
        Entry two = createEntry("bar", new Object());

        Directory directory = createDirectory(Arrays.asList(one, two));

        assertEquals(2, directory.size());

        Iterator<Entry> entries = directory.iterator();
        if (!directory.isReadOnly()) {
            while (entries.hasNext()) {
                entries.next();
                entries.remove();
            }
            
            assertEquals(0, directory.size());
        }
        else {
            while (entries.hasNext()) {
                try {
                    entries.next();
                    entries.remove();
                    fail("Expected UnsupportedOperationException");
                }
                catch (UnsupportedOperationException expected) {
                }
            }

            assertEquals(2, directory.size());
        }
    }

    @Test
    public void testMultipleIterator() {
        Entry one = createEntry("foo", new Object());
        Entry two = createEntry("bar", new Object());
        Entry three = createEntry("baz", new Object());

        List<Entry> all = Arrays.asList(one, two, three);
        Directory directory = createDirectory(all);

        // Test that each element is contained, and only once
        List<Entry> entries = new ArrayList<Entry>(all);

        int count = 0;
        for (Entry entry : directory) {
            assertTrue(entries.contains(entry));
            assertTrue(entries.remove(entry));

            count++;
        }

        assertTrue(entries.isEmpty());
        assertEquals(3, count);
    }

    protected static class TestEntry extends AbstractEntry {
        public TestEntry(final String identifier, final Object value) {
            super(identifier, value);
        }
    }
}
