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
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletParametersMapAdapterTestCase.java#1 $
 */
public class ServletParametersMapAdapterTestCase extends MapAbstractTestCase {
    private static final List<String> PARAM_VALUE_ETAG = Arrays.asList("\"1234567890abcdef\"");
    private static final List<String> PARAM_VALUE_DATE = Arrays.asList(new Date().toString());
    private static final List<String> PARAM_VALUE_FOO = Arrays.asList("one", "two");

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
        mockRequest.stubs().method("getParameterNames").will(returnValue(Collections.enumeration(Collections.emptyList())));
        mockRequest.stubs().method("getParameterValues").will(returnValue(null));

        return new SerlvetParametersMapAdapter((HttpServletRequest) mockRequest.proxy());
    }

    @Override
    public Map makeFullMap() {
        Mock mockRequest = mock(HttpServletRequest.class);

        mockRequest.stubs().method("getParameterNames").will(returnEnumeration("tag", "date", "foo"));
        mockRequest.stubs().method("getParameterValues").with(eq("date")).will(returnValue(PARAM_VALUE_DATE.toArray(new String[PARAM_VALUE_DATE.size()])));
        mockRequest.stubs().method("getParameterValues").with(eq("tag")).will(returnValue(PARAM_VALUE_ETAG.toArray(new String[PARAM_VALUE_ETAG.size()])));
        mockRequest.stubs().method("getParameterValues").with(eq("foo")).will(returnValue(PARAM_VALUE_FOO.toArray(new String[PARAM_VALUE_FOO.size()])));
        mockRequest.stubs().method("getParameterValues").with(not(or(eq("date"), or(eq("tag"), eq("foo"))))).will(returnValue(null));

        return new SerlvetParametersMapAdapter((HttpServletRequest) mockRequest.proxy());
    }

    @Override
    public Object[] getSampleKeys() {
        return new String[] {"date", "tag", "foo"};
    }

    @Override
    public Object[] getSampleValues() {
        return new Object[] {PARAM_VALUE_DATE, PARAM_VALUE_ETAG, PARAM_VALUE_FOO};
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