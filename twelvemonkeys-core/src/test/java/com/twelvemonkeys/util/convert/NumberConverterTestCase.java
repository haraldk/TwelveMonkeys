package com.twelvemonkeys.util.convert;

/**
 * NumberConverterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/NumberConverterTestCase.java#2 $
 */
public class NumberConverterTestCase extends PropertyConverterAbstractTestCase {
    protected PropertyConverter makePropertyConverter() {
        return new NumberConverter();
    }

    protected Conversion[] getTestConversions() {
        return new Conversion[] {
                new Conversion("0", 0),
                new Conversion("1", 1),
                new Conversion("-1001", -1001),
                new Conversion("1E3", 1000, null, "1000"),

                new Conversion("-2", -2l),
                new Conversion("2000651651854", 2000651651854l),
                new Conversion("2E10", 20000000000l, null, "20000000000"),

                new Conversion("3", 3.0f),
                new Conversion("3.1", 3.1f),
                new Conversion("3.2", 3.2f, "#.#"),
                //new Conversion("3,3", new Float(3), "#", "3"), // Seems to need parseIntegerOnly
                new Conversion("-3.4", -3.4f),
                new Conversion("-3.5E10", -3.5e10f, null, "-35000000512"),

                new Conversion("4", 4.0),
                new Conversion("4.1", 4.1),
                new Conversion("4.2", 4.2, "#.#"),
                //new Conversion("4,3", new Double(4), "#", "4"), // Seems to need parseIntegerOnly
                new Conversion("-4.4", -4.4),
                new Conversion("-4.5E97", -4.5e97, null, "-45000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
        };
    }
}
