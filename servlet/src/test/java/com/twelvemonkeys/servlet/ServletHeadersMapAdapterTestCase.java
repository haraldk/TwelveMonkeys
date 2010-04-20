package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.MapAbstractTestCase;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;
import org.jmock.core.stub.CustomStub;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * ServletConfigMapAdapterTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletHeadersMapAdapterTestCase.java#1 $
 */
public class ServletHeadersMapAdapterTestCase extends MapAbstractTestCase {
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
        Mock mockRequest = mock(HttpServletRequest.class);
        mockRequest.stubs().method("getHeaderNames").will(returnValue(Collections.enumeration(Collections.emptyList())));
        mockRequest.stubs().method("getHeaders").will(returnValue(null));

        return new SerlvetHeadersMapAdapter((HttpServletRequest) mockRequest.proxy());
    }

    @Override
    public Map makeFullMap() {
        Mock mockRequest = mock(HttpServletRequest.class);

        mockRequest.stubs().method("getHeaderNames").will(returnEnumeration("ETag", "Date", "X-Foo"));
        mockRequest.stubs().method("getHeaders").with(eq("Date")).will(returnEnumeration(HEADER_VALUE_DATE));
        mockRequest.stubs().method("getHeaders").with(eq("ETag")).will(returnEnumeration(HEADER_VALUE_ETAG));
        mockRequest.stubs().method("getHeaders").with(eq("X-Foo")).will(returnEnumeration(HEADER_VALUE_FOO));
        mockRequest.stubs().method("getHeaders").with(not(or(eq("Date"), or(eq("ETag"), eq("X-Foo"))))).will(returnValue(null));

        return new SerlvetHeadersMapAdapter((HttpServletRequest) mockRequest.proxy());
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

    protected Stub returnEnumeration(final Object... pValues) {
        return new EnumerationStub(Arrays.asList(pValues));
    }

    protected Stub returnEnumeration(final List<?> pValues) {
        return new EnumerationStub(pValues);
    }

    private static class EnumerationStub extends CustomStub {
        private List<?> mValues;

        public EnumerationStub(final List<?> pValues) {
            super("Returns a new enumeration");
            mValues = pValues;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            return Collections.enumeration(mValues);
        }
    }
}