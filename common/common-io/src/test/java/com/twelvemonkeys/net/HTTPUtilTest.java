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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * HTTPUtilTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HTTPUtilTest.java,v 1.0 08.09.13 13:57 haraldk Exp$
 */
public class HTTPUtilTest {
    @Test
    public void testParseHTTPDateRFC1123() {
        long time = HTTPUtil.parseHTTPDate("Sun, 06 Nov 1994 08:49:37 GMT");
        assertEquals(784111777000l, time);

        time = HTTPUtil.parseHTTPDate("Sunday, 06 Nov 1994 08:49:37 GMT");
        assertEquals(784111777000l, time);
    }

    @Test
    public void testParseHTTPDateRFC850() {
        long time = HTTPUtil.parseHTTPDate("Sunday, 06-Nov-1994 08:49:37 GMT");
        assertEquals(784111777000l, time);

        time = HTTPUtil.parseHTTPDate("Sun, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, time);

        // NOTE: This test will fail some time, around 2044,
        // as the 50 year window will slide...
        time = HTTPUtil.parseHTTPDate("Sunday, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, time);

        time = HTTPUtil.parseHTTPDate("Sun, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, time);
    }

    @Test
    public void testParseHTTPDateAsctime() {
        long time = HTTPUtil.parseHTTPDate("Sun Nov  6 08:49:37 1994");
        assertEquals(784111777000l, time);

        time = HTTPUtil.parseHTTPDate("Sun Nov  6 08:49:37 94");
        assertEquals(784111777000l, time);
    }

    @Test
    public void testFormatHTTPDateRFC1123() {
        long time = 784111777000l;
        assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", HTTPUtil.formatHTTPDate(time));
    }
}
