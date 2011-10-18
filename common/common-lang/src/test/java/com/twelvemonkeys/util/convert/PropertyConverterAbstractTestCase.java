package com.twelvemonkeys.util.convert;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * PropertyConverterAbstractTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/PropertyConverterAbstractTestCase.java#2 $
 */
public abstract class PropertyConverterAbstractTestCase extends ObjectAbstractTestCase {
    protected Object makeObject() {
        return makePropertyConverter();
    }

    protected abstract PropertyConverter makePropertyConverter();

    protected abstract Conversion[] getTestConversions();

    @Test
    public void testConvert() {
        PropertyConverter converter = makePropertyConverter();

        Conversion[] tests = getTestConversions();

        for (Conversion test : tests) {
            Object obj;
            try {
                obj = converter.toObject(test.original(), test.type(), test.format());

                assertEquals("'" + test.original() + "' converted to incorrect type", test.type(), obj.getClass());
                if (test.type().isArray()) {
                    assertTrue("'" + test.original() + "' not converted", arrayEquals(test.value(), obj));
                }
                else {
                    assertEquals("'" + test.original() + "' not converted", test.value(), obj);
                }

                String result = converter.toString(test.value(), test.format());

                assertEquals("'" + test.converted() + "' does not match", test.converted(), result);

                obj = converter.toObject(result, test.type(), test.format());
                assertEquals("'" + test.original() + "' converted to incorrect type", test.type(), obj.getClass());

                if (test.type().isArray()) {
                    assertTrue("'" + test.original() + "' did not survive round trip conversion", arrayEquals(test.value(), obj));
                }
                else {
                    assertEquals("'" + test.original() + "' did not survive round trip conversion", test.value(), obj);
                }
            }
            catch (ConversionException e) {
                e.printStackTrace();
                fail("Converting '" + test.original() + "' to " + test.type() + " failed: " + e.getMessage());
            }
        }
    }

    // TODO: Util method?
    private boolean arrayEquals(final Object left, final Object right) {
        if (left.getClass().getComponentType().isPrimitive()) {
            if (int.class == left.getClass().getComponentType()) {
                return Arrays.equals((int[]) left, (int[]) right);
            }
            if (short.class == left.getClass().getComponentType()) {
                return Arrays.equals((short[]) left, (short[]) right);
            }
            if (long.class == left.getClass().getComponentType()) {
                return Arrays.equals((long[]) left, (long[]) right);
            }
            if (float.class == left.getClass().getComponentType()) {
                return Arrays.equals((float[]) left, (float[]) right);
            }
            if (double.class == left.getClass().getComponentType()) {
                return Arrays.equals((double[]) left, (double[]) right);
            }
            if (boolean.class == left.getClass().getComponentType()) {
                return Arrays.equals((boolean[]) left, (boolean[]) right);
            }
            if (byte.class == left.getClass().getComponentType()) {
                return Arrays.equals((byte[]) left, (byte[]) right);
            }
            if (char.class == left.getClass().getComponentType()) {
                return Arrays.equals((char[]) left, (char[]) right);
            }
            // Else blow up below...
        }

        return Arrays.equals((Object[]) left, (Object[]) right);
    }

    public static final class Conversion {
        private final String mStrVal;
        private final Object mObjVal;
        private final String mFormat;
        private final String mConvertedStrVal;

        public Conversion(String pStrVal, Object pObjVal) {
            this(pStrVal, pObjVal, null, null);
        }

        public Conversion(String pStrVal, Object pObjVal, String pFormat) {
            this(pStrVal, pObjVal, pFormat, null);
        }

        public Conversion(String pStrVal, Object pObjVal, String pFormat, String pConvertedStrVal) {
            if (pStrVal == null) {
                throw new IllegalArgumentException("pStrVal == null");
            }
            if (pObjVal == null) {
                throw new IllegalArgumentException("pObjVal == null");
            }

            mStrVal = pStrVal;
            mObjVal = pObjVal;
            mFormat = pFormat;
            mConvertedStrVal = pConvertedStrVal != null ? pConvertedStrVal : pStrVal;
        }

        public String original() {
            return mStrVal;
        }

        public Object value() {
            return mObjVal;
        }

        public Class type() {
            return mObjVal.getClass();
        }

        public String format() {
            return mFormat;
        }

        public String converted() {
            return mConvertedStrVal;
        }
    }
}
