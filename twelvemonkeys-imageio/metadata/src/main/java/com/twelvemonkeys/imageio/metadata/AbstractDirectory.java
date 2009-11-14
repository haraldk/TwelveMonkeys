package com.twelvemonkeys.imageio.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * AbstractDirectory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractDirectory.java,v 1.0 Nov 11, 2009 5:31:04 PM haraldk Exp$
 */
public abstract class AbstractDirectory implements Directory {
    // A linked hashmap or a stable bag structure might also work..
    private final List<Entry> mEntries = new ArrayList<Entry>();

    protected AbstractDirectory(final Collection<? extends Entry> pEntries) {
        if (pEntries != null) {
            mEntries.addAll(pEntries);
        }
    }

    public Entry getEntryById(final Object pIdentifier) {
        for (Entry entry : this) {
            if (entry.getIdentifier().equals(pIdentifier)) {
                return entry;
            }
        }

        return null;
    }

    public Entry getEntryByName(final String pName) {
        for (Entry entry : this) {
            if (entry.getFieldName().equals(pName)) {
                return entry;
            }
        }

        return null;
    }

    public Iterator<Entry> iterator() {
        return mEntries.iterator();
    }

    /**
     * Throws {@code UnsupportedOperationException} if this directory is read-only.
     * 
     * @throws UnsupportedOperationException if this directory is read-only.
     * @see #isReadOnly()
     */
    protected final void assertMutable() {
        if (isReadOnly()) {
            throw new UnsupportedOperationException("Directory is read-only");
        }
    }

    public boolean add(final Entry pEntry) {
        assertMutable();

        // TODO: Replace if entry is already present?
        // Some directories may need special ordering, or may/may not support multiple entries for certain ids...
        return mEntries.add(pEntry);
    }

    public boolean remove(final Object pEntry) {
        assertMutable();

        return mEntries.remove(pEntry);
    }

    public int size() {
        return mEntries.size();
    }

    /**
     * This implementation returns {@code true}.
     * Subclasses should override this method, if the directory is mutable.
     *
     * @return {@code true}
     */
    public boolean isReadOnly() {
        return true;
    }

    /// Standard object support

    @Override
    public int hashCode() {
        return mEntries.hashCode();
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }

        if (getClass() != pOther.getClass()) {
            return false;
        }

        // Safe cast, as it must be a subclass for the classes to be equal
        AbstractDirectory other = (AbstractDirectory) pOther;

        return mEntries.equals(other.mEntries);
    }

    @Override
    public String toString() {
        return String.format("%s%s", getClass().getSimpleName(), mEntries.toString());
    }
}
