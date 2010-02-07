package com.twelvemonkeys.util.convert;

import com.twelvemonkeys.lang.ObjectAbstractTestCase;

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

    public void testConvert() {

        PropertyConverter converter = makePropertyConverter();

        Conversion[] tests = getTestConversions();

        for (Conversion test : tests) {
            Object obj;
            try {
                obj = converter.toObject(test.original(), test.type(), test.format());
                assertEquals("'" + test.original() + "' convtered to incorrect type", test.type(), obj.getClass());
                assertEquals("'" + test.original() + "' not converted", test.value(), obj);

                String result = converter.toString(test.value(), test.format());

                assertEquals("'" + test.converted() + "' does not macth", test.converted(), result);

                obj = converter.toObject(result, test.type(), test.format());
                assertEquals("'" + test.original() + "' convtered to incorrect type", test.type(), obj.getClass());
                assertEquals("'" + test.original() + "' did not survive roundrip conversion", test.value(), obj);
            }
            catch (ConversionException e) {
                e.printStackTrace();
                fail("Converting '" + test.original() + "' to " + test.type() + " failed: " + e.getMessage());
            }
        }
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
