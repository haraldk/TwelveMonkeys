/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.servlet.OutputStreamAdapter;
import com.twelvemonkeys.util.StringTokenIterator;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ImageFilterTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageFilterTestCase.java,v 1.0 07.04.11 14.14 haraldk Exp$
 */
public class ImageFilterTestCase {

    @Test
    public void passThroughIfNotTrigger() throws ServletException, IOException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getFilterName()).thenReturn("dummy");
        when(filterConfig.getServletContext()).thenReturn(context);
        when(filterConfig.getInitParameterNames()).thenReturn(new StringTokenIterator("foo, bar"));

        DummyFilter filter = new DummyFilter() {
            @Override
            protected boolean trigger(ServletRequest pRequest) {
                return false;
            }
        };
        filter.init(filterConfig);

        // Request/response setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);

        // Execute
        filter.doFilter(request, response, chain);

        // Verifications
        verify(chain).doFilter(request, response);
    }

    @Test
    public void normalFilter() throws ServletException, IOException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getFilterName()).thenReturn("dummy");
        when(filterConfig.getServletContext()).thenReturn(context);
        when(filterConfig.getInitParameterNames()).thenReturn(new StringTokenIterator("foo, bar"));

        DummyFilter filter = new DummyFilter();
        filter.init(filterConfig);

        // Request/response setup
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ServletOutputStream out = spy(new OutputStreamAdapter(stream));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(out);

        FilterChain chain = mock(FilterChain.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

                response.setContentType("image/png");
                response.setContentLength(104417);
                InputStream stream = getClass().getResourceAsStream("/com/twelvemonkeys/servlet/image/12monkeys-splash.png");
                assertNotNull("Missing test resource", stream);
                FileUtil.copy(stream, response.getOutputStream());

                return null;
            }
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        // Execute
        filter.doFilter(request, response, chain);

        // Verifications
        int length = stream.size();

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(response).setContentType("image/png");
        verify(response, atMost(1)).setContentLength(length); // setContentLength not implemented, avoid future bugs
        verify(out, atLeastOnce()).flush();

        // Extra verification here, until we come up with something better
        assertTrue(
                String.format("Unlikely length for PNG (please run manual check): %s bytes, expected about 85000 bytes", length),
                length >= 60000 && length <= 120000
        );
    }

    @Test
    public void filterNoContent() throws ServletException, IOException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getFilterName()).thenReturn("dummy");
        when(filterConfig.getServletContext()).thenReturn(context);
        when(filterConfig.getInitParameterNames()).thenReturn(new StringTokenIterator("foo, bar"));

        DummyFilter filter = new DummyFilter();
        filter.init(filterConfig);

        // Request/response setup
        ServletOutputStream out = mock(ServletOutputStream.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(out);

        FilterChain chain = mock(FilterChain.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

                response.setContentType("image/x-imaginary");

                return null;
            }
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        // Execute
        filter.doFilter(request, response, chain);

        // Verifications
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(response).setContentType("image/x-imaginary");
        verify(out, atLeastOnce()).flush();
    }

    @Test
    public void triggerWhenTriggerParamsNull() throws ServletException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getFilterName()).thenReturn("dummy");
        when(filterConfig.getServletContext()).thenReturn(context);
        when(filterConfig.getInitParameterNames()).thenReturn(new StringTokenIterator("foo, bar"));

        DummyFilter filter = new DummyFilter();

        filter.init(filterConfig);

        // Execute/Verifications
        assertTrue(filter.trigger(mock(HttpServletRequest.class)));
    }

    @Test
    public void triggerWithTriggerParams() throws ServletException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getFilterName()).thenReturn("dummy");
        when(filterConfig.getServletContext()).thenReturn(context);
        when(filterConfig.getInitParameterNames()).thenReturn(new StringTokenIterator("triggerParams"));
        when(filterConfig.getInitParameter("triggerParams")).thenReturn("foo");

        DummyFilter filter = new DummyFilter();

        filter.init(filterConfig);

        // Request/response setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("foo")).thenReturn("doit");


        // Execute/Verifications
        assertTrue(filter.trigger(request));
    }

    @Test
    public void dontTriggerWithoutTriggerParams() throws ServletException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getFilterName()).thenReturn("dummy");
        when(filterConfig.getServletContext()).thenReturn(context);
        when(filterConfig.getInitParameterNames()).thenReturn(new StringTokenIterator("triggerParams"));
        when(filterConfig.getInitParameter("triggerParams")).thenReturn("foo");

        DummyFilter filter = new DummyFilter();

        filter.init(filterConfig);

        // Request/response setup
        HttpServletRequest request = mock(HttpServletRequest.class);


        // Execute/Verifications
        assertFalse(filter.trigger(request));
    }

    @Test
    public void testChaining() throws ServletException, IOException {
        // Filter init & setup
        ServletContext context = mock(ServletContext.class);

        FilterConfig fooConfig = mock(FilterConfig.class);
        when(fooConfig.getFilterName()).thenReturn("foo");
        when(fooConfig.getServletContext()).thenReturn(context);
        when(fooConfig.getInitParameterNames()).thenReturn(new StringTokenIterator(""));

        final AtomicReference<BufferedImage> imageRef = new AtomicReference<BufferedImage>();
        final AtomicReference<ImageServletResponse> responseRef = new AtomicReference<ImageServletResponse>();

        DummyFilter fooFilter = new DummyFilter() {
            @Override
            protected RenderedImage doFilter(BufferedImage image, ServletRequest request, ImageServletResponse response) throws IOException {
                // NOTE: Post-filtering, this method is run after barFilter.doFilter
                assertEquals(imageRef.get(), image);
                assertEquals(responseRef.get(), response);

                return image;
            }
        };
        fooFilter.init(fooConfig);

        FilterConfig barConfig = mock(FilterConfig.class);
        when(barConfig.getFilterName()).thenReturn("bar");
        when(barConfig.getServletContext()).thenReturn(context);
        when(barConfig.getInitParameterNames()).thenReturn(new StringTokenIterator(""));

        final DummyFilter barFilter = new DummyFilter() {
            @Override
            protected RenderedImage doFilter(BufferedImage image, ServletRequest request, ImageServletResponse response) throws IOException {
                // NOTE: Post-filtering, this method is run before fooFilter.doFilter
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.drawRect(10, 10, 100, 100);
                }
                finally {
                    graphics.dispose();
                }

                // Store references for later, make sure this is first and only set.
                assertTrue(imageRef.compareAndSet(null, image));
                assertTrue(responseRef.compareAndSet(null, response));

                return image;
            }
        };
        barFilter.init(barConfig);

        // Request/response setup
        ServletOutputStream out = mock(ServletOutputStream.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(out);

        FilterChain chain = mock(FilterChain.class);
        final AtomicBoolean first = new AtomicBoolean(false);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                HttpServletRequest request = (HttpServletRequest) invocation.getArguments()[0];
                HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];

                // Fake chaining here..
                if (first.compareAndSet(false, true)) {
                    barFilter.doFilter(request, response, (FilterChain) invocation.getMock());
                    return null;
                }

                // Otherwise, fake servlet/file response
                response.setContentType("image/gif");
                InputStream stream = getClass().getResourceAsStream("/com/twelvemonkeys/servlet/image/tux.gif");
                assertNotNull("Missing test resource", stream);
                FileUtil.copy(stream, response.getOutputStream());

                return null;
            }
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        // Execute
        fooFilter.doFilter(request, response, chain);

        // Verifications
        verify(chain, times(2)).doFilter(any(ServletRequest.class), any(ImageServletResponse.class));
        verify(response).setContentType("image/gif");
        verify(out, atLeastOnce()).flush();

        // NOTE:
        // We verify that the image is the same in both ImageFilter implementations, to make sure the image is only
        // decoded once, then encoded once
    }

    private static class DummyFilter extends ImageFilter {
        @Override
        protected RenderedImage doFilter(BufferedImage image, ServletRequest request, ImageServletResponse response) throws IOException {
            return image;
        }
    }
}
