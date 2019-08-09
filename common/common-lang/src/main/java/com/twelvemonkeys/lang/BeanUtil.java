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

import com.twelvemonkeys.util.convert.ConversionException;
import com.twelvemonkeys.util.convert.Converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * A utility class with some useful bean-related functions.
 * <p>
 * <em>NOTE: This class is not considered part of the public API and may be changed without notice</em>
 * </p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/BeanUtil.java#2 $
 */
public final class BeanUtil {

    // Disallow creating objects of this type
    private BeanUtil() {
    }

    /**
     * Gets a property value from the given object, using reflection.
     * Now supports getting values from properties of properties
     * (recursive).
     *
     * @param pObject The object to get the property from
     * @param pProperty The name of the property
     *
     * @return A string containing the value of the given property, or {@code null}
     *         if it can not be found.
     */
    public static Object getPropertyValue(Object pObject, String pProperty) {
        // TODO: Remove System.err's... Create new Exception? Hmm..
        // TODO: Support get(Object) method of Collections!
        //       Handle lists and arrays with [] (index) operator

        if (pObject == null || pProperty == null || pProperty.length() < 1) {
            return null;
        }

        Class<?> objClass = pObject.getClass();

        Object result = pObject;

        // Method for method...
        String subProp;
        int begIdx = 0;
        int endIdx = begIdx;

        while (begIdx < pProperty.length() && begIdx >= 0) {

            endIdx = pProperty.indexOf(".", endIdx + 1);
            if (endIdx > 0) {
                subProp = pProperty.substring(begIdx, endIdx);
                begIdx = endIdx + 1;
            }
            else {
                // The final property!
                // If there's just the first-level property, subProp will be
                // equal to property
                subProp = pProperty.substring(begIdx);
                begIdx = -1;
            }

            // Check for "[" and "]"
            Object[] param = null;
            Class[] paramClass = new Class[0];

            int begBracket;
            if ((begBracket = subProp.indexOf("[")) > 0) {
                // An error if there is no matching bracket
                if (!subProp.endsWith("]")) {
                    return null;
                }

                String between = subProp.substring(begBracket + 1,
                                                   subProp.length() - 1);
                subProp = subProp.substring(0, begBracket);

                // If brackets exist, check type of argument between brackets
                param = new Object[1];
                paramClass = new Class[1];

                //try {
                // TODO: isNumber returns true, even if too big for integer...
                if (StringUtil.isNumber(between)) {
                    // We have a number
                    // Integer -> array subscript -> getXXX(int i)
                    try {
                        // Insert param and it's Class
                        param[0] = Integer.valueOf(between);
                        paramClass[0] = Integer.TYPE; // int.class
                    }
                    catch (NumberFormatException e) {
                        // ??
                        // Probably too small or too large value..
                    }
                }
                else {
                    //catch (NumberFormatException e) {
                    // Not a number... Try String
                    // String -> Hashtable key -> getXXX(String str)
                    // Insert param and it's Class
                    param[0] = between.toLowerCase();
                    paramClass[0] = String.class;
                }
            }

            Method method;
            String methodName = "get" + StringUtil.capitalize(subProp);
            try {
                // Try to get the "get" method for the given property
                method = objClass.getMethod(methodName, paramClass);
            }
            catch (NoSuchMethodException e) {
                System.err.print("No method named \"" + methodName + "()\"");
                // The array might be of size 0...
                if (paramClass.length > 0 && paramClass[0] != null) {
                    System.err.print(" with the parameter " + paramClass[0].getName());
                }

                System.err.println(" in class " + objClass.getName() + "!");
                return null;
            }

            // If method for some reason should be null, give up
            if (method == null) {
                return null;
            }

            try {
                // We have a method, try to invoke it
                // The resutling object will be either the property we are
                // Looking for, or the parent

                //		System.err.println("Trying " + objClass.getName() + "." + method.getName() + "(" + ((param != null && param.length > 0) ? param[0] : "") + ")");
                result = method.invoke(result, param);
            }
            catch (InvocationTargetException e) {
                System.err.println("property=" + pProperty + " & result=" + result + " & param=" + Arrays.toString(param));
                e.getTargetException().printStackTrace();
                e.printStackTrace();
                return null;
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
            catch (NullPointerException e) {
                System.err.println(objClass.getName() + "." + method.getName() + "(" + ((paramClass.length > 0 && paramClass[0] != null) ? paramClass[0].getName() : "") + ")");
                e.printStackTrace();
                return null;
            }

            if (result != null) {
                // Get the class of the reulting object
                objClass = result.getClass();
            }
            else {
                return null;
            }
        } // while

        return result;
    }

    /**
     * Sets the property value to an object using reflection.
     * Supports setting values of properties that are properties of
     * properties (recursive).
     *
     * @param pObject      The object to get a property from
     * @param pProperty The name of the property
     * @param pValue    The property value
     *
     * @throws NoSuchMethodException if there's no write method for the
     *         given property
     * @throws InvocationTargetException if invoking the write method failed
     * @throws IllegalAccessException if the caller class has no access to the
     *         write method
     */
    public static void setPropertyValue(Object pObject, String pProperty, Object pValue)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //
        // TODO: Support set(Object, Object)/put(Object, Object) methods
        //       of Collections!
        //       Handle lists and arrays with [] (index) operator

        Class paramType = pValue != null ? pValue.getClass() : Object.class;

        // Preserve references
        Object obj = pObject;
        String property = pProperty;

        // Recurse and find real parent if property contains a '.'
        int dotIdx = property.indexOf('.');
        if (dotIdx >= 0) {
            // Get real parent
            obj = getPropertyValue(obj, property.substring(0, dotIdx));
            // Get the property of the parent
            property = property.substring(dotIdx + 1);
        }

        // Find method
        Object[] params = {pValue};
        Method method = getMethodMayModifyParams(obj, "set" + StringUtil.capitalize(property),
                                                 new Class[] {paramType}, params);

        // Invoke it
        method.invoke(obj, params);
    }

    private static Method getMethodMayModifyParams(Object pObject, String pName, Class[] pParams, Object[] pValues)
            throws NoSuchMethodException {
        // NOTE: This method assumes pParams.length == 1 && pValues.length == 1

        Method method = null;
        Class paramType = pParams[0];

        try {
            method = pObject.getClass().getMethod(pName, pParams);
        }
        catch (NoSuchMethodException e) {
            // No direct match

            // 1: If primitive wrapper, try unwrap conversion first
            /*if (paramType.isPrimitive()) { // NOTE: Can't be primitive type
                params[0] = ReflectUtil.wrapType(paramType);
            }
            else*/ if (ReflectUtil.isPrimitiveWrapper(paramType)) {
                pParams[0] = ReflectUtil.unwrapType(paramType);
            }

            try {
                // If this does not throw an exception, it works
                method = pObject.getClass().getMethod(pName, pParams);
            }
            catch (Throwable t) {
                // Ignore
            }

            // 2: Try any super-types of paramType, to see if we have a match
            if (method == null) {
                while ((paramType = paramType.getSuperclass()) != null) {
                    pParams[0] = paramType;
                    try {
                        // If this does not throw an exception, it works
                        method = pObject.getClass().getMethod(pName, pParams);
                    }
                    catch (Throwable t) {
                        // Ignore/Continue
                        continue;
                    }

                    break;
                }
            }

            // 3: Try to find a different method with the same name, that has
            // a parameter type we can convert to...
            // NOTE: There's no ordering here..
            // TODO: Should we try to do that? What would the ordering be?
            if (method == null) {
                Method[] methods = pObject.getClass().getMethods();
                for (Method candidate : methods) {
                    if (Modifier.isPublic(candidate.getModifiers()) && candidate.getName().equals(pName)
                            && candidate.getReturnType() == Void.TYPE && candidate.getParameterTypes().length == 1) {
                        // NOTE: Assumes paramTypes.length == 1

                        Class type = candidate.getParameterTypes()[0];

                        try {
                            pValues[0] = convertValueToType(pValues[0], type);
                        }
                        catch (Throwable t) {
                            continue;
                        }

                        // We were able to convert the parameter, let's try
                        method = candidate;
                        break;
                    }
                }
            }

            // Give up...
            if (method == null) {
                throw e;
            }
        }
        return method;
    }

    private static Object convertValueToType(Object pValue, Class<?> pType) throws ConversionException {
        if (pType.isPrimitive()) {
            if (pType == Boolean.TYPE && pValue instanceof Boolean) {
                return pValue;
            }
            else if (pType == Byte.TYPE && pValue instanceof Byte) {
                return pValue;
            }
            else if (pType == Character.TYPE && pValue instanceof Character) {
                return pValue;
            }
            else if (pType == Double.TYPE && pValue instanceof Double) {
                return pValue;
            }
            else if (pType == Float.TYPE && pValue instanceof Float) {
                return pValue;
            }
            else if (pType == Integer.TYPE && pValue instanceof Integer) {
                return pValue;
            }
            else if (pType == Long.TYPE && pValue instanceof Long) {
                return pValue;
            }
            else if (pType == Short.TYPE && pValue instanceof Short) {
                return pValue;
            }
        }

        // TODO: Convert value to single-value array if needed
        // TODO: Convert CSV String to string array (or potentially any type of array)

        // TODO: Convert other types
        if (pValue instanceof String) {
            Converter converter = Converter.getInstance();
            return converter.toObject((String) pValue, pType);
        }
        else if (pType == String.class) {
            Converter converter = Converter.getInstance();
            return converter.toString(pValue);
        }
        else {
            throw new ConversionException("Cannot convert " + pValue.getClass().getName() + " to " + pType.getName());
        }
    }

    /**
     * Creates an object from the given class' single argument constructor.
     *
     * @param pClass The class to create instance from
     * @param pParam The parameters to the constructor
     *
     * @return The object created from the constructor.
     *         If the constructor could not be invoked for any reason, null is
     *         returned.
     *
     * @throws InvocationTargetException if the constructor failed
     */
    // TODO: Move to ReflectUtil
    public static <T> T createInstance(Class<T> pClass, Object pParam)
            throws InvocationTargetException {
        return createInstance(pClass, new Object[] {pParam});
    }

    /**
     * Creates an object from the given class' constructor that matches
     * the given paramaters.
     *
     * @param pClass  The class to create instance from
     * @param pParams The parameters to the constructor
     *
     * @return The object created from the constructor.
     *         If the constructor could not be invoked for any reason, null is
     *         returned.
     *
     * @throws InvocationTargetException if the constructor failed
     */
    // TODO: Move to ReflectUtil
    public static <T> T createInstance(Class<T> pClass, Object... pParams)
            throws InvocationTargetException {
        T value;

        try {
            // Create param and argument arrays
            Class[] paramTypes = null;
            if (pParams != null && pParams.length > 0) {
                paramTypes = new Class[pParams.length];
                for (int i = 0; i < pParams.length; i++) {
                    paramTypes[i] = pParams[i].getClass();
                }
            }

            // Get constructor
            Constructor<T> constructor = pClass.getConstructor(paramTypes);

            // Invoke and create instance
            value = constructor.newInstance(pParams);
        }
        /* All this to let InvocationTargetException pass on */
        catch (NoSuchMethodException nsme) {
            return null;
        }
        catch (IllegalAccessException iae) {
            return null;
        }
        catch (IllegalArgumentException iarge) {
            return null;
        }
        catch (InstantiationException ie) {
            return null;
        }
        catch (ExceptionInInitializerError err) {
            return null;
        }

        return value;
    }

    /**
     * Gets an object from any given static method, with the given parameter.
     *
     * @param pClass  The class to invoke method on
     * @param pMethod The name of the method to invoke
     * @param pParam  The parameter to the method
     *
     * @return The object returned by the static method.
     *         If the return type of the method is a primitive type, it is wrapped in
     *         the corresponding wrapper object (int is wrapped in an Integer).
     *         If the return type of the method is void, null is returned.
     *         If the method could not be invoked for any reason, null is returned.
     *
     * @throws InvocationTargetException if the invocation failed
     */
    // TODO: Move to ReflectUtil
    // TODO: Rename to invokeStatic?
    public static Object invokeStaticMethod(Class<?> pClass, String pMethod, Object pParam)
            throws InvocationTargetException {

        return invokeStaticMethod(pClass, pMethod, new Object[] {pParam});
    }

    /**
     * Gets an object from any given static method, with the given parameter.
     *
     * @param pClass  The class to invoke method on
     * @param pMethod The name of the method to invoke
     * @param pParams The parameters to the method
     *
     * @return The object returned by the static method.
     *         If the return type of the method is a primitive type, it is wrapped in
     *         the corresponding wrapper object (int is wrapped in an Integer).
     *         If the return type of the method is void, null is returned.
     *         If the method could not be invoked for any reason, null is returned.
     *
     * @throws InvocationTargetException if the invocation failed
     */
    // TODO: Move to ReflectUtil
    // TODO: Rename to invokeStatic?
    public static Object invokeStaticMethod(Class<?> pClass, String pMethod, Object... pParams)
            throws InvocationTargetException {

        Object value = null;

        try {
            // Create param and argument arrays
            Class[] paramTypes = new Class[pParams.length];
            for (int i = 0; i < pParams.length; i++) {
                paramTypes[i] = pParams[i].getClass();
            }

            // Get method
            // *** If more than one such method is found in the class, and one
            // of these methods has a RETURN TYPE that is more specific than
            // any of the others, that method is reflected; otherwise one of
            // the methods is chosen ARBITRARILY.
            // java/lang/Class.html#getMethod(java.lang.String, java.lang.Class[])
            Method method = pClass.getMethod(pMethod, paramTypes);

            // Invoke public static method
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                value = method.invoke(null, pParams);
            }

        }
        /* All this to let InvocationTargetException pass on */
        catch (NoSuchMethodException nsme) {
            return null;
        }
        catch (IllegalAccessException iae) {
            return null;
        }
        catch (IllegalArgumentException iarge) {
            return null;
        }

        return value;
    }

    /**
     * Configures the bean according to the given mapping.
     * For each {@code Map.Entry} in {@code Map.values()},
     * a method named
     * {@code set + capitalize(entry.getKey())} is called on the bean,
     * with {@code entry.getValue()} as its argument.
     * <p>
     * Properties that has no matching set-method in the bean, are simply
     * discarded.
     * </p>
     *
     * @param pBean    The bean to configure
     * @param pMapping The mapping for the bean
     *
     * @throws NullPointerException if any of the parameters are null.
     * @throws InvocationTargetException if an error occurs when invoking the
     *         setter-method.
     */
    // TODO: Add a version that takes a ConfigurationErrorListener callback interface
    // TODO: ...or a boolean pFailOnError parameter
    // TODO: ...or return Exceptions as an array?!
    // TODO: ...or something whatsoever that makes clients able to determine something's not right 
    public static void configure(final Object pBean, final Map<String, ?> pMapping) throws InvocationTargetException {
        configure(pBean, pMapping, false);
    }

    /**
     * Configures the bean according to the given mapping.
     * For each {@code Map.Entry} in {@code Map.values()},
     * a method named
     * {@code set + capitalize(entry.getKey())} is called on the bean,
     * with {@code entry.getValue()} as its argument.
     * <p>
     * Optionally, lisp-style names are allowed, and automatically converted
     * to Java-style camel-case names.
     * </p>
     * <p>
     * Properties that has no matching set-method in the bean, are simply
     * discarded.
     * </p>
     *
     * @see StringUtil#lispToCamel(String)
     *
     * @param pBean    The bean to configure
     * @param pMapping The mapping for the bean
     * @param pLispToCamel Allow lisp-style names, and automatically convert
     *        them to Java-style camel-case.
     *
     * @throws NullPointerException if any of the parameters are null.
     * @throws InvocationTargetException if an error occurs when invoking the
     *         setter-method.
     */
    public static void configure(final Object pBean, final Map<String, ?> pMapping, final boolean pLispToCamel) throws InvocationTargetException {
        // Loop over properties in mapping
        for (final Map.Entry<String, ?> entry : pMapping.entrySet()) {
            try {
                // Configure each property in turn
                final String property = StringUtil.valueOf(entry.getKey());
                try {
                    setPropertyValue(pBean, property, entry.getValue());
                }
                catch (NoSuchMethodException ignore) {
                    // If invocation failed, convert lisp-style and try again
                    if (pLispToCamel && property.indexOf('-') > 0) {
                        setPropertyValue(pBean, StringUtil.lispToCamel(property, false), entry.getValue());
                    }
                }
            }
            catch (NoSuchMethodException nsme) {
                // This property was not configured
            }
            catch (IllegalAccessException iae) {
                // This property was not configured
            }
        }
    }
}
