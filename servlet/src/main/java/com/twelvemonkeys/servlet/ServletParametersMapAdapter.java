package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.CollectionUtil;

import javax.servlet.ServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ServletParametersMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ServletParametersMapAdapter.java#1 $
 */
class ServletParametersMapAdapter extends AbstractServletMapAdapter<List<String>> {
    // TODO: Be able to piggyback on HttpServletRequest.getParameterMap when available?

    protected final ServletRequest request;

    public ServletParametersMapAdapter(final ServletRequest pRequest) {
        request = notNull(pRequest, "request");
    }

    protected List<String> valueImpl(String pName) {
        String[] values = request.getParameterValues(pName);
        return values == null ? null : Arrays.asList(values);
    }

    protected Iterator<String> keysImpl() {
        @SuppressWarnings("unchecked")
        Enumeration<String> names = request.getParameterNames();
        return names == null ? null : CollectionUtil.iterator(names);
    }
}