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

package com.twelvemonkeys.lang;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.*;

/**
 * ValidateTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ValidateTest.java,v 1.0 11.04.12 09:06 haraldk Exp$
 */
public class ValidateTest {
    // Not null

    @Test
    public void testNotNull() {
        assertEquals("foo", Validate.notNull("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotNullNull() {
        Validate.notNull(null);
    }

    @Test
    public void testNotNullWithParameter() {
        assertEquals("foo", Validate.notNull("foo", "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotNullWithParameterNull() {
        try {
            Validate.notNull(null, "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test
    public void testNotNullWithNullParameter() {
        Validate.notNull("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotNullWithNullParameterNull() {
        Validate.notNull(null, null);
    }

    // Not empty (CharSequence)

    @Test
    public void testNotEmptyCharSequence() {
        assertEquals("foo", Validate.notEmpty("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceNull() {
        Validate.notEmpty((CharSequence) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceEmpty() {
        Validate.notEmpty("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceOnlyWS() {
        Validate.notEmpty(" \t\r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceNullWithParameter() {
        try {
            Validate.notEmpty((CharSequence) null, "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceEmptyWithParameter() {
        try {
            Validate.notEmpty("", "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceOnlyWSWithParameter() {
        try {
            Validate.notEmpty(" \t", "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test
    public void testNotEmptyCharSequenceWithParameter() {
        assertEquals("foo", Validate.notEmpty("foo", "bar"));
    }

    @Test
    public void testNotEmptyCharSequenceWithParameterNull() {
        assertEquals("foo", Validate.notEmpty("foo", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceNullWithParameterNull() {
        try {
            Validate.notEmpty((CharSequence) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceEmptyWithParameterNull() {
        try {
            Validate.notEmpty("", null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCharSequenceOnlyWSWithParameterNull() {
        try {
            Validate.notEmpty(" \t\t  \n", null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    // Not empty (array)

    @Test
    public void testNotEmptyArray() {
        Integer[] array = new Integer[2];
        assertSame(array, Validate.notEmpty(array));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyArrayNull() {
        Validate.notEmpty((Object[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyArrayEmpty() {
        Validate.notEmpty(new String[0]);
    }

    @Test
    public void testNotEmptyArrayParameter() {
        Integer[] array = new Integer[2];
        assertSame(array, Validate.notEmpty(array, "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyArrayNullParameter() {
        try {
            Validate.notEmpty((Object[]) null, "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyArrayEmptyParameter() {
        try {
            Validate.notEmpty(new Float[0], "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test
    public void testNotEmptyArrayWithParameterNull() {
        Byte[] array = new Byte[1];
        assertSame(array, Validate.notEmpty(array, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyArrayNullWithParameterNull() {
        try {
            Validate.notEmpty((Object[]) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyArrayEmptyWithParameterNull() {
        try {
            Validate.notEmpty(new Object[0], null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    // Not empty (Collection)

    @Test
    public void testNotEmptyCollection() {
        Collection<Integer> collection = Arrays.asList(new Integer[2]);
        assertSame(collection, Validate.notEmpty(collection));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCollectionNull() {
        Validate.notEmpty((Collection<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCollectionEmpty() {
        Validate.notEmpty(Collections.emptySet());
    }

    @Test
    public void testNotEmptyCollectionParameter() {
        List<Integer> collection = Collections.singletonList(1);
        assertSame(collection, Validate.notEmpty(collection, "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCollectionNullParameter() {
        try {
            Validate.notEmpty((Collection<?>) null, "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCollectionEmptyParameter() {
        try {
            Validate.notEmpty(new ArrayList<Object>(), "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test
    public void testNotEmptyCollectionWithParameterNull() {
        Set<Byte> collection = Collections.singleton((byte) 1);
        assertSame(collection, Validate.notEmpty(collection, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCollectionNullWithParameterNull() {
        try {
            Validate.notEmpty((Collection<?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyCollectionEmptyWithParameterNull() {
        try {
            Validate.notEmpty((Collection<?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    // Not empty (Map)

    @Test
    public void testNotEmptyMap() {
        Map<Integer, ?> map = new HashMap<Integer, Object>() {{
        put(1, null);
        put(2, null);
        }};
        assertSame(map, Validate.notEmpty(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyMapNull() {
        Validate.notEmpty((Map<?, ?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyMapEmpty() {
        Validate.notEmpty(Collections.emptyMap());
    }

    @Test
    public void testNotEmptyMapParameter() {
        Map<Integer, ?> map = Collections.singletonMap(1, null);
        assertSame(map, Validate.notEmpty(map, "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyMapNullParameter() {
        try {
            Validate.notEmpty((Map<?, ?>) null, "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyMapEmptyParameter() {
        try {
            Validate.notEmpty(new HashMap<Object, Object>(), "xyzzy");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("xyzzy"));
            throw e;
        }
    }

    @Test
    public void testNotEmptyMapWithParameterNull() {
        Map<Byte, Object> map = Collections.singletonMap((byte) 1, null);
        assertSame(map, Validate.notEmpty(map, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyMapNullWithParameterNull() {
        try {
            Validate.notEmpty((Map<?, ?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEmptyMapEmptyWithParameterNull() {
        try {
            Validate.notEmpty((Map<?, ?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("parameter"));
            throw e;
        }
    }

    // No null elements (array)

    @Test
    public void testNoNullElementsArray() {
        String[] array = new String[] {"foo", "bar", "baz"};
        assertSame(array, Validate.noNullElements(array));
    }

    @Test
    public void testNoNullElementsArrayEmpty() {
        Object[] array = new Object[0];
        assertSame(array, Validate.noNullElements(array));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayNull() {
        Validate.noNullElements((Object[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayNullElements() {
        Validate.noNullElements(new Object[3]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayMixed() {
        String[] array = new String[] {"foo", null, "bar"};
        Validate.noNullElements(array);
    }

    @Test
    public void testNoNullElementsArrayParameter() {
        String[] array = new String[] {"foo", "bar", "baz"};
        assertSame(array, Validate.noNullElements(array, "foo"));
    }

    @Test
    public void testNoNullElementsArrayEmptyParameter() {
        Object[] array = new Object[0];
        assertSame(array, Validate.noNullElements(array, "foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayNullParameter() {
        try {
            Validate.noNullElements((Object[]) null, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayNullElementsParameter() {
        try {
            Validate.noNullElements(new Object[3], "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayMixedParameter() {
        try {
            Validate.noNullElements(new String[] {"foo", null, "bar"}, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test
    public void testNoNullElementsArrayParameterNull() {
        String[] array = new String[] {"foo", "bar", "baz"};
        assertSame(array, Validate.noNullElements(array, null));
    }

    @Test
    public void testNoNullElementsArrayEmptyParameterNull() {
        Object[] array = new Object[0];
        assertSame(array, Validate.noNullElements(array, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayNullParameterNull() {
        try {
            Validate.noNullElements((Object[]) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayNullElementsParameterNull() {
        try {
            Validate.noNullElements(new Object[3], null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsArrayMixedParameterNull() {
        try {
            Validate.noNullElements(new String[] {"foo", null, "bar"}, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    // No null elements (Collection)

    @Test
    public void testNoNullElementsCollection() {
        List<String> collection = Arrays.asList("foo", "bar", "baz");
        assertSame(collection, Validate.noNullElements(collection));
    }

    @Test
    public void testNoNullElementsCollectionEmpty() {
        Set<?> collection = Collections.emptySet();
        assertSame(collection, Validate.noNullElements(collection));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionNull() {
        Validate.noNullElements((Collection<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionNullElements() {
        Validate.noNullElements(Arrays.asList(null, null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionMixed() {
        Validate.noNullElements(Arrays.asList("foo", null, "bar"));
    }

    @Test
    public void testNoNullElementsCollectionParameter() {
        List<String> collection = Arrays.asList("foo", "bar", "baz");
        assertSame(collection, Validate.noNullElements(collection, "foo"));
    }

    @Test
    public void testNoNullElementsCollectionEmptyParameter() {
        List<?> collection = new CopyOnWriteArrayList<Object>();
        assertSame(collection, Validate.noNullElements(collection, "foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionNullParameter() {
        try {
            Validate.noNullElements((Set<?>) null, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionNullElementsParameter() {
        try {
            Validate.noNullElements(Collections.singletonList(null), "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionMixedParameter() {
        try {
            Validate.noNullElements(Arrays.asList("foo", null, "bar"), "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test
    public void testNoNullElementsCollectionParameterNull() {
        List<String> collection = Arrays.asList("foo", "bar", "baz");
        assertSame(collection, Validate.noNullElements(collection, null));
    }

    @Test
    public void testNoNullElementsCollectionEmptyParameterNull() {
        Collection<?> collection = Collections.emptySet();
        assertSame(collection, Validate.noNullElements(collection, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionNullParameterNull() {
        try {
            Validate.noNullElements((ArrayList<?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionNullElementsParameterNull() {
        try {
            Validate.noNullElements(Collections.singleton(null), null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullElementsCollectionMixedParameterNull() {
        Collection<?> collection = Arrays.asList("foo", null, "bar");
        try {
            Validate.noNullElements(collection, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    // No null values (Map)

    @Test
    public void testNoNullValuesMap() {
        Map<String, ?> map = new HashMap<String, Object>() {{
            put("foo", 1);
            put("bar", 2);
            put("baz", 3);
        }};
        assertSame(map, Validate.noNullValues(map));
    }

    @Test
    public void testNoNullValuesEmpty() {
        Map<?, ?> map = Collections.emptyMap();
        assertSame(map, Validate.noNullValues(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesNull() {
        Validate.noNullValues((Map<?, ?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesNullElements() {
        Validate.noNullValues(Collections.singletonMap("foo", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesMixed() {
        Validate.noNullValues(new HashMap<String, Object>() {{
            put("foo", 1);
            put(null, null);
            put("baz", null);
        }});
    }

    @Test
    public void testNoNullValuesParameter() {
        Map<String, ?> map = new HashMap<String, Object>() {{
            put("foo", 1);
            put("bar", 2);
            put("baz", 3);
        }};
        assertSame(map, Validate.noNullValues(map, "foo"));
    }

    @Test
    public void testNoNullValuesEmptyParameter() {
        Map<?, ?> map = new HashMap<Object, Object>();
        assertSame(map, Validate.noNullValues(map, "foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesNullParameter() {
        try {
            Validate.noNullValues((Map<?, ?>) null, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesNullElementsParameter() {
        try {
            Validate.noNullValues(Collections.singletonMap("bar", null), "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesMixedParameter() {
        try {
            Validate.noNullValues(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test
    public void testNoNullValuesParameterNull() {
        Map<String, ?> map = new HashMap<String, Object>() {{
            put("foo", 1);
            put("bar", 2);
            put("baz", 3);
        }};
        assertSame(map, Validate.noNullValues(map, null));
    }

    @Test
    public void testNoNullValuesEmptyParameterNull() {
        Map<?, ?> map = Collections.emptyMap();
        assertSame(map, Validate.noNullValues(map, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesNullParameterNull() {
        try {
            Validate.noNullValues((Map<?, ?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesNullElementsParameterNull() {
        try {
            Validate.noNullValues(Collections.singletonMap(null, null), null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullValuesMixedParameterNull() {

        try {
            Validate.noNullValues(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    // No null keys (Map)

    @Test
    public void testNoNullKeysMap() {
        Map<String, ?> map = new HashMap<String, Object>() {{
            put("foo", 1);
            put("bar", 2);
            put("baz", 3);
        }};
        assertSame(map, Validate.noNullKeys(map));
    }

    @Test
    public void testNoNullKeysEmpty() {
        Map<?, ?> map = Collections.emptyMap();
        assertSame(map, Validate.noNullKeys(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysNull() {
        Validate.noNullKeys((Map<?, ?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysNullElements() {
        Validate.noNullKeys(Collections.singletonMap(null, "foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysMixed() {
        Validate.noNullKeys(new HashMap<String, Object>() {{
            put("foo", 1);
            put(null, null);
            put("baz", null);
        }});
    }

    @Test
    public void testNoNullKeysParameter() {
        Map<String, ?> map = new HashMap<String, Object>() {{
            put("foo", 1);
            put("bar", 2);
            put("baz", 3);
        }};
        assertSame(map, Validate.noNullKeys(map, "foo"));
    }

    @Test
    public void testNoNullKeysEmptyParameter() {
        Map<?, ?> map = new HashMap<Object, Object>();
        assertSame(map, Validate.noNullKeys(map, "foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysNullParameter() {
        try {
            Validate.noNullKeys((Map<?, ?>) null, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysNullElementsParameter() {
        try {
            Validate.noNullKeys(Collections.singletonMap(null, "bar"), "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysMixedParameter() {
        try {
            Validate.noNullKeys(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, "foo");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test
    public void testNoNullKeysParameterNull() {
        Map<String, ?> map = new HashMap<String, Object>() {{
            put("foo", 1);
            put("bar", 2);
            put("baz", 3);
        }};
        assertSame(map, Validate.noNullKeys(map, null));
    }

    @Test
    public void testNoNullKeysEmptyParameterNull() {
        Map<?, ?> map = Collections.emptyMap();
        assertSame(map, Validate.noNullKeys(map, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysNullParameterNull() {
        try {
            Validate.noNullKeys((Map<?, ?>) null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysNullElementsParameterNull() {
        try {
            Validate.noNullKeys(Collections.singletonMap(null, null), null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoNullKeysMixedParameterNull() {

        try {
            Validate.noNullKeys(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("method parameter"));
            throw e;
        }
    }

    // Is true

    @Test
    public void testIsTrue() {
        assertTrue(Validate.isTrue(true, "%s"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTrueFalse() {
        try {
            Validate.isTrue(false, "is %s");
        }
        catch (IllegalArgumentException e) {
            assertEquals("is false", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTrueFalseNullParam() {
        try {
            Validate.isTrue(false, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("false"));
            throw e;
        }
    }

    @Test
    public void testIsTrueValue() {
        Object object = new Object();
        assertSame(object, Validate.isTrue(true, object, "%s"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTrueFalseValue() {
        try {
            Validate.isTrue(false, "baz", "foo is '%s'");
        }
        catch (IllegalArgumentException e) {
            assertEquals("foo is 'baz'", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testIsTrueValueParamNull() {
        assertEquals("foo", Validate.isTrue(true, "foo", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTrueFalseValueParamNull() {
        try {
            Validate.isTrue(false, "foo", null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foo"));
            throw e;
        }
    }

    @Test
    public void testIsTrueValueNullParamNull() {
        assertNull(Validate.isTrue(true, null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsTrueFalseValueNullParamNull() {
        try {
            Validate.isTrue(false, null, null);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null"));
            throw e;
        }
    }

}
