package com.twelvemonkeys.util.convert;

/**
 * DefaultConverterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/DefaultConverterTestCase.java#1 $
 */
public class DefaultConverterTestCase extends PropertyConverterAbstractTestCase {
    protected PropertyConverter makePropertyConverter() {
        return new DefaultConverter();
    }

    protected Conversion[] getTestConversions() {
        //noinspection BooleanConstructorCall
        return new Conversion[] {
                // Booleans
                new Conversion("true", Boolean.TRUE),
                new Conversion("TRUE", Boolean.TRUE, null, "true"),
                new Conversion("false", Boolean.FALSE),
                new Conversion("FALSE", new Boolean(false), null, "false"),

                // Stupid but valid
                new Conversion("fooBar", "fooBar"),
                //new Conversion("fooBar", new StringBuilder("fooBar")), - StringBuilder does not impl equals()...

                // Stupid test class that reveres chars
                new Conversion("fooBar", new FooBar("fooBar")),

                // TODO: More tests
        };
    }

    // TODO: Test boolean -> Boolean conversion

    public static class FooBar {
        private final String mBar;

        public FooBar(String pFoo) {
            if (pFoo == null) {
                throw new IllegalArgumentException("pFoo == null");
            }
            mBar = reverse(pFoo);
        }

        private String reverse(String pFoo) {
            StringBuilder buffer = new StringBuilder(pFoo.length());

            for (int i = pFoo.length() - 1; i >= 0; i--) {
                buffer.append(pFoo.charAt(i));
            }

            return buffer.toString();
        }

        public String toString() {
            return reverse(mBar);
        }

        public boolean equals(Object obj) {
            return obj == this || (obj instanceof FooBar && ((FooBar) obj).mBar.equals(mBar));
        }

        public int hashCode() {
            return 7 * mBar.hashCode();
        }
    }

}
