/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.util;

import com.twelvemonkeys.util.MappedBeanFactory;
import junit.framework.AssertionFailedError;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

/**
 * MappedBeanFactoryTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/test/java/com/twelvemonkeys/sandbox/MappedBeanFactoryTestCase.java#1 $
 */
public class MappedBeanFactoryTestCase {

    public static interface Foo {
        boolean isFoo();

        int getBar();
        void setBar(int bar);

        Rectangle getBounds();
        void setBounds(Rectangle bounds);
    }

    public static interface DefaultFoo extends Foo {
//        @MappedBeanFactory.DefaultBooleanValue
        @MappedBeanFactory.DefaultValue(booleanValue = false)
        boolean isFoo();

        @MappedBeanFactory.DefaultIntValue
        int getBar();
        void setBar(int bar);

        Rectangle getBounds();
        void setBounds(Rectangle bounds);

        @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
        DefaultFoo clone();
    }

    static interface ObservableFoo extends DefaultFoo {
        @MappedBeanFactory.DefaultBooleanValue(true)
        boolean isFoo();

        @MappedBeanFactory.DefaultIntValue(1)
        int getBar();

        @MappedBeanFactory.NotNull
        Rectangle getBounds();

        @MappedBeanFactory.Observable
        void setBounds(@MappedBeanFactory.NotNull Rectangle bounds);

        // TODO: This method should be implicitly supported, and throw IllegalArgument, if NoSuchProperty
        // TODO: An observable interface to extend?
        void addPropertyChangeListener(String property, PropertyChangeListener listener);
    }

    @Test
    public void testToString() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);
        assertNotNull(foo);
        assertNotNull(foo.toString());
        assertTrue(foo.toString().contains(DefaultFoo.class.getName()));

        // TODO: Consider this:
//        assertTrue(foo.toString().contains("foo=false"));
//        assertTrue(foo.toString().contains("bar=0"));
//        assertTrue(foo.toString().contains("bounds=null"));
    }

    @Test
    public void testClone() {
        DefaultFoo foo = MappedBeanFactory.as(DefaultFoo.class, Collections.singletonMap("foo", true));
        DefaultFoo clone = foo.clone();
        assertNotSame(foo, clone);
        assertEquals(foo, clone);
        assertEquals(foo.hashCode(), clone.hashCode());
        assertEquals(foo.isFoo(), clone.isFoo());
    }

    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, Collections.singletonMap("foo", true));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(bytes);
        outputStream.writeObject(foo);

        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Foo bar = (Foo) inputStream.readObject();
        
        assertNotSame(foo, bar);
        assertEquals(foo, bar);
        assertEquals(foo.hashCode(), bar.hashCode());
        assertEquals(foo.isFoo(), bar.isFoo());
    }

    @Test
    public void testNotEqualsNull() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);

        @SuppressWarnings({"ObjectEqualsNull"})
        boolean equalsNull = foo.equals(null);

        assertFalse(equalsNull);
    }

    @Test
    public void testEqualsSelf() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>() {
            @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
            @Override
            public boolean equals(Object o) {
                throw new AssertionFailedError("Don't need to test map for equals if same object");
            }
        });

        assertTrue(foo.equals(foo));
    }

    @Test
    public void testEqualsOther() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);
        Foo other = MappedBeanFactory.as(DefaultFoo.class);

        assertEquals(foo, other);
    }

    @Test
    public void testEqualsOtherModifiedSameValue() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>());
        Foo other = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bar", 0)));
        
        assertEquals(foo, other);

        // No real change
        other.setBar(foo.getBar());
        assertTrue(foo.equals(other));
    }

    @Test
    public void testNotEqualsOtherModified() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);
        Foo other = MappedBeanFactory.as(DefaultFoo.class);

        assertEquals(foo, other);

        // Real change
        other.setBar(42);
        assertFalse(foo.equals(other));
    }

    @Test
    public void testEqualsSubclass() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);

        Foo sub = MappedBeanFactory.as(ObservableFoo.class);
        assertEquals(foo, sub);
    }

    @Test
    public void testNotEqualsDifferentValues() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);
        Foo bar = MappedBeanFactory.as(DefaultFoo.class, Collections.singletonMap("bar", true));
        assertFalse(foo.equals(bar));
    }

    @Test
    public void testNotEqualsDifferentClass() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class);
        ActionListener actionListener = MappedBeanFactory.as(ActionListener.class);
        assertFalse(foo.equals(actionListener));
    }

    @Test
    public void testBooleanReadOnly() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, Collections.singletonMap("foo", true));

        assertNotNull(foo);
        assertEquals(true, foo.isFoo());
    }

    @Test
    public void testBooleanEmpty() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>());

        assertNotNull(foo);

        try {
            foo.isFoo();
            fail("Expected NullPointerException");
        }
        catch (NullPointerException expected) {
        }
    }

    @Test
    public void testBooleanEmptyWithConverter() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(), new NullBooleanConverter(true));

        assertNotNull(foo);

        assertEquals(true, foo.isFoo());
    }

    @Test
    public void testIntReadOnly() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, Collections.singletonMap("bar", 1));

        assertNotNull(foo);
        assertEquals(1, foo.getBar());

        try {
            foo.setBar(42);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testIntReadWrite() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bar", 1)));

        assertNotNull(foo);
        assertEquals(1, foo.getBar());

        foo.setBar(42);
        assertEquals(42, foo.getBar());
    }

    @Test
    public void testIntNull() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bar", null)));

        assertNotNull(foo);

        // TODO: Handle null-values smarter, maybe throw a better exception?
        // TODO: Consider allowing custom initializers?
        try {
            foo.getBar();
            fail("Expected NullPointerException");
        }
        catch (NullPointerException expected) {
        }

        foo.setBar(42);
        assertEquals(42, foo.getBar());
    }

    @Test
    public void testIntNullWithConverter() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bar", null)), new NullIntConverter(1));

        assertNotNull(foo);

        assertEquals(1, foo.getBar());

        foo.setBar(42);
        assertEquals(42, foo.getBar());
    }

    @Test
    public void testIntWrongType() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bar", "1")));

        assertNotNull(foo);

        // TODO: Handle conversion smarter, maybe throw a better exception?
        try {
            foo.getBar();
            fail("Expected ClassCastException");
        }
        catch (ClassCastException expected) {
        }

        // TODO: Should we allow changing type?
        try {
            foo.setBar(42);
            fail("Expected ClassCastException");
        }
        catch (ClassCastException expected) {
        }
    }

    @Test
    public void testBounds() {
        Rectangle rectangle = new Rectangle(2, 2, 4, 4);
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bounds", rectangle)));

        assertNotNull(foo);
        assertEquals(rectangle, foo.getBounds());

        foo.setBounds(new Rectangle());
        assertEquals(new Rectangle(), foo.getBounds());
    }

    @Test
    public void testBoundsNull() {
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("bounds", null)));

        assertNotNull(foo);
        assertNull(foo.getBounds());

        Rectangle rectangle = new Rectangle(2, 2, 4, 4);
        foo.setBounds(rectangle);
        assertEquals(rectangle, foo.getBounds());

        foo.setBounds(null);
        assertEquals(null, foo.getBounds());
    }

    @Test
    public void testBoundsNullWithConverter() {
        // TODO: Allow @NotNull annotations, to say that null is not a valid return value/paramter?
        Foo foo = MappedBeanFactory.as(ObservableFoo.class, new HashMap<String, Object>(Collections.singletonMap("bounds", null)), new MappedBeanFactory.Converter<Void, Rectangle>() {
            public Class<Void> getFromType() {
                return Void.class;
            }

            public Class<Rectangle> getToType() {
                return Rectangle.class;
            }

            public Rectangle convert(Void value, Rectangle old) {
                return new Rectangle(10, 10, 10, 10);
            }
        });

        assertNotNull(foo);
        // TODO: The current problem is that null is okay as return value, even if not specified for interface...
        assertEquals(new Rectangle(10, 10, 10, 10), foo.getBounds());

        Rectangle rectangle = new Rectangle(2, 2, 4, 4);
        foo.setBounds(rectangle);
        assertEquals(rectangle, foo.getBounds());
    }

    @Test
    public void testBoundsAsMapWithConverter() throws IntrospectionException {
        Rectangle rectangle = new Rectangle(2, 2, 4, 4);
        Map<String, Object> recAsMap = new HashMap<String, Object>();
        recAsMap.put("x", 2);
        recAsMap.put("y", 2);
        recAsMap.put("width", 4);
        recAsMap.put("height", 4);

        HashMap<String, Object> map = new HashMap<String, Object>(Collections.singletonMap("bounds", recAsMap));

        // TODO: Allow for registering superclasses/interfaces like Map...
        Foo foo = MappedBeanFactory.as(DefaultFoo.class, map, new MapRectangleConverter(), new RectangleMapConverter());

        assertNotNull(foo);

        assertEquals(rectangle, foo.getBounds());

        foo.setBounds(new Rectangle());
        assertEquals(new Rectangle(), foo.getBounds());
        assertEquals(recAsMap, map.get("bounds"));
        assertSame(recAsMap, map.get("bounds"));

        // TODO: The converter should maybe not have to handle this
        foo.setBounds(null);
        assertNull(foo.getBounds());
        assertEquals(recAsMap, map.get("bounds"));
        assertSame(recAsMap, map.get("bounds"));

        Rectangle bounds = new Rectangle(1, 1, 1, 1);
        foo.setBounds(bounds);
        assertEquals(bounds, foo.getBounds());
        assertEquals(1, foo.getBounds().x);
        assertEquals(1, foo.getBounds().y);
        assertEquals(1, foo.getBounds().width);
        assertEquals(1, foo.getBounds().height);
        assertEquals(recAsMap, map.get("bounds"));
        assertSame(recAsMap, map.get("bounds"));
    }

    @Test
    public void testSpeed() {
        // How many times faster may the direct access be, before we declare failure?
        final int threshold = 50;

        Foo foo = MappedBeanFactory.as(DefaultFoo.class, new HashMap<String, Object>(Collections.singletonMap("foo", false)));

        Foo bar = new Foo() {
            public boolean isFoo() {
                return false;
            }

            public int getBar() {
                throw new UnsupportedOperationException("Method getBar not implemented");
            }

            public void setBar(int bar) {
                throw new UnsupportedOperationException("Method setBar not implemented");
            }

            public Rectangle getBounds() {
                throw new UnsupportedOperationException("Method getBounds not implemented"); // TODO: Implement
            }

            public void setBounds(Rectangle bounds) {
                throw new UnsupportedOperationException("Method setBounds not implemented"); // TODO: Implement
            }
        };

        final int warmup = 50005;
        final int iter = 2000000;
        for (int i = 0; i < warmup; i++) {
            if (foo.isFoo()) {
                fail();
            }
            if (bar.isFoo()) {
                fail();
            }
        }

        long startProxy = System.nanoTime();
        for (int i = 0; i < iter; i++) {
            if (foo.isFoo()) {
                fail();
            }
        }
        long proxyTime = System.nanoTime() - startProxy;

        long startJava = System.nanoTime();
        for (int i = 0; i < iter; i++) {
            if (bar.isFoo()) {
                fail();
            }
        }
        long javaTime = System.nanoTime() - startJava;

        assertTrue(
                String.format(
                        "Proxy time (%1$,d ms) greater than %3$d times direct invocation (%2$,d ms)",
                        proxyTime / 1000, javaTime / 1000, threshold
                ),
                proxyTime < threshold * javaTime);
    }

    private static class MapRectangleConverter implements MappedBeanFactory.Converter<HashMap, Rectangle> {
        public Class<HashMap> getFromType() {
            return HashMap.class;
        }

        public Class<Rectangle> getToType() {
            return Rectangle.class;
        }

        public Rectangle convert(final HashMap pMap, Rectangle pOldValue) {
            if (pMap == null || pMap.isEmpty()) {
                return null;
            }

            Rectangle rectangle = pOldValue != null ? pOldValue : new Rectangle();

            rectangle.x = (Integer) pMap.get("x");
            rectangle.y = (Integer) pMap.get("y");
            rectangle.width = (Integer) pMap.get("width");
            rectangle.height = (Integer) pMap.get("height");

            return rectangle;
        }
    }

    private static class RectangleMapConverter implements MappedBeanFactory.Converter<Rectangle, HashMap> {
        public Class<HashMap> getToType() {
            return HashMap.class;
        }

        public Class<Rectangle> getFromType() {
            return Rectangle.class;
        }

        public HashMap convert(final Rectangle pRectangle, HashMap pOldValue) {
            @SuppressWarnings("unchecked")
            HashMap<String, Integer> map = pOldValue != null ? pOldValue : new HashMap<String, Integer>();

            if (pRectangle != null) {
                map.put("x", pRectangle.x);
                map.put("y", pRectangle.y);
                map.put("width", pRectangle.width);
                map.put("height", pRectangle.height);
            }
            else {
                map.remove("x");
                map.remove("y");
                map.remove("width");
                map.remove("height");
            }

            return map;
        }
    }

    private static class NullIntConverter implements MappedBeanFactory.Converter<Void, Integer> {
        private Integer mInitialValue;

        public NullIntConverter(int pValue) {
            mInitialValue = pValue;
        }

        public Class<Void> getFromType() {
            return Void.class;
        }

        public Class<Integer> getToType() {
            return Integer.class;
        }

        public Integer convert(Void value, Integer old) {
            return mInitialValue;
        }
    }

    private static class NullBooleanConverter implements MappedBeanFactory.Converter<Void, Boolean> {
        private Boolean mInitialValue;

        public NullBooleanConverter(boolean pValue) {
            mInitialValue = pValue;
        }

        public Class<Void> getFromType() {
            return Void.class;
        }

        public Class<Boolean> getToType() {
            return Boolean.class;
        }

        public Boolean convert(Void value, Boolean old) {
            return mInitialValue;
        }
    }
}
