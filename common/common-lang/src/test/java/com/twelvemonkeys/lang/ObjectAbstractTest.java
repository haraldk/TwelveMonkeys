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

package com.twelvemonkeys.lang;

import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * AbstractObjectTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/lang/ObjectAbstractTestCase.java#1 $
 */
public abstract class ObjectAbstractTest {
    // TODO: See com.tm.util.ObjectAbstractTestCase
    // TODO: The idea is that this should be some generic base-class that
    // implements the basic object tests

    // TODO: Create Serializable test similar way
    // TODO: Create Comparable test similar way

    /**
     * Returns an instance of the class we are testing.
     * Implement this method to return the object to test.
     *
     * @return the object to test
     */
    protected abstract Object makeObject();

    // TODO: Can we really do serious testing with just one object?
    // TODO: How can we make sure we create equal or different objects?!
    //protected abstract Object makeDifferentObject(Object pObject);
    //protected abstract Object makeEqualObject(Object pObject);


    @Test
    public void testToString() {
        assertNotNull(makeObject().toString());
        // TODO: What more can we test?
    }

    // TODO: assert that either BOTH or NONE of equals/hashcode is overridden
    @Test
    public void testEqualsHashCode(){
        Object obj = makeObject();

        Class cl = obj.getClass();
        if (isEqualsOverriden(cl)) {
            assertTrue("Class " + cl.getName() + " implements equals but not hashCode", isHashCodeOverriden(cl));
        }
        else if (isHashCodeOverriden(cl)) {
            assertTrue("Class " + cl.getName() + " implements hashCode but not equals", isEqualsOverriden(cl));
        }

    }

    protected static boolean isEqualsOverriden(Class pClass) {
        return getDeclaredMethod(pClass, "equals", new Class[]{Object.class}) != null;
    }

    protected static boolean isHashCodeOverriden(Class pClass) {
        return getDeclaredMethod(pClass, "hashCode", null) != null;
    }

    private static Method getDeclaredMethod(Class pClass, String pName, Class[] pArameters) {
        try {
            return pClass.getDeclaredMethod(pName, pArameters);
        }
        catch (NoSuchMethodException ignore) {
            return null;
        }
    }

    @Test
    public void testObjectEqualsSelf() {
        Object obj = makeObject();
        assertEquals("An Object should equal itself", obj, obj);
    }

    @Test
    public void testEqualsNull() {
        Object obj = makeObject();
        // NOTE: Makes sure this doesn't throw NPE either
        //noinspection ObjectEqualsNull
        assertFalse("An object should never equal null", obj.equals(null));
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
        // TODO: Make sure we create at least one equal object, and one different object
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

    /*
    public void testFinalize() {
        // TODO: Implement
    }
    */

    ////////////////////////////////////////////////////////////////////////////
    // Cloneable interface
    @Test
    public void testClone() throws Exception {
        Object obj = makeObject();
        if (obj instanceof Cloneable) {
            Class cl = obj.getClass();

            Method clone = findMethod(cl, "clone");

            // Disregard protected modifier
            // NOTE: This will throw a SecurityException if a SecurityManager
            // disallows access, but should not happen in a test context
            if (!clone.isAccessible()) {
                clone.setAccessible(true);
            }

            Object cloned = clone.invoke(obj);

            assertNotNull("Cloned object should never be null", cloned);

            // TODO: This can only be asserted if equals() test is based on
            // value equality, not reference (identity) equality
            // Maybe it's possible to do a reflective introspection of
            // the objects fields?
            if (isHashCodeOverriden(cl)) {
                assertEquals("Cloned object not equal", obj, cloned);
            }
        }
    }

    private static Method findMethod(Class pClass, String pName) throws NoSuchMethodException {
        if (pClass == null) {
            throw new IllegalArgumentException("class == null");
        }
        if (pName == null) {
            throw new IllegalArgumentException("name == null");
        }

        Class cl = pClass;

        while (cl != null) {
            try {
                return cl.getDeclaredMethod(pName, new Class[0]);
            }
            catch (NoSuchMethodException e) {
            }
            catch (SecurityException e) {
            }

            cl = cl.getSuperclass();
        }

        throw new NoSuchMethodException(pName + " in class " + pClass.getName());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Serializable interface
    @Test
    public void testSerializeDeserializeThenCompare() throws Exception {
        Object obj = makeObject();
        if (obj instanceof Serializable) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buffer);
            try {
                out.writeObject(obj);
            }
            finally {
                out.close();
            }

            Object dest;
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
            try {
                dest = in.readObject();
            }
            finally {
                in.close();
            }

            // TODO: This can only be asserted if equals() test is based on
            // value equality, not reference (identity) equality
            // Maybe it's possible to do a reflective introspection of
            // the objects fields?
            if (isEqualsOverriden(obj.getClass())) {
                assertEquals("obj != deserialize(serialize(obj))", obj, dest);
            }
        }
    }

    /**
     * Sanity check method, makes sure that any {@code Serializable}
     * class can be serialized and de-serialized in memory,
     * using the handy makeObject() method
     *
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testSimpleSerialization() throws Exception {
        Object o = makeObject();
        if (o instanceof Serializable) {
            byte[] object = writeExternalFormToBytes((Serializable) o);
            readExternalFormFromBytes(object);
        }
    }

    /**
     * Write a Serializable or Externalizable object as
     * a file at the given path.
     * <em>NOT USEFUL as part
     * of a unit test; this is just a utility method
     * for creating disk-based objects in CVS that can become
     * the basis for compatibility tests using
     * readExternalFormFromDisk(String path)</em>
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

    public static final class SanityTestTest extends ObjectAbstractTest {
        protected Object makeObject() {
            return new Cloneable() {};
        }
    }

}
