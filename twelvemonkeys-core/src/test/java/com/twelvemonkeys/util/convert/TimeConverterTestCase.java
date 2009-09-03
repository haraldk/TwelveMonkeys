package com.twelvemonkeys.util.convert;

/**
 * TimeConverterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/TimeConverterTestCase.java#1 $
 */
public class TimeConverterTestCase extends PropertyConverterAbstractTestCase {
    protected PropertyConverter makePropertyConverter() {
        return new TimeConverter();
    }

    protected Conversion[] getTestConversions() {
        return new Conversion[0];// TODO: Implement
    }
}
