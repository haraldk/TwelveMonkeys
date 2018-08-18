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

package com.twelvemonkeys.util;

import java.beans.IntrospectionException;
import java.io.Serializable;
import java.util.Map;

/**
 * BeanMapTestCase
 * <p/>
 * @todo Extend with BeanMap specific tests
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/BeanMapTestCase.java#2 $
 */
public class BeanMapTest extends MapAbstractTest {

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
