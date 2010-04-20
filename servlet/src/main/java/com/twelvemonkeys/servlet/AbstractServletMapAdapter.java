package com.twelvemonkeys.servlet;

import com.twelvemonkeys.util.CollectionUtil;

import java.util.*;

/**
 * AbstractServletMapAdapter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/AbstractServletMapAdapter.java#1 $
 */
abstract class AbstractServletMapAdapter extends AbstractMap<String, List<String>> {
    // TODO: This map is now a little too lazy.. Should cache entries too (instead?) !

    private final static List<String> NULL_LIST = new ArrayList<String>();

    private transient Map<String, List<String>> mCache = new HashMap<String, List<String>>();
    private transient int mSize = -1;
    private transient AbstractSet<Entry<String, List<String>>> mEntries;

    protected abstract Iterator<String> keysImpl();

    protected abstract Iterator<String> valuesImpl(String pName);

    @Override
    public List<String> get(Object pKey) {
        if (pKey instanceof String) {
            return getValues((String) pKey);
        }
        return null;
    }

    private List<String> getValues(String pName) {
        List<String> values = mCache.get(pName);

        if (values == null) {
            //noinspection unchecked
            Iterator<String> headers = valuesImpl(pName);
            if (headers == null) {
                mCache.put(pName, NULL_LIST);
            }
            else {
                values = toList(headers);
                mCache.put(pName, values);
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
        if (mSize == -1) {
            computeSize();
        }
        return mSize;
    }

    private void computeSize() {
        Iterator<String> names = keysImpl();
        mSize = 0;
        for (;names.hasNext(); names.next()) {
            mSize++;
        }
    }

    public Set<Entry<String, List<String>>> entrySet() {
        if (mEntries == null) {
            mEntries = new AbstractSet<Entry<String, List<String>>>() {
                public Iterator<Entry<String, List<String>>> iterator() {
                    return new Iterator<Entry<String, List<String>>>() {
                        Iterator<String> mHeaderNames = keysImpl();

                        public boolean hasNext() {
                            return mHeaderNames.hasNext();
                        }

                        public Entry<String, List<String>> next() {
                            // TODO: Replace with cached lookup
                            return new HeaderEntry(mHeaderNames.next());
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

        return mEntries;
    }

    private class HeaderEntry implements Entry<String, List<String>> {
        String mHeaderName;

        public HeaderEntry(String pHeaderName) {
            mHeaderName = pHeaderName;
        }

        public String getKey() {
            return mHeaderName;
        }

        public List<String> getValue() {
            return get(mHeaderName);
        }

        public List<String> setValue(List<String> pValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            List<String> value;
            return (mHeaderName   == null ? 0 :   mHeaderName.hashCode()) ^
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
