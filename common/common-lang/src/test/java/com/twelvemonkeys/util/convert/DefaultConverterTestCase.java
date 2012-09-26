package com.twelvemonkeys.util.convert;

import java.io.File;
import java.net.URI;

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
                new Conversion("FALSE", false, null, "false"),

                new Conversion("2", 2),

                // Stupid but valid
                new Conversion("fooBar", "fooBar"),
                //new Conversion("fooBar", new StringBuilder("fooBar")), - StringBuilder does not impl equals()...

                // Stupid test class that reveres chars
                new Conversion("fooBar", new FooBar("fooBar")),

                // String array tests
                new Conversion("foo, bar, baz", new String[] {"foo", "bar", "baz"}),
                new Conversion("foo", new String[] {"foo"}),
                new Conversion("foo;bar; baz", new String[] {"foo", "bar", "baz"}, "; ", "foo; bar; baz"),

                // Native array tests
                new Conversion("1, 2, 3", new int[] {1, 2, 3}),
                new Conversion("-1, 42, 0", new long[] {-1, 42, 0}),
                new Conversion("true, true, false", new boolean[] {true, true, false}),
                new Conversion(".3, 4E7, .97", new float[] {.3f, 4e7f, .97f}, ", ", "0.3, 4.0E7, 0.97"),

                // Object array test
                new Conversion("foo, bar", new FooBar[] {new FooBar("foo"), new FooBar("bar")}),
                new Conversion("/temp, /usr/local/bin", new File[] {new File("/temp"), new File("/usr/local/bin")}),
                new Conversion("file:/temp, http://java.net/", new URI[] {URI.create("file:/temp"), URI.create("http://java.net/")}),

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
