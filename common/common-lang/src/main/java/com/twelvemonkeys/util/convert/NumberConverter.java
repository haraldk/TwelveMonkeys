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
import com.twelvemonkeys.util.LRUHashMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Converts strings to numbers and back.
 * <p>
 * <small>This class has a static cache of {@code NumberFormats}, to avoid
 * creation and  parsing of number formats every time one is used.</small>
 * </p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/NumberConverter.java#2 $
 */
public class NumberConverter implements PropertyConverter {
    // TODO: Need to either make this non-locale aware, or document that it is...

    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);
    private static final NumberFormat sDefaultFormat = new DecimalFormat("#0.#", SYMBOLS);
    private static final Map<String, Format> sFormats = new LRUHashMap<String, Format>(50);

    public NumberConverter() {
    }
    
    /**
     * Converts the string to a number, using the given format for parsing.
     *
     * @param pString the string to convert.
     * @param pType the type to convert to. PropertyConverter
     * implementations may choose to ignore this parameter.
     * @param pFormat the format used for parsing. PropertyConverter
     * implementations may choose to ignore this parameter. Also, 
     * implementations that require a parser format, should provide a default
     * format, and allow {@code null} as the format argument.
     *
     * @return the object created from the given string. May safely be typecast
     * to {@code java.lang.Number} or the class of the {@code type} parameter.
     *
     * @see Number
     * @see java.text.NumberFormat
     *
     * @throws ConversionException
     */
    public Object toObject(final String pString, final Class pType, final String pFormat) throws ConversionException {
        if (StringUtil.isEmpty(pString)) {
            return null;
        }

        try {
            if (pType.equals(BigInteger.class)) {
                return new BigInteger(pString); // No format?
            }
            if (pType.equals(BigDecimal.class)) {
                return new BigDecimal(pString); // No format?
            }

            NumberFormat format;
	    
            if (pFormat == null) {
                // Use system default format, using default locale
                format = sDefaultFormat;
            }
            else {
                // Get format from cache
                format = getNumberFormat(pFormat);
            }

            Number num;
            synchronized (format) {
                num = format.parse(pString);
            }

            if (pType == Integer.TYPE || pType == Integer.class) {
                return num.intValue();
            }
            else if (pType == Long.TYPE || pType == Long.class) {
                return num.longValue();
            }
            else if (pType == Double.TYPE || pType == Double.class) {
                return num.doubleValue();
            }
            else if (pType == Float.TYPE || pType == Float.class) {
                return num.floatValue();
            }
            else if (pType == Byte.TYPE || pType == Byte.class) {
                return num.byteValue();
            }
            else if (pType == Short.TYPE || pType == Short.class) {
                return num.shortValue();
            }

            return num;
        }
        catch (ParseException pe) {
            throw new ConversionException(pe);
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    /**
     * Converts the object to a string, using the given format
     *
     * @param pObject the object to convert.
     * @param pFormat the format used for parsing. PropertyConverter
     * implementations may choose to ignore this parameter. Also, 
     * implementations that require a parser format, should provide a default
     * format, and allow {@code null} as the format argument.
     *
     * @return the string representation of the object, on the correct format.
     *
     * @throws ConversionException if the object is not a subclass of {@link java.lang.Number}
     */
    public String toString(final Object pObject, final String pFormat)
        throws ConversionException {

        if (pObject == null) {
            return null;
        }

        if (!(pObject instanceof Number)) {
            throw new TypeMismathException(pObject.getClass());
        }

        try {
            // Convert to string, default way
            if (StringUtil.isEmpty(pFormat)) {
                return sDefaultFormat.format(pObject);
            }
	    
            // Convert to string, using format
            NumberFormat format = getNumberFormat(pFormat);

            synchronized (format) {
                return format.format(pObject);
            }
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }   

    private NumberFormat getNumberFormat(String pFormat) {
        return (NumberFormat) getFormat(DecimalFormat.class,  pFormat, SYMBOLS);
    }

    protected final Format getFormat(Class pFormatterClass, Object... pFormat) {
        // Try to get format from cache
        synchronized (sFormats) {
            String key = pFormatterClass.getName() + ":" + Arrays.toString(pFormat);
            Format format = sFormats.get(key);

            if (format == null) {
                // If not found, create...
                try {
                    format = (Format) BeanUtil.createInstance(pFormatterClass, pFormat);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                // ...and store in cache
                sFormats.put(key, format);
            }

            return format;
        }
    }
}
