package com.twelvemonkeys.servlet;

import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.CollectionUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * ServletParametersMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ServletParametersMapAdapter.java#1 $
 */
class ServletParametersMapAdapter extends AbstractServletMapAdapter {

    protected final HttpServletRequest request;

    public ServletParametersMapAdapter(HttpServletRequest pRequest) {
        request = Validate.notNull(pRequest, "request");
    }

    protected Iterator<String> valuesImpl(String pName) {
        String[] values = request.getParameterValues(pName);
        return values == null ? null : CollectionUtil.iterator(values);
    }

    protected Iterator<String> keysImpl() {
        //noinspection unchecked
        Enumeration<String> names = request.getParameterNames();
        return names == null ? null : CollectionUtil.iterator(names);
    }

}