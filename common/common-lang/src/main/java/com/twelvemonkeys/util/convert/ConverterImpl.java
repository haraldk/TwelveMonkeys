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

/**
 * The converter (singleton). Converts strings to objects and back. 
 * This is the entrypoint to the converter framework.
 *
 * @see #registerConverter(Class, PropertyConverter)
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/ConverterImpl.java#1 $
 */
class ConverterImpl extends Converter {

    /**
     * Creates a Converter.
     */
    ConverterImpl() {
    }

    /**
     * Gets the registered converter for the given type.
     *
     * @param pType the type to convert to
     * @return an instance of a {@code PropertyConverter} or {@code null}
     */
    private PropertyConverter getConverterForType(Class pType) {
        Object converter;
        Class cl = pType;

        // Loop until we find a suitable converter
        do {
            // Have a match, return converter
            if ((converter = getInstance().converters.get(cl)) != null) {
                return (PropertyConverter) converter;
            }

        }
        while ((cl = cl.getSuperclass()) != null);

        // No converter found, return null
        return null;
    }

    /**
     * Converts the string to an object of the given type, parsing after the 
     * given format.
     *
     * @param pString the string to convert
     * @param pType the type to convert to
     * @param pFormat the vonversion format
     *
     * @return the object created from the given string.
     *
     * @throws ConversionException if the string cannot be converted for any 
     *         reason.
     */
    public Object toObject(String pString, Class pType, String pFormat)
        throws ConversionException {

        if (pString == null) {
            return null;
        }
	
        if (pType == null) {
            throw new MissingTypeException();
        }
	
        // Get converter
        PropertyConverter converter = getConverterForType(pType);

        if (converter == null) {
            throw new NoAvailableConverterException("Cannot convert to object, no converter available for type \"" + pType.getName() + "\"");
        }

        // Convert and return 
        return converter.toObject(pString, pType, pFormat);
    }

    /**
     * Converts the object to a string, using {@code object.toString()}
     *
     * @param pBean the object to convert
     * @param pFormat the conversion format
     *
     * @return the string representation of the object, on the correct format.
     *
     * @throws ConversionException if the object cannot be converted to a 
     *         string for any reason.
     */
    public String toString(Object pBean, String pFormat)
        throws ConversionException {
        if (pBean == null) {
            return null;
        }

        // Get converter
        PropertyConverter converter = getConverterForType(pBean.getClass());

        if (converter == null) {
            throw new NoAvailableConverterException("Cannot object to string, no converter available for type \"" + pBean.getClass().getName() + "\"");
        }

        // Convert and return string
        return converter.toString(pBean, pFormat);
    }
}
