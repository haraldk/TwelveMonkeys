/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.util.convert;

import com.twelvemonkeys.lang.ObjectAbstractTest;
import com.twelvemonkeys.lang.Validate;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PropertyConverterAbstractTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/PropertyConverterAbstractTestCase.java#2 $
 */
public abstract class PropertyConverterAbstractTest extends ObjectAbstractTest {
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

                assertEquals(test.type(), obj.getClass(), String.format("'%s' converted to incorrect type", test.original()));
                if (test.type().isArray()) {
                    assertArrayEquals0(String.format("'%s' not converted", test.original()), test.value(), obj);
                }
                else {
                    assertEquals(test.value(), obj, String.format("'%s' not converted", test.original()));
                }

                String result = converter.toString(test.value(), test.format());

                assertEquals(test.converted(), result, String.format("'%s' does not match", test.converted()));

                obj = converter.toObject(result, test.type(), test.format());
                assertEquals(test.type(), obj.getClass(), String.format("'%s' converted to incorrect type", test.original()));

                if (test.type().isArray()) {
                    assertArrayEquals0(String.format("'%s' did not survive round trip conversion", test.original()), test.value(), obj);
                }
                else {
                    assertEquals(test.value(), obj, String.format("'%s' did not survive round trip conversion", test.original()));
                }
            }
            catch (ConversionException e) {
                failBecause(String.format("Converting '%s' to %s failed", test.original(), test.type()), e);
            }
        }
    }

    private static void assertArrayEquals0(final String message, final Object left, final Object right) {
        Class<?> componentType = left.getClass().getComponentType();
        if (componentType.isPrimitive()) {
            if (int.class == componentType) {
                assertArrayEquals((int[]) left, (int[]) right, message);
            }
            else if (short.class == componentType) {
                assertArrayEquals((short[]) left, (short[]) right, message);
            }
            else if (long.class == componentType) {
                assertArrayEquals((long[]) left, (long[]) right, message);
            }
            else if (float.class == componentType) {
                assertArrayEquals((float[]) left, (float[]) right, 0f, message);
            }
            else if (double.class == componentType) {
                assertArrayEquals((double[]) left, (double[]) right, 0d, message);
            }
            else if (boolean.class == componentType) {
                assertTrue(Arrays.equals((boolean[]) left, (boolean[]) right), message);
            }
            else if (byte.class == componentType) {
                assertArrayEquals((byte[]) left, (byte[]) right, message);
            }
            else if (char.class == componentType) {
                assertArrayEquals((char[]) left, (char[]) right, message);
            }
            else {
                fail(String.format("Unknown primitive type: %s", componentType));
            }
        }
        else {
            assertArrayEquals((Object[]) left, (Object[]) right, message);
        }
    }

    private static void failBecause(String message, Throwable exception) {
        AssertionError error = new AssertionError(message);
        error.initCause(exception);
        throw error;
    }

    public static final class Conversion {
        private final String strVal;
        private final Object objVal;
        private final String format;
        private final String convertedStrVal;

        public Conversion(String pStrVal, Object pObjVal) {
            this(pStrVal, pObjVal, null);
        }

        public Conversion(String pStrVal, Object pObjVal, String pFormat) {
            this(pStrVal, pObjVal, pFormat, pStrVal);
        }

        public Conversion(String pStrVal, Object pObjVal, String pFormat, String pConvertedStrVal) {
            Validate.notNull(pStrVal, "strVal");
            Validate.notNull(pObjVal, "objVal");
            Validate.notNull(pConvertedStrVal, "convertedStrVal");

            strVal = pStrVal;
            objVal = pObjVal;
            format = pFormat;
            convertedStrVal = pConvertedStrVal;
        }

        public String original() {
            return strVal;
        }

        public Object value() {
            return objVal;
        }

        public Class type() {
            return objVal.getClass();
        }

        public String format() {
            return format;
        }

        public String converted() {
            return convertedStrVal;
        }
    }
}
