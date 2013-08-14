package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.MapAbstractTestCase;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.mockito.Mockito.when;

/**
 * ServletConfigMapAdapterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ServletHeadersMapAdapterTestCase.java#1 $
 */
public class ServletHeadersMapAdapterTest extends MapAbstractTestCase {
    private static final List<String> HEADER_VALUE_ETAG = Arrays.asList("\"1234567890abcdef\"");
    private static final List<String> HEADER_VALUE_DATE = Arrays.asList(new Date().toString());
    private static final List<String> HEADER_VALUE_FOO = Arrays.asList("one", "two");

    public boolean isPutAddSupported() {
        return false;
    }

    public boolean isPutChangeSupported() {
        return false;
    }

    public boolean isRemoveSupported() {
        return false;
    }

    public boolean isSetValueSupported() {
        return false;
    }

    @Override
    public boolean isTestSerialization() {
        return false;
    }

    public Map makeEmptyMap() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenAnswer(returnEnumeration(Collections.emptyList()));

        return new ServletHeadersMapAdapter(request);
    }

    @Override
    public Map makeFullMap() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenAnswer(returnEnumeration(Arrays.asList(getSampleKeys())));
        when(request.getHeaders("Date")).thenAnswer(returnEnumeration(HEADER_VALUE_DATE));
        when(request.getHeaders("ETag")).thenAnswer(returnEnumeration(HEADER_VALUE_ETAG));
        when(request.getHeaders("X-Foo")).thenAnswer(returnEnumeration(HEADER_VALUE_FOO));

        return new ServletHeadersMapAdapter(request);
    }

    @Override
    public Object[] getSampleKeys() {
        return new String[] {"Date", "ETag", "X-Foo"};
    }

    @Override
    public Object[] getSampleValues() {
        return new Object[] {HEADER_VALUE_DATE, HEADER_VALUE_ETAG, HEADER_VALUE_FOO};
    }

    @Override
    public Object[] getNewSampleValues() {
        // Needs to be same length but different values
        return new Object[3];
    }

    protected static <T> ReturnNewEnumeration<T> returnEnumeration(final Collection<T> collection) {
        return new ReturnNewEnumeration<T>(collection);
    }

    private static class ReturnNewEnumeration<T> implements Answer<Enumeration<T>> {
        private final Collection<T> collection;

        private ReturnNewEnumeration(final Collection<T> collection) {
            this.collection = collection;
        }

        public Enumeration<T> answer(InvocationOnMock invocation) throws Throwable {
            return Collections.enumeration(collection);
        }
    }
}
