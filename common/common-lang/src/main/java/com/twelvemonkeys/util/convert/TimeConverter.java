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

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.Time;
import com.twelvemonkeys.util.TimeFormat;

/**
 * Converts strings to times and back.
 * <p/>
 * <small>This class has a static cache of {@code TimeFormats}, to avoid creation and
 * parsing of timeformats every time one is used.</small>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/TimeConverter.java#1 $
 */
public class TimeConverter extends NumberConverter  {

    public TimeConverter() {
    }
    
    /**
     * Converts the string to a time, using the given format for parsing.
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
     * to {@code com.twelvemonkeys.util.Time}
     *
     * @see com.twelvemonkeys.util.Time
     * @see com.twelvemonkeys.util.TimeFormat
     *
     * @throws ConversionException
     */
    public Object toObject(String pString, Class pType, String pFormat) 
        throws ConversionException {
        if (StringUtil.isEmpty(pString))
            return null;

        TimeFormat format;

        try {
            if (pFormat == null) {
                // Use system default format
                format = TimeFormat.getInstance();
            }
            else {
                // Get format from cache
                format = getTimeFormat(pFormat);
            }

            return format.parse(pString);
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
     * @throws ConversionException if the object is not a subclass of
     * {@code com.twelvemonkeys.util.Time}
     *
     * @see com.twelvemonkeys.util.Time
     * @see com.twelvemonkeys.util.TimeFormat
     */
    public String toString(Object pObject, String pFormat) 
        throws ConversionException {
        if (pObject == null)
            return null;

        if (!(pObject instanceof com.twelvemonkeys.util.Time))
            throw new TypeMismathException(pObject.getClass());

        try {
            // Convert to string, default way
            if (StringUtil.isEmpty(pFormat))
                return pObject.toString();
	
            // Convert to string, using format
            TimeFormat format = getTimeFormat(pFormat);
            return format.format((Time) pObject);
        }
        catch (RuntimeException rte) {
            throw new ConversionException(rte);
        }
    }

    private TimeFormat getTimeFormat(String pFormat) {
        return (TimeFormat) getFormat(TimeFormat.class, pFormat);
    }
}
