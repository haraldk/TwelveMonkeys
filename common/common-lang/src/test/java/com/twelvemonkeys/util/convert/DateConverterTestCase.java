package com.twelvemonkeys.util.convert;

import com.twelvemonkeys.lang.DateUtil;
import org.junit.Test;

import java.text.DateFormat;
import java.util.*;

/**
 * DateConverterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/DateConverterTestCase.java#2 $
 */
public class DateConverterTestCase extends PropertyConverterAbstractTestCase {
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