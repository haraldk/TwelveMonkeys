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

import com.twelvemonkeys.lang.DateUtil;
import org.junit.Test;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * DateConverterTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/DateConverterTestCase.java#2 $
 */
public class DateConverterTest extends PropertyConverterAbstractTest {
    protected final static String FORMAT_STR_1 = "dd.MM.yyyy HH:mm:ss";
    protected final static String FORMAT_STR_2 = "dd-MM-yyyy hh:mm:ss a";

    protected PropertyConverter makePropertyConverter() {
        return new DateConverter();
    }

    protected Conversion[] getTestConversions() {
        // The default format doesn't contain milliseconds, so we have to round
        long time = System.currentTimeMillis();
        final Date now = new Date(DateUtil.roundToSecond(time));
        DateFormat df = DateFormat.getDateTimeInstance();

        return new Conversion[] {
                new Conversion("01.11.2006 15:26:23", new GregorianCalendar(2006, 10, 1, 15, 26, 23).getTime(), FORMAT_STR_1),

                // This doesn't really work.. But close enough
                new Conversion(df.format(now), now),

                // This format is really stupid
                new Conversion("01-11-2006 03:27:44 pm", new GregorianCalendar(2006, 10, 1, 15, 27, 44).getTime(), FORMAT_STR_2, "01-11-2006 03:27:44 PM"),

                // These seems to be an hour off (no timezone?)...
                new Conversion("42", new Date(42l), "S"),
                new Conversion(String.valueOf(time % 1000l), new Date(time % 1000l), "S"),
        };
    }

    @Test
    @Override
    public void testConvert() {
        // Custom setup, to make test cases stable: Always use GMT
        TimeZone oldTZ = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            super.testConvert();
        }
        finally {
            // Restore
            TimeZone.setDefault(oldTZ);
        }
    }
}