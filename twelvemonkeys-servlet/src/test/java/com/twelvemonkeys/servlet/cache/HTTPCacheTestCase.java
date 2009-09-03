package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.net.NetUtil;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import javax.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * CacheManagerTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/cache/HTTPCacheTestCase.java#2 $
 */
public class HTTPCacheTestCase extends MockObjectTestCase {
    // TODO: Clean up!

    private static final File TEMP_ROOT = new File(FileUtil.getTempDirFile(), "cache-test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertTrue("Could not create temp dir, tests can not run", (TEMP_ROOT.exists() && TEMP_ROOT.isDirectory()) || TEMP_ROOT.mkdirs());
        // Clear temp dir
        File[] files = TEMP_ROOT.listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateNegativeNoName() {
        try {
            new HTTPCache(null, (ServletContext) newDummy(ServletContext.class), 500, 0, 10, true);
            fail("Expected creation failure, no name");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("name"));
            assertTrue(message.contains("null"));
        }

        try {
            new HTTPCache("", (ServletContext) newDummy(ServletContext.class), 500, 0, 10, true);
            fail("Expected creation failure, empty name");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("name"));
            assertTrue(message.contains("empty"));
        }
    }

    public void testCreateNegativeNoContext() {
        try {
            new HTTPCache("Dummy", null, 500, 0, 10, true);
            fail("Expected creation failure, no context");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("context"));
            assertTrue(message.contains("null"));
        }

    }

    public void testCreateNegativeNoTempFolder() {
        try {
            new HTTPCache(null, 500, 0, 10, true);
            fail("Expected creation failure, no temp folder");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("temp"));
            assertTrue(message.contains("folder"));
            assertTrue(message.contains("null"));
        }
    }

    public void testCreateNegativeValues() {
        try {
            new HTTPCache(TEMP_ROOT, -1, 0, 10, true);
            fail("Expected creation failure");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("negative"));
            assertTrue(message.contains("expiry time"));
        }

        try {
            new HTTPCache(TEMP_ROOT, 1000, -1, 10, false);
            fail("Expected creation failure");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("negative"));
            assertTrue(message.contains("cache size"));
        }

        try {
            new HTTPCache(TEMP_ROOT, 1000, 128, -1, true);
            fail("Expected creation failure");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("negative"));
            assertTrue(message.contains("number"));
        }
    }

    public void testCreate() {
        new HTTPCache(TEMP_ROOT, 500, 0, 10, true);
    }

    public void testCreateServletContext() {
        Mock mockContext = mock(ServletContext.class);
        // Currently context is used for tempdir and logging
        mockContext.stubs().method("getAttribute").with(eq("javax.servlet.context.tempdir")).will(returnValue(TEMP_ROOT));
        new HTTPCache("cache", (ServletContext) mockContext.proxy(), 500, 0, 10, true);
    }

    public void testCacheableRequest() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);
                
                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);
        
        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    private String createRequestURI() {
        return "http://www.foo.com/" + getName() + ".bar";
    }

    public void testCacheableRequestWithParameters() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        parameters.put("foo", Collections.singletonList("bar"));
        parameters.put("params", Arrays.asList("une", "due", "tres"));
        CacheRequest request = configureRequest(mockRequest, "GET", createRequestURI(), parameters, null);

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    private CacheRequest configureRequest(Mock pMockRequest, String pRequestURI) {
        return configureRequest(pMockRequest, "GET", pRequestURI, null, null);
    }

    private CacheRequest configureRequest(Mock pMockRequest, String pMethod, String pRequestURI, Map<String, List<String>> pParameters, final Map<String, List<String>> pHeaders) {
        pMockRequest.reset();
        pMockRequest.stubs().method("getRequestURI").will(returnValue(URI.create(pRequestURI)));
        pMockRequest.stubs().method("getParameters").will(returnValue(pParameters == null ? Collections.emptyMap() : pParameters));
        pMockRequest.stubs().method("getHeaders").will(returnValue(pHeaders == null ? Collections.emptyMap() : pHeaders));
        pMockRequest.stubs().method("getMethod").will(returnValue(pMethod));
        return (CacheRequest) pMockRequest.proxy();
    }

    public void testCacheablePersistentRepeatedRequest() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
        mockRequest.verify();
        mockResponse.verify();
        mockResolver.verify();

        // Reset
        result.reset();
        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        // Test request again, make sure resolve is executed exactly once
        HTTPCache cache2 = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);
        cache2.doCached(request, response, resolver);

        // Test that second response is equal to first
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testCacheableRepeatedRequest() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
        mockRequest.verify();
        mockResponse.verify();
        mockResolver.verify();

        // Reset
        result.reset();
        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        // Test request again, make sure resolve is executed exactly once
        cache.doCached(request, response, resolver);

        // Test that second response is equal to first
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testNonCacheableRequestHeader() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, "GET", createRequestURI(), null, Collections.singletonMap("Cache-Control", Collections.singletonList("no-store")));

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // TODO: How do we know that the response was NOT cached?
        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testNonCacheableRequestHeaderRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);
        String requestURI = createRequestURI();
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, requestURI);

        Mock mockResponse = mock(CacheResponse.class);
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockResolver.verify();

        // Reset
        result.reset();

        mockRequest.reset();
        mockRequest.stubs().method("getRequestURI").will(returnValue(URI.create(requestURI)));
        mockRequest.stubs().method("getParameters").will(returnValue(Collections.emptyMap()));
        mockRequest.stubs().method("getHeaders").will(returnValue(Collections.singletonMap("Cache-Control", Collections.singletonList("no-cache")))); // Force non-cached version of cached content
        mockRequest.stubs().method("getMethod").will(returnValue("GET"));

        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        mockResolver.reset();
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub2") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        value[3] = 'B';

        // This cache should not be cached
        cache.doCached(request, response, resolver);

         // Verify that second reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testNonCacheableResponseHeader() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Cache-Control"), eq("no-cache"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Cache-Control", "no-cache");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testNonCacheableResponseHeaderRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Cache-Control"), eq("no-store"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Cache-Control", "no-store");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockRequest.verify();

        // Reset
        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Cache-Control"), eq("no-store"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        mockResolver.reset();
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Cache-Control", "no-store");
                res.getOutputStream().write(value);

                return null;
            }
        });
        result.reset();
        value[3] = 'B';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    // Test non-cacheable response
    public void testNonCacheableResponse() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Mock mockResponse = mock(CacheResponse.class);
        mockResponse.expects(once()).method("setStatus").with(eq(500));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(500);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    // Test non-cacheable response
    public void testNonCacheableResponseRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Mock mockResponse = mock(CacheResponse.class);
        mockResponse.expects(once()).method("setStatus").with(eq(500));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(500);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockResolver.verify();

        // Test request again, should do new resolve...
        result.reset();
        value[3] = 'B';

        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(500));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        mockResolver.reset();
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(500);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.getOutputStream().write(value);

                return null;
            }
        });

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }


    // Test that request headers are forwarded to resolver...
    public void testRequestHeadersForwarded() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        headers.put("Cache-Control", Arrays.asList("no-cache"));
        headers.put("X-Custom", Arrays.asList("FOO", "BAR"));

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, "HEAD", createRequestURI(), null, headers);

        Mock mockResponse = mock(CacheResponse.class);
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);

                Map<String, List<String>> reqHeaders = req.getHeaders();
                assertEquals(headers, reqHeaders);

                // Make sure that we preserve insertion order
                Set<Map.Entry<String, List<String>>> expected = headers.entrySet();
                Iterator<Map.Entry<String,List<String>>> actual = reqHeaders.entrySet().iterator();
                for (Map.Entry<String, List<String>> entry : expected) {
                    assertEquals(entry, actual.next());
                }

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);
    }

    // Test that response headers are preserved
    public void testCacheablePreserveResponseHeaders() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Mock mockResponse = mock(CacheResponse.class);
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method(or(eq("setHeader"), eq("addHeader"))).with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method(or(eq("setHeader"), eq("addHeader"))).with(eq("Cache-Control"), eq("public"));
        mockResponse.expects(atLeastOnce()).method(or(eq("setHeader"), eq("addHeader"))).with(eq("X-Custom"), eq("FOO")).id("firstCustom");
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("addHeader").with(eq("X-Custom"), eq("BAR")).after("firstCustom");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.stubs().method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Cache-Control", "public");
                res.addHeader("X-Custom", "FOO");
                res.addHeader("X-Custom", "BAR");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    // Test Vary
    public void testVaryMissingRequestHeader() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("X-Foo"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader(HTTPCache.HEADER_CONTENT_TYPE, "x-foo/bar");
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "X-Foo");
                res.setHeader("X-Foo", "foobar header");
                res.setHeader("X-Other", "don't care");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockRequest.verify();

        // Reset
        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("X-Foo"), ANYTHING);
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        result.reset();

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testVaryMissingRequestHeaderRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);
        String requestURI = createRequestURI();

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, requestURI);

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader(HTTPCache.HEADER_CONTENT_TYPE, "x-foo/bar");
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "X-Foo");
                res.setHeader("X-Other", "don't care");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();


        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockRequest.verify();

        // Reset
        request = configureRequest(mockRequest, "GET", requestURI, null, Collections.singletonMap("X-Foo", Collections.singletonList("foobar")));

        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        mockResolver.reset();
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "X-Foo");
                res.setHeader("X-Other", "don't care");
                res.getOutputStream().write(value);

                return null;
            }
        });
        result.reset();
        value[3] = 'B';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testVarySameResourceIsCached() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, "GET", createRequestURI(), null, Collections.singletonMap("X-Foo", Collections.singletonList("foobar value")));

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "X-Foo");
                res.setHeader("X-Other", "don't care");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockRequest.verify();

        // Reset
        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        result.reset();

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testVaryDifferentResources() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);
        String requestURI = createRequestURI();

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, "GET", requestURI, null, Collections.singletonMap("X-Foo", Collections.singletonList("foo")));

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foo".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "X-Foo");
                res.setHeader("X-Other", "don't care");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockRequest.verify();

        // Reset
        request = configureRequest(mockRequest, "GET", requestURI, null, Collections.singletonMap("X-Foo", Collections.singletonList("bar")));

        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("X-Foo"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        mockResolver.reset();
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "X-Foo");
                res.setHeader("Cache-Control", "no-store");
                res.getOutputStream().write(value);

                return null;
            }
        });
        result.reset();
        value[0] = 'b';
        value[1] = 'a';
        value[2] = 'r';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    public void testVaryVariations() {
        fail("TODO");
    }

    public void testVarationsWithSameContentType() {
        // I believe there is a bug if two variations has same content type...
        fail("TODO");
    }

    public void testVaryStarNonCached() throws Exception {
        HTTPCache cache = new HTTPCache(TEMP_ROOT, 60000, 1024 * 1024, 10, true);

        // Custom setup
        Mock mockRequest = mock(CacheRequest.class);
        CacheRequest request = configureRequest(mockRequest, createRequestURI());

        Mock mockResponse = mock(CacheResponse.class);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("*"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));
        CacheResponse response = (CacheResponse) mockResponse.proxy();

        final byte[] value = "foobar".getBytes("UTF-8");

        Mock mockResolver = mock(ResponseResolver.class);
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "*");
                res.getOutputStream().write(value);

                return null;
            }
        });
        ResponseResolver resolver = (ResponseResolver) mockResolver.proxy();

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        mockRequest.verify();
        mockResponse.verify();
        mockRequest.verify();

        // Reset
        mockResponse.reset();
        mockResponse.expects(once()).method("setStatus").with(eq(HTTPCache.STATUS_OK));
        mockResponse.stubs().method("setHeader");
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Date"), ANYTHING);
        mockResponse.expects(atLeastOnce()).method("setHeader").with(eq("Vary"), eq("*"));
        mockResponse.stubs().method("addHeader");
        mockResponse.expects(atLeastOnce()).method("getOutputStream").will(returnValue(result));

        mockResolver.reset();
        mockResolver.expects(once()).method("resolve").will(new CustomStub("request resolver stub") {
            public Void invoke(Invocation invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.parameterValues.get(0);
                CacheResponse res = (CacheResponse) invocation.parameterValues.get(1);

                res.setStatus(HTTPCache.STATUS_OK);
                res.setHeader("Date", NetUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Vary", "*");
                res.getOutputStream().write(value);

                return null;
            }
        });
        result.reset();
        value[3] = 'B';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
    }

    // Test failing request (IOException)
    public void testIOException() {
        fail("TODO");
    }

    public void testCacheException() {
        fail("TODO");
    }

    public void testRuntimeException() {
        fail("TODO");
    }

    // Test failing (negative) HTTP response (401, 404, 410, 500, etc)
    public void testNegativeCache() {
        fail("TODO");
    }

    public void testNegativeCacheExpires() {
        fail("TODO");
    }

    // Test If-None-Match/ETag support
    public void testIfNoneMatch() {
        fail("TODO");
    }

    // Test If-Modified-Since support
    public void testIfModifiedSince() {
        fail("TODO");
    }

    // Test that data really expires when TTL is over
    public void testTimeToLive() {
        fail("TODO");
    }

    public void testMaxAgeRequest() {
        fail("TODO");    
    }

    // Test that for requests with authorization, responses are not shared between different authorized users, unless response is marked as Cache-Control: public 
    public void testAuthorizedRequestPublic() {
        fail("TODO");
    }

    public void testAuthorizedRequestPrivate() {
        fail("TODO");
    }

    public void testPutPostDeleteInvalidatesCache() {
        fail("TODO");
    }

    // TODO: Move out to separate package/test, just keep it here for PoC
    public void testClientRequest() {
        fail("Not implemented");
    }

    // TODO: Move out to separate package/test, just keep it here for PoC
    public void testServletRequest() {
        fail("Not implemented");
    }
}
