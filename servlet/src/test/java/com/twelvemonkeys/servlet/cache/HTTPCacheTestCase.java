package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.net.HTTPUtil;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * CacheManagerTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/cache/HTTPCacheTestCase.java#2 $
 */
public class HTTPCacheTestCase {
    @Rule
    public final TestName name = new TestName();

    // TODO: Replace with rule: TemporaryFolder
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File getTempRoot() {
        return temporaryFolder.newFolder("cache-test");
    }

    private CacheRequest configureRequest(CacheRequest request, String pRequestURI) {
        return configureRequest(request, "GET", pRequestURI, null, null);
    }

    private CacheRequest configureRequest(CacheRequest request, String pMethod, String pRequestURI, Map<String, List<String>> pParameters, final Map<String, List<String>> pHeaders) {
        reset(request);

        when(request.getRequestURI()).thenReturn(URI.create(pRequestURI));
        when(request.getParameters()).thenReturn(pParameters == null ? Collections.<String, List<String>>emptyMap() : pParameters);
        when(request.getHeaders()).thenReturn(pHeaders == null ? Collections.<String, List<String>>emptyMap() : pHeaders);
        when(request.getMethod()).thenReturn(pMethod);

        return request;
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testCreateNegativeNoName() {
        try {
            new HTTPCache(null, mock(ServletContext.class), 500, 0, 10, true);
            fail("Expected creation failure, no name");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("name"));
            assertTrue(message.contains("null"));
        }

        try {
            new HTTPCache("", mock(ServletContext.class), 500, 0, 10, true);
            fail("Expected creation failure, empty name");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("name"));
            assertTrue(message.contains("empty"));
        }
    }

    @SuppressWarnings("deprecation")
    @Test
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

    @Test
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

    @Test
    public void testCreateNegativeValues() {
        try {
            new HTTPCache(getTempRoot(), -1, 0, 10, true);
            fail("Expected creation failure");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("negative"));
            assertTrue(message.contains("expiry time"));
        }

        try {
            new HTTPCache(getTempRoot(), 1000, -1, 10, false);
            fail("Expected creation failure");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("negative"));
            assertTrue(message.contains("cache size"));
        }

        try {
            new HTTPCache(getTempRoot(), 1000, 128, -1, true);
            fail("Expected creation failure");
        }
        catch (IllegalArgumentException expected) {
            String message = expected.getMessage().toLowerCase();
            assertTrue(message.contains("negative"));
            assertTrue(message.contains("number"));
        }
    }

    @Test
    public void testCreate() {
        new HTTPCache(getTempRoot(), 500, 0, 10, true);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateServletContext() {
        ServletContext mockContext = mock(ServletContext.class);
        // Currently context is used for tempdir and logging
        when(mockContext.getAttribute(eq("javax.servlet.context.tempdir"))).thenReturn(getTempRoot());
        
        new HTTPCache("cache", mockContext, 500, 0, 10, true);
    }

    @Test
    public void testCacheableRequest() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);
        
        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));
        
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    private String createRequestURI() {
        return "http://www.foo.com/" + name.getMethodName() + ".bar";
    }

    @Test
    public void testCacheableRequestWithParameters() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        parameters.put("foo", Collections.singletonList("bar"));
        parameters.put("params", Arrays.asList("une", "due", "tres"));
        CacheRequest request = configureRequest(mock(CacheRequest.class), "GET", createRequestURI(), parameters, null);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class, "MY-RESOLVER-1");
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testCacheablePersistentRepeatedRequest() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class, "MY-RESOLVER-2");
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();

        // Reset
        result.reset();
        reset(response);
        when(response.getOutputStream()).thenReturn(result);

        // Test request again, make sure resolve is executed exactly once
        HTTPCache cache2 = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);
        cache2.doCached(request, response, resolver);

        // Test that second response is equal to first
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testCacheableRepeatedRequest() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class, "MY-RESOLVER-3");
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();

        // Reset
        result.reset();
        reset(response);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);

        // Test request again, make sure resolve is executed exactly once
        cache.doCached(request, response, resolver);

        // Test that second response is equal to first
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testNonCacheableRequestHeader() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), "GET", createRequestURI(), null, Collections.singletonMap("Cache-Control", Collections.singletonList("no-store")));

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // TODO: How do we know that the response was NOT cached?
        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testNonCacheableRequestHeaderRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);
        String requestURI = createRequestURI();

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), requestURI);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);


        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        reset(response, resolver);

        // Reconfigure
        request = configureRequest(request, "GET", requestURI, null, Collections.singletonMap("Cache-Control", Collections.singletonList("no-cache")));
        when(response.getOutputStream()).thenReturn(result);
        doAnswer(new ResolveAnswer(value)).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
        value[3] = 'B'; // Make sure we have a different value second time

        // This request should not be cached
        cache.doCached(request, response, resolver);

         // Verify that second response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testNonCacheableResponseHeader() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, Collections.singletonMap("Cache-Control", Collections.singletonList("no-cache"))))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader(eq("Cache-Control"), eq("no-cache"));
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testNonCacheableResponseHeaderRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, Collections.singletonMap("Cache-Control", Collections.singletonList("no-store"))))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader(eq("Cache-Control"), eq("no-store"));
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        reset(response, resolver);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, Collections.singletonMap("Cache-Control", Collections.singletonList("no-store"))))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        value[3] = 'B';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader(eq("Cache-Control"), eq("no-store"));
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    // Test non-cacheable response
    @Test
    public void testNonCacheableResponse() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, value, null))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    // Test non-cacheable response
    @Test
    public void testNonCacheableResponseRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, value, null))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        reset(response, resolver);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, value, null))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
        value[3] = 'B';

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        // Verify new resolve
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    // Test that request headers are forwarded to resolver...
    @Test
    public void testRequestHeadersForwarded() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        headers.put("Cache-Control", Arrays.asList("no-cache"));
        headers.put("X-Custom", Arrays.asList("FOO", "BAR"));

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), "HEAD", createRequestURI(), null, headers);
        CacheResponse response = mock(CacheResponse.class);

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CacheRequest req = (CacheRequest) invocation.getArguments()[0];

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
        }).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that custom resolve was invoked
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    // Test that response headers are preserved
    @Test
    public void testCacheablePreserveResponseHeaders() throws IOException, CacheException {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());
        
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        final byte[] value = "foobar".getBytes("UTF-8");

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CacheResponse res = (CacheResponse) invocation.getArguments()[1];

                res.setStatus(HttpServletResponse.SC_OK);
                res.setHeader("Date", HTTPUtil.formatHTTPDate(System.currentTimeMillis()));
                res.setHeader("Cache-Control", "public");
                res.addHeader("X-Custom", "FOO");
                res.addHeader("X-Custom", "BAR");
                res.getOutputStream().write(value);

                return null;
            }
        }).when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response).setHeader(eq("Cache-Control"), eq("public"));

        InOrder ordered = inOrder(response);
        // TODO: This test is to fragile, as it relies on internal knowledge of the code tested
        // (it doesn't really matter if first invocation is setHeader or addHeader)
        // See if we can create a custom VerificationMode for "either/or"
        ordered.verify(response).setHeader(eq("X-Custom"), eq("FOO"));
        ordered.verify(response).addHeader(eq("X-Custom"), eq("BAR"));

        verify(response, atLeastOnce()).getOutputStream();
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    // Test Vary
    @Test
    public void testVaryMissingRequestHeader() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        byte[] value = "foobar".getBytes("UTF-8");

        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HTTPCache.HEADER_CONTENT_TYPE, Arrays.asList("x-foo/bar"));
        headers.put("Vary", Arrays.asList("X-Foo"));
        headers.put("X-Foo", Arrays.asList("foobar"));
        headers.put("X-Other", Arrays.asList("don't care"));

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");

        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        reset(response, resolver);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");

        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void testVaryMissingRequestHeaderRepeated() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        String requestURI = createRequestURI();
        CacheRequest request = configureRequest(mock(CacheRequest.class), requestURI);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        byte[] value = "foobar".getBytes("UTF-8");

        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HTTPCache.HEADER_CONTENT_TYPE, Arrays.asList("x-foo/bar"));
        headers.put("Vary", Arrays.asList("X-Foo"));
        headers.put("X-Other", Arrays.asList("don't care"));

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");

        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        request = configureRequest(request, "GET", requestURI, null, Collections.singletonMap("X-Foo", Collections.singletonList("foobar")));
        reset(response, resolver);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);

        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
        value[3] = 'B';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");

        // Different X-Foo header, must re-validate
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }
  
    @Test
    public void testVarySameResourceIsCached() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), "GET", createRequestURI(), null, Collections.singletonMap("X-Foo", Collections.singletonList("foobar value")));

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        byte[] value = "foobar".getBytes("UTF-8");

        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HTTPCache.HEADER_CONTENT_TYPE, Arrays.asList("x-foo/bar"));
        headers.put("Vary", Arrays.asList("X-Foo"));
        headers.put("X-Other", Arrays.asList("don't care"));

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");
        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        reset(response);
        result.reset();

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");

        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void testVaryDifferentResources() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        String requestURI = createRequestURI();
        CacheRequest request = configureRequest(mock(CacheRequest.class), requestURI);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        byte[] value = "foobar".getBytes("UTF-8");

        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HTTPCache.HEADER_CONTENT_TYPE, Arrays.asList("x-foo/bar"));
        headers.put("Vary", Arrays.asList("X-Foo"));
        headers.put("X-Other", Arrays.asList("don't care"));

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");

        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        request = configureRequest(request, "GET", requestURI, null, Collections.singletonMap("X-Foo", Collections.singletonList("bar")));
        reset(response, resolver);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);
        headers.put("Cache-Control", Arrays.asList("no-store"));
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        value[0] = 'b';
        value[1] = 'a';
        value[2] = 'r';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "X-Foo");
        verify(response, atLeastOnce()).setHeader("Cache-Control", "no-store");

        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Test
    public void testVaryStarNonCached() throws Exception {
        HTTPCache cache = new HTTPCache(getTempRoot(), 60000, 1024 * 1024, 10, true);

        // Custom setup
        CacheRequest request = configureRequest(mock(CacheRequest.class), createRequestURI());

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        CacheResponse response = mock(CacheResponse.class);
        when(response.getOutputStream()).thenReturn(result);

        byte[] value = "foobar".getBytes("UTF-8");

        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(HTTPCache.HEADER_CONTENT_TYPE, Arrays.asList("x-foo/bar"));
        headers.put("Vary", Arrays.asList("*"));

        ResponseResolver resolver = mock(ResponseResolver.class);
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Do the invocation
        cache.doCached(request, response, resolver);

        // Verify that reponse is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "*");

        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        // Reset
        result.reset();
        reset(response, resolver);

        // Reconfigure
        when(response.getOutputStream()).thenReturn(result);
        headers.put("Cache-Control", Arrays.asList("no-store"));
        doAnswer(new ResolveAnswer(HttpServletResponse.SC_OK, value, headers))
                .when(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));

        value[3] = 'B';

        // Repeat invocation
        cache.doCached(request, response, resolver);

        // Verify that response is ok
        assertEquals(value.length, result.size());
        assertEquals(new String(value, "UTF-8"), new String(result.toByteArray(), "UTF-8"));
        assertTrue(Arrays.equals(value, result.toByteArray()));

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response, atLeastOnce()).getOutputStream();
        verify(response, atLeastOnce()).setHeader(eq("Date"), anyString());
        verify(response, atLeastOnce()).setHeader("Vary", "*");

        verify(resolver).resolve(any(CacheRequest.class), any(CacheResponse.class));
    }

    @Ignore("TODO")
    @Test
    public void testVaryVariations() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testVariationsWithSameContentType() {
        // I believe there is a bug if two variations has same content type...
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    // Test failing request (IOException)
    public void testIOException() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testCacheException() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testRuntimeException() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    // Test failing (negative) HTTP response (401, 404, 410, 500, etc)
    public void testNegativeCache() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testNegativeCacheExpires() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    // Test If-None-Match/ETag support
    public void testIfNoneMatch() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    // Test If-Modified-Since support
    public void testIfModifiedSince() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    // Test that data really expires when TTL is over
    public void testTimeToLive() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testMaxAgeRequest() {
        fail("TODO");    
    }

    @Ignore("TODO")
    @Test
    // Test that for requests with authorization, responses are not shared between different authorized users, unless response is marked as Cache-Control: public
    public void testAuthorizedRequestPublic() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testAuthorizedRequestPrivate() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    public void testPutPostDeleteInvalidatesCache() {
        fail("TODO");
    }

    @Ignore("TODO")
    @Test
    // TODO: Move out to separate package/test, just keep it here for PoC
    public void testClientRequest() {
        fail("Not implemented");
    }

    @Ignore("TODO")
    @Test
    // TODO: Move out to separate package/test, just keep it here for PoC
    public void testServletRequest() {
        fail("Not implemented");
    }

    private static class ResolveAnswer implements Answer<Void> {
        private final int status;
        private final byte[] value;
        private final Map<String, List<String>> headers;

        public ResolveAnswer(byte[] value) {
            this(HttpServletResponse.SC_OK, value, null);
        }

        public ResolveAnswer(final int status, byte[] value, Map<String, List<String>> headers) {
            this.status = status;
            this.value = value;
            this.headers = headers != null ? headers : Collections.<String, List<String>>emptyMap();
        }

        public Void answer(InvocationOnMock invocation) throws Throwable {
            CacheResponse res = (CacheResponse) invocation.getArguments()[1];

            res.setStatus(status);
            res.setHeader("Date", HTTPUtil.formatHTTPDate(System.currentTimeMillis()));

            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                for (String value : header.getValue()) {
                    res.addHeader(header.getKey(), value);
                }
            }
            
            res.getOutputStream().write(value);

            return null;
        }
    }
}
