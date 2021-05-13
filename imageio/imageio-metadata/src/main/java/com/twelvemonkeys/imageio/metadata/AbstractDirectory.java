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

package com.twelvemonkeys.imageio.metadata;

import java.util.*;

import static com.twelvemonkeys.lang.Validate.noNullElements;

/**
 * AbstractDirectory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractDirectory.java,v 1.0 Nov 11, 2009 5:31:04 PM haraldk Exp$
 */
public abstract class AbstractDirectory implements Directory {
    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> unmodifiable = Collections.unmodifiableList(entries);

    protected AbstractDirectory(final Collection<? extends Entry> entries) {
        if (entries != null) {
            this.entries.addAll(noNullElements(entries));
        }
    }

    public Entry getEntryById(final Object identifier) {
        for (Entry entry : this) {
            if (entry.getIdentifier().equals(identifier)) {
                return entry;
            }
        }

        return null;
    }

    public Entry getEntryByFieldName(final String fieldName) {
        for (Entry entry : this) {
            if (entry.getFieldName() != null && entry.getFieldName().equals(fieldName)) {
                return entry;
            }
        }

        return null;
    }

    public Iterator<Entry> iterator() {
        return isReadOnly() ? unmodifiable.iterator() : entries.iterator();
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

    public boolean add(final Entry entry) {
        assertMutable();

        // TODO: Replace if entry is already present?
        // Some directories may need special ordering, or may/may not support multiple entries for certain ids...
        return entries.add(entry);
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    public boolean remove(final Object entry) {
        assertMutable();

        return entries.remove(entry);
    }

    public int size() {
        return entries.size();
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
        return entries.hashCode();
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }

        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }

        // Safe cast, as it must be a subclass for the classes to be equal
        AbstractDirectory other = (AbstractDirectory) pOther;

        return entries.equals(other.entries);
    }

    @Override
    public String toString() {
        return String.format("%s%s", getClass().getSimpleName(), entries.toString());
    }
}
