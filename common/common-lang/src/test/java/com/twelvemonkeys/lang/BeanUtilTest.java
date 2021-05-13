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

package com.twelvemonkeys.lang;

import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * BeanUtilTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/lang/BeanUtilTestCase.java#1 $
 */
public class BeanUtilTest {

    @Test
    public void testConfigureNoMehtod() {
        TestBean bean = new TestBean();

        Map<String, String> map = new HashMap<String, String>();

        map.put("noSuchMethod", "jaffa");

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testConfigureNoMethodArgs() {
        TestBean bean = new TestBean();

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("doubleValue", new Object()); // Should not be able to convert this

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNull(bean.getDoubleValue());

    }

    @Test
    public void testConfigureNullValue() {
        TestBean bean = new TestBean();

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("stringValue", null);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNull(bean.getStringValue());
    }

    public void testConfigureSimple() {
        TestBean bean = new TestBean();

        Map<String, Serializable> map = new HashMap<String, Serializable>();

        map.put("stringValue", "one");
        map.put("intValue", 2);
        map.put("doubleValue", .3);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertEquals("one", bean.getStringValue());
        assertEquals(2, bean.getIntValue());
        assertEquals(.3, bean.getDoubleValue(), 0);
    }

    @Test
    public void testConfigureConvert() {
        TestBean bean = new TestBean();

        Map<String,Serializable> map = new HashMap<String, Serializable>();

        map.put("stringValue", 1);
        map.put("intValue", "2");
        map.put("doubleValue", ".3");

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertEquals("1", bean.getStringValue());
        assertEquals(2, bean.getIntValue());
        assertEquals(0.3, bean.getDoubleValue(), 0);
    }

    @Test
    public void testConfigureAmbiguous1() {
        TestBean bean = new TestBean();

        Map<String, String> map = new HashMap<String, String>();

        String value = "one";
        map.put("ambiguous", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbiguous());
        assertEquals("String converted rather than invoking setAmbiguous(String), ordering not predictable",
                     "one", bean.getAmbiguous());
        assertSame("String converted rather than invoking setAmbiguous(String), ordering not predictable",
                   value, bean.getAmbiguous());
    }

    @Test
    public void testConfigureAmbiguous2() {
        TestBean bean = new TestBean();

        Map<String, Integer> map = new HashMap<String, Integer>();

        Integer value = 2;
        map.put("ambiguous", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbiguous());
        assertEquals("Integer converted rather than invoking setAmbiguous(Integer), ordering not predictable",
                2, bean.getAmbiguous());
        assertSame("Integer converted rather than invoking setAmbiguous(Integer), ordering not predictable",
                   value, bean.getAmbiguous());
    }

    @Test
    public void testConfigureAmbiguous3() {
        TestBean bean = new TestBean();

        Map<String, Double> map = new HashMap<String, Double>();

        Double value = .3;
        map.put("ambiguous", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbiguous());
        assertEquals("Object converted rather than invoking setAmbiguous(Object), ordering not predictable",
                     value.getClass(), bean.getAmbiguous().getClass());
        assertSame("Object converted rather than invoking setAmbiguous(Object), ordering not predictable",
                   value, bean.getAmbiguous());
    }

    static class TestBean {
        private String stringVal;
        private int intVal;
        private Double doubleVal;

        private Object ambiguous;

        public Double getDoubleValue() {
            return doubleVal;
        }

        public int getIntValue() {
            return intVal;
        }

        public String getStringValue() {
            return stringVal;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setStringValue(String pString) {
            stringVal = pString;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setIntValue(int pInt) {
            intVal = pInt;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setDoubleValue(Double pDouble) {
            doubleVal = pDouble;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setAmbiguous(String pString) {
            ambiguous = pString;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setAmbiguous(Object pObject) {
            ambiguous = pObject;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setAmbiguous(Integer pInteger) {
            ambiguous = pInteger;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setAmbiguous(int pInt) {
            ambiguous = (long) pInt; // Just to differentiate...
        }

        public Object getAmbiguous() {
            return ambiguous;
        }
    }
}
