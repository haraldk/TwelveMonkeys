package com.twelvemonkeys.net;

import junit.framework.TestCase;

/**
 * NetUtilTestCase
 * <p/>
 * <!-- To change this template use Options | File Templates. -->
 * <!-- Created by IntelliJ IDEA. -->
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/net/NetUtilTestCase.java#1 $
 */
public class NetUtilTestCase extends TestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testParseHTTPDateRFC1123() {
        long time = NetUtil.parseHTTPDate("Sun, 06 Nov 1994 08:49:37 GMT");
        assertEquals(784111777000l, time);

        time = NetUtil.parseHTTPDate("Sunday, 06 Nov 1994 08:49:37 GMT");
        assertEquals(784111777000l, time);
    }

    public void testParseHTTPDateRFC850() {
        long time = NetUtil.parseHTTPDate("Sunday, 06-Nov-1994 08:49:37 GMT");
        assertEquals(784111777000l, time);

        time = NetUtil.parseHTTPDate("Sun, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, time);

        // NOTE: This test will fail some time, around 2044,
        // as the 50 year window will slide...
        time = NetUtil.parseHTTPDate("Sunday, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, time);

        time = NetUtil.parseHTTPDate("Sun, 06-Nov-94 08:49:37 GMT");
        assertEquals(784111777000l, time);
    }

    public void testParseHTTPDateAsctime() {
        long time = NetUtil.parseHTTPDate("Sun Nov  6 08:49:37 1994");
        assertEquals(784111777000l, time);

        time = NetUtil.parseHTTPDate("Sun Nov  6 08:49:37 94");
        assertEquals(784111777000l, time);
    }

    public void testFormatHTTPDateRFC1123() {
        long time = 784111777000l;
        assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", NetUtil.formatHTTPDate(time));
    }
}
