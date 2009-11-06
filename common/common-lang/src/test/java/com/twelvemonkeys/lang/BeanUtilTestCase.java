package com.twelvemonkeys.lang;

import junit.framework.TestCase;

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

        Map map = new HashMap();

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

        Map map = new HashMap();

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

        Map map = new HashMap();

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

        Map map = new HashMap();

        map.put("stringValue", "one");
        map.put("intValue", new Integer(2));
        map.put("doubleValue", new Double(.3));

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertEquals("one", bean.getStringValue());
        assertEquals(2, bean.getIntValue());
        assertEquals(new Double(.3), bean.getDoubleValue());
    }

    public void testConfigureConvert() {
        TestBean bean = new TestBean();

        Map map = new HashMap();

        map.put("stringValue", new Integer(1));
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
        assertEquals(new Double(.3), bean.getDoubleValue());
    }

    public void testConfigureAmbigious1() {
        TestBean bean = new TestBean();

        Map map = new HashMap();

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

        Map map = new HashMap();

        Integer value = new Integer(2);
        map.put("ambigious", value);

        try {
            BeanUtil.configure(bean, map);
        }
        catch (InvocationTargetException e) {
            fail(e.getMessage());
        }

        assertNotNull(bean.getAmbigious());
        assertEquals("Integer converted rather than invoking setAmbigiouos(Integer), ordering not predictable",
                     new Integer(2), bean.getAmbigious());
        assertSame("Integer converted rather than invoking setAmbigiouos(Integer), ordering not predictable",
                   value, bean.getAmbigious());
    }

    public void testConfigureAmbigious3() {
        TestBean bean = new TestBean();

        Map map = new HashMap();

        Double value = new Double(.3);
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
        private String mString;
        private int mInt;
        private Double mDouble;

        private Object mAmbigious;

        public Double getDoubleValue() {
            return mDouble;
        }

        public int getIntValue() {
            return mInt;
        }

        public String getStringValue() {
            return mString;
        }

        public void setStringValue(String pString) {
            mString = pString;
        }

        public void setIntValue(int pInt) {
            mInt = pInt;
        }

        public void setDoubleValue(Double pDouble) {
            mDouble = pDouble;
        }

        public void setAmbigious(String pString) {
            mAmbigious = pString;
        }

        public void setAmbigious(Object pObject) {
            mAmbigious = pObject;
        }

        public void setAmbigious(Integer pInteger) {
            mAmbigious = pInteger;
        }

        public void setAmbigious(int pInt) {
            mAmbigious = new Long(pInt); // Just to differentiate...
        }

        public Object getAmbigious() {
            return mAmbigious;
        }
    }
}
