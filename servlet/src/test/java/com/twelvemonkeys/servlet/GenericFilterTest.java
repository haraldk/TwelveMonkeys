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

import javax.servlet.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * GenericFilterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/GenericFilterTestCase.java#1 $
 */
public final class GenericFilterTest extends FilterAbstractTest {
    protected Filter makeFilter() {
        return new GenericFilterImpl();
    }

    @Test
    public void testInitOncePerRequest() {
        // Default FALSE
        GenericFilter filter = new GenericFilterImpl();

        try {
            filter.init(makeFilterConfig());
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertFalse("OncePerRequest should default to false", filter.oncePerRequest);
        filter.destroy();

        // TRUE
        filter = new GenericFilterImpl();
        Map<String, String> params = new HashMap<String, String>();
        params.put("once-per-request", "true");

        try {
            filter.init(makeFilterConfig(params));
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertTrue("oncePerRequest should be true", filter.oncePerRequest);
        filter.destroy();

        // TRUE
        filter = new GenericFilterImpl();
        params = new HashMap<String, String>();
        params.put("oncePerRequest", "true");

        try {
            filter.init(makeFilterConfig(params));
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertTrue("oncePerRequest should be true", filter.oncePerRequest);
        filter.destroy();
    }

    @Test
    public void testFilterOnlyOnce() {
        final GenericFilterImpl filter = new GenericFilterImpl();
        filter.setOncePerRequest(true);

        try {
            filter.init(makeFilterConfig());
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        FilterChain chain = new MyFilterChain(new Filter[] {filter, filter, filter});

        try {
            chain.doFilter(makeRequest(), makeResponse());
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertEquals("Filter was invoked more than once!", 1, filter.invocationCount);

        filter.destroy();
    }

    @Test
    public void testFilterMultiple() {
        final GenericFilterImpl filter = new GenericFilterImpl();

        try {
            filter.init(makeFilterConfig());
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        FilterChain chain = new MyFilterChain(new Filter[] {
                filter, filter, filter, filter, filter
        });

        try {
            chain.doFilter(makeRequest(), makeResponse());
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertEquals("Filter was invoked not invoked five times!", 5, filter.invocationCount);

        filter.destroy();
    }

    private static class GenericFilterImpl extends GenericFilter {
        int invocationCount;
        protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
            invocationCount++;
            pChain.doFilter(pRequest, pResponse);
        }
    }

    private static class MyFilterChain implements FilterChain {

        Filter[] mFilters;
        int mCurrentFilter;

        public MyFilterChain(Filter[] pFilters) {
            if (pFilters == null) {
                throw new IllegalArgumentException("filters == null");
            }
            mFilters = pFilters;
            mCurrentFilter = 0;
        }

        public void doFilter(ServletRequest pRequest, ServletResponse pResponse) throws IOException, ServletException {
            if (mCurrentFilter < mFilters.length) {
                mFilters[mCurrentFilter++].doFilter(pRequest, pResponse, this);
            }
        }
    }
}
