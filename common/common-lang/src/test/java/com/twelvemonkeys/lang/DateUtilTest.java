/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.lang;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * DateUtilTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DateUtilTest.java,v 1.0 11.04.12 16:21 haraldk Exp$
 */
@RunWith(Parameterized.class)
public class DateUtilTest {

    private final TimeZone timeZone;

    @Parameterized.Parameters
    public static List<Object[]> timeZones() {
        return Arrays.asList(new Object[][] {
                {TimeZone.getTimeZone("UTC")},
                {TimeZone.getTimeZone("CET")},
                {TimeZone.getTimeZone("IST")}, // 30 min off
        });
    }

    public DateUtilTest(final TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    private Calendar getCalendar(long time) {
        return getCalendar(time, TimeZone.getDefault());
    }

    private Calendar getCalendar(long time, final TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(time);

        return calendar;
    }

    @Test
    public void testRoundToSecond() {
        Calendar calendar = getCalendar(DateUtil.roundToSecond(System.currentTimeMillis()));

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testRoundToMinute() {
        Calendar calendar = getCalendar(DateUtil.roundToMinute(System.currentTimeMillis()));

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
    }

    @Test
    public void testRoundToHour() {
        Calendar calendar = getCalendar(DateUtil.roundToHour(System.currentTimeMillis()));

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MINUTE));
    }

    @Test
    public void testRoundToHourTZ() {
        Calendar calendar = getCalendar(DateUtil.roundToHour(System.currentTimeMillis(), timeZone), timeZone);

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MINUTE));
    }

    @Test
    public void testRoundToDay() {
        Calendar calendar = getCalendar(DateUtil.roundToDay(System.currentTimeMillis()));

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testRoundToDayTZ() {
        Calendar calendar = getCalendar(DateUtil.roundToDay(System.currentTimeMillis(), timeZone), timeZone);

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testCurrentTimeSecond() {
        Calendar calendar = getCalendar(DateUtil.currentTimeSecond());

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testCurrentTimeMinute() {
        Calendar calendar = getCalendar(DateUtil.currentTimeMinute());

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
    }

    @Test
    public void testCurrentTimeHour() {
        Calendar calendar = getCalendar(DateUtil.currentTimeHour());

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MINUTE));
    }

    @Test
    public void testCurrentTimeDay() {
        Calendar calendar = getCalendar(DateUtil.currentTimeDay());

        assertEquals(0, calendar.get(Calendar.MILLISECOND));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
    }
}
