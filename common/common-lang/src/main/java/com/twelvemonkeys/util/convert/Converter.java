/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.util.convert;

import com.twelvemonkeys.util.Time;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

/**
 * The converter (singleton). Converts strings to objects and back. 
 * This is the entry point to the converter framework.
 * <p/>
 * By default, converters for {@link com.twelvemonkeys.util.Time}, {@link Date}
 * and {@link Object}
 * (the {@link DefaultConverter}) are registered by this class' static
 * initializer. You might remove them using the 
 * {@code unregisterConverter} method.
 *
 * @see #registerConverter(Class, PropertyConverter)
 * @see #unregisterConverter(Class)
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/Converter.java#1 $
 */
// TODO: Get rid of singleton stuff
// Can probably be a pure static class, but is that a good idea?
// Maybe have BeanUtil act as a "proxy", and hide this class all together?
// TODO: ServiceRegistry for registering 3rd party converters
// TODO: URI scheme, for implicit typing? Is that a good idea?
// TODO: Array converters?
public abstract class Converter implements PropertyConverter {

    /** Our singleton instance */
    protected static final Converter sInstance = new ConverterImpl(); // Thread safe & EASY

    /** The converters Map */
    protected final Map<Class, PropertyConverter> converters = new Hashtable<Class, PropertyConverter>();

    // Register our predefined converters
    static {
        PropertyConverter defaultConverter = new DefaultConverter();
        registerConverter(Object.class, defaultConverter);
        registerConverter(Boolean.TYPE, defaultConverter);

        PropertyConverter numberConverter = new NumberConverter();
        registerConverter(Number.class, numberConverter);
        registerConverter(Byte.TYPE, numberConverter);
        registerConverter(Double.TYPE, numberConverter);
        registerConverter(Float.TYPE, numberConverter);
        registerConverter(Integer.TYPE, numberConverter);
        registerConverter(Long.TYPE, numberConverter);
        registerConverter(Short.TYPE, numberConverter);

        registerConverter(Date.class, new DateConverter());
        registerConverter(Time.class, new TimeConverter());
    }

    /**
     * Creates a Converter.
     */
    protected Converter() {
    }

    /**
     * Gets the Converter instance.
     *
     * @return the converter instance
     */
    public static Converter getInstance() {
        return sInstance;
    }

    /**
     * Registers a converter for a given type.
     * This converter will also be used for all subclasses, unless a more
     * specific version is registered.
     * </p>
     * By default, converters for {@link com.twelvemonkeys.util.Time}, {@link Date}
     * and {@link Object}
     * (the {@link DefaultConverter}) are registered by this class' static
     * initializer. You might remove them using the 
     * {@code unregisterConverter} method.
     *
     * @param pType the (super) type to register a converter for
     * @param pConverter the converter
     *
     * @see #unregisterConverter(Class)
     */
    public static void registerConverter(final Class<?> pType, final PropertyConverter pConverter) {
        getInstance().converters.put(pType, pConverter);
    }

    /**
     * Un-registers a converter for a given type. That is, making it unavailable
     * for the converter framework, and making it (potentially) available for
     * garbage collection.
     *
     * @param pType the (super) type to remove converter for
     *
     * @see #registerConverter(Class,PropertyConverter)
     */
    @SuppressWarnings("UnusedDeclaration")
    public static void unregisterConverter(final Class<?> pType) {
        getInstance().converters.remove(pType);
    }
    
    /**
     * Converts the string to an object of the given type.
     *
     * @param pString the string to convert
     * @param pType the type to convert to
     *
     * @return the object created from the given string.
     *
     * @throws ConversionException if the string cannot be converted for any 
     *         reason.
     */
    public Object toObject(final String pString, final Class pType) throws ConversionException {
        return toObject(pString, pType, null);
    }

    /**
     * Converts the string to an object of the given type, parsing after the 
     * given format.
     *
     * @param pString the string to convert
     * @param pType the type to convert to
     * @param pFormat the (optional) conversion format
     *
     * @return the object created from the given string.
     *
     * @throws ConversionException if the string cannot be converted for any 
     *         reason.
     */
    public abstract Object toObject(String pString, Class pType, String pFormat)
        throws ConversionException;

    /**
     * Converts the object to a string, using {@code object.toString()}
     *
     * @param pObject the object to convert.
     *
     * @return the string representation of the object, on the correct format.
     *
     * @throws ConversionException if the object cannot be converted to a 
     *         string for any reason.
     */
    public String toString(final Object pObject) throws ConversionException {
        return toString(pObject, null);
    }

    /**
     * Converts the object to a string, using {@code object.toString()}
     *
     * @param pObject the object to convert.
     * @param pFormat the (optional) conversion format
     *
     * @return the string representation of the object, on the correct format.
     *
     * @throws ConversionException if the object cannot be converted to a 
     *         string for any reason.
     */
    public abstract String toString(Object pObject, String pFormat)
        throws ConversionException;
}
