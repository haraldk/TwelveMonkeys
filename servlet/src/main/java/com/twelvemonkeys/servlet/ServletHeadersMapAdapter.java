package com.twelvemonkeys.servlet;

import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.CollectionUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * ServletHeadersMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/ServletHeadersMapAdapter.java#1 $
 */
class ServletHeadersMapAdapter extends AbstractServletMapAdapter {

    protected final HttpServletRequest request;

    public ServletHeadersMapAdapter(HttpServletRequest pRequest) {
        request = Validate.notNull(pRequest, "request");
    }

    protected Iterator<String> valuesImpl(String pName) {
        //noinspection unchecked
        Enumeration<String> headers = request.getHeaders(pName);
        return headers == null ? null : CollectionUtil.iterator(headers);
    }

    protected Iterator<String> keysImpl() {
        //noinspection unchecked
        Enumeration<String> headerNames = request.getHeaderNames();
        return headerNames == null ? null : CollectionUtil.iterator(headerNames);
    }

}
