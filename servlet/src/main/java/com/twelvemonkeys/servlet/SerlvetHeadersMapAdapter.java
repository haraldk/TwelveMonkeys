package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.CollectionUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * HeaderMap
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/SerlvetHeadersMapAdapter.java#1 $
 */
class SerlvetHeadersMapAdapter extends AbstractServletMapAdapter {

    protected final HttpServletRequest mRequest;

    public SerlvetHeadersMapAdapter(HttpServletRequest pRequest) {
        if (pRequest == null) {
            throw new IllegalArgumentException("request == null");
        }
        mRequest = pRequest;
    }


    protected Iterator<String> valuesImpl(String pName) {
        //noinspection unchecked
        Enumeration<String> headers = mRequest.getHeaders(pName);
        return headers == null ? null : CollectionUtil.iterator(headers);
    }

    protected Iterator<String> keysImpl() {
        //noinspection unchecked
        Enumeration<String> headerNames = mRequest.getHeaderNames();
        return headerNames == null ? null : CollectionUtil.iterator(headerNames);
    }

}
