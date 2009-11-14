package com.twelvemonkeys.imageio.metadata;

import com.twelvemonkeys.lang.Validate;

import java.lang.reflect.Array;

/**
 * AbstractEntry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractEntry.java,v 1.0 Nov 12, 2009 12:43:13 AM haraldk Exp$
 */
public abstract class AbstractEntry implements Entry {

    private final Object mIdentifier;
    private final Object mValue; // TODO: Might need to be mutable..

    protected AbstractEntry(final Object pIdentifier, final Object pValue) {
        Validate.notNull(pIdentifier, "identifier");

        mIdentifier = pIdentifier;
        mValue = pValue;
    }

    public Object getIdentifier() {
        return mIdentifier;
    }

    /**
     * Returns {@code null}, meaning unknown or undefined.
     *
     * @return {@code null}.
     */
    public String getFieldName() {
        return null;
    }

    public Object getValue() {
        return mValue;
    }

    public String getValueAsString() {
        return String.valueOf(mValue);
    }

    public String getTypeName() {
        if (mValue == null) {
            return null;
        }

        return mValue.getClass().getSimpleName();
    }

    public int valueCount() {
        // TODO: Collection support?
        if (mValue != null && mValue.getClass().isArray()) {
            return Array.getLength(mValue);
        }

        return 1;
    }


    /// Object


    @Override
    public int hashCode() {
        return mIdentifier.hashCode() + 31 * mValue.hashCode();
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (!(pOther instanceof AbstractEntry)) {
            return false;
        }

        AbstractEntry other = (AbstractEntry) pOther;
        
        return mIdentifier.equals(other.mIdentifier) && (
                mValue == null && other.mValue == null || mValue != null && mValue.equals(other.mValue)
        );
    }

    @Override
    public String toString() {
        String name = getFieldName();
        String nameStr = name != null ? "/" + name + "" : "";

        String type = getTypeName();
        String typeStr = type != null ? " (" + type + ")" : "";

        return String.format("%s%s: %s%s", getIdentifier(), nameStr, getValueAsString(), typeStr);
    }
}
