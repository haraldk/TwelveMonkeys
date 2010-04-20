package com.twelvemonkeys.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.*;

/**
 * GenericFilterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/GenericFilterTestCase.java#1 $
 */
public final class GenericFilterTestCase extends FilterAbstractTestCase {
    protected Filter makeFilter() {
        return new GenericFilterImpl();
    }

    public void testInitOncePerRequest() {
        // Default FALSE
        GenericFilter filter = new GenericFilterImpl();

        try {
            filter.init(makeFilterConfig());
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertFalse("OncePerRequest should default to false", filter.mOncePerRequest);
        filter.destroy();

        // TRUE
        filter = new GenericFilterImpl();
        Map params = new HashMap();
        params.put("once-per-request", "true");

        try {
            filter.init(makeFilterConfig(params));
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertTrue("oncePerRequest should be true", filter.mOncePerRequest);
        filter.destroy();

        // TRUE
        filter = new GenericFilterImpl();
        params = new HashMap();
        params.put("oncePerRequest", "true");

        try {
            filter.init(makeFilterConfig(params));
        }
        catch (ServletException e) {
            fail(e.getMessage());
        }

        assertTrue("oncePerRequest should be true", filter.mOncePerRequest);
        filter.destroy();
    }

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
