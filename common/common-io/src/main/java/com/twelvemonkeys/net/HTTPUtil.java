/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.net;

import com.twelvemonkeys.lang.DateUtil;
import com.twelvemonkeys.lang.StringUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * HTTPUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HTTPUtil.java,v 1.0 08.09.13 13:57 haraldk Exp$
 */
public class HTTPUtil {
    /**
     * RFC 1123 date format, as recommended by RFC 2616 (HTTP/1.1), sec 3.3
     * NOTE: All date formats are private, to ensure synchronized access.
     */
    private static final SimpleDateFormat HTTP_RFC1123_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    static {
        HTTP_RFC1123_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * RFC 850 date format, (almost) as described in RFC 2616 (HTTP/1.1), sec 3.3
     * USE FOR PARSING ONLY (format is not 100% correct, to be more robust).
     */
    private static final SimpleDateFormat HTTP_RFC850_FORMAT = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss z", Locale.US);
    /**
     * ANSI C asctime() date format, (almost) as described in RFC 2616 (HTTP/1.1), sec 3.3.
     * USE FOR PARSING ONLY (format is not 100% correct, to be more robust).
     */
    private static final SimpleDateFormat HTTP_ASCTIME_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yy", Locale.US);

    private static long sNext50YearWindowChange = DateUtil.currentTimeDay();
    static {
        HTTP_RFC850_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
        HTTP_ASCTIME_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));

        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html#sec19.3:
        // - HTTP/1.1 clients and caches SHOULD assume that an RFC-850 date
        //   which appears to be more than 50 years in the future is in fact
        //   in the past (this helps solve the "year 2000" problem).
        update50YearWindowIfNeeded();
    }

    private static void update50YearWindowIfNeeded() {
        // Avoid class synchronization
        long next = sNext50YearWindowChange;

        if (next < System.currentTimeMillis()) {
            // Next check in one day
            next += DateUtil.DAY;
            sNext50YearWindowChange = next;

            Date startDate = new Date(next - (50l * DateUtil.CALENDAR_YEAR));
            //System.out.println("next test: " + new Date(next) + ", 50 year start: " + startDate);
            synchronized (HTTP_RFC850_FORMAT) {
                HTTP_RFC850_FORMAT.set2DigitYearStart(startDate);
            }
            synchronized (HTTP_ASCTIME_FORMAT) {
                HTTP_ASCTIME_FORMAT.set2DigitYearStart(startDate);
            }
        }
    }

    private HTTPUtil() {}

    /**
     * Formats the time to a HTTP date, using the RFC 1123 format, as described
     * in <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3"
     * >RFC 2616 (HTTP/1.1), sec. 3.3</a>.
     *
     * @param pTime the time
     * @return a {@code String} representation of the time
     */
    public static String formatHTTPDate(long pTime) {
        return formatHTTPDate(new Date(pTime));
    }

    /**
     * Formats the time to a HTTP date, using the RFC 1123 format, as described
     * in <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3"
     * >RFC 2616 (HTTP/1.1), sec. 3.3</a>.
     *
     * @param pTime the time
     * @return a {@code String} representation of the time
     */
    public static String formatHTTPDate(Date pTime) {
        synchronized (HTTP_RFC1123_FORMAT) {
            return HTTP_RFC1123_FORMAT.format(pTime);
        }
    }

    /**
     * Parses a HTTP date string into a {@code long} representing milliseconds
     * since January 1, 1970 GMT.
     * <p>
     * Use this method with headers that contain dates, such as
     * {@code If-Modified-Since} or {@code Last-Modified}.
     * <p>
     * The date string may be in either RFC 1123, RFC 850 or ANSI C asctime()
     * format, as described in
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3"
     * >RFC 2616 (HTTP/1.1), sec. 3.3</a>
     *
     * @param pDate the date to parse
     *
     * @return a {@code long} value representing the date, expressed as the
     * number of milliseconds since January 1, 1970 GMT,
     * @throws NumberFormatException if the date parameter is not parseable.
     * @throws IllegalArgumentException if the date paramter is {@code null}
     */
    public static long parseHTTPDate(String pDate) throws NumberFormatException {
        return parseHTTPDateImpl(pDate).getTime();
    }

    /**
     * ParseHTTPDate implementation
     *
     * @param pDate the date string to parse
     *
     * @return a {@code Date}
     * @throws NumberFormatException if the date parameter is not parseable.
     * @throws IllegalArgumentException if the date paramter is {@code null}
     */
    private static Date parseHTTPDateImpl(final String pDate) throws NumberFormatException  {
        if (pDate == null) {
            throw new IllegalArgumentException("date == null");
        }

        if (StringUtil.isEmpty(pDate)) {
            throw new NumberFormatException("Invalid HTTP date: \"" + pDate + "\"");
        }

        DateFormat format;

        if (pDate.indexOf('-') >= 0) {
            format = HTTP_RFC850_FORMAT;
            update50YearWindowIfNeeded();
        }
        else if (pDate.indexOf(',') < 0) {
            format = HTTP_ASCTIME_FORMAT;
            update50YearWindowIfNeeded();
        }
        else {
            format = HTTP_RFC1123_FORMAT;
            // NOTE: RFC1123 always uses 4-digit years
        }

        Date date;
        try {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (format) {
                date = format.parse(pDate);
            }
        }
        catch (ParseException e) {
            NumberFormatException nfe = new NumberFormatException("Invalid HTTP date: \"" + pDate + "\"");
            nfe.initCause(e);
            throw nfe;
        }

        if (date == null) {
            throw new NumberFormatException("Invalid HTTP date: \"" + pDate + "\"");
        }

        return date;
    }
}
