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

import com.twelvemonkeys.lang.*;

import java.lang.reflect.*;

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
     * {@code valueof} method.
     */
    public Object toObject(String pString, final Class pType, String pFormat)
        throws ConversionException {

        if (pString == null) {
            return null;
        }
	
        if (pType == null) {
            throw new MissingTypeException();
        }

        // Primitive -> wrapper
        Class type;
        if (pType == Boolean.TYPE) {
            type = Boolean.class;
        }
        else {
            type = pType;
        }

        try {
            // Try to create instance from <Constructor>(String)
            Object value = BeanUtil.createInstance(type, pString);
	    
            if (value == null) {
                // createInstance failed for some reason
		
                // Try to invoke the static method valueof(String)
                value = BeanUtil.invokeStaticMethod(type, "valueOf", pString);
		
                if (value == null) {
                    // If the value is still null, well, then I cannot help...
                    throw new ConversionException("Could not convert String to " + pType.getName() + ": No constructor " + type.getName() + "(String) or static " + type.getName() + ".valueof(String) method found!");
                }
            }

            return value;
        }
        catch (InvocationTargetException ite) {
            throw new ConversionException(ite.getTargetException());
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    /**
     * Converts the object to a string, using {@code pObject.toString()}.
     *
     * @param pObject the object to convert.
     * @param pFormat ignored.
     *
     * @return the string representation of the object, or {@code null} if
     *         {@code pObject == null}
     */
    public String toString(Object pObject, String pFormat)
        throws ConversionException {

        try {
            return (pObject != null ? pObject.toString() : null);
        }	    
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }
}
