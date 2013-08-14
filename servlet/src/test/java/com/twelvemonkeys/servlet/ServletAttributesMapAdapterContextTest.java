/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.MapAbstractTestCase;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.mock;

/**
 * ServletConfigMapAdapterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ServletAttributesMapAdapterTestCase.java#1 $
 */
public class ServletAttributesMapAdapterContextTest extends MapAbstractTestCase {
    private static final String ATTRIB_VALUE_ETAG = "\"1234567890abcdef\"";
    private static final Date ATTRIB_VALUE_DATE = new Date();
    private static final List<Integer> ATTRIB_VALUE_FOO = Arrays.asList(1, 2);

    @Override
    public boolean isTestSerialization() {
        return false;
    }

    @Override
    public boolean isAllowNullKey() {
        return false; // Makes no sense...
    }

    @Override
    public boolean isAllowNullValue() {
        return false; // Should be allowed, but the tests don't handle the put(foo, null) == remove(foo) semantics
    }

    public Map makeEmptyMap() {
        MockServletContextImpl context = mock(MockServletContextImpl.class, Mockito.CALLS_REAL_METHODS);
        context.attributes = createAttributes(false);

        return new ServletAttributesMapAdapter(context);
    }

    @Override
    public Map makeFullMap() {
        MockServletContextImpl context = mock(MockServletContextImpl.class, Mockito.CALLS_REAL_METHODS);
        context.attributes = createAttributes(true);

        return new ServletAttributesMapAdapter(context);
    }

    private Map<String, Object> createAttributes(boolean initialValues) {
        Map<String, Object> map = new ConcurrentHashMap<String, Object>();

        if (initialValues) {
            String[] sampleKeys = (String[]) getSampleKeys();
            for (int i = 0; i < sampleKeys.length; i++) {
                map.put(sampleKeys[i], getSampleValues()[i]);
            }
        }

        return map;
    }

    @Override
    public Object[] getSampleKeys() {
        return new String[] {"Date", "ETag", "X-Foo"};
    }

    @Override
    public Object[] getSampleValues() {
        return new Object[] {ATTRIB_VALUE_DATE, ATTRIB_VALUE_ETAG, ATTRIB_VALUE_FOO};
    }

    @Override
    public Object[] getNewSampleValues() {
        // Needs to be same length but different values
        return new Object[] {new Date(-1l), "foo/bar", Arrays.asList(2, 3, 4)};
    }

    @SuppressWarnings("unchecked")
    @Override
    public void testMapPutNullValue() {
        // Special null semantics
        resetFull();

        int size = map.size();
        String key = getClass().getName() + ".someNewKey";
        map.put(key, null);
        assertEquals(size, map.size());
        assertFalse(map.containsKey(key));

        map.put(getSampleKeys()[0], null);
        assertEquals(size - 1, map.size());
        assertFalse(map.containsKey(getSampleKeys()[0]));

        map.remove(getSampleKeys()[1]);
        assertEquals(size - 2, map.size());
        assertFalse(map.containsKey(getSampleKeys()[1]));
    }

    private static abstract class MockServletContextImpl implements ServletContext {
        Map<String, Object> attributes;

        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        public Enumeration getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        public void setAttribute(String name, Object o) {
            if (o == null) {
                attributes.remove(name);
            }
            else {
                attributes.put(name, o);
            }
        }

        public void removeAttribute(String name) {
            attributes.remove(name);
        }
    }
}
