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

package com.twelvemonkeys.lang;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * “If it walks like a duck, looks like a duck, quacks like a duck, it must be…”.
 * <p/>
 * Based on an idea found at
 * <a href="http://www.coconut-palm-software.com/the_visual_editor/?p=25">The Visual Editor</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/lang/DuckType.java#1 $
 *
 * @see java.lang.reflect.Proxy
 */
public final class DuckType {
    /*
    EXAMPLE:
     public ImageMgr(Object receiver, Image image) {
       if (!DuckType.instanceOf(IImageHolder.class, receiver)) {
         throw new ClassCastException("Cannot implement IImageHolder");
       }

       this.image = image;

        IImageHolder imageHolder = (IImageHolder) DuckType.implement(IImageHolder.class, receiver);
        imageHolder.setImage(image);
        imageHolder.addDisposeListener(this);
      }
    */

    // TODO: Implement some weak caching of proxy classes and instances
    // TODO: Make the proxy classes serializable...

    private DuckType() {}

    public static boolean instanceOf(Class pInterface, Object pObject) {
        return instanceOf(new Class[] {pInterface}, new Object[] {pObject});
    }

    public static boolean instanceOf(Class[] pInterfaces, Object pObject) {
        return instanceOf(pInterfaces, new Object[] {pObject});
    }

    public static boolean instanceOf(final Class[] pInterfaces, final Object[] pObjects) {
        // Get all methods of all Class in pInterfaces, and see if pObjects has
        // matching implementations

        // TODO: Possible optimization: If any of the interfaces are implemented
        // by one of the objects' classes, we don't need to find every method...

        for (int i = 0; i < pInterfaces.length; i++) {
            Class interfce = pInterfaces[i];

            Method[] methods = interfce.getMethods();

            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];

                //if (findMethodImplementation(method, getClasses(pObjects)) < 0) {
                //if (findMethodImplementation(method, getClasses(pObjects)) == null) {
                if (findMethodImplementation(method, pObjects) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    // TODO: Might be moved to ReflectUtil
    private static Class[] getClasses(final Object[] pObjects) {
        Class[] classes = new Class[pObjects.length];

        for (int i = 0; i < pObjects.length; i++) {
            classes[i] = pObjects[i].getClass();
        }

        return classes;
    }

    /**
     * Searches for a class that has a method maching the given signature.
     * Returns the index of the class in the {@code pClasses} array that has a
     * matching method.
     * If there is more than one class that has a matching method the first
     * index will be returned.
     * If there is no match in any of the classes, {@code -1} is returned.
     *
     * @param pMethod
     * @param pObjects
     *
     * @return the first index of the object in the {@code pObjects} array that
     * has a matching method, or {@code -1} if none was found.
     */
    // TODO: Might be moved to ReflectUtil
    //static int findMethodImplementation(final Method pMethod, final Class[] pClasses) {
    static MethodProxy findMethodImplementation(final Method pMethod, final Object[] pObjects) {
        // TODO: Optimization: Each getParameterTypes() invokation creates a
        // new clone of the array. If we do it once and store the refs, that
        // would be a good idea

        // Optimization, don't test class more than once
        Set tested = new HashSet(pObjects.length);

        for (int i = 0; i < pObjects.length; i++) {
            Class cls = pObjects[i].getClass();

            if (tested.contains(cls)) {
                continue;
            }
            else {
                tested.add(cls);
            }

            try {
                // NOTE: This test might be too restrictive
                // We could actually go ahead with
                // supertype parameters or subtype return types...
                // However, we should only do that after we have tried all
                // classes for direct mathces.
                Method method = cls.getMethod(pMethod.getName(),
                                              pMethod.getParameterTypes());

                if (matches(pMethod, method)) {
                    //return i;
                    // TODO: This is a waste of time if we are only testing if there's a method here...
                    return new MethodProxy(method, pObjects[i]);
                }
            }
            catch (NoSuchMethodException e) {
                // Ingore
            }
        }

        if (hasSuperTypes(pMethod.getParameterTypes())) {
            SortedSet uniqueMethods = new TreeSet();
            for (int i = 0; i < pObjects.length; i++) {
                Class cls = pObjects[i].getClass();

                Method[] methods = cls.getMethods();

                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];

                    // Now, for each method
                    // 1 test if the name matches
                    // 2 test if the parameter types match for superclass
                    // 3 Test return types for assignability?
                    if (pMethod.getName().equals(method.getName())
                            && isAssignableFrom(method.getParameterTypes(), pMethod.getParameterTypes())
                            && pMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                        // 4 TODO: How to find the most specific match?!
                        //return new MethodProxy(method, pObjects[i]);
                        uniqueMethods.add(new MethodProxy(method, pObjects[i]));
                    }
                }
            }
            if (uniqueMethods.size() == 1) {
                return (MethodProxy) uniqueMethods.first();
            }
            else {
                // TODO: We need to figure out what method is the best match..
            }
        }

        //return -1;
        return null;
    }

    private static boolean isAssignableFrom(Class[] pTypes, Class[] pSubTypes) {
        if (pTypes.length != pSubTypes.length) {
            return false;
        }

        for (int i = 0; i < pTypes.length; i++) {
            if (!pTypes[i].isAssignableFrom(pSubTypes[i])) {
                return false;
            }

        }
        return true;
    }

    private static boolean hasSuperTypes(Class[] pParameterTypes) {
        for (int i = 0; i < pParameterTypes.length; i++) {
            Class type = pParameterTypes[i];

            if (type != Object.class
                    && (type.isInterface() || type.getSuperclass() != null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests two {@code Method}s for match.
     * That is, they have same name and equal parameters.
     *
     * @param pLeft
     * @param pRight
     *
     * @return
     *
     * @see Method#equals(Object)
     */
    private static boolean matches(Method pLeft, Method pRight) {
        if (pLeft == pRight) {
            return true;
        }
        else if (pLeft.getName().equals(pRight.getName())
                && pLeft.getReturnType().isAssignableFrom(pRight.getReturnType())) {

            // Avoid unnecessary cloning
            Class[] params1 = pLeft.getParameterTypes();
            Class[] params2 = pRight.getParameterTypes();
            if (params1.length == params2.length) {
                for (int i = 0; i < params1.length; i++) {
                    if (params1[i] != params2[i]) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

    public static Object implement(Class pInterface, Object pObject) throws NoMatchingMethodException {
        return implement(new Class[] {pInterface}, new Object[] {pObject}, false);
    }

    public static Object implement(Class[] pInterfaces, Object pObject) throws NoMatchingMethodException {
        return implement(pInterfaces, new Object[] {pObject}, false);
    }

    // TODO: What about the interfaces pObjects allready implements?
    // TODO: Use first object as "identity"? Allow user to supply "indentity"
    // that is not exposed as part of the implemented interfaces?
    public static Object implement(final Class[] pInterfaces, final Object[] pObjects) throws NoMatchingMethodException {
        return implement(pInterfaces, pObjects, false);
    }

    public static Object implement(final Class[] pInterfaces, final Object[] pObjects, boolean pStubAbstract) throws NoMatchingMethodException {
        Map delegates = new HashMap(pObjects.length * 10);

        for (int i = 0; i < pInterfaces.length; i++) {
            Class interfce = pInterfaces[i];

            Method[] methods = interfce.getMethods();

            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];

                //int idx = findMethodImplementation(method, getClasses(pObjects));
                //Method impl = findMethodImplementation(method, getClasses(pObjects));
                MethodProxy impl = findMethodImplementation(method, pObjects);
                //if (idx < 0) {
                if (impl == null) {
                    // TODO: Maybe optionally create stubs that fails when invoked?!
                    if (pStubAbstract) {
                        impl = MethodProxy.createAbstract(method);
                    }
                    else {
                    throw new NoMatchingMethodException(interfce.getName() + "."
                                    + method.getName()
                                    + parameterTypesToString(method.getParameterTypes()));
                    }
                }

                if (!delegates.containsKey(method)) {
                    // TODO: Must find the correct object...
                    //delegates.put(method, new MethodProxy(method, pObjects[idx]));
                    delegates.put(method, impl);
                }
            }
        }

        // TODO: It's probably not good enough to use the current context class loader
        // TODO: Either let user specify classloader directly
        // TODO: ...or use one of the classloaders from pInterfaces or pObjects
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                      pInterfaces, new DelegationHandler(delegates));
    }

    private static String parameterTypesToString(Class[] pTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (pTypes != null) {
            for (int i = 0; i < pTypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Class c = pTypes[i];
                buf.append((c == null) ? "null" : c.getName());
            }
        }
        buf.append(")");
        return buf.toString();
    }

    static class MethodProxy {
        private final Method mMethod;
        private final Object mDelegate;

        private final static Object ABSTRACT_METHOD_DELEGATE = new Object() {
        };

        public static MethodProxy createAbstract(Method pMethod) {
            return new MethodProxy(pMethod, ABSTRACT_METHOD_DELEGATE) {
                public Object invoke(Object[] pArguments) throws Throwable {
                    throw abstractMehthodError();
                }
            };
        }

        public MethodProxy(Method pMethod, Object pDelegate) {
            if (pMethod == null) {
                throw new IllegalArgumentException("method == null");
            }
            if (pDelegate == null) {
                throw new IllegalArgumentException("delegate == null");
            }

            mMethod = pMethod;
            mDelegate = pDelegate;
        }

        public Object invoke(Object[] pArguments) throws Throwable {
            try {
                return mMethod.invoke(mDelegate, pArguments);
            }
            catch (IllegalAccessException e) {
                throw new Error(e); // This is an error in the impl
            }
            catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        Error abstractMehthodError() {
            return new AbstractMethodError(mMethod.toString());
        }

        public int hashCode() {
            return mMethod.hashCode() ^ mDelegate.hashCode();
        }

        public boolean equals(Object pOther) {
            if (pOther == this) {
                return true;
            }
            if (pOther instanceof MethodProxy) {
                MethodProxy other = (MethodProxy) pOther;
                return mMethod.equals(other.mMethod) && mDelegate.equals(other.mDelegate);
            }
            return false;
        }

        public String toString() {
            return mMethod.toString() + mDelegate.toString();
        }
    }

    public static class NoMatchingMethodException extends IllegalArgumentException {
        public NoMatchingMethodException() {
            super();
        }

        public NoMatchingMethodException(String s) {
            super(s);
        }

        public NoMatchingMethodException(Exception e) {
            super(e.getMessage());
            initCause(e);
        }
    }

    // TODO: Must handle identity...
    // TODO: equals/hashCode
    // TODO: Allow clients to pass in Identity subclasses?
    private static class DelegationHandler implements InvocationHandler {
        private final Map mDelegates;

        public DelegationHandler(Map pDelegates) {
            mDelegates = pDelegates;
        }

        public final Object invoke(Object pProxy, Method pMethod, Object[] pArguments)
                throws Throwable
        {
            if (pMethod.getDeclaringClass() == Object.class) {
                // Intercept equals/hashCode/toString
                String name = pMethod.getName();
                if (name.equals("equals")) {
                    return proxyEquals(pProxy, pArguments[0]);
                }
                else if (name.equals("hashCode")) {
                    return proxyHashCode(pProxy);
                }
                else if (name.equals("toString")) {
                    return proxyToString(pProxy);
                }

                // Other methods are handled by their default Object
                // implementations
                return pMethod.invoke(this, pArguments);
            }

            MethodProxy mp = (MethodProxy) mDelegates.get(pMethod);

            return mp.invoke(pArguments);
        }

        protected Integer proxyHashCode(Object pProxy) {
            //return new Integer(System.identityHashCode(pProxy));
            return new Integer(mDelegates.hashCode());
        }

        protected Boolean proxyEquals(Object pProxy, Object pOther) {
            return pProxy == pOther ||
                    (Proxy.isProxyClass(pOther.getClass())
                    && Proxy.getInvocationHandler(pOther) instanceof DelegationHandler
                    && ((DelegationHandler) Proxy.getInvocationHandler(pOther)).mDelegates.equals(mDelegates))
                    ? Boolean.TRUE : Boolean.FALSE;
        }

        protected String proxyToString(Object pProxy) {
            return pProxy.getClass().getName() + '@' +
                    Integer.toHexString(pProxy.hashCode());
        }
    }
}
