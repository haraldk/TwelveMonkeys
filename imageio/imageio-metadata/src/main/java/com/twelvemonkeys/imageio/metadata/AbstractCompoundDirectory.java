/*
 * Copyright (c) 2012, Harald Kuhr
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.twelvemonkeys.lang.Validate.noNullElements;

/**
 * AbstractCompoundDirectory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractCompoundDirectory.java,v 1.0 02.01.12 12:43 haraldk Exp$
 */
public abstract class AbstractCompoundDirectory extends AbstractDirectory implements CompoundDirectory {
    private final List<Directory> directories = new ArrayList<Directory>();

    protected AbstractCompoundDirectory(final Collection<? extends Directory> directories) {
        super(null);

        if (directories != null) {
            this.directories.addAll(noNullElements(directories));
        }
    }

    public Directory getDirectory(int index) {
        return directories.get(index);
    }

    public int directoryCount() {
        return directories.size();
    }

    @Override
    public Entry getEntryById(final Object identifier) {
        for (Directory directory : directories) {
            Entry entry = directory.getEntryById(identifier);

            if (entry != null) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public Entry getEntryByFieldName(final String fieldName) {
        for (Directory directory : directories) {
            Entry entry = directory.getEntryByFieldName(fieldName);

            if (entry != null) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
            Iterator<Directory> directoryIterator = directories.iterator();
            Iterator<Entry> current;

            public boolean hasNext() {
                return current != null && current.hasNext() || directoryIterator.hasNext() && (current = directoryIterator.next().iterator()).hasNext();
            }

            public Entry next() {
                hasNext();

                return current.next();
            }

            public void remove() {
                current.remove();
            }
        };
    }

    // TODO: Define semantics, or leave to subclasses?
    // Add to first/last directory?
    // Introduce a "current" directory? And a way to advance/go back
    // Remove form the first directory that contains entry?
    @Override
    public boolean add(final Entry entry) {
        throw new UnsupportedOperationException("Directory is read-only");
    }

    @Override
    public boolean remove(final Object entry) {
        throw new UnsupportedOperationException("Directory is read-only");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public int size() {
        int size = 0;

        for (Directory directory : directories) {
            size += directory.size();
        }

        return size;
    }

    @Override
    public String toString() {
        return String.format("%s%s", getClass().getSimpleName(), directories.toString());
    }

    @Override
    public int hashCode() {
        int hash = 0;

        for (Directory ifd : directories) {
            hash ^= ifd.hashCode();
        }

        return hash;
    }

    @Override
    public boolean equals(Object pOther) {
        if (pOther == this) {
            return true;
        }
        if (pOther == null) {
            return false;
        }
        if (pOther.getClass() != getClass()) {
            return false;
        }

        CompoundDirectory other = (CompoundDirectory) pOther;

        if (directoryCount() != other.directoryCount()) {
            return false;
        }

        for (int i = 0; i < directoryCount(); i++) {
            if (!getDirectory(i).equals(other.getDirectory(i))) {
                return false;
            }
        }

        return true;
    }
}
