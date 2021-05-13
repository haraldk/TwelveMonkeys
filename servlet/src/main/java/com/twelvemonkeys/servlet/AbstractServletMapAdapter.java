/*
 * Copyright (c) 2009, Harald Kuhr
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * AbstractServletMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: AbstractServletMapAdapter.java#1 $
 */
abstract class AbstractServletMapAdapter<T> extends AbstractMap<String, T> {
    // TODO: This map is now a little too lazy.. Should cache entries!
    private transient Set<Entry<String, T>> entries;

    protected abstract Iterator<String> keysImpl();

    protected abstract T valueImpl(String pName);

    @Override
    public T get(final Object pKey) {
        if (pKey instanceof String) {
            return valueImpl((String) pKey);
        }

        return null;
    }

    @Override
    public int size() {
        // Avoid creating expensive entry set for computing size
        int size = 0;

        for (Iterator<String> names = keysImpl(); names.hasNext(); names.next()) {
            size++;
        }

        return size;
    }

    public Set<Entry<String, T>> entrySet() {
        if (entries == null) {
            entries = new AbstractSet<Entry<String, T>>() {
                public Iterator<Entry<String, T>> iterator() {
                    return new Iterator<Entry<String, T>>() {
                        Iterator<String> keys = keysImpl();

                        public boolean hasNext() {
                            return keys.hasNext();
                        }

                        public Entry<String, T> next() {
                            // TODO: Replace with cached lookup
                            return new HeaderEntry(keys.next());
                        }

                        public void remove() {
                            keys.remove();
                        }
                    };
                }

                public int size() {
                    return AbstractServletMapAdapter.this.size();
                }
            };
        }

        return entries;
    }

    private class HeaderEntry implements Entry<String, T> {
        final String key;

        public HeaderEntry(final String pKey) {
            key = pKey;
        }

        public String getKey() {
            return key;
        }

        public T getValue() {
            return get(key);
        }

        public T setValue(final T pValue) {
            // Write-through if supported
            return put(key, pValue);
        }

        @Override
        public int hashCode() {
            T value = getValue();
            return (key == null ? 0 : key.hashCode()) ^
                    (value == null ? 0 : value.hashCode());
        }

        @Override
        public boolean equals(final Object pOther) {
            if (pOther == this) {
                return true;
            }

            if (pOther instanceof Entry) {
                Entry other = (Entry) pOther;
                return ((other.getKey() == null && getKey() == null) ||
                        (getKey() != null && getKey().equals(other.getKey()))) &&
                        ((other.getValue() == null && getValue() == null) ||
                                (getValue() != null && getValue().equals(other.getValue())));
            }

            return false;
        }
    }
}
