package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.CollectionUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Enumeration;

/**
 * ServletParametersMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/ServletParametersMapAdapter.java#1 $
 */
class ServletParametersMapAdapter extends AbstractServletMapAdapter {

    protected final HttpServletRequest mRequest;

    public ServletParametersMapAdapter(HttpServletRequest pRequest) {
        if (pRequest == null) {
            throw new IllegalArgumentException("request == null");
        }
        mRequest = pRequest;
    }

    protected Iterator<String> valuesImpl(String pName) {
        String[] values = mRequest.getParameterValues(pName);
        return values == null ? null : CollectionUtil.iterator(values);
    }

    protected Iterator<String> keysImpl() {
        //noinspection unchecked
        Enumeration<String> names = mRequest.getParameterNames();
        return names == null ? null : CollectionUtil.iterator(names);
    }

}