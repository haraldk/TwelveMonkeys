/*
 * Copyright (c) 2013, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import java.util.Enumeration;
import java.util.Iterator;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ServletAttributesMap
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ServletAttributesMap.java,v 1.0 01.03.13 10:34 haraldk Exp$
 */
class ServletAttributesMapAdapter extends AbstractServletMapAdapter<Object> {
    private final ServletContext context;
    private final ServletRequest request;

    ServletAttributesMapAdapter(final ServletContext context) {
        this(notNull(context), null);
    }

    ServletAttributesMapAdapter(final ServletRequest request) {
        this(null, notNull(request));
    }

    private ServletAttributesMapAdapter(final ServletContext context, final ServletRequest request) {
        this.context = context;
        this.request = request;
    }

    @SuppressWarnings("unchecked")
    private Enumeration<String> getAttributeNames() {
        return context != null ? context.getAttributeNames() : request.getAttributeNames();
    }

    private Object getAttribute(final String name) {
        return context != null ? context.getAttribute(name) : request.getAttribute(name);
    }

    private Object setAttribute(String name, Object value) {
        Object oldValue = getAttribute(name);

        if (context != null) {
            context.setAttribute(name, value);
        }
        else {
            request.setAttribute(name, value);
        }

        return oldValue;
    }

    private Object removeAttribute(String name) {
        Object oldValue = getAttribute(name);

        if (context != null) {
            context.removeAttribute(name);
        }
        else {
            request.removeAttribute(name);
        }

        return oldValue;
    }

    @Override
    protected Iterator<String> keysImpl() {
        final Enumeration<String> keys = getAttributeNames();
        return new Iterator<String>() {
            private String key;

            public boolean hasNext() {
                return keys.hasMoreElements();
            }

            public String next() {
                key = keys.nextElement();
                return key;
            }

            public void remove() {
                // Support removal of attribute through key iterator
                removeAttribute(key);
            }
        };

    }

    @Override
    protected Object valueImpl(String pName) {
        return getAttribute(pName);
    }

    @Override
    public Object put(String key, Object value) {
        return setAttribute(key, value);
    }

    @Override
    public Object remove(Object key) {
        return key instanceof String ? removeAttribute((String) key) : null;
    }
}
