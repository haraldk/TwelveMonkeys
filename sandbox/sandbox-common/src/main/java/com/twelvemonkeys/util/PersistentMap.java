/*
 * Copyright (c) 2009, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util;

import com.twelvemonkeys.io.FileUtil;

import java.io.*;
import java.util.*;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * PersistentMap
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PersistentMap.java,v 1.0 May 13, 2009 2:31:29 PM haraldk Exp$
 */
public class PersistentMap<K extends Serializable, V extends Serializable> extends AbstractMap<K, V>{
    public static final FileFilter DIRECTORIES = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory();
        }

        @Override
        public String toString() {
            return "[All folders]";
        }
    };
    private static final String INDEX = ".index";

    private final File root;
    private final Map<K, UUID> index = new LinkedHashMap<K, UUID>();

    private boolean mutable = true;


    // Idea 2.0:
    // - Create directory per hashCode
    // - Create file per object in that directory
    // - Name file after serialized form of key? Base64?
    //      - Special case for String/Integer/Long etc?
    // - Or create index file in directory with serialized objects + name (uuid) of file

    // TODO: Consider single index file? Or a few? In root directory instead of each directory
    // Consider a RAF/FileChannel approach instead of streams - how do we discard portions of a RAF?
    // - Need to keep track of used/unused parts of file, scan for gaps etc...?
    // - Need to periodically truncate and re-build the index (always as startup, then at every N puts/removes?)

    /*public */PersistentMap(String id) {
        this(new File(FileUtil.getTempDirFile(), id));
    }

    public PersistentMap(File root) {
        this.root = notNull(root);

        init();
    }

    private void init() {
        if (!root.exists() && !root.mkdirs()) {
            throw new IllegalStateException(String.format("'%s' does not exist/could not be created", root.getAbsolutePath()));
        }
        else if (!root.isDirectory()) {
            throw new IllegalStateException(String.format("'%s' exists but is not a directory", root.getAbsolutePath()));
        }

        if (!root.canRead()) {
            throw new IllegalStateException(String.format("'%s' is not readable", root.getAbsolutePath()));
        }

        if (!root.canWrite()) {
            mutable = false;
        }

        FileUtil.visitFiles(root, DIRECTORIES, new Visitor<File>() {
            public void visit(File dir) {
                // - Read .index file
                // - Add entries to index
                ObjectInputStream input = null;
                try {
                    input = new ObjectInputStream(new FileInputStream(new File(dir, INDEX)));
                    while (true) {
                        @SuppressWarnings({"unchecked"})
                        K key = (K) input.readObject();
                        String fileName = (String) input.readObject();
                        index.put(key, UUID.fromString(fileName));
                    }
                }
                catch (EOFException eof) {
                    // break here
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                finally {
                    FileUtil.close(input);
                }
            }
        });
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    Iterator<Entry<K, UUID>> indexIter = index.entrySet().iterator();

                    public boolean hasNext() {
                        return indexIter.hasNext();
                    }

                    public Entry<K, V> next() {
                        return new Entry<K, V>() {
                            final Entry<K, UUID> entry = indexIter.next();

                            public K getKey() {
                                return entry.getKey();
                            }

                            public V getValue() {
                                K key = entry.getKey();
                                int hash = key != null ? key.hashCode() : 0;
                                return readVal(hash, entry.getValue());
                            }

                            public V setValue(V value) {
                                K key = entry.getKey();
                                int hash = key != null ? key.hashCode() : 0;
                                return writeVal(key, hash, entry.getValue(), value, getValue());
                            }
                        };
                    }

                    public void remove() {
                        indexIter.remove();
                    }
                };
            }

            @Override
            public int size() {
                return index.size();
            }
        };
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public V put(K key, V value) {
        V oldVal = null;

        UUID uuid = index.get(key);
        int hash = key != null ? key.hashCode() : 0;

        if (uuid != null) {
            oldVal = readVal(hash, uuid);
        }

        return writeVal(key, hash, uuid, value, oldVal);
    }

    private V writeVal(K key, int hash, UUID uuid, V value, V oldVal) {
        if (!mutable) {
            throw new UnsupportedOperationException();
        }
        
        File bucket = new File(root, hashToFileName(hash));
        if (!bucket.exists() && !bucket.mkdirs()) {
            throw new IllegalStateException(String.format("Could not create bucket '%s'", bucket));
        }

        if (uuid == null) {
            // No uuid means new entry
            uuid = UUID.randomUUID();

            File idx = new File(bucket, INDEX);

            ObjectOutputStream output = null;
            try {
                output = new ObjectOutputStream(new FileOutputStream(idx, true));
                output.writeObject(key);
                output.writeObject(uuid.toString());

                index.put(key, uuid);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                FileUtil.close(output);
            }
        }

        File entry = new File(bucket, uuid.toString());
        if (value != null) {
            ObjectOutputStream output = null;
            try {
                output = new ObjectOutputStream(new FileOutputStream(entry));
                output.writeObject(value);

            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                FileUtil.close(output);
            }
        }
        else if (entry.exists()) {
            if (!entry.delete()) {
                throw new IllegalStateException(String.format("'%s' could not be deleted", entry));
            }
        }

        return oldVal;
    }

    private String hashToFileName(int hash) {
        return Integer.toString(hash, 16);
    }

    @Override
    public V get(Object key) {
        UUID uuid = index.get(key);

        if (uuid != null) {
            int hash = key != null ? key.hashCode() : 0;
            return readVal(hash, uuid);
        }

        return null;
    }

    private V readVal(final int hash, final UUID uuid) {
        File bucket = new File(root, hashToFileName(hash));
        File entry = new File(bucket, uuid.toString());

        if (entry.exists()) {
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream(entry));
                //noinspection unchecked
                return (V) input.readObject();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            finally {
                FileUtil.close(input);
            }
        }

        return null;
    }

    @Override
    public V remove(Object key) {
        // TODO!!!
        return super.remove(key);
    }

    // TODO: Should override size, put, get, remove, containsKey and containsValue



}



/*
Memory mapped file?
Delta sync?

Persistent format

Header
    File ID 4-8 bytes
    Size (entries)

    PersistentEntry pointer array block (PersistentEntry 0)
        Size (bytes)
        Next entry pointer block address (0 if last)
        PersistentEntry 1 address/offset + key
        ...
        PersistentEntry n address/offset + key

    PersistentEntry 1
        Size (bytes)?
        Serialized value or pointer array block
        ...
    PersistentEntry n
        Size (bytes)?
        Serialized value or pointer array block

*/