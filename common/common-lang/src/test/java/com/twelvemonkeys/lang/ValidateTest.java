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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    public void testNotNullNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notNull(null);
        });
    }

    @Test
    public void testNotNullWithParameter() {
        assertEquals("foo", Validate.notNull("foo", "bar"));
    }

    @Test
    public void testNotNullWithParameterNull() {
        try {
            assertThrows(IllegalArgumentException.class, () -> {
                Validate.notNull(null, "xyzzy");
            });
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

    @Test
    public void testNotNullWithNullParameterNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notNull(null, null);
        });
    }

    // Not empty (CharSequence)

    @Test
    public void testNotEmptyCharSequence() {
        assertEquals("foo", Validate.notEmpty("foo"));
    }

    @Test
    public void testNotEmptyCharSequenceNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((CharSequence) null);
        });
    }

    @Test
    public void testNotEmptyCharSequenceEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty("");
        });
    }

    @Test
    public void testNotEmptyCharSequenceOnlyWS() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(" \t\r");
        });
    }

    @Test
    public void testNotEmptyCharSequenceNullWithParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((CharSequence) null, "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyCharSequenceEmptyWithParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty("", "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyCharSequenceOnlyWSWithParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(" \t", "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyCharSequenceWithParameter() {
        assertEquals("foo", Validate.notEmpty("foo", "bar"));
    }

    @Test
    public void testNotEmptyCharSequenceWithParameterNull() {
        assertEquals("foo", Validate.notEmpty("foo", null));
    }

    @Test
    public void testNotEmptyCharSequenceNullWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((CharSequence) null, null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    @Test
    public void testNotEmptyCharSequenceEmptyWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty("", null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    @Test
    public void testNotEmptyCharSequenceOnlyWSWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(" \t\t  \n", null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    // Not empty (array)

    @Test
    public void testNotEmptyArray() {
        Integer[] array = new Integer[2];
        assertSame(array, Validate.notEmpty(array));
    }

    @Test
    public void testNotEmptyArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Object[]) null);
        });
    }

    @Test
    public void testNotEmptyArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(new String[0]);
        });
    }

    @Test
    public void testNotEmptyArrayParameter() {
        Integer[] array = new Integer[2];
        assertSame(array, Validate.notEmpty(array, "bar"));
    }

    @Test
    public void testNotEmptyArrayNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Object[]) null, "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyArrayEmptyParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(new Float[0], "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyArrayWithParameterNull() {
        Byte[] array = new Byte[1];
        assertSame(array, Validate.notEmpty(array, null));
    }

    @Test
    public void testNotEmptyArrayNullWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Object[]) null, null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    @Test
    public void testNotEmptyArrayEmptyWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(new Object[0], null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    // Not empty (Collection)

    @Test
    public void testNotEmptyCollection() {
        Collection<Integer> collection = Arrays.asList(new Integer[2]);
        assertSame(collection, Validate.notEmpty(collection));
    }

    @Test
    public void testNotEmptyCollectionNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Collection<?>) null);
        });
    }

    @Test
    public void testNotEmptyCollectionEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(Collections.emptySet());
        });
    }

    @Test
    public void testNotEmptyCollectionParameter() {
        List<Integer> collection = Collections.singletonList(1);
        assertSame(collection, Validate.notEmpty(collection, "bar"));
    }

    @Test
    public void testNotEmptyCollectionNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Collection<?>) null, "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyCollectionEmptyParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(new ArrayList<Object>(), "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyCollectionWithParameterNull() {
        Set<Byte> collection = Collections.singleton((byte) 1);
        assertSame(collection, Validate.notEmpty(collection, null));
    }

    @Test
    public void testNotEmptyCollectionNullWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Collection<?>) null, null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    @Test
    public void testNotEmptyCollectionEmptyWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Collection<?>) null, null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
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

    @Test
    public void testNotEmptyMapNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Map<?, ?>) null);
        });
    }

    @Test
    public void testNotEmptyMapEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(Collections.emptyMap());
        });
    }

    @Test
    public void testNotEmptyMapParameter() {
        Map<Integer, ?> map = Collections.singletonMap(1, null);
        assertSame(map, Validate.notEmpty(map, "bar"));
    }

    @Test
    public void testNotEmptyMapNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Map<?, ?>) null, "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyMapEmptyParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty(new HashMap<Object, Object>(), "xyzzy");
        });
        assertTrue(exception.getMessage().contains("xyzzy"));
    }

    @Test
    public void testNotEmptyMapWithParameterNull() {
        Map<Byte, Object> map = Collections.singletonMap((byte) 1, null);
        assertSame(map, Validate.notEmpty(map, null));
    }

    @Test
    public void testNotEmptyMapNullWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Map<?, ?>) null, null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
    }

    @Test
    public void testNotEmptyMapEmptyWithParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.notEmpty((Map<?, ?>) null, null);
        });
        assertTrue(exception.getMessage().contains("parameter"));
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

    @Test
    public void testNoNullElementsArrayNull() {

        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements((Object[]) null);
        });
    }

    @Test
    public void testNoNullElementsArrayNullElements() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(new Object[3]);
        });
    }

    @Test
    public void testNoNullElementsArrayMixed() {
        String[] array = new String[] {"foo", null, "bar"};
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(array);
        });
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

    @Test
    public void testNoNullElementsArrayNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements((Object[]) null, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullElementsArrayNullElementsParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(new Object[3], "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullElementsArrayMixedParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(new String[] {"foo", null, "bar"}, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
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

    @Test
    public void testNoNullElementsArrayNullParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements((Object[]) null, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullElementsArrayNullElementsParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(new Object[3], null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullElementsArrayMixedParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(new String[] {"foo", null, "bar"}, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
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

    @Test
    public void testNoNullElementsCollectionNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements((Collection<?>) null);
        });
    }

    @Test
    public void testNoNullElementsCollectionNullElements() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(Arrays.asList(null, null, null));
        });
    }

    @Test
    public void testNoNullElementsCollectionMixed() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(Arrays.asList("foo", null, "bar"));
        });
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

    @Test
    public void testNoNullElementsCollectionNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements((Set<?>) null, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullElementsCollectionNullElementsParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(Collections.singletonList(null), "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullElementsCollectionMixedParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(Arrays.asList("foo", null, "bar"), "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
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

    @Test
    public void testNoNullElementsCollectionNullParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements((ArrayList<?>) null, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullElementsCollectionNullElementsParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullElements(Collections.singleton(null), null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullElementsCollectionMixedParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Collection<?> collection = Arrays.asList("foo", null, "bar");
            Validate.noNullElements(collection, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
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

    @Test
    public void testNoNullValuesNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues((Map<?, ?>) null);
        });
    }

    @Test
    public void testNoNullValuesNullElements() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues(Collections.singletonMap("foo", null));
        });
    }

    @Test
    public void testNoNullValuesMixed() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("baz", null);
            }});
        });
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

    @Test
    public void testNoNullValuesNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues((Map<?, ?>) null, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullValuesNullElementsParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues(Collections.singletonMap("bar", null), "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullValuesMixedParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
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

    @Test
    public void testNoNullValuesNullParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues((Map<?, ?>) null, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullValuesNullElementsParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues(Collections.singletonMap(null, null), null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullValuesMixedParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullValues(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
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

    @Test
    public void testNoNullKeysNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys((Map<?, ?>) null);
        });
    }

    @Test
    public void testNoNullKeysNullElements() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys(Collections.singletonMap(null, "foo"));
        });
    }

    @Test
    public void testNoNullKeysMixed() {
        assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("baz", null);
            }});
        });
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

    @Test
    public void testNoNullKeysNullParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys((Map<?, ?>) null, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullKeysNullElementsParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys(Collections.singletonMap(null, "bar"), "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testNoNullKeysMixedParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, "foo");
        });
        assertTrue(exception.getMessage().contains("foo"));
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

    @Test
    public void testNoNullKeysNullParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys((Map<?, ?>) null, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullKeysNullElementsParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys(Collections.singletonMap(null, null), null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    @Test
    public void testNoNullKeysMixedParameterNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.noNullKeys(new HashMap<String, Object>() {{
                put("foo", 1);
                put(null, null);
                put("bar", null);
            }}, null);
        });
        assertTrue(exception.getMessage().contains("method parameter"));
    }

    // Is true

    @Test
    public void testIsTrue() {
        assertTrue(Validate.isTrue(true, "%s"));
    }

    @Test
    public void testIsTrueFalse() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.isTrue(false, "is %s");
        });
        assertEquals("is false", exception.getMessage());
    }

    @Test
    public void testIsTrueFalseNullParam() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.isTrue(false, null);
        });
        assertTrue(exception.getMessage().contains("false"));
    }

    @Test
    public void testIsTrueValue() {
        Object object = new Object();
        assertSame(object, Validate.isTrue(true, object, "%s"));
    }

    @Test
    public void testIsTrueFalseValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.isTrue(false, "baz", "foo is '%s'");
        });
        assertEquals("foo is 'baz'", exception.getMessage());
    }

    @Test
    public void testIsTrueValueParamNull() {
        assertEquals("foo", Validate.isTrue(true, "foo", null));
    }

    @Test
    public void testIsTrueFalseValueParamNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.isTrue(false, "foo", null);
        });
        assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void testIsTrueValueNullParamNull() {
        assertNull(Validate.isTrue(true, null, null));
    }

    @Test
    public void testIsTrueFalseValueNullParamNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Validate.isTrue(false, null, null);
        });
        assertTrue(exception.getMessage().contains("null"));
    }
}
