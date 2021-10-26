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

import com.twelvemonkeys.util.MapAbstractTest;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.mockito.Mockito.when;

/**
 * ServletConfigMapAdapterTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletParametersMapAdapterTest.java#1 $
 */
public class ServletParametersMapAdapterTest extends MapAbstractTest {
    private static final List<String> PARAM_VALUE_ETAG = Collections.singletonList("\"1234567890abcdef\"");
    private static final List<String> PARAM_VALUE_DATE = Collections.singletonList(new Date().toString());
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
        return new ReturnNewEnumeration<>(collection);
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
