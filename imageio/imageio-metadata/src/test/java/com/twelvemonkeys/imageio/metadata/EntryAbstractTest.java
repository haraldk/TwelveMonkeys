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

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EntryTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EntryTest.java,v 1.0 02.01.12 17:33 haraldk Exp$
 */
public abstract class EntryAbstractTest extends ObjectAbstractTest {
    @Override
    protected Object makeObject() {
        return createEntry(new Object());
    }

    protected abstract Entry createEntry(Object value);

    @Test
    public void testCreateEntryNullValue() {
        Entry foo = createEntry(null);

        assertNotNull(foo.getIdentifier());
        assertEquals(null, foo.getValue());

        assertEquals("null", foo.getValueAsString());
    }

    @Test
    public void testEntryStringValue() {
        Entry foo = createEntry("bar");

        assertNotNull(foo.getIdentifier());
        assertEquals("bar", foo.getValue());
        assertEquals("bar", foo.getValueAsString());
    }

    @Test
    public void testEntryValue() {
        Entry foo = createEntry(77);

        assertNotNull(foo.getIdentifier());
        assertEquals(77, foo.getValue());
        assertEquals("77", foo.getValueAsString());
    }

    @Test
    public void testNullValueHashCode() {
        Entry foo = createEntry(null);

        // Doesn't really matter, as long as it doesn't throw NPE, but this should hold for all entries
        assertNotSame(0, foo.hashCode());
    }

    @Test
    public void testArrayValueHashCode() {
        // Doesn't really matter, as long as it doesn't throw NPE, but this should hold for all entries
        assertNotSame(0, createEntry(new int[0]).hashCode());
        assertNotSame(0, createEntry(new int[1]).hashCode());
    }

    @Test
    public void testArrayValue() {
        int[] array = {42, -1, 77, 99, 55};
        Entry foo = createEntry(array);

        assertEquals(5, foo.valueCount());
        assertArrayEquals(array, (int[]) foo.getValue());

        // Not strictly necessary, but nice
        assertEquals(Arrays.toString(array), foo.getValueAsString());
    }

    @Test
    public void testCharArrayValue() {
        char[] array = {'f', '0', '0'};
        Entry foo = createEntry(array);

        assertEquals(3, foo.valueCount());
        assertArrayEquals(array, (char[]) foo.getValue());

        // Not strictly necessary, but nice
        assertEquals("f00", foo.getValueAsString());
    }
}
