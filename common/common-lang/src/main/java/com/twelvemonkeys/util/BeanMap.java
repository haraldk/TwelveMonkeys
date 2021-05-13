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

package com.twelvemonkeys.util;

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A {@code Map} adapter for a Java Bean.
 * <p>
 * Ruthlessly stolen from
 * <a href="http://binkley.blogspot.com/2006/08/mapping-java-bean.html">Binkley's Blog</a>
 * </p>
 */
public final class BeanMap extends AbstractMap<String, Object> implements Serializable, Cloneable {
    private final Object bean;
    private transient Set<PropertyDescriptor> descriptors;

    public BeanMap(Object pBean) throws IntrospectionException {
        if (pBean == null) {
            throw new IllegalArgumentException("bean == null");
        }

        bean = pBean;
        descriptors = initDescriptors(pBean);
    }

    private static Set<PropertyDescriptor> initDescriptors(Object pBean) throws IntrospectionException {
        final Set<PropertyDescriptor> descriptors = new HashSet<PropertyDescriptor>();

        PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(pBean.getClass()).getPropertyDescriptors();
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            // Skip Object.getClass(), as you probably never want it
            if ("class".equals(descriptor.getName()) && descriptor.getPropertyType() == Class.class) {
                continue;
            }

            // Only support simple setter/getters.
            if (!(descriptor instanceof IndexedPropertyDescriptor)) {
                descriptors.add(descriptor);
            }
        }

        return Collections.unmodifiableSet(descriptors);
    }

    public Set<Entry<String, Object>> entrySet() {
        return new BeanSet();
    }

    public Object get(final Object pKey) {
        return super.get(pKey);
    }

    public Object put(final String pKey, final Object pValue) {
        checkKey(pKey);

        for (Entry<String, Object> entry : entrySet()) {
            if (entry.getKey().equals(pKey)) {
                return entry.setValue(pValue);
            }
        }

        return null;
    }

    public Object remove(final Object pKey) {
        return super.remove(checkKey(pKey));
    }

    public int size() {
        return descriptors.size();
    }

    private String checkKey(final Object pKey) {
        if (pKey == null) {
            throw new IllegalArgumentException("key == null");
        }
        // NB - the cast forces CCE if key is the wrong type.
        final String name = (String) pKey;

        if (!containsKey(name)) {
            throw new IllegalArgumentException("Bad key: " + pKey);
        }

        return name;
    }

    private Object readResolve() throws IntrospectionException {
        // Initialize the property descriptors
        descriptors = initDescriptors(bean);
        return this;
    }

    private class BeanSet extends AbstractSet<Entry<String, Object>> {
        public Iterator<Entry<String, Object>> iterator() {
            return new BeanIterator(descriptors.iterator());
        }

        public int size() {
            return descriptors.size();
        }
    }

    private class BeanIterator implements Iterator<Entry<String, Object>> {
        private final Iterator<PropertyDescriptor> mIterator;

        public BeanIterator(final Iterator<PropertyDescriptor> pIterator) {
            mIterator = pIterator;
        }

        public boolean hasNext() {
            return mIterator.hasNext();
        }

        public BeanEntry next() {
            return new BeanEntry(mIterator.next());
        }

        public void remove() {
            mIterator.remove();
        }
    }

    private class BeanEntry implements Entry<String, Object> {
        private final PropertyDescriptor mDescriptor;

        public BeanEntry(final PropertyDescriptor pDescriptor) {
            this.mDescriptor = pDescriptor;
        }

        public String getKey() {
            return mDescriptor.getName();
        }

        public Object getValue() {
            return unwrap(new Wrapped() {
                public Object run() throws IllegalAccessException, InvocationTargetException {
                    final Method method = mDescriptor.getReadMethod();
                    // A write-only bean.
                    if (method == null) {
                        throw new UnsupportedOperationException("No getter: " + mDescriptor.getName());
                    }

                    return method.invoke(bean);
                }
            });
        }

        public Object setValue(final Object pValue) {
            return unwrap(new Wrapped() {
                public Object run() throws IllegalAccessException, InvocationTargetException {
                    final Method method = mDescriptor.getWriteMethod();
                    // A read-only bean.
                    if (method == null) {
                        throw new UnsupportedOperationException("No write method for property: " + mDescriptor.getName());
                    }

                    final Object old = getValue();
                    method.invoke(bean, pValue);
                    return old;
                }
            });
        }

        public boolean equals(Object pOther) {
            if (!(pOther instanceof Map.Entry)) {
                return false;
            }

            Map.Entry entry = (Map.Entry) pOther;

            Object k1 = getKey();
            Object k2 = entry.getKey();

            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = entry.getValue();

                if (v1 == v2 || (v1 != null && v1.equals(v2))) {
                    return true;
                }
            }

            return false;
        }

        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode()) ^
                   (getValue() == null ? 0 : getValue().hashCode());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private static interface Wrapped {
        Object run() throws IllegalAccessException, InvocationTargetException;
    }

    private static Object unwrap(final Wrapped wrapped) {
        try {
            return wrapped.run();
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (final InvocationTargetException e) {
            // Javadocs for setValue indicate cast is ok.
            throw (RuntimeException) e.getCause();
        }
    }
}