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

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A utility class with some useful system-related functions.
 * <p/>
 * <em>NOTE: This class is not considered part of the public API and may be
 * changed without notice</em>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/SystemUtil.java#3 $
 *
 */
public final class SystemUtil {
    /** {@code ".xml"} */
    public static String XML_PROPERTIES = ".xml";
    /** {@code ".properties"} */
    public static String STD_PROPERTIES = ".properties";

    // Disallow creating objects of this type    
    private SystemUtil() {
    }

    /** This class marks an inputstream as containing XML, does nothing */
    private static class XMLPropertiesInputStream extends FilterInputStream {
        public XMLPropertiesInputStream(InputStream pIS) {
            super(pIS);
        }
    }

    /**
     * Gets the named resource as a stream from the given Class' Classoader.
     * If the pGuessSuffix parameter is true, the method will try to append 
     * typical properties file suffixes, such as ".properties" or ".xml".
     *
     * @param pClassLoader the class loader to use
     * @param pName name of the resource
     * @param pGuessSuffix guess suffix
     *
     * @return an input stream reading from the resource
     */
    private static InputStream getResourceAsStream(ClassLoader pClassLoader, String pName, boolean pGuessSuffix) {
        InputStream is;

        if (!pGuessSuffix) {
            is = pClassLoader.getResourceAsStream(pName);

            // If XML, wrap stream
            if (is != null && pName.endsWith(XML_PROPERTIES)) {
                is = new XMLPropertiesInputStream(is);
            }
        }
        else {
            // Try normal properties
            is = pClassLoader.getResourceAsStream(pName + STD_PROPERTIES);

            // Try XML
            if (is == null) {
                is = pClassLoader.getResourceAsStream(pName + XML_PROPERTIES);

                // Wrap stream
                if (is != null) {
                    is = new XMLPropertiesInputStream(is);
                }
            }
        }

        // Return stream 
        return is;
    }

    /**
     * Gets the named file as a stream from the current directory.
     * If the pGuessSuffix parameter is true, the method will try to append
     * typical properties file suffixes, such as ".properties" or ".xml".
     *
     * @param pName name of the resource
     * @param pGuessSuffix guess suffix
     *
     * @return an input stream reading from the resource
     */
    private static InputStream getFileAsStream(String pName, boolean pGuessSuffix) {
        InputStream is = null;
        File propertiesFile;

        try {
            if (!pGuessSuffix) {
                // Get file 
                propertiesFile = new File(pName);

                if (propertiesFile.exists()) {
                    is = new FileInputStream(propertiesFile);

                    // If XML, wrap stream
                    if (pName.endsWith(XML_PROPERTIES)) {
                        is = new XMLPropertiesInputStream(is);
                    }
                }
            }
            else {
                // Try normal properties 
                propertiesFile = new File(pName + STD_PROPERTIES);

                if (propertiesFile.exists()) {
                    is = new FileInputStream(propertiesFile);
                }
                else {
                    // Try XML
                    propertiesFile = new File(pName + XML_PROPERTIES);

                    if (propertiesFile.exists()) {
                        // Wrap stream
                        is = new XMLPropertiesInputStream(new FileInputStream(propertiesFile));
                    }
                }
            }
        }
        catch (FileNotFoundException fnf) {
            // Should not happen, as we always test that the file .exists()
            // before creating InputStream
            // assert false;
        }

        return is;
    }

    /**
     * Utility method for loading a named properties-file for a class. 
     * <P>
     * The properties-file is loaded through either:
     * <OL> 
     * <LI>The given class' class loader (from classpath)</LI>
     * <LI>Or, the system class loader (from classpath)</LI>
     * <LI>Or, if it cannot be found in the classpath, an attempt to read from 
     *     the current directory (or full path if given).</LI>
     * </OL>
     * <P>
     * Both normal java.util.Properties and com.twelvemonkeys.util.XMLProperties
     * are supported (XML-properties must have ".xml" as its file extension).
     *
     * @param pClass The class to load properties for. If this parameter is 
     *        {@code null}, the method will work exactly as
     *        {@link #loadProperties(String)}
     * @param pName The name of the properties-file. If this parameter is
     *        {@code null}, the method will work exactly as
     *        {@link #loadProperties(Class)}
     *
     * @return A Properties mapping read from the given file or for the given 
     * class. <!--If no properties-file was found, an empty Properties object is
     * returned.-->
     *
     * @throws NullPointerException if both {@code pName} and
     *         {@code pClass} paramters are {@code null}
     * @throws IOException if an error occurs during load.
     * @throws FileNotFoundException if no properties-file could be found.
     *
     * @see #loadProperties(String)
     * @see #loadProperties(Class)
     * @see java.lang.ClassLoader#getResourceAsStream
     * @see java.lang.ClassLoader#getSystemResourceAsStream
     *
     * @todo Reconsider ever using the System ClassLoader: http://www.javaworld.com/javaworld/javaqa/2003-06/01-qa-0606-load.html
     * @todo Consider using Context Classloader instead?
     */
    public static Properties loadProperties(Class pClass, String pName) throws IOException
    {
        // Convert to name the classloader understands
        String name = !StringUtil.isEmpty(pName) ? pName : pClass.getName().replace('.', '/');

        // Should we try to guess suffix?
        boolean guessSuffix = (pName == null || pName.indexOf('.') < 0);

        InputStream is;

        // TODO: WHAT IF MULTIPLE RESOURCES EXISTS?!
        // Try loading resource through the current class' classloader
        if (pClass != null && (is = getResourceAsStream(pClass.getClassLoader(), name, guessSuffix)) != null) {
            //&& (is = getResourceAsStream(pClass, name, guessSuffix)) != null) {
            // Nothing to do
            //System.out.println(((is instanceof XMLPropertiesInputStream) ?
            //                    "XML-properties" : "Normal .properties")
            //                   + " from Class' ClassLoader");
        }
        // If that fails, try the system classloader
        else if ((is = getResourceAsStream(ClassLoader.getSystemClassLoader(), name, guessSuffix)) != null) {
            //else if ((is = getSystemResourceAsStream(name, guessSuffix)) != null) {
                    // Nothing to do
            //System.out.println(((is instanceof XMLPropertiesInputStream) ?
            //                    "XML-properties" : "Normal .properties")
            //                   + " from System ClassLoader");
        }
        // All failed, try loading from file
        else if ((is = getFileAsStream(name, guessSuffix)) != null) {
            //System.out.println(((is instanceof XMLPropertiesInputStream) ?
            //                    "XML-properties" : "Normal .properties")
            //                   + " from System ClassLoader");
        }
        else {
            if (guessSuffix) {
                // TODO: file extension iterator or something...
                throw new FileNotFoundException(name + ".properties or " + name + ".xml");
            }
            else {
                throw new FileNotFoundException(name);
            }
        }

        // We have inputstream now, load...
        try {
            return loadProperties(is);
        }
        finally {
            // NOTE: If is == null, a FileNotFoundException must have been thrown above
            try {
                is.close();
            }
            catch (IOException ioe) {
                // Not critical...
            }
        }
    }

    /**
     * Utility method for loading a properties-file for a given class.
     * The properties are searched for on the form 
     * "com/package/ClassName.properties" or 
     * "com/package/ClassName.xml". 
     * <P>
     * The properties-file is loaded through either:
     * <OL> 
     * <LI>The given class' class loader (from classpath)</LI>
     * <LI>Or, the system class loader (from classpath)</LI>
     * <LI>Or, if it cannot be found in the classpath, an attempt to read from 
     *     the current directory (or full path if given).</LI>
     * </OL>
     * <P>
     * Both normal java.util.Properties and com.twelvemonkeys.util.XMLProperties
     * are supported (XML-properties must have ".xml" as its file extension).
     *
     * @param pClass The class to load properties for
     * @return A Properties mapping for the given class. <!--If no properties-
     *         file was found, an empty Properties object is returned.-->
     *
     * @throws NullPointerException if the {@code pClass} paramters is
     *         {@code null}
     * @throws IOException if an error occurs during load.
     * @throws FileNotFoundException if no properties-file could be found.
     *
     * @see #loadProperties(String)
     * @see #loadProperties(Class, String)
     * @see java.lang.ClassLoader#getResourceAsStream
     * @see java.lang.ClassLoader#getSystemResourceAsStream
     *
     */
    public static Properties loadProperties(Class pClass) throws IOException {
        return loadProperties(pClass, null);
    }

    /**
     * Utility method for loading a named properties-file. 
     * <P>
     * The properties-file is loaded through either:
     * <OL> 
     * <LI>The system class loader (from classpath)</LI>
     * <LI>Or, if it cannot be found in the classpath, an attempt to read from 
     *     the current directory.</LI>
     * </OL>
     * <P>
     * Both normal java.util.Properties and com.twelvemonkeys.util.XMLProperties
     * are supported (XML-properties must have ".xml" as its file extension).
     *
     * @param pName The name of the properties-file.
     * @return A Properties mapping read from the given file. <!--If no properties-
     *         file was found, an empty Properties object is returned.-->
     *
     * @throws NullPointerException if the {@code pName} paramters is
     *         {@code null}
     * @throws IOException if an error occurs during load.
     * @throws FileNotFoundException if no properties-file could be found.
     *
     * @see #loadProperties(Class)
     * @see #loadProperties(Class, String)
     * @see java.lang.ClassLoader#getSystemResourceAsStream
     *
     */
    public static Properties loadProperties(String pName) throws IOException {
        return loadProperties(null, pName);
    }

    /*
     * Utility method for loading a properties-file.
     * <P>
     * The properties files may also be contained in a zip/jar-file named
     * by the {@code com.twelvemonkeys.util.Config} system property (use "java -D"
     * to override). Default is "config.zip" in the current directory.
     *
     * @param pName The name of the file to loaded
     * @return A Properties mapping for the given class. If no properties-
     *         file was found, an empty Properties object is returned.
     *
     */
    /*
    public static Properties loadProperties(String pName) throws IOException {
        // Use XML?
        boolean useXML = pName.endsWith(XML_PROPERTIES) ? true : false;
        
        InputStream is = null;
        
        File file = new File(pName);
        
        String configName = System.getProperty("com.twelvemonkeys.util.Config");
        File configArchive = new File(!StringUtil.isEmpty(configName) 
                                      ? configName : DEFAULT_CONFIG);
        
        // Get input stream to the file containing the properties
        if (file.exists()) {
            // Try reading from file, normal way
            is = new FileInputStream(file);
        }
        else if (configArchive.exists()) {
            // Try reading properties from zip-file
            ZipFile zip = new ZipFile(configArchive);
            ZipEntry ze = zip.getEntry(pName);
            if (ze != null) {
                is = zip.getInputStream(ze);
            }
            
        }
        
        // Do the loading
        try {
            // Load the properties
            return loadProperties(is, useXML);
        }
        finally {
            // Try closing the archive to free resources
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ioe) {
                    // Not critical...
                }
            }
        }
        
    }
    */

    /**
     * Returns a Properties, loaded from the given inputstream. If the given 
     * inputstream is null, then an empty Properties object is returned.
     *
     * @param pInput the inputstream to read from
     *
     * @return a Properties object read from the given stream, or an empty 
     *         Properties mapping, if the stream is null.
     *
     * @throws IOException if an error occurred when reading from the input 
     *                     stream.
     *
     */
    private static Properties loadProperties(InputStream pInput)
        throws IOException {

        if (pInput == null) {
            throw new IllegalArgumentException("InputStream == null!");
        }

        Properties mapping = new Properties();
        /*if (pInput instanceof  XMLPropertiesInputStream) {
            mapping = new XMLProperties();
        }
        else {
            mapping = new Properties();
        }*/

        // Load the properties
        mapping.load(pInput);

        return mapping;
    }

    @SuppressWarnings({"SuspiciousSystemArraycopy"})
    public static Object clone(final Cloneable pObject) throws CloneNotSupportedException {
        if (pObject == null) {
            return null; // Null is clonable.. Easy. ;-)
        }

        // All arrays does have a clone method, but it's invisible for reflection...
        // By luck, multi-dimensional primitive arrays are instances of Object[]
        if (pObject instanceof Object[]) {
            return ((Object[]) pObject).clone();
        }
        else if (pObject.getClass().isArray()) {
            // One-dimensional primitive array, cloned manually
            int lenght = Array.getLength(pObject);
            Object clone = Array.newInstance(pObject.getClass().getComponentType(), lenght);
            System.arraycopy(pObject, 0, clone, 0, lenght);
            return clone;
        }

        try {
            // Find the clone method
            Method clone = null;
            Class clazz = pObject.getClass();
            do {
                try {
                    clone = clazz.getDeclaredMethod("clone");
                    break; // Found, or throws exception above
                }
                catch (NoSuchMethodException ignore) {
                    // Ignore
                }
            }
            while ((clazz = clazz.getSuperclass()) != null);

            // NOTE: This should never happen
            if (clone == null) {
                throw new CloneNotSupportedException(pObject.getClass().getName());
            }

            // Override access if needed
            if (!clone.isAccessible()) {
                clone.setAccessible(true);
            }

            // Invoke clone method on original object
            return clone.invoke(pObject);
        }
        catch (SecurityException e) {
            CloneNotSupportedException cns = new CloneNotSupportedException(pObject.getClass().getName());
            cns.initCause(e);
            throw cns;
        }
        catch (IllegalAccessException e) {
            throw new CloneNotSupportedException(pObject.getClass().getName());
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof CloneNotSupportedException) {
                throw (CloneNotSupportedException) e.getTargetException();
            }
            else if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            else if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            }

            throw new CloneNotSupportedException(pObject.getClass().getName());
        }
    }

//    public static void loadLibrary(String pLibrary) {
//        NativeLoader.loadLibrary(pLibrary);
//    }
//
//    public static void loadLibrary(String pLibrary, ClassLoader pLoader) {
//        NativeLoader.loadLibrary(pLibrary, pLoader);
//    }

    public static void main(String[] args) throws CloneNotSupportedException {

        System.out.println("clone: " + args.clone().length + " (" + args.length + ")");
        System.out.println("copy: " + ((String[]) clone(args)).length + " (" + args.length + ")");

        int[] ints = {1,2,3};
        int[] copies = (int[]) clone(ints);
        System.out.println("Copies: " + copies.length + " (" + ints.length + ")");

        int[][] intsToo = {{1}, {2,3}, {4,5,6}};
        int[][] copiesToo = (int[][]) clone(intsToo);
        System.out.println("Copies: " + copiesToo.length + " (" + intsToo.length + ")");
        System.out.println("Copies0: " + copiesToo[0].length + " (" + intsToo[0].length + ")");
        System.out.println("Copies1: " + copiesToo[1].length + " (" + intsToo[1].length + ")");
        System.out.println("Copies2: " + copiesToo[2].length + " (" + intsToo[2].length + ")");

        Map<String, String> map = new HashMap<String, String>();

        for (String arg : args) {
            map.put(arg, arg);
        }

        Map copy = (Map) clone((Cloneable) map);

        System.out.println("Map : " + map);
        System.out.println("Copy: " + copy);

        /*
        SecurityManager sm = System.getSecurityManager();

        try {
            System.setSecurityManager(new SecurityManager() {
                public void checkPermission(Permission perm) {
                    if (perm.getName().equals("suppressAccessChecks")) {
                        throw new SecurityException();
                    }
                    //super.checkPermission(perm);
                }
        });
        */

        Cloneable cloneable = new Cloneable() {}; // No public clone method
        Cloneable clone = (Cloneable) clone(cloneable);

        System.out.println("cloneable: " + cloneable);
        System.out.println("clone: " + clone);

        /*
        }
        finally {
            System.setSecurityManager(sm);
        }
        */

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                return null;
            }
        }, AccessController.getContext());

        //String string = args.length > 0 ? args[0] : "jaffa";
        //clone(string);
    }

    /**
     * Tests if a named class is generally available.
     * If a class is considered available, a call to
     * {@code Class.forName(pClassName)} will not result in an exception.
     *
     * @param pClassName the class name to test
     * @return {@code true} if available
     */
    public static boolean isClassAvailable(String pClassName) {
        return isClassAvailable(pClassName, (ClassLoader) null);
    }

    /**
     * Tests if a named class is available from another class.
     * If a class is considered available, a call to
     * {@code Class.forName(pClassName, true, pFromClass.getClassLoader())}
     * will not result in an exception.
     *
     * @param pClassName the class name to test
     * @param pFromClass the class to test from
     * @return {@code true} if available
     */
    public static boolean isClassAvailable(String pClassName, Class pFromClass) {
        ClassLoader loader = pFromClass != null ? pFromClass.getClassLoader() : null;
        return isClassAvailable(pClassName, loader);
    }

    private static boolean isClassAvailable(String pClassName, ClassLoader pLoader) {
        try {
            // TODO: Sometimes init is not needed, but need to find a way to know...
            getClass(pClassName, true, pLoader);
            return true;
        }
        catch (SecurityException ignore) {
            // Ignore            
        }
        catch (ClassNotFoundException ignore) {
            // Ignore
        }
        catch (LinkageError ignore) {
            // Ignore
        }

        return false;
    }

    public static boolean isFieldAvailable(final String pClassName, final String pFieldName) {
        return isFieldAvailable(pClassName, pFieldName, (ClassLoader) null);
    }

    public static boolean isFieldAvailable(final String pClassName, final String pFieldName, final Class pFromClass) {
        ClassLoader loader = pFromClass != null ? pFromClass.getClassLoader() : null;
        return isFieldAvailable(pClassName, pFieldName, loader);
    }

    private static boolean isFieldAvailable(final String pClassName, final String pFieldName, final ClassLoader pLoader) {
        try {
            Class cl = getClass(pClassName, false, pLoader);

            Field field = cl.getField(pFieldName);
            if (field != null) {
                return true;
            }
        }
        catch (ClassNotFoundException ignore) {
            // Ignore
        }
        catch (LinkageError ignore) {
            // Ignore
        }
        catch (NoSuchFieldException ignore) {
            // Ignore
        }
        return false;
    }

    public static boolean isMethodAvailable(String pClassName, String pMethodName) {
        // Finds void only
        return isMethodAvailable(pClassName, pMethodName, null, (ClassLoader) null);
    }

    public static boolean isMethodAvailable(String pClassName, String pMethodName, Class[] pParams) {
        return isMethodAvailable(pClassName, pMethodName, pParams, (ClassLoader) null);
    }

    public static boolean isMethodAvailable(String pClassName, String pMethodName, Class[] pParams, Class pFromClass) {
        ClassLoader loader = pFromClass != null ? pFromClass.getClassLoader() : null;
        return isMethodAvailable(pClassName, pMethodName, pParams, loader);
    }

    private static boolean isMethodAvailable(String pClassName, String pMethodName, Class[] pParams, ClassLoader pLoader) {
        try {
            Class cl = getClass(pClassName, false, pLoader);

            Method method = cl.getMethod(pMethodName, pParams);
            if (method != null) {
                return true;
            }
        }
        catch (ClassNotFoundException ignore) {
            // Ignore
        }
        catch (LinkageError ignore) {
            // Ignore
        }
        catch (NoSuchMethodException ignore) {
            // Ignore
        }
        return false;
    }

    private static Class getClass(String pClassName, boolean pInitialize, ClassLoader pLoader) throws ClassNotFoundException {
        // NOTE: We need the context class loader, as SystemUtil's
        // class loader may have a totally different class loader than
        // the original caller class (as in Class.forName(cn, false, null)).
        ClassLoader loader = pLoader != null ? pLoader :
                Thread.currentThread().getContextClassLoader();

        return Class.forName(pClassName, pInitialize, loader);
    }
}
