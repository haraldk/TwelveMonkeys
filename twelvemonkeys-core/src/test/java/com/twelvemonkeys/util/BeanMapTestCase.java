package com.twelvemonkeys.util;

import java.util.Map;
import java.beans.IntrospectionException;
import java.io.Serializable;

/**
 * BeanMapTestCase
 * <p/>
 * @todo Extend with BeanMap specific tests
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/BeanMapTestCase.java#2 $
 */
public class BeanMapTestCase extends MapAbstractTestCase {

    public boolean isPutAddSupported() {
        return false;
    }

    public boolean isRemoveSupported() {
        return false;
    }

    public boolean isSetValueSupported() {
        return true;
    }

    public boolean isAllowNullKey() {
        return false;
    }

    public Object[] getSampleKeys() {
        return new Object[] {
                "blah", "foo", "bar", "baz", "tmp", "gosh", "golly", "gee"
        };
    }

    public Object[] getSampleValues() {
        return new Object[] {
                "blahv", "foov", "barv", "bazv", "tmpv", "goshv", "gollyv", "geev"
        };
    }

    public Object[] getNewSampleValues() {
        return new Object[] {
                "newblahv", "newfoov", "newbarv", "newbazv", "newtmpv", "newgoshv", "newgollyv", "newgeev"
        };
    }

    public Map makeEmptyMap() {
        try {
            return new BeanMap(new NullBean());
        }
        catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    public Map makeFullMap() {
        try {
            return new BeanMap(new MyBean());
        }
        catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    public static class MyBean implements Serializable {
        Object blah = "blahv";
        Object foo = "foov";
        Object bar = "barv";
        Object baz = "bazv";
        Object tmp = "tmpv";
        Object gosh = "goshv";
        Object golly = "gollyv";
        Object gee = "geev";

        public Object getBar() {
            return bar;
        }

        public void setBar(Object pBar) {
            bar = pBar;
        }

        public Object getBaz() {
            return baz;
        }

        public void setBaz(Object pBaz) {
            baz = pBaz;
        }

        public Object getBlah() {
            return blah;
        }

        public void setBlah(Object pBlah) {
            blah = pBlah;
        }

        public Object getFoo() {
            return foo;
        }

        public void setFoo(Object pFoo) {
            foo = pFoo;
        }

        public Object getGee() {
            return gee;
        }

        public void setGee(Object pGee) {
            gee = pGee;
        }

        public Object getGolly() {
            return golly;
        }

        public void setGolly(Object pGolly) {
            golly = pGolly;
        }

        public Object getGosh() {
            return gosh;
        }

        public void setGosh(Object pGosh) {
            gosh = pGosh;
        }

        public Object getTmp() {
            return tmp;
        }

        public void setTmp(Object pTmp) {
            tmp = pTmp;
        }
    }

    static class NullBean implements Serializable { }
}
