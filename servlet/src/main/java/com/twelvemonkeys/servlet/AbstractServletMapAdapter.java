package com.twelvemonkeys.servlet;

import java.util.*;

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
