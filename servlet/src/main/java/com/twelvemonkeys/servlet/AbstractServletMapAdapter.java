package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.CollectionUtil;

import java.util.*;

/**
 * AbstractServletMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: AbstractServletMapAdapter.java#1 $
 */
abstract class AbstractServletMapAdapter extends AbstractMap<String, List<String>> {
    // TODO: This map is now a little too lazy.. Should cache entries too (instead?) !

    private final static List<String> NULL_LIST = new ArrayList<String>();

    private transient Map<String, List<String>> cache = new HashMap<String, List<String>>();
    private transient int size = -1;
    private transient AbstractSet<Entry<String, List<String>>> entries;

    protected abstract Iterator<String> keysImpl();

    protected abstract Iterator<String> valuesImpl(String pName);

    @Override
    public List<String> get(final Object pKey) {
        if (pKey instanceof String) {
            return getValues((String) pKey);
        }

        return null;
    }

    private List<String> getValues(final String pName) {
        List<String> values = cache.get(pName);

        if (values == null) {
            //noinspection unchecked
            Iterator<String> headers = valuesImpl(pName);

            if (headers == null) {
                cache.put(pName, NULL_LIST);
            }
            else {
                values = toList(headers);
                cache.put(pName, values);
            }
        }

        return values == NULL_LIST ? null : values;
    }

    private static List<String> toList(final Iterator<String> pValues) {
        List<String> list = new ArrayList<String>();
        CollectionUtil.addAll(list, pValues);
        return Collections.unmodifiableList(list);
    }

    @Override
    public int size() {
        if (size == -1) {
            computeSize();
        }

        return size;
    }

    private void computeSize() {
        size = 0;

        for (Iterator<String> names = keysImpl(); names.hasNext(); names.next()) {
            size++;
        }
    }

    public Set<Entry<String, List<String>>> entrySet() {
        if (entries == null) {
            entries = new AbstractSet<Entry<String, List<String>>>() {
                public Iterator<Entry<String, List<String>>> iterator() {
                    return new Iterator<Entry<String, List<String>>>() {
                        Iterator<String> headerNames = keysImpl();

                        public boolean hasNext() {
                            return headerNames.hasNext();
                        }

                        public Entry<String, List<String>> next() {
                            // TODO: Replace with cached lookup
                            return new HeaderEntry(headerNames.next());
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
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

    private class HeaderEntry implements Entry<String, List<String>> {
        String headerName;

        public HeaderEntry(String pHeaderName) {
            headerName = pHeaderName;
        }

        public String getKey() {
            return headerName;
        }

        public List<String> getValue() {
            return get(headerName);
        }

        public List<String> setValue(List<String> pValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            List<String> value;
            return (headerName == null ? 0 :   headerName.hashCode()) ^
                   ((value = getValue()) == null ? 0 : value.hashCode());
        }

        @Override
        public boolean equals(Object pOther) {
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
