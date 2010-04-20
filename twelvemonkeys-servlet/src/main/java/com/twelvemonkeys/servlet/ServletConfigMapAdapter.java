/*
 * Copyright (c) 2008, Harald Kuhr
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

import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.Serializable;
import java.util.*;

/**
 * {@code ServletConfig} or {@code FilterConfig} adapter, that implements
 * the {@code Map} interface for interoperability with collection-based API's.
 * <p/>
 * This {@code Map} is not synchronized.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/ServletConfigMapAdapter.java#2 $
 */
class ServletConfigMapAdapter extends AbstractMap<String, String> implements Map<String, String>, Serializable, Cloneable {

    enum ConfigType {
        ServletConfig, FilterConfig, ServletContext
    }

//    private final boolean mIsServlet;
    private final ConfigType mType;

    private final ServletConfig mServletConfig;
    private final FilterConfig mFilterConfig;
    private final ServletContext mServletContext;

    // Cache the entry set
    private transient Set<Entry<String, String>> mEntrySet;

    public ServletConfigMapAdapter(ServletConfig pConfig) {
        this(pConfig, ConfigType.ServletConfig);
    }

    public ServletConfigMapAdapter(FilterConfig pConfig) {
        this(pConfig, ConfigType.FilterConfig);
    }

    public ServletConfigMapAdapter(ServletContext pContext) {
        this(pContext, ConfigType.ServletContext);
    }

    private ServletConfigMapAdapter(Object pConfig, ConfigType pType) {
        if (pConfig == null) {
            // Could happen of client code invokes with null reference
            throw new IllegalArgumentException("Config == null");
        }

        mType = pType;

        switch (mType) {
            case ServletConfig:
                mServletConfig = (ServletConfig) pConfig;
                mFilterConfig = null;
                mServletContext = null;
                break;
            case FilterConfig:
                mServletConfig = null;
                mFilterConfig = (FilterConfig) pConfig;
                mServletContext = null;
                break;
            case ServletContext:
                mServletConfig = null;
                mFilterConfig = null;
                mServletContext = (ServletContext) pConfig;
                break;
            default:
                throw new IllegalArgumentException("Wrong type: " + pType);
        }
    }

    /**
     * Gets the servlet or filter name from the config.
     *
     * @return the servlet or filter name
     */
    public final String getName() {
        switch (mType) {
            case ServletConfig:
                return mServletConfig.getServletName();
            case FilterConfig:
                return mFilterConfig.getFilterName();
            case ServletContext:
                return mServletContext.getServletContextName();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Gets the servlet context from the config.
     *
     * @return the servlet context
     */
    public final ServletContext getServletContext() {
        switch (mType) {
            case ServletConfig:
                return mServletConfig.getServletContext();
            case FilterConfig:
                return mFilterConfig.getServletContext();
            case ServletContext:
                return mServletContext;
            default:
                throw new IllegalStateException();
        }
    }

    public final Enumeration getInitParameterNames() {
        switch (mType) {
            case ServletConfig:
                return mServletConfig.getInitParameterNames();
            case FilterConfig:
                return mFilterConfig.getInitParameterNames();
            case ServletContext:
                return mServletContext.getInitParameterNames();
            default:
                throw new IllegalStateException();
        }
    }

    public final String getInitParameter(final String pName) {
        switch (mType) {
            case ServletConfig:
                return mServletConfig.getInitParameter(pName);
            case FilterConfig:
                return mFilterConfig.getInitParameter(pName);
            case ServletContext:
                return mServletContext.getInitParameter(pName);
            default:
                throw new IllegalStateException();
        }
    }

    public Set<Entry<String, String>> entrySet() {
        if (mEntrySet == null) {
            mEntrySet = createEntrySet();
        }
        return mEntrySet;
    }

    private Set<Entry<String, String>> createEntrySet() {
        return new AbstractSet<Entry<String, String>>() {
            // Cache size, if requested, -1 means not calculated
            private int mSize = -1;

            public Iterator<Entry<String, String>> iterator() {
                return new Iterator<Entry<String, String>>() {
                    // Iterator is backed by initParameterNames enumeration
                    final Enumeration mNames = getInitParameterNames();

                    public boolean hasNext() {
                        return mNames.hasMoreElements();
                    }

                    public Entry<String, String> next() {
                        final String key = (String) mNames.nextElement();
                        return new Entry<String, String>() {
                            public String getKey() {
                                return key;
                            }

                            public String getValue() {
                                return get(key);
                            }

                            public String setValue(String pValue) {
                                throw new UnsupportedOperationException();
                            }

                            // NOTE: Override equals
                            public boolean equals(Object pOther) {
                                if (!(pOther instanceof Map.Entry)) {
                                    return false;
                                }

                                Map.Entry e = (Map.Entry) pOther;
                                Object value = get(key);
                                Object rKey = e.getKey();
                                Object rValue = e.getValue();
                                return (key == null ? rKey == null : key.equals(rKey))
                                        && (value == null ? rValue == null : value.equals(rValue));
                            }

                            // NOTE: Override hashCode to keep the map's
                            // hashCode constant and compatible
                            public int hashCode() {
                                Object value = get(key);
                                return ((key == null) ? 0 : key.hashCode()) ^
                                        ((value == null) ? 0 : value.hashCode());
                            }

                            public String toString() {
                                return key + "=" + get(key);
                            }
                        };
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            public int size() {
                if (mSize < 0) {
                    mSize = calculateSize();
                }

                return mSize;
            }

            private int calculateSize() {
                final Enumeration names = getInitParameterNames();

                int size = 0;
                while (names.hasMoreElements()) {
                    size++;
                    names.nextElement();
                }

                return size;
            }
        };
    }

    public String get(Object pKey) {
        return getInitParameter(StringUtil.valueOf(pKey));
    }

    /// Unsupported Map methods
    @Override
    public String put(String pKey, String pValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object pKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map pMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}