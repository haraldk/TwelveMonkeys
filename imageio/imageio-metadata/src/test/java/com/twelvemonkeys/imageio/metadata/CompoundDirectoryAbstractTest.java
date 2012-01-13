/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * CompoundDirectoryTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CompoundDirectoryTest.java,v 1.0 02.01.12 15:07 haraldk Exp$
 */
public abstract class CompoundDirectoryAbstractTest extends DirectoryAbstractTest {
    protected abstract CompoundDirectory createCompoundDirectory(Collection<Directory> directories);

    // Override by subclasses that require special kind of directory
    protected Directory createSingleDirectory(final Collection<Entry> entries) {
        return new TestDirectory(entries);
    }

    @Override
    protected  final Directory createDirectory(final Collection<Entry> entries) {
        // A compound directory should behave like a normal directory
        return createCompoundDirectory(Collections.<Directory>singleton(createSingleDirectory(entries)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNullDirectories() {
        createCompoundDirectory(Collections.<Directory>singleton(null));
    }

    @Test
    public void testSingle() {
        Directory only = createSingleDirectory(null);

        CompoundDirectory directory = createCompoundDirectory(Collections.<Directory>singleton(only));

        assertEquals(1, directory.directoryCount());
        assertSame(only, directory.getDirectory(0));
    }

    @Test
    public void testMultiple() {
        Directory one = createSingleDirectory(null);
        Directory two = createSingleDirectory(null);
        Directory three = createSingleDirectory(null);

        CompoundDirectory directory = createCompoundDirectory(Arrays.<Directory>asList(one, two, three));

        assertEquals(3, directory.directoryCount());
        assertSame(one, directory.getDirectory(0));
        assertSame(two, directory.getDirectory(1));
        assertSame(three, directory.getDirectory(2));
    }

    @Test
    public void testEntries() {
        Directory one = createSingleDirectory(null);
        Directory two = createSingleDirectory(null);
        Directory three = createSingleDirectory(null);

        CompoundDirectory directory = createCompoundDirectory(Arrays.<Directory>asList(one, two, three));

        assertEquals(3, directory.directoryCount());
        assertSame(one, directory.getDirectory(0));
        assertSame(two, directory.getDirectory(1));
        assertSame(three, directory.getDirectory(2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testOutOfBounds() {
        Directory only = createSingleDirectory(null);
        CompoundDirectory directory = createCompoundDirectory(Collections.<Directory>singleton(only));

        directory.getDirectory(directory.directoryCount());
    }

    protected static final class TestDirectory extends AbstractDirectory {
        public TestDirectory(final Collection<Entry> entries) {
            super(entries);
        }
    }
}
