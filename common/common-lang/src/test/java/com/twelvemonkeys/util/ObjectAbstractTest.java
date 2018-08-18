/*
 * Copyright (c) 2008, Harald Kuhr
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
/*
 *  Copyright 2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twelvemonkeys.util;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Abstract test class for {@link Object} methods and contracts.
 * <p>
 * To use, simply extend this class, and implement
 * the {@link #makeObject()} method.
 * <p>
 * If your {@link Object} fails one of these tests by design,
 * you may still use this base set of cases.  Simply override the
 * test case (method) your {@link Object} fails.
 *
 * @version $Revision: #2 $ $Date: 2008/07/15 $
 * 
 * @author Rodney Waldhoff
 * @author Stephen Colebourne
 * @author Anonymous
 */
public abstract class ObjectAbstractTest {
    //-----------------------------------------------------------------------

    /**
     * Implement this method to return the object to test.
     * 
     * @return the object to test
     */
    public abstract Object makeObject();

    /**
     * Override this method if a subclass is testing an object
     * that cannot serialize an "empty" Collection.
     * (e.g. Comparators have no contents)
     * 
     * @return true
     */
    public boolean supportsEmptyCollections() {
        return true;
    }

    /**
     * Override this method if a subclass is testing an object
     * that cannot serialize a "full" Collection.
     * (e.g. Comparators have no contents)
     * 
     * @return true
     */
    public boolean supportsFullCollections() {
        return true;
    }

    /**
     * Is serialization testing supported.
     * Default is true.
     */
    public boolean isTestSerialization() {
        return true;
    }

    /**
     * Returns true to indicate that the collection supports equals() comparisons.
     * This implementation returns true;
     */
    public boolean isEqualsCheckable() {
        return true;
    }

    //-----------------------------------------------------------------------
    @Test
    public void testObjectEqualsSelf() {
        Object obj = makeObject();
        assertEquals("A Object should equal itself", obj, obj);
    }

    @Test
    public void testEqualsNull() {
        Object obj = makeObject();
        assertEquals(false, obj.equals(null)); // make sure this doesn't throw NPE either
    }

    @Test
    public void testObjectHashCodeEqualsSelfHashCode() {
        Object obj = makeObject();
        assertEquals("hashCode should be repeatable", obj.hashCode(), obj.hashCode());
    }

    @Test
    public void testObjectHashCodeEqualsContract() {
        Object obj1 = makeObject();
        if (obj1.equals(obj1)) {
            assertEquals(
                "[1] When two objects are equal, their hashCodes should be also.",
                obj1.hashCode(), obj1.hashCode());
        }
        Object obj2 = makeObject();
        if (obj1.equals(obj2)) {
            assertEquals(
                "[2] When two objects are equal, their hashCodes should be also.",
                obj1.hashCode(), obj2.hashCode());
            assertTrue(
                "When obj1.equals(obj2) is true, then obj2.equals(obj1) should also be true",
                obj2.equals(obj1));
        }
    }

    @Test
    public void testSerializeDeserializeThenCompare() throws Exception {
        Object obj = makeObject();
        if (obj instanceof Serializable && isTestSerialization()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buffer);
            out.writeObject(obj);
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
            Object dest = in.readObject();
            in.close();
            if (isEqualsCheckable()) {
                assertEquals("obj != deserialize(serialize(obj))", obj, dest);
            }
        }
    }

    /**
     * Sanity check method, makes sure that any Serializable
     * class can be serialized and de-serialized in memory, 
     * using the handy makeObject() method
     * 
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testSimpleSerialization() throws Exception {
        Object o = makeObject();
        if (o instanceof Serializable && isTestSerialization()) {
            byte[] objekt = writeExternalFormToBytes((Serializable) o);
            Object p = readExternalFormFromBytes(objekt);
        }
    }

    /**
     * Tests serialization by comparing against a previously stored version in CVS.
     * If the test object is serializable, confirm that a canonical form exists.
     */
    @Test
    public void testCanonicalEmptyCollectionExists() {
        if (supportsEmptyCollections() && isTestSerialization() && !skipSerializedCanonicalTests()) {
            Object object = makeObject();
            if (object instanceof Serializable) {
                String name = getCanonicalEmptyCollectionName(object);
                assertTrue(
                    "Canonical empty collection (" + name + ") is not in CVS",
                    new File(name).exists());
            }
        }
    }

    /**
     * Tests serialization by comparing against a previously stored version in CVS.
     * If the test object is serializable, confirm that a canonical form exists.
     */
    @Test
    public void testCanonicalFullCollectionExists() {
        if (supportsFullCollections() && isTestSerialization() && !skipSerializedCanonicalTests()) {
            Object object = makeObject();
            if (object instanceof Serializable) {
                String name = getCanonicalFullCollectionName(object);
                assertTrue(
                    "Canonical full collection (" + name + ") is not in CVS",
                    new File(name).exists());
            }
        }
    }

    // protected implementation
    //-----------------------------------------------------------------------
    /**
     * Get the version of Collections that this object tries to
     * maintain serialization compatibility with. Defaults to 1, the
     * earliest Collections version. (Note: some collections did not
     * even exist in this version).
     * 
     * This constant makes it possible for TestMap (and other subclasses,
     * if necessary) to automatically check CVS for a versionX copy of a
     * Serialized object, so we can make sure that compatibility is maintained.
     * See, for example, TestMap.getCanonicalFullMapName(Map map).
     * Subclasses can override this variable, indicating compatibility
     * with earlier Collections versions.
     * 
     * @return The version, or {@code null} if this object shouldn't be
     * tested for compatibility with previous versions.
     */
    public String getCompatibilityVersion() {
        return "1";
    }

    protected String getCanonicalEmptyCollectionName(Object object) {
        StringBuilder retval = new StringBuilder();
        retval.append("data/test/");
        String colName = object.getClass().getName();
        colName = colName.substring(colName.lastIndexOf(".") + 1, colName.length());
        retval.append(colName);
        retval.append(".emptyCollection.version");
        retval.append(getCompatibilityVersion());
        retval.append(".obj");
        return retval.toString();
    }

    protected String getCanonicalFullCollectionName(Object object) {
        StringBuilder retval = new StringBuilder();
        retval.append("data/test/");
        String colName = object.getClass().getName();
        colName = colName.substring(colName.lastIndexOf(".") + 1, colName.length());
        retval.append(colName);
        retval.append(".fullCollection.version");
        retval.append(getCompatibilityVersion());
        retval.append(".obj");
        return retval.toString();
    }

    /**
     * Write a Serializable or Externalizable object as
     * a file at the given path.  NOT USEFUL as part
     * of a unit test; this is just a utility method
     * for creating disk-based objects in CVS that can become
     * the basis for compatibility tests using
     * readExternalFormFromDisk(String path)
     * 
     * @param o Object to serialize
     * @param path path to write the serialized Object
     * @exception java.io.IOException
     */
    protected void writeExternalFormToDisk(Serializable o, String path) throws IOException {
        FileOutputStream fileStream = new FileOutputStream(path);
        writeExternalFormToStream(o, fileStream);
    }

    /**
     * Converts a Serializable or Externalizable object to
     * bytes.  Useful for in-memory tests of serialization
     * 
     * @param o Object to convert to bytes
     * @return serialized form of the Object
     * @exception java.io.IOException
     */
    protected byte[] writeExternalFormToBytes(Serializable o) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        writeExternalFormToStream(o, byteStream);
        return byteStream.toByteArray();
    }

    /**
     * Reads a Serialized or Externalized Object from disk.
     * Useful for creating compatibility tests between
     * different CVS versions of the same class
     * 
     * @param path path to the serialized Object
     * @return the Object at the given path
     * @exception java.io.IOException
     * @exception ClassNotFoundException
     */
    protected Object readExternalFormFromDisk(String path) throws IOException, ClassNotFoundException {
        FileInputStream stream = new FileInputStream(path);
        return readExternalFormFromStream(stream);
    }

    /**
     * Read a Serialized or Externalized Object from bytes.
     * Useful for verifying serialization in memory.
     * 
     * @param b byte array containing a serialized Object
     * @return Object contained in the bytes
     * @exception java.io.IOException
     * @exception ClassNotFoundException
     */
    protected Object readExternalFormFromBytes(byte[] b) throws IOException, ClassNotFoundException {
        ByteArrayInputStream stream = new ByteArrayInputStream(b);
        return readExternalFormFromStream(stream);
    }

    protected boolean skipSerializedCanonicalTests() {
        return Boolean.getBoolean("org.apache.commons.collections:with-clover");
    }

    // private implementation
    //-----------------------------------------------------------------------
    private Object readExternalFormFromStream(InputStream stream) throws IOException, ClassNotFoundException {
        ObjectInputStream oStream = new ObjectInputStream(stream);
        return oStream.readObject();
    }

    private void writeExternalFormToStream(Serializable o, OutputStream stream) throws IOException {
        ObjectOutputStream oStream = new ObjectOutputStream(stream);
        oStream.writeObject(o);
    }

}
