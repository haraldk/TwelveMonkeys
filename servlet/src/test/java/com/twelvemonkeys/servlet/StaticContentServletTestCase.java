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

package com.twelvemonkeys.servlet;

import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


/**
 * StaticContentServletTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: StaticContentServletTestCase.java,v 1.0 12.12.11 15:10 haraldk Exp$
 */
public class StaticContentServletTestCase {

    private static final String IMAGE_RESOURCE = "/12monkeys-splash.png";

    private static String getFileSystemRoot() {
        File root = getResourceAsFile(IMAGE_RESOURCE).getParentFile();
        return root.getAbsolutePath();
    }

    private static File getResourceAsFile(String resourceName) {
        URL resource = StaticContentServletTestCase.class.getResource("/com/twelvemonkeys/servlet/image" + resourceName);

        try {
            return new File(resource.toURI());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = ServletException.class)
    public void testBadInitNoRoot() throws ServletException {
        StaticContentServlet servlet = new StaticContentServlet();
        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

        servlet.init(config);
    }

    @Test(expected = ServletException.class)
    public void testBadInit() throws ServletException {
        StaticContentServlet servlet = new StaticContentServlet();
        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn("foo/bar");

        servlet.init(config);
    }

    @Test
    public void testNotFound() throws ServletException, IOException {
        StaticContentServlet servlet = new StaticContentServlet();

        ServletContext context = mock(ServletContext.class);
        when(context.getServletContextName()).thenReturn("foo");
        when(context.getMimeType(anyString())).thenReturn("image/jpeg");

        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn(getFileSystemRoot());
        when(config.getServletContext()).thenReturn(context);

        servlet.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getPathInfo()).thenReturn("/missing.jpg");
        when(request.getRequestURI()).thenReturn("/foo/missing.jpg");
        when(request.getContextPath()).thenReturn("/foo");

        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "/foo/missing.jpg");
    }

    @Test
    public void testDirectoryListingForbidden() throws ServletException, IOException {
        StaticContentServlet servlet = new StaticContentServlet();

        ServletContext context = mock(ServletContext.class);
        when(context.getServletContextName()).thenReturn("foo");
        when(context.getMimeType(anyString())).thenReturn("image/png");

        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn(getFileSystemRoot());
        when(config.getServletContext()).thenReturn(context);

        servlet.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getPathInfo()).thenReturn("/");       // Attempt directory listing
        when(request.getRequestURI()).thenReturn("/foo/"); // Attempt directory listing
        when(request.getContextPath()).thenReturn("/foo");

        ServletOutputStream stream = mock(ServletOutputStream.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(stream);

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "/foo/");
    }

    @Test
    public void testGet() throws ServletException, IOException {
        StaticContentServlet servlet = new StaticContentServlet();

        ServletContext context = mock(ServletContext.class);
        when(context.getServletContextName()).thenReturn("foo");
        when(context.getMimeType(anyString())).thenReturn("image/png");

        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn(getFileSystemRoot());
        when(config.getServletContext()).thenReturn(context);

        servlet.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getPathInfo()).thenReturn(IMAGE_RESOURCE);
        when(request.getRequestURI()).thenReturn("/foo" + IMAGE_RESOURCE);
        when(request.getContextPath()).thenReturn("/foo");

        ServletOutputStream stream = mock(ServletOutputStream.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(stream);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
//        verify(stream, atLeastOnce()).write((byte[]) any(), anyInt(), anyInt()); // Mockito bug?
        verify(stream, times(51)).write((byte[]) any(), anyInt(), anyInt()); // This test is fragile, but the above throws exception..?
        verify(stream, atLeastOnce()).flush();
    }

    @Test
    public void testGetConfiguredForSingleFile() throws ServletException, IOException {
        StaticContentServlet servlet = new StaticContentServlet();

        ServletContext context = mock(ServletContext.class);
        when(context.getServletContextName()).thenReturn("foo");
        when(context.getMimeType(anyString())).thenReturn("text/plain");

        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn(getFileSystemRoot() + "/foo.txt");
        when(config.getServletContext()).thenReturn(context);

        servlet.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/foo");
        when(request.getContextPath()).thenReturn("/foo");

        ServletOutputStream stream = mock(ServletOutputStream.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(stream);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
//        verify(stream, atLeastOnce()).write((byte[]) any(), anyInt(), anyInt()); // Mockito bug?
        verify(stream, times(1)).write((byte[]) any(), anyInt(), anyInt()); // This test is fragile, but the above throws exception..?
        verify(stream, atLeastOnce()).flush();
    }

    @Test
    public void testGetNotModified() throws ServletException, IOException {
        StaticContentServlet servlet = new StaticContentServlet();

        ServletContext context = mock(ServletContext.class);
        when(context.getServletContextName()).thenReturn("foo");
        when(context.getMimeType(anyString())).thenReturn("image/png");

        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn(getFileSystemRoot());
        when(config.getServletContext()).thenReturn(context);

        servlet.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getPathInfo()).thenReturn(IMAGE_RESOURCE);
        when(request.getRequestURI()).thenReturn("/foo" + IMAGE_RESOURCE);
        when(request.getContextPath()).thenReturn("/foo");
        when(request.getDateHeader("If-Modified-Since")).thenReturn(getResourceAsFile(IMAGE_RESOURCE).lastModified());

        ServletOutputStream stream = mock(ServletOutputStream.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(stream);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        verifyZeroInteractions(stream);
    }

    @Test
    public void testHead() throws ServletException, IOException {
        StaticContentServlet servlet = new StaticContentServlet();

        ServletContext context = mock(ServletContext.class);
        when(context.getServletContextName()).thenReturn("foo");
        when(context.getMimeType(anyString())).thenReturn("image/png");

        ServletConfig config = mock(ServletConfig.class);
        when(config.getInitParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("root")));
        when(config.getInitParameter("root")).thenReturn(getFileSystemRoot());
        when(config.getServletContext()).thenReturn(context);

        servlet.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("HEAD");
        when(request.getPathInfo()).thenReturn(IMAGE_RESOURCE);
        when(request.getRequestURI()).thenReturn("/foo" + IMAGE_RESOURCE);
        when(request.getContextPath()).thenReturn("/foo");

        ServletOutputStream stream = mock(ServletOutputStream.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(stream);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verifyZeroInteractions(stream);
    }
}
