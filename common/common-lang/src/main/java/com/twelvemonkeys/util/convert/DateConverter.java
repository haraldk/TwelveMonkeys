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

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Converts strings to dates and back.
 * <p>
 * <small>This class has a static cache of {@code DateFormats}, to avoid
 * creation and  parsing of date formats every time one is used.</small>
 * </p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/DateConverter.java#2 $
 */
public class DateConverter extends NumberConverter  {

    /** Creates a {@code DateConverter} */
    public DateConverter() {
    }
    
    /**
     * Converts the string to a date, using the given format for parsing.
     *
     * @param pString the string to convert.
     * @param pType the type to convert to. {@code java.util.Date} and
     * subclasses allowed.
     * @param pFormat the format used for parsing. Must be a legal
     * {@code SimpleDateFormat} format, or {@code null} which will use the
     * default format.
     *
     * @return the object created from the given string. May safely be typecast
     * to {@code java.util.Date}
     *
     * @see Date
     * @see java.text.DateFormat
     *
     * @throws ConversionException
     */
    public Object toObject(String pString, Class pType, String pFormat) throws ConversionException {
        if (StringUtil.isEmpty(pString))
            return null;

        try {
            DateFormat format;
	    
            if (pFormat == null) {
                // Use system default format, using default locale
                format = DateFormat.getDateTimeInstance();
            }
            else {
                // Get format from cache
                format = getDateFormat(pFormat);
            }
            
            Date date =  StringUtil.toDate(pString, format);

            // Allow for conversion to Date subclasses (ie. java.sql.*)
            if (pType != Date.class) {
                try {
                    date = (Date) BeanUtil.createInstance(pType, new Long(date.getTime()));
                }
                catch (ClassCastException e) {
                    throw new TypeMismathException(pType);
                }
                catch (InvocationTargetException e) {
                    throw new ConversionException(e);
                }
            }

            return date;
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    /**
     * Converts the object to a string, using the given format
     *
     * @param pObject the object to convert.
     * @param pFormat the format used for conversion. Must be a legal
     * {@code SimpleDateFormat} format, or {@code null} which will use the
     * default format.
     *
     * @return the string representation of the object, on the correct format.
     *
     * @throws ConversionException if the object is not a subclass of
     * {@code java.util.Date}
     *
     * @see Date
     * @see java.text.DateFormat
     */
    public String toString(Object pObject, String pFormat) throws ConversionException {
        if (pObject == null)
            return null;

        if (!(pObject instanceof Date)) {
            throw new TypeMismathException(pObject.getClass());
        }

        try {
            // Convert to string, default way
            if (StringUtil.isEmpty(pFormat)) {
                return DateFormat.getDateTimeInstance().format(pObject);
            }
	    
            // Convert to string, using format
            DateFormat format = getDateFormat(pFormat);

            return format.format(pObject);
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    private DateFormat getDateFormat(String pFormat) {
        return (DateFormat) getFormat(SimpleDateFormat.class, pFormat, Locale.US);
    }
}
