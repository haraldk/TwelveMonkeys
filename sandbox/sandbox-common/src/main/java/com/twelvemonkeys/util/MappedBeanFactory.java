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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MappedBeanFactory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sandbox/MappedBeanFactory.java#1 $
 */
public final class MappedBeanFactory {

    // TODO: Map<String, ?> getMap(Object pProxy)

    // TODO: Consider a @NotNull annotation that will allow for throwing IllegalArgumentExceptions
    //       - Or a more general validator approach for custom fields...

    // NOTE: Specifying default values does not make much sense, as it would be possible to just add values to the map
    //  in the first place

    // TODO: Replace Converter varargs with new class a PropertyConverterConfiguration
    //       - setPropertyConverter(String propertyName, Converter from, Converter to)
    //       - setDefaultConverter(Class from, Class to, Converter)
    // TODO: Validators? Allows for more than just NotNull checking
    // TODO: Mixin support for other methods, and we are on the way to full-blown AOP.. ;-)
    // TODO: Delegate for behaviour?

    // TODO: Consider being fail-fast for primitives without default values?
    // Or have default values be the same as they would have been if class members (false/0/null)
    // NOTE: There's a difference between a map with a null value for a key, and no presence of that key at all

    // TODO: ProperyChange support!

    private MappedBeanFactory() {
    }

    static <T> T as(final Class<T> pClass, final Converter... pConverters) {
        // TODO: Add neccessary default initializer stuff here.
        return as(pClass, new LinkedHashMap<String, Object>(), pConverters);
    }

    @SuppressWarnings({"unchecked"})
    static <T> T as(final Class<T> pClass, final Map<String, ?> pMap, final Converter... pConverters) {
        return asImpl(pClass, (Map<String, Object>) pMap, pConverters);
    }

    static <T> T asImpl(final Class<T> pClass, final Map<String, Object> pMap, final Converter[] pConverters) {
        // TODO: Report clashing? Named converters?
        final Map<ConverterKey, Converter> converters = new HashMap<ConverterKey, Converter>() {
            @Override
            public Converter get(Object key) {
                Converter converter = super.get(key);
                return converter != null ? converter : Converter.NULL;
            }
        };

        for (Converter converter : pConverters) {
            converters.put(new ConverterKey(converter.getFromType(), converter.getToType()), converter);
        }

        return pClass.cast(
                Proxy.newProxyInstance(
                        pClass.getClassLoader(),
                        new Class<?>[]{pClass, Serializable.class}, // TODO: Maybe Serializable should be specified by pClass?
                        new MappedBeanInvocationHandler(pClass, pMap, converters)
                )
        );
    }

    private static class ConverterKey {
        private Class<?> to;
        private Class<?> from;

        ConverterKey(Class<?> pFrom, Class<?> pTo) {
            to = pTo;
            from = pFrom;
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            }
            if (pOther == null || getClass() != pOther.getClass()) {
                return false;
            }

            ConverterKey that = (ConverterKey) pOther;

            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            int result = to != null ? to.hashCode() : 0;
            result = 31 * result + (from != null ? from.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s->%s", from, to);
        }
    }

    public static interface Converter<F, T> {

        Converter NULL = new Converter() {
            public Class<?> getFromType() {
                return null;
            }

            public Class<?> getToType() {
                return null;
            }

            public Object convert(Object value, Object old) {
                if (value == null) {
                    return value;
                }
                throw new ClassCastException(value.getClass().getName());
            }
        };

        Class<F> getFromType();

        Class<T> getToType();

        T convert(F value, T old);
    }

    // Add guards for null values by throwing IllegalArgumentExceptions for parameters
    // TODO: Throw IllegalArgumentException at CREATION time, if value in map is null for a method with @NotNull return type
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    static @interface NotNull {
    }

    // For setter methods to have automatic property change support
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    // TODO: Consider field as well?
    static @interface Observable {
    }

    // TODO: Decide on default value annotation
    // Alternate default value annotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultValue {
        boolean booleanValue() default false;
        byte byteValue() default 0;
        char charValue() default 0;
        short shortValue() default 0;
        int intValue() default 0;
        float floatValue() default 0f;
        long longValue() default 0l;
        double doubleValue() default 0d;
    }


    // Default values for primitive types
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultBooleanValue {
        boolean value() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultByteValue {
        byte value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultCharValue {
        char value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultShortValue {
        short value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultIntValue {
        int value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultFloatValue {
        float value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultLongValue {
        long value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    static @interface DefaultDouleValue {
        double value() default 0;
    }

    // TODO: Does it make sense to NOT just put the value in the map? 
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private static @interface DefaultStringValue {
        String value(); // TODO: Do we want a default empty string?
    }

    private static class MappedBeanInvocationHandler implements InvocationHandler, Serializable {

        private static final Method OBJECT_TO_STRING = getObjectMethod("toString");
        private static final Method OBJECT_HASH_CODE = getObjectMethod("hashCode");
        private static final Method OBJECT_EQUALS = getObjectMethod("equals", Object.class);
        private static final Method OBJECT_CLONE = getObjectMethod("clone");

        private final Class<?> mClass;
        private final Map<String, Object> mMap;
        private final Map<ConverterKey, Converter> mConverters;

        private transient Map<Method, String> mReadMethods = new HashMap<Method, String>();
        private transient Map<Method, String> mWriteMethods = new HashMap<Method, String>();

        private static Method getObjectMethod(final String pMethodName, final Class<?>... pParams) {
            try {
                return Object.class.getDeclaredMethod(pMethodName, pParams);
            }
            catch (NoSuchMethodException e) {
                throw new Error(e.getMessage(), e);
            }
        }

        private Object readResolve() throws ObjectStreamException {
            mReadMethods = new HashMap<Method, String>();
            mWriteMethods = new HashMap<Method, String>();

            introspectBean(mClass, mReadMethods, mWriteMethods);

            return this;
        }

        public MappedBeanInvocationHandler(Class<?> pClass, Map<String, Object> pMap, Map<ConverterKey, Converter> pConverters) {
            mClass = pClass;
            mMap = pMap;
            mConverters = pConverters;

            introspectBean(mClass, mReadMethods, mWriteMethods);
        }

        private void introspectBean(Class<?> pClass, Map<Method, String> pReadMethods, Map<Method, String> pWriteMethods) {
            try {
                BeanInfo info = Introspector.getBeanInfo(pClass);
                PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
                for (PropertyDescriptor descriptor : descriptors) {
                    String name = descriptor.getName();

                    Method read = descriptor.getReadMethod();
                    if (read != null) {
                        pReadMethods.put(read, name);
                    }

                    Method write = descriptor.getWriteMethod();
                    if (write != null) {
                        pWriteMethods.put(write, name);
                    }
                }
            }
            catch (IntrospectionException e) {
                throw new IllegalArgumentException(String.format("Class %s not introspectable: %s", pClass, e.getMessage()) , e);
            }
        }

        public Object invoke(final Object pProxy, final Method pMethod, final Object[] pArguments) throws Throwable {
            String property = mReadMethods.get(pMethod);
            if (property != null) {
                if (pArguments == null || pArguments.length == 0) {
                    Object value = mMap.get(property);
                    Class<?> type = pMethod.getReturnType();

                    if (!isCompatible(value, type)) {
                        return mConverters.get(new ConverterKey(value != null ? value.getClass() : Void.class, unBoxType(type))).convert(value, null);
                    }
                    return value;
                }
                else {
                    throw new IllegalArgumentException("Unknown parameters for " + pMethod + ": " + Arrays.toString(pArguments));
                }
            }

            property = mWriteMethods.get(pMethod);
            if (property != null) {
                if (pArguments.length == 1) {
                    Object value = pArguments[0];

                    // Make sure we don't accidentally overwrite a value that looks like ours...
                    Object oldValue = mMap.get(property);
                    Class<?> type = pMethod.getParameterTypes()[0];
                    if (oldValue != null && !isCompatible(oldValue, type)) {
                        value = mConverters.get(new ConverterKey(type, oldValue.getClass())).convert(value, oldValue);
                    }
                    return mMap.put(property, value);
                }
                else {
                    throw new IllegalArgumentException("Unknown parameters for " + pMethod + ": " + Arrays.toString(pArguments));
                }
            }

            if (pMethod.equals(OBJECT_TO_STRING)) {
                return proxyToString();
            }
            if (pMethod.equals(OBJECT_EQUALS)) {
                return proxyEquals(pProxy, pArguments[0]);
            }
            if (pMethod.equals(OBJECT_HASH_CODE)) {
                return proxyHashCode();
            }
            if (pMethod.getName().equals(OBJECT_CLONE.getName())
                    && Arrays.equals(pMethod.getParameterTypes(), OBJECT_CLONE.getParameterTypes())
                    && OBJECT_CLONE.getReturnType().isAssignableFrom(pMethod.getReturnType())) {
                return proxyClone();
            }

            // Other methods not handled (for now)
            throw new AbstractMethodError(pMethod.getName());
        }

        private boolean isCompatible(final Object pValue, final Class<?> pType) {
            return pValue == null && !pType.isPrimitive() || unBoxType(pType).isInstance(pValue);
        }

        private Class<?> unBoxType(final Class<?> pType) {
            if (pType.isPrimitive()) {
                if (pType == boolean.class) {
                    return Boolean.class;
                }
                if (pType == byte.class) {
                    return Byte.class;
                }
                if (pType == char.class) {
                    return Character.class;
                }
                if (pType == short.class) {
                    return Short.class;
                }
                if (pType == int.class) {
                    return Integer.class;
                }
                if (pType == float.class) {
                    return Float.class;
                }
                if (pType == long.class) {
                    return Long.class;
                }
                if (pType == double.class) {
                    return Double.class;
                }

                throw new IllegalArgumentException("Unknown type: " + pType);
            }
            return pType;
        }

        private int proxyHashCode() {
            // NOTE: Implies mMap instance must follow Map.equals contract
            return mMap.hashCode();
        }

        private boolean proxyEquals(final Object pThisProxy, final Object pThat) {
            if (pThisProxy == pThat) {
                return true;
            }
            if (pThat == null) {
                return false;
            }

            // TODO: Document that subclasses are considered equal (if no extra properties)
            if (!mClass.isInstance(pThat)) {
                return false;
            }
            if (!Proxy.isProxyClass(pThat.getClass())) {
                return false;
            }

            // NOTE: This implies that we should put default values in map at creation time
            // NOTE: Implies mMap instance must follow Map.equals contract
            InvocationHandler handler = Proxy.getInvocationHandler(pThat);
            return handler.getClass() == getClass() && mMap.equals(((MappedBeanInvocationHandler) handler).mMap);

        }

        private Object proxyClone() throws CloneNotSupportedException {
            return as(
                    mClass,
                    new LinkedHashMap<String, Object>(mMap),
                    mConverters.values().toArray(new Converter[mConverters.values().size()])
            );
        }

        private String proxyToString() {
            return String.format("%s$MapProxy@%s: %s", mClass.getName(), System.identityHashCode(this), mMap);
        }
    }
}
