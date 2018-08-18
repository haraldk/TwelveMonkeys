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

package com.twelvemonkeys.util.convert;

import com.twelvemonkeys.lang.BeanUtil;
import com.twelvemonkeys.lang.StringUtil;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 * Converts strings to objects and back.
 * <p/>
 * This converter first tries to create an object, using the class' single 
 * string argument constructor ({@code &lt;type&gt;(String)}) if found,
 * otherwise, an attempt to call
 * the class' static {@code valueOf(String)} method. If both fails, a
 * {@link ConversionException} is thrown.
 *
 * @author <A href="haraldk@iconmedialab.no">Harald Kuhr</A>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/DefaultConverter.java#2 $
 *
 */
public final class DefaultConverter implements PropertyConverter {

    /**
     * Creates a {@code DefaultConverter}.
     */
    public DefaultConverter() {
    }

    /**
     * Converts the string to an object of the given type.
     *
     * @param pString the string to convert
     * @param pType the type to convert to
     * @param pFormat ignored.
     *
     * @return the object created from the given string.
     *
     * @throws ConversionException if the type is null, or if the string cannot
     * be converted into the given type, using a string constructor or static
     * {@code valueOf} method.
     */
    public Object toObject(final String pString, final Class pType, final String pFormat) throws ConversionException {
        if (pString == null) {
            return null;
        }
	
        if (pType == null) {
            throw new MissingTypeException();
        }

        if (pType.isArray()) {
            return toArray(pString, pType, pFormat);
        }

        // TODO: Separate CollectionConverter?
        // should however, be simple to wrap array using Arrays.asList
        // But what about generic type?! It's erased...

        // Primitive -> wrapper
        Class type = unBoxType(pType);

        try {
            // Try to create instance from <Constructor>(String)
            Object value = BeanUtil.createInstance(type, pString);
	    
            if (value == null) {
                // createInstance failed for some reason
                // Try to invoke the static method valueOf(String)
                value = BeanUtil.invokeStaticMethod(type, "valueOf", pString);
		
                if (value == null) {
                    // If the value is still null, well, then I cannot help...
                    throw new ConversionException(String.format(
                            "Could not convert String to %1$s: No constructor %1$s(String) or static %1$s.valueOf(String) method found!",
                            type.getName()
                    ));
                }
            }

            return value;
        }
        catch (InvocationTargetException ite) {
            throw new ConversionException(ite.getTargetException());
        }
        catch (ConversionException ce) {
            throw ce;
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    private Object toArray(final String pString, final Class pType, final String pFormat) {
        String[] strings = StringUtil.toStringArray(pString, pFormat != null ? pFormat : StringUtil.DELIMITER_STRING);
        Class type = pType.getComponentType();
        if (type == String.class) {
            return strings;
        }

        Object array = Array.newInstance(type, strings.length);
        try {
            for (int i = 0; i < strings.length; i++) {
                Array.set(array, i, Converter.getInstance().toObject(strings[i], type));
            }
        }
        catch (ConversionException e) {
            if (pFormat != null) {
                throw new ConversionException(String.format("%s for string \"%s\" with format \"%s\"", e.getMessage(), pString, pFormat), e);
            }
            else {
                throw new ConversionException(String.format("%s for string \"%s\"", e.getMessage(), pString), e);
            }
        }

        return array;
    }

    /**
     * Converts the object to a string, using {@code pObject.toString()}.
     *
     * @param pObject the object to convert.
     * @param pFormat ignored.
     *
     * @return the string representation of the object, or {@code null} if {@code pObject == null}
     */
    public String toString(final Object pObject, final String pFormat)
        throws ConversionException {

        try {
            return pObject == null ? null : pObject.getClass().isArray() ? arrayToString(toObjectArray(pObject), pFormat) : pObject.toString();
        }	    
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    private String arrayToString(final Object[] pArray, final String pFormat) {
        return pFormat == null ? StringUtil.toCSVString(pArray) : StringUtil.toCSVString(pArray, pFormat);
    }

    private Object[] toObjectArray(final Object pObject) {
        // TODO: Extract util method for wrapping/unwrapping native arrays?
        Object[] array;
        Class<?> componentType = pObject.getClass().getComponentType();
        if (componentType.isPrimitive()) {
            if (int.class == componentType) {
                array = new Integer[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (short.class == componentType) {
                array = new Short[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (long.class == componentType) {
                array = new Long[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (float.class == componentType) {
                array = new Float[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (double.class == componentType) {
                array = new Double[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (boolean.class == componentType) {
                array = new Boolean[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (byte.class == componentType) {
                array = new Byte[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else if (char.class == componentType) {
                array = new Character[Array.getLength(pObject)];
                for (int i = 0; i < array.length; i++) {
                    Array.set(array, i, Array.get(pObject, i));
                }
            }
            else {
                throw new IllegalArgumentException("Unknown type " + componentType);
            }
        }
        else {
            array = (Object[]) pObject;
        }
        return array;
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
}
