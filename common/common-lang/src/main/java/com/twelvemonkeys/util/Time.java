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

package com.twelvemonkeys.util;

/**
 * Utility class for storing times in a simple way. The internal time is stored
 * as an int, counting seconds.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @todo Milliseconds!
 */
public class Time {

    private int time = -1;
    public final static int SECONDS_IN_MINUTE = 60;

    /**
     * Creates a new time with 0 seconds, 0 minutes.
     */
    public Time() {
        this(0);
    }

    /**
     * Creates a new time with the given time (in seconds).
     */
    public Time(int pTime) {
        setTime(pTime);
    }

    /**
     * Sets the full time in seconds
     */
    public void setTime(int pTime) {
        if (pTime < 0) {
            throw new IllegalArgumentException("Time argument must be 0 or positive!");
        }
        time = pTime;
    }

    /**
     * Gets the full time in seconds.
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets the full time in milliseconds, for use in creating dates or
     * similar.
     *
     * @see java.util.Date#setTime(long)
     */
    public long getTimeInMillis() {
        return (long) time * 1000L;
    }

    /**
     * Sets the seconds part of the time. Note, if the seconds argument is 60
     * or greater, the value will "wrap", and increase the minutes also.
     *
     * @param pSeconds an integer that should be between 0 and 59.
     */
    public void setSeconds(int pSeconds) {
        time = getMinutes() * SECONDS_IN_MINUTE + pSeconds;
    }

    /**
     * Gets the seconds part of the time.
     *
     * @return an integer between 0 and 59
     */
    public int getSeconds() {
        return time % SECONDS_IN_MINUTE;
    }

    /**
     * Sets the minutes part of the time.
     *
     * @param pMinutes an integer
     */
    public void setMinutes(int pMinutes) {
        time = pMinutes * SECONDS_IN_MINUTE + getSeconds();
    }

    /**
     * Gets the minutes part of the time.
     *
     * @return an integer
     */
    public int getMinutes() {
        return time / SECONDS_IN_MINUTE;
    }

    /**
     * Creates a string representation of the time object.
     * The string is returned on the form m:ss,
     * where m is variable digits minutes and ss is two digits seconds.
     *
     * @return a string representation of the time object
     * @see #toString(String)
     */
    public String toString() {
        return "" + getMinutes() + ":"
                + (getSeconds() < 10 ? "0" : "") + getSeconds();
    }

    /**
     * Creates a string representation of the time object.
     * The string returned is on the format of the formatstring.
     * <DL>
     * <DD>m (or any multiple of m's)
     * <DT>the minutes part (padded with 0's, if number has less digits than
     * the number of m's)
     * m -> 0,1,...,59,60,61,...
     * mm -> 00,01,...,59,60,61,...
     * <DD>s or ss
     * <DT>the seconds part (padded with 0's, if number has less digits than
     * the number of s's)
     * s -> 0,1,...,59
     * ss -> 00,01,...,59
     * <DD>S
     * <DT>all seconds (including the ones above 59)
     * </DL>
     *
     * @param pFormatStr the format where
     * @return a string representation of the time object
     * @throws NumberFormatException
     * @see TimeFormat#format(Time)
     * @see #parseTime(String)
     * @deprecated
     */
    public String toString(String pFormatStr) {
        TimeFormat tf = new TimeFormat(pFormatStr);

        return tf.format(this);
    }

    /**
     * Creates a string representation of the time object.
     * The string is returned on the form m:ss,
     * where m is variable digits minutes and ss is two digits seconds.
     *
     * @return a string representation of the time object
     * @throws NumberFormatException
     * @see TimeFormat#parse(String)
     * @see #toString(String)
     * @deprecated
     */
    public static Time parseTime(String pStr) {
        TimeFormat tf = TimeFormat.getInstance();

        return tf.parse(pStr);
    }
}
