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

package com.twelvemonkeys.servlet;

import org.junit.Test;
import org.mockito.Matchers;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * ServletConfiguratorTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ServletConfiguratorTestCase.java,v 1.0 May 2, 2010 3:08:33 PM haraldk Exp$
 */
public class ServletConfiguratorTest {

    // TODO: Test error conditions:
    // - Missing name = ... or non-bean conforming method
    // - Non-accessible? How..?
    // - Missing required value

    // TODO: Clean up tests to test only one thing at a time
    // - Public method
    // - Public method with override
    // - Public method overridden without annotation
    // - Protected method
    // - Protected method with override
    // - Protected method overridden without annotation
    // - Package protected method
    // - Package protected method with override
    // - Package protected method overridden without annotation
    // - Private method
    // - Multiple private methods with same signature (should invoke all, as private methods can't be overridden)

    @Test
    public void testConfigureAnnotatedServlet() throws ServletConfigException {
        AnnotatedServlet servlet = mock(AnnotatedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("x", "foo", "bar")));
        when(config.getInitParameter("x")).thenReturn("99");
        when(config.getInitParameter("foo")).thenReturn("Foo");
        when(config.getInitParameter("bar")).thenReturn("-1, 2, 0, 42");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setX(99);
        verify(servlet, times(1)).setFoo("Foo");
        verify(servlet, times(1)).configTheBar(-1, 2, 0, 42);
    }

    @Test
    public void testConfigureAnnotatedFilter() throws ServletConfigException {
        AnnotatedServlet servlet = mock(AnnotatedServlet.class);

        FilterConfig config = mock(FilterConfig.class);
        when(config.getFilterName()).thenReturn("FooFilter");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("x", "foo", "bar")));
        when(config.getInitParameter("x")).thenReturn("99");
        when(config.getInitParameter("foo")).thenReturn("Foo");
        when(config.getInitParameter("bar")).thenReturn("-1, 2, 0, 42");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setX(99);
        verify(servlet, times(1)).setFoo("Foo");
        verify(servlet, times(1)).configTheBar(-1, 2, 0, 42);
    }

    @Test
    public void testConfigurePrivateMethod() throws ServletConfigException {
        AnnotatedServlet servlet = mock(AnnotatedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("private")));
        when(config.getInitParameter("private")).thenReturn("99");

        ServletConfigurator.configure(servlet, config);

        // Verify
        assertEquals(servlet.priv, "99");
    }

    @Test
    public void testConfigurePrivateShadowedMethod() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @InitParam(name = "package-private")
            abstract void setPrivate(String priv);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("private")));
        when(config.getInitParameter("private")).thenReturn("private");
        when(config.getInitParameter("package-private")).thenReturn("package");

        ServletConfigurator.configure(servlet, config);

        // Verify
        assertEquals(servlet.priv, "private");
        verify(servlet, times(1)).setPrivate("package");
    }

    @Test
    public void testConfigureSubclassedServlet() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @InitParam(name = "flag")
            abstract void configureMeToo(boolean flag);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("x", "foo", "bar", "flag")));
        when(config.getInitParameter("x")).thenReturn("99");
        when(config.getInitParameter("foo")).thenReturn("Foo");
        when(config.getInitParameter("bar")).thenReturn("-1, 2, 0, 42");
        when(config.getInitParameter("flag")).thenReturn("true");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setX(99);
        verify(servlet, times(1)).setFoo("Foo");
        verify(servlet, times(1)).configTheBar(-1, 2, 0, 42);
        verify(servlet, times(1)).configureMeToo(true);
    }

    @Test
    public void testConfigureAnnotatedServletWithLispStyle() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @InitParam(name = "the-explicit-x")
            abstract public void setExplicitX(int x);

            @InitParam
            abstract public void setTheOtherX(int x);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("the-explicit-x", "the-other-x")));
        when(config.getInitParameter("the-explicit-x")).thenReturn("-1");
        when(config.getInitParameter("the-other-x")).thenReturn("42");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setExplicitX(-1);
        verify(servlet, times(1)).setTheOtherX(42);
    }

    @Test
    public void testConfigureSubclassedServletWithOverride() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @Override
            @InitParam(name = "y")
            public void setX(int x) {
            }
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("x", "y")));
        when(config.getInitParameter("x")).thenReturn("99");
        when(config.getInitParameter("y")).thenReturn("-66");
        
        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setX(-66);
        verify(servlet, times(1)).setX(anyInt()); // We don't want multiple invocations, only the overridden method
    }

    @Test
    public void testConfigureSubclassedServletWithOverrideNoParam() throws ServletConfigException {
        // NOTE: We must allow overriding the methods without annotation present, in order to allow CGLib/proxies of the class...
        abstract class SubclassedServlet extends AnnotatedServlet {
            @Override
            @InitParam(name = "<THIS PARAMETER DOES NOT EXIST>")
            public void setX(int x) {
            }

            @Override
            public void setFoo(String foo) {
            }
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("x", "foo")));
        when(config.getInitParameter("x")).thenReturn("99");
        when(config.getInitParameter("foo")).thenReturn("Foo");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, never()).setX(anyInt());
        verify(servlet, times(1)).setFoo("Foo");
        verify(servlet, times(1)).setFoo(Matchers.<String>any()); // We don't want multiple invocations
    }

    // Test interface
    @Test
    public void testConfigureServletWithInterface() throws ServletConfigException {
        abstract class InterfacedServlet implements Servlet, Annotated {
        }

        InterfacedServlet servlet = mock(InterfacedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("foo")));
        when(config.getInitParameter("foo")).thenReturn("Foo");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).annotated("Foo");
    }

    // TODO: Test override/shadow of package protected method outside package 

    @Test
    public void testRequiredParameter() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @InitParam(required = true)
            abstract void setRequired(String value);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("required")));
        when(config.getInitParameter("required")).thenReturn("the required value");

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setRequired("the required value");
        verify(servlet, times(1)).setRequired(Matchers.<String>any()); // We don't want multiple invocations
    }

    @Test
    public void testMissingParameter() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @InitParam()
            abstract void setNonRequired(String value);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.<Object>emptyList()));

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, never()).setNonRequired(Matchers.<String>any()); // Simply not configured
    }

    @Test(expected = ServletConfigException.class)
    public void testMissingRequiredParameter() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @Override
            @InitParam(required = true)
            protected abstract void setFoo(String value);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.<Object>emptyList()));

        ServletConfigurator.configure(servlet, config); // Should throw exception
    }

    @Test
    public void testMissingParameterDefaultValue() throws ServletConfigException {
        abstract class SubclassedServlet extends AnnotatedServlet {
            @InitParam(defaultValue = "1, 2, 3")
            abstract void setNonRequired(int[] value);
        }

        SubclassedServlet servlet = mock(SubclassedServlet.class);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("FooServlet");
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.<Object>emptyList()));

        ServletConfigurator.configure(servlet, config);

        // Verify
        verify(servlet, times(1)).setNonRequired(new int[] {1, 2, 3});
        verify(servlet, times(1)).setNonRequired(Matchers.<int[]>any());
    }


    public interface Annotated {
        @InitParam(name = "foo")
        public void annotated(String an);
    }

    public abstract class AnnotatedServlet implements Servlet, Filter {
        String priv;

        // Implicit name "x"
        @InitParam
        public abstract void setX(int x);

        // Implicit name "foo"
        @InitParam
        protected abstract void setFoo(String foo);

        @InitParam(name = "bar")
        abstract void configTheBar(int... bar);

        @InitParam(name = "private")
        private void setPrivate(String priv) {
            this.priv = priv;
        }
    }
}
