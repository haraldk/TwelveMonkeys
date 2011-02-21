package com.twelvemonkeys.lang;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;

/**
 * BeanUtilTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/lang/BeanUtilTestCase.java#1 $
 */
public class BeanUtilTestCase extends TestCase {

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
        assertEquals(.3, bean.getDoubleValue());
    }

    public void testConfigureConvert() {
        TestBean bean = new TestBean();

        Map<String,Serializable> map = new HashMap<String, Serializable>();

        map.put("stringValue", 1);
        map.put("intValue", "2");
        map.put("doubleValue", NumberFormat.getNumberInstance().format(0.3)); // Note, format is locale specific...

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertEquals("1", bean.getStringValue());
        assertEquals(2, bean.getIntValue());
        assertEquals(.3, bean.getDoubleValue());
    }

    public void testConfigureAmbigious1() {
        TestBean bean = new TestBean();

        Map<String, String> map = new HashMap<String, String>();

        String value = "one";
        map.put("ambigious", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbigious());
        assertEquals("String converted rather than invoking setAmbigiouos(String), ordering not predictable",
                     "one", bean.getAmbigious());
        assertSame("String converted rather than invoking setAmbigiouos(String), ordering not predictable",
                   value, bean.getAmbigious());
    }

    public void testConfigureAmbigious2() {
        TestBean bean = new TestBean();

        Map<String, Integer> map = new HashMap<String, Integer>();

        Integer value = 2;
        map.put("ambigious", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbigious());
        assertEquals("Integer converted rather than invoking setAmbigiouos(Integer), ordering not predictable",
                2, bean.getAmbigious());
        assertSame("Integer converted rather than invoking setAmbigiouos(Integer), ordering not predictable",
                   value, bean.getAmbigious());
    }

    public void testConfigureAmbigious3() {
        TestBean bean = new TestBean();

        Map<String, Double> map = new HashMap<String, Double>();

        Double value = .3;
        map.put("ambigious", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbigious());
        assertEquals("Object converted rather than invoking setAmbigious(Object), ordering not predictable",
                     value.getClass(), bean.getAmbigious().getClass());
        assertSame("Object converted rather than invoking setAmbigious(Object), ordering not predictable",
                   value, bean.getAmbigious());
    }

    static class TestBean {
        private String stringVal;
        private int intVal;
        private Double doubleVal;

        private Object ambigious;

        public Double getDoubleValue() {
            return doubleVal;
        }

        public int getIntValue() {
            return intVal;
        }

        public String getStringValue() {
            return stringVal;
        }

        public void setStringValue(String pString) {
            stringVal = pString;
        }

        public void setIntValue(int pInt) {
            intVal = pInt;
        }

        public void setDoubleValue(Double pDouble) {
            doubleVal = pDouble;
        }

        public void setAmbigious(String pString) {
            ambigious = pString;
        }

        public void setAmbigious(Object pObject) {
            ambigious = pObject;
        }

        public void setAmbigious(Integer pInteger) {
            ambigious = pInteger;
        }

        public void setAmbigious(int pInt) {
            ambigious = (long) pInt; // Just to differentiate...
        }

        public Object getAmbigious() {
            return ambigious;
        }
    }
}
