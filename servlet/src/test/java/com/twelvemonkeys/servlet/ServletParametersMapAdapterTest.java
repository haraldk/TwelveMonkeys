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
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletParametersMapAdapterTestCase.java#1 $
 */
public class ServletParametersMapAdapterTest extends MapAbstractTestCase {
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
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterNames()).thenAnswer(returnEnumeration(Collections.emptyList()));

        return new ServletParametersMapAdapter(request);
    }

    @Override
    public Map makeFullMap() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getParameterNames()).thenAnswer(returnEnumeration(Arrays.asList("tag", "date", "foo")));
        when(request.getParameterValues("date")).thenReturn(PARAM_VALUE_DATE.toArray(new String[PARAM_VALUE_DATE.size()]));
        when(request.getParameterValues("tag")).thenReturn(PARAM_VALUE_ETAG.toArray(new String[PARAM_VALUE_ETAG.size()]));
        when(request.getParameterValues("foo")).thenReturn(PARAM_VALUE_FOO.toArray(new String[PARAM_VALUE_FOO.size()]));

        return new ServletParametersMapAdapter(request);
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