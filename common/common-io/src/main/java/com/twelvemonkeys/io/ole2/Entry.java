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

package com.twelvemonkeys.io.ole2;

import com.twelvemonkeys.io.SeekableInputStream;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents an OLE 2 compound document entry.
 * This is similar to a file in a file system, or an entry in a ZIP or JAR file.
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/ole2/Entry.java#4 $
 * @see com.twelvemonkeys.io.ole2.CompoundDocument
 */
// TODO: Consider extending java.io.File...
public final class Entry implements Comparable<Entry> {
    String name;
    byte type;
    byte nodeColor;

    int prevDId;
    int nextDId;
    int rootNodeDId;

    long createdTimestamp;
    long modifiedTimestamp;

    int startSId;
    int streamSize;

    CompoundDocument document;
    Entry parent;
    SortedSet<Entry> children;

    public final static int LENGTH = 128;

    static final int EMPTY = 0;
    static final int USER_STORAGE = 1;
    static final int USER_STREAM = 2;
    static final int LOCK_BYTES = 3;
    static final int PROPERTY = 4;
    static final int ROOT_STORAGE = 5;

    private static final SortedSet<Entry> NO_CHILDREN = Collections.unmodifiableSortedSet(new TreeSet<Entry>());

    private Entry() {
    }

    /**
     * Reads an entry from the input.
     *
     * @param pInput the input data
     * @return the {@code Entry} read from the input data
     * @throws IOException if an i/o exception occurs during reading
     */
    static Entry readEntry(final DataInput pInput) throws IOException {
        Entry p = new Entry();
        p.read(pInput);
        return p;
    }

    /**
     * Reads this entry
     *
     * @param pInput the input data
     * @throws IOException if an i/o exception occurs during reading
     */
    private void read(final DataInput pInput) throws IOException {
        byte[] bytes = new byte[64];
        pInput.readFully(bytes);

        // NOTE: Length is in bytes, including the null-terminator...
        int nameLength = pInput.readShort();
        name = new String(bytes, 0, nameLength - 2, Charset.forName("UTF-16LE"));
//        System.out.println("name: " + name);

        type = pInput.readByte();
//        System.out.println("type: " + type);

        nodeColor = pInput.readByte();
//        System.out.println("nodeColor: " + nodeColor);

        prevDId = pInput.readInt();
//        System.out.println("prevDId: " + prevDId);
        nextDId = pInput.readInt();
//        System.out.println("nextDId: " + nextDId);
        rootNodeDId = pInput.readInt();
//        System.out.println("rootNodeDId: " + rootNodeDId);

        // UID (16) + user flags (4), ignored
        if (pInput.skipBytes(20) != 20) {
            throw new CorruptDocumentException();
        }

        createdTimestamp = CompoundDocument.toJavaTimeInMillis(pInput.readLong());
        modifiedTimestamp = CompoundDocument.toJavaTimeInMillis(pInput.readLong());

        startSId = pInput.readInt();
//        System.out.println("startSId: " + startSId);
        streamSize = pInput.readInt();
//        System.out.println("streamSize: " + streamSize);

        // Reserved
        pInput.readInt();
    }

    /**
     * If {@code true} this {@code Entry} is the root {@code Entry}.
     *
     * @return {@code true} if this is the root {@code Entry}
     */
    public boolean isRoot() {
        return type == ROOT_STORAGE;
    }

    /**
     * If {@code true} this {@code Entry} is a directory
     * {@code Entry}.
     *
     * @return {@code true} if this is a directory {@code Entry}
     */
    public boolean isDirectory() {
        return type == USER_STORAGE;
    }

    /**
     * If {@code true} this {@code Entry} is a file (document)
     * {@code Entry}.
     *
     * @return {@code true} if this is a document {@code Entry}
     */
    public boolean isFile() {
        return type == USER_STREAM;
    }

    /**
     * Returns the name of this {@code Entry}
     *
     * @return the name of this {@code Entry}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@code InputStream} for this {@code Entry}
     *
     * @return an {@code InputStream} containing the data for this
     *         {@code Entry} or {@code null} if this is a directory {@code Entry}
     * @throws java.io.IOException if an I/O exception occurs
     * @see #length()
     */
    public SeekableInputStream getInputStream() throws IOException {
        if (!isFile()) {
            return null;
        }

        return document.getInputStreamForSId(startSId, streamSize);
    }

    /**
     * Returns the length of this entry
     *
     * @return the length of the stream for this entry, or {@code 0} if this is
     *         a directory {@code Entry}
     * @see #getInputStream()
     */
    public long length() {
        if (!isFile()) {
            return 0L;
        }

        return streamSize;
    }

    /**
     * Returns the time that this entry was created.
     * The time is converted from its internal representation to standard Java
     * representation, milliseconds since the epoch
     * (00:00:00 GMT, January 1, 1970).
     * <p/>
     * Note that most applications leaves this value empty ({@code 0L}).
     *
     * @return  A {@code long} value representing the time this entry was
     *          created, measured in milliseconds since the epoch
     *          (00:00:00 GMT, January 1, 1970), or {@code 0L} if no
     *          creation time stamp exists for this entry.
     */
    public long created() {
        return createdTimestamp;
    }

    /**
     * Returns the time that this entry was last modified.
     * The time is converted from its internal representation to standard Java
     * representation, milliseconds since the epoch
     * (00:00:00 GMT, January 1, 1970).
     * <p/>
     * Note that many applications leaves this value empty ({@code 0L}).
     *
     * @return  A {@code long} value representing the time this entry was
     *          last modified, measured in milliseconds since the epoch
     *          (00:00:00 GMT, January 1, 1970), or {@code 0L} if no
     *          modification time stamp exists for this entry.
     */
    public long lastModified() {
        return modifiedTimestamp;
    }

    /**
     * Return the parent of this {@code Entry}
     *
     * @return the parent of this {@code Entry}, or {@code null} if this is
     *         the root {@code Entry}
     */
    public Entry getParentEntry() {
        return parent;
    }

    /**
     * Returns the child of this {@code Entry} with the given name.
     *
     * @param pName the name of the child {@code Entry}
     * @return the child {@code Entry} or {@code null} if thee is no such
     *         child
     * @throws java.io.IOException if an I/O exception occurs
     */
    public Entry getChildEntry(final String pName) throws IOException {
        if (isFile() || rootNodeDId == -1) {
            return null;
        }

        Entry dummy = new Entry();
        dummy.name = pName;
        dummy.parent = this;

        SortedSet child = getChildEntries().tailSet(dummy);
        return (Entry) child.first();
    }

    /**
     * Returns the children of this {@code Entry}.
     *
     * @return a {@code SortedSet} of {@code Entry} objects
     * @throws java.io.IOException if an I/O exception occurs
     */
    public SortedSet<Entry> getChildEntries() throws IOException {
        if (children == null) {
            if (isFile() || rootNodeDId == -1) {
                children = NO_CHILDREN;
            }
            else {
                // Start at root node in R/B tree, and read to the left and right,
                // re-build tree, according to the docs
                children = Collections.unmodifiableSortedSet(document.getEntries(rootNodeDId, this));
            }
        }

        return children;
    }

    @Override
    public String toString() {
        return "\"" + name + "\""
                + " (" + (isFile() ? "Document" : (isDirectory() ? "Directory" : "Root"))
                + (parent != null ? ", parent: \"" + parent.getName() + "\"" : "")
                + (isFile() ? "" : ", children: " + (children != null ? String.valueOf(children.size()) : "(unknown)"))
                + ", SId=" + startSId + ", length=" + streamSize + ")";
    }

    @Override
    public boolean equals(final Object pOther) {
        if (pOther == this) {
            return true;
        }
        if (!(pOther instanceof Entry)) {
            return false;
        }

        Entry other = (Entry) pOther;
        return name.equals(other.name) && (parent == other.parent
                || (parent != null && parent.equals(other.parent)));
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ startSId;
    }

    public int compareTo(final Entry pOther) {
        if (this == pOther) {
            return 0;
        }

        // NOTE: This is the sorting algorthm defined by the Compound Document:
        //  - first sort by name length
        //  - if lengths are equal, sort by comparing strings, case sensitive 

        int diff = name.length() - pOther.name.length();
        if (diff != 0) {
            return diff;
        }

        return name.compareTo(pOther.name);
    }
}
