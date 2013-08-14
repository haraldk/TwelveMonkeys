package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.CollectionUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ServletHeadersMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ServletHeadersMapAdapter.java#1 $
 */
class ServletHeadersMapAdapter extends AbstractServletMapAdapter<List<String>> {

    protected final HttpServletRequest request;

    public ServletHeadersMapAdapter(final HttpServletRequest pRequest) {
        request = notNull(pRequest, "request");
    }

    protected List<String> valueImpl(final String pName) {
        @SuppressWarnings("unchecked")
        Enumeration<String> headers = request.getHeaders(pName);
        return headers == null ? null : toList(CollectionUtil.iterator(headers));
    }

    private static List<String> toList(final Iterator<String> pValues) {
        List<String> list = new ArrayList<String>();
        CollectionUtil.addAll(list, pValues);
        return Collections.unmodifiableList(list);
    }

    protected Iterator<String> keysImpl() {
        @SuppressWarnings("unchecked")
        Enumeration<String> headerNames = request.getHeaderNames();
        return headerNames == null ? null : CollectionUtil.iterator(headerNames);
    }
}
