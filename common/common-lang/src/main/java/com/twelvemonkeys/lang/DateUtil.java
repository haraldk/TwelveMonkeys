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

package com.twelvemonkeys.lang;

import java.util.Date;
import java.util.TimeZone;

/**
 * A utility class with useful date manipulation methods and constants.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/DateUtil.java#1 $
 */
public final class DateUtil {

    /** One second: 1000 milliseconds. */
    public static final long SECOND = 1000l;

    /** One minute: 60 seconds (60 000 milliseconds). */
    public static final long MINUTE = 60l * SECOND;

    /**
     * One hour: 60 minutes (3 600 000 milliseconds).
     * 60 minutes = 3 600 seconds = 3 600 000 milliseconds
     */
    public static final long HOUR = 60l * MINUTE;

    /**
     * One day: 24 hours (86 400 000 milliseconds).
     * 24 hours = 1 440 minutes = 86 400 seconds = 86 400 000 milliseconds.
     */
    public static final long DAY = 24l * HOUR;

    /**
     * One calendar year: 365.2425 days (31556952000 milliseconds).
     * 365.2425 days = 8765.82 hours = 525949.2 minutes = 31556952 seconds
     * = 31556952000 milliseconds.
     */
    public static final long CALENDAR_YEAR = 3652425l * 24l * 60l * 6l;

    private DateUtil() {
    }

    /**
     * Returns the time between the given start time and now (as defined by
     * {@link System#currentTimeMillis()}).
     *
     * @param pStart the start time
     *
     * @return the time between the given start time and now.
     */
    public static long delta(long pStart) {
        return System.currentTimeMillis() - pStart;
    }

    /**
     * Returns the time between the given start time and now (as defined by
     * {@link System#currentTimeMillis()}).
     *
     * @param pStart the start time
     *
     * @return the time between the given start time and now.
     */
    public static long delta(Date pStart) {
        return System.currentTimeMillis() - pStart.getTime();
    }

    /**
     * Gets the current time, rounded down to the closest second.
     * Equivalent to invoking
     * {@code roundToSecond(System.currentTimeMillis())}.
     *
     * @return the current time, rounded to the closest second.
     */
    public static long currentTimeSecond() {
        return roundToSecond(System.currentTimeMillis());
    }

    /**
     * Gets the current time, rounded down to the closest minute.
     * Equivalent to invoking
     * {@code roundToMinute(System.currentTimeMillis())}.
     *
     * @return the current time, rounded to the closest minute.
     */
    public static long currentTimeMinute() {
        return roundToMinute(System.currentTimeMillis());
    }

    /**
     * Gets the current time, rounded down to the closest hour.
     * Equivalent to invoking
     * {@code roundToHour(System.currentTimeMillis())}.
     *
     * @return the current time, rounded to the closest hour.
     */
    public static long currentTimeHour() {
        return roundToHour(System.currentTimeMillis());
    }

    /**
     * Gets the current time, rounded down to the closest day.
     * Equivalent to invoking
     * {@code roundToDay(System.currentTimeMillis())}.
     *
     * @return the current time, rounded to the closest day.
     */
    public static long currentTimeDay() {
        return roundToDay(System.currentTimeMillis());
    }

    /**
     * Rounds the given time down to the closest second.
     *
     * @param pTime time
     * @return the time rounded to the closest second.
     */
    public static long roundToSecond(final long pTime) {
        return (pTime / SECOND) * SECOND;
    }

    /**
     * Rounds the given time down to the closest minute.
     *
     * @param pTime time
     * @return the time rounded to the closest minute.
     */
    public static long roundToMinute(final long pTime) {
        return (pTime / MINUTE) * MINUTE;
    }

    /**
     * Rounds the given time down to the closest hour, using the default timezone.
     *
     * @param pTime time
     * @return the time rounded to the closest hour.
     */
    public static long roundToHour(final long pTime) {
        return roundToHour(pTime, TimeZone.getDefault());
    }

    /**
     * Rounds the given time down to the closest hour, using the given timezone.
     *
     * @param pTime time
     * @param pTimeZone the timezone to use when rounding
     * @return the time rounded to the closest hour.
     */
    public static long roundToHour(final long pTime, final TimeZone pTimeZone) {
        int offset = pTimeZone.getOffset(pTime);
        return ((pTime / HOUR) * HOUR) - offset;
    }

    /**
     * Rounds the given time down to the closest day, using the default timezone.
     *
     * @param pTime time
     * @return the time rounded to the closest day.
     */
    public static long roundToDay(final long pTime) {
        return roundToDay(pTime, TimeZone.getDefault());
    }

    /**
     * Rounds the given time down to the closest day, using the given timezone.
     *
     * @param pTime time
     * @param pTimeZone the timezone to use when rounding
     * @return the time rounded to the closest day.
     */
    public static long roundToDay(final long pTime, final TimeZone pTimeZone) {
        int offset = pTimeZone.getOffset(pTime);
        return (((pTime  + offset) / DAY) * DAY) - offset;
    }
}
