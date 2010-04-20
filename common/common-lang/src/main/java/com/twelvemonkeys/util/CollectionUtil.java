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

package com.twelvemonkeys.util;

import java.lang.reflect.Array;
import java.util.*;

/**
 * A utility class with some useful collection-related functions.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <A href="mailto:eirik.torske@twelvemonkeys.no">Eirik Torske</A>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/CollectionUtil.java#3 $
 * @todo move makeList and makeSet to StringUtil?
 * @see Collections
 * @see Arrays
 */
public final class CollectionUtil {

    /**
     * Testing only.
     *
     * @param pArgs command line arguents
     */
    @SuppressWarnings({"UnusedDeclaration", "UnusedAssignment", "unchecked"})
    public static void main(String[] pArgs) {
        test();

        int howMany = 1000;

        if (pArgs.length > 0) {
            howMany = Integer.parseInt(pArgs[0]);
        }
        long start;
        long end;

        /*
      int[] intArr1 = new int[] {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9
      };
      int[] intArr2 = new int[] {
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19
      };
      start = System.currentTimeMillis();

      for (int i = 0; i < howMany; i++) {
        intArr1 = (int[]) mergeArrays(intArr1, 0, intArr1.length, intArr2, 0, intArr2.length);

      }
      end = System.currentTimeMillis();

      System.out.println("mergeArrays: " + howMany + " * " + intArr2.length + "  ints took " + (end - start) + " milliseconds (" + intArr1.length
                         + ")");
    */

        ////////////////////////////////
        String[] stringArr1 = new String[]{
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
        };

        /*
        String[] stringArr2 = new String[] {
          "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
        };

        start = System.currentTimeMillis();
        for (int i = 0; i < howMany; i++) {
          stringArr1 = (String[]) mergeArrays(stringArr1, 0, stringArr1.length, stringArr2, 0, stringArr2.length);

        }
        end = System.currentTimeMillis();
        System.out.println("mergeArrays: " + howMany + " * " + stringArr2.length + "  Strings took " + (end - start) + " milliseconds ("
                           + stringArr1.length + ")");


          start   = System.currentTimeMillis();
          while (intArr1.length > stringArr2.length) {
            intArr1 = (int[]) subArray(intArr1, 0, intArr1.length - stringArr2.length);

          }
          end = System.currentTimeMillis();

          System.out.println("subArray: " + howMany + " * " + intArr2.length + "  ints took " + (end - start) + " milliseconds (" + intArr1.length
                             + ")");

          start = System.currentTimeMillis();
          while (stringArr1.length > stringArr2.length) {
            stringArr1 = (String[]) subArray(stringArr1, stringArr2.length);

          }
          end = System.currentTimeMillis();
          System.out.println("subArray: " + howMany + " * " + stringArr2.length + "  Strings took " + (end - start) + " milliseconds ("
                             + stringArr1.length + ")");

    */
        stringArr1 = new String[]{
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
        };
        System.out.println("\nFilterIterators:\n");
        List list = Arrays.asList(stringArr1);
        Iterator iter = new FilterIterator(list.iterator(), new FilterIterator.Filter() {

            public boolean accept(Object pElement) {
                return ((String) pElement).length() > 5;
            }
        });

        while (iter.hasNext()) {
            String str = (String) iter.next();

            System.out.println(str + " has more than 5 letters!");
        }
        iter = new FilterIterator(list.iterator(), new FilterIterator.Filter() {

            public boolean accept(Object pElement) {
                return ((String) pElement).length() <= 5;
            }
        });

        while (iter.hasNext()) {
            String str = (String) iter.next();

            System.out.println(str + " has less than, or exactly 5 letters!");
        }
        start = System.currentTimeMillis();

        for (int i = 0; i < howMany; i++) {
            iter = new FilterIterator(list.iterator(), new FilterIterator.Filter() {

                public boolean accept(Object pElement) {
                    return ((String) pElement).length() <= 5;
                }
            });
            while (iter.hasNext()) {
                iter.next();
                System.out.print("");
            }
        }

//        end = System.currentTimeMillis();
//        System.out.println("Time: " + (end - start) + " ms");
//        System.out.println("\nClosureCollection:\n");
//        forEach(list, new Closure() {
//
//            public void execute(Object pElement) {
//
//                String str = (String) pElement;
//
//                if (str.length() > 5) {
//                    System.out.println(str + " has more than 5 letters!");
//                }
//                else {
//                    System.out.println(str + " has less than, or exactly 5 letters!");
//                }
//            }
//        });
//        start = System.currentTimeMillis();
//        for (int i = 0; i < howMany; i++) {
//            forEach(list, new Closure() {
//
//                public void execute(Object pElement) {
//
//                    String str = (String) pElement;
//
//                    if (str.length() <= 5) {
//                        System.out.print("");
//                    }
//                }
//            });
//        }
//        end = System.currentTimeMillis();
//        System.out.println("Time: " + (end - start) + " ms");
    }

    // Disallow creating objects of this type
    private CollectionUtil() {}

    /**
     * Merges two arrays into a new array. Elements from array1 and array2 will
     * be copied into a new array, that has array1.length + array2.length
     * elements.
     *
     * @param pArray1 First array
     * @param pArray2 Second array, must be compatible with (assignable from)
     *                the first array
     * @return A new array, containing the values of array1 and array2. The
     *         array (wrapped as an object), will have the length of array1 +
     *         array2, and can be safely cast to the type of the array1
     *         parameter.
     * @see #mergeArrays(Object,int,int,Object,int,int)
     * @see java.lang.System#arraycopy(Object,int,Object,int,int)
     */
    public static Object mergeArrays(Object pArray1, Object pArray2) {
        return mergeArrays(pArray1, 0, Array.getLength(pArray1), pArray2, 0, Array.getLength(pArray2));
    }

    /**
     * Merges two arrays into a new array. Elements from pArray1 and pArray2 will
     * be copied into a new array, that has pLength1 + pLength2 elements.
     *
     * @param pArray1  First array
     * @param pOffset1 the offset into the first array
     * @param pLength1 the number of elements to copy from the first array
     * @param pArray2  Second array, must be compatible with (assignable from)
     *                 the first array
     * @param pOffset2 the offset into the second array
     * @param pLength2 the number of elements to copy from the second array
     * @return A new array, containing the values of pArray1 and pArray2. The
     *         array (wrapped as an object), will have the length of pArray1 +
     *         pArray2, and can be safely cast to the type of the pArray1
     *         parameter.
     * @see java.lang.System#arraycopy(Object,int,Object,int,int)
     */
    @SuppressWarnings({"SuspiciousSystemArraycopy"})
    public static Object mergeArrays(Object pArray1, int pOffset1, int pLength1, Object pArray2, int pOffset2, int pLength2) {
        Class class1 = pArray1.getClass();
        Class type = class1.getComponentType();

        // Create new array of the new length
        Object array = Array.newInstance(type, pLength1 + pLength2);

        System.arraycopy(pArray1, pOffset1, array, 0, pLength1);
        System.arraycopy(pArray2, pOffset2, array, pLength1, pLength2);
        return array;
    }

    /**
     * Creates an array containing a subset of the original array.
     * If the sub array is same length as the original
     * ({@code pStart == 0}), the original array will be returned.
     *
     * @param pArray the origianl array
     * @param pStart the start index of the original array
     * @return a subset of the original array, or the original array itself,
     *         if {@code pStart} is 0.
     *
     * @throws IllegalArgumentException if {@code pArray} is {@code null} or
     *         if {@code pArray} is not an array.
     * @throws ArrayIndexOutOfBoundsException if {@code pStart} < 0
     */
    public static Object subArray(Object pArray, int pStart) {
        return subArray(pArray, pStart, -1);
    }

    /**
     * Creates an array containing a subset of the original array.
     * If the {@code pLength} parameter is negative, it will be ignored.
     * If there are not {@code pLength} elements in the original array
     * after {@code pStart}, the {@code pLength} paramter will be
     * ignored.
     * If the sub array is same length as the original, the original array will
     * be returned.
     *
     * @param pArray  the origianl array
     * @param pStart  the start index of the original array
     * @param pLength the length of the new array
     * @return a subset of the original array, or the original array itself,
     *         if {@code pStart} is 0 and {@code pLength} is either
     *         negative, or greater or equal to {@code pArray.length}.
     *
     * @throws IllegalArgumentException if {@code pArray} is {@code null} or
     *         if {@code pArray} is not an array.
     * @throws ArrayIndexOutOfBoundsException if {@code pStart} < 0
     */
    @SuppressWarnings({"SuspiciousSystemArraycopy"})
    public static Object subArray(Object pArray, int pStart, int pLength) {
        if (pArray == null) {
            throw new IllegalArgumentException("array == null");
        }

        // Get component type
        Class type;

        // Sanity check start index
        if (pStart < 0) {
            throw new ArrayIndexOutOfBoundsException(pStart + " < 0");
        }
        // Check if argument is array
        else if ((type = pArray.getClass().getComponentType()) == null) {
            // NOTE: No need to test class.isArray(), really
            throw new IllegalArgumentException("Not an array: " + pArray);
        }

        // Store original length
        int originalLength = Array.getLength(pArray);

        // Find new length, stay within bounds
        int newLength = (pLength < 0)
                ? Math.max(0, originalLength - pStart)
                : Math.min(pLength, Math.max(0, originalLength - pStart));

        // Store result
        Object result;

        if (newLength < originalLength) {
            // Create subarray & copy into
            result = Array.newInstance(type, newLength);
            System.arraycopy(pArray, pStart, result, 0, newLength);
        }
        else {
            // Just return original array
            // NOTE: This can ONLY happen if pStart == 0
            result = pArray;
        }

        // Return
        return result;
    }

    public static <T> Iterator<T> iterator(final Enumeration<T> pEnum) {
        return new Iterator<T>() {
            public boolean hasNext() {
                return pEnum.hasMoreElements();
            }

            public T next() {
                return pEnum.nextElement();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Adds all elements of the iterator to the collection.
     *
     * @param pCollection the collection
     * @param pIterator the elements to add
     *
     * @throws UnsupportedOperationException if {@code add} is not supported by
     *         the given collection.
     * @throws ClassCastException class of the specified element prevents it
     *         from being added to this collection.
     * @throws NullPointerException if the specified element is null and this
     *         collection does not support null elements.
     * @throws IllegalArgumentException some aspect of this element prevents
     *         it from being added to this collection.
     */
    public static <E> void addAll(Collection<E> pCollection, Iterator<? extends E> pIterator) {
        while (pIterator.hasNext()) {
            pCollection.add(pIterator.next());
        }
    }

    // Is there a usecase where Arrays.asList(pArray).iterator() can't ne used?
    /**
     * Creates a thin {@link Iterator} wrapper around an array.
     *
     * @param pArray the array to iterate
     * @return a new {@link ListIterator}
     * @throws IllegalArgumentException if {@code pArray} is {@code null},
     *         {@code pStart < 0}, or
     *         {@code pLength > pArray.length - pStart}
     */
    public static <E> ListIterator<E> iterator(final E[] pArray) {
        return iterator(pArray, 0, pArray.length);
    }

    /**
     * Creates a thin {@link Iterator} wrapper around an array.
     *
     * @param pArray the array to iterate
     * @param pStart the offset into the array
     * @param pLength the number of elements to include in the iterator
     * @return a new {@link ListIterator}
     * @throws IllegalArgumentException if {@code pArray} is {@code null},
     *         {@code pStart < 0}, or
     *         {@code pLength > pArray.length - pStart}
     */
    public static <E> ListIterator<E> iterator(final E[] pArray, final int pStart, final int pLength) {
        return new ArrayIterator<E>(pArray, pStart, pLength);
    }

    /**
     * Creates an inverted mapping of the key/value pairs in the given map.
     *
     * @param pSource the source map
     * @return a new {@code Map} of same type as {@code pSource}
     * @throws IllegalArgumentException if {@code pSource == null},
     *         or if a new map can't be instantiated,
     *         or if source map contains duplaicates.
     *
     * @see #invert(java.util.Map, java.util.Map, DuplicateHandler)
     */
    public static <K, V> Map<V, K> invert(Map<K, V> pSource) {
        return invert(pSource, null, null);
    }

    /**
     * Creates an inverted mapping of the key/value pairs in the given map.
     * Optionally, a duplicate handler may be specified, to resolve duplicate keys in the result map.
     *
     * @param pSource the source map
     * @param pResult the map used to contain the result, may be {@code null},
     *        in that case a new {@code Map} of same type as {@code pSource} is created.
     *        The result map <em>should</em> be empty, otherwise duplicate values will need to be resolved.
     * @param pHandler duplicate handler, may be {@code null} if source map don't contain dupliate values
     * @return {@code pResult}, or a new {@code Map} if {@code pResult == null}
     * @throws IllegalArgumentException if {@code pSource == null},
     *         or if result map is {@code null} and a new map can't be instantiated,
     *         or if source map contains duplicate values and {@code pHandler == null}.
     */
    // TODO: Create a better duplicate handler, that takes Entries as parameters and returns an Entry
    public static <K, V> Map<V, K> invert(Map<K, V> pSource, Map<V, K> pResult, DuplicateHandler<K> pHandler) {
        if (pSource == null) {
            throw new IllegalArgumentException("source == null");
        }

        Map<V, K> result = pResult;
        if (result == null) {
            try {
                //noinspection unchecked
                result = pSource.getClass().newInstance();
            }
            catch (InstantiationException e) {
                // Handled below
            }
            catch (IllegalAccessException e) {
                // Handled below
            }

            if (result == null) {
                throw new IllegalArgumentException("result == null and source class " + pSource.getClass() + " cannot be instantiated.");
            }
        }

        // Copy entries into result map, inversed
        Set<Map.Entry<K, V>> entries = pSource.entrySet();
        for (Map.Entry<K, V> entry : entries) {
            V newKey = entry.getValue();
            K newValue = entry.getKey();

            // Handle dupliates
            if (result.containsKey(newKey)) {
                if (pHandler != null) {
                    newValue = pHandler.resolve(result.get(newKey), newValue);
                }
                else {
                    throw new IllegalArgumentException("Result would include duplicate keys, but no DuplicateHandler specified.");
                }
            }

            result.put(newKey, newValue);
        }

        return result;
    }

    public static <T> Comparator<T> reverseOrder(Comparator<T> pOriginal) {
        return new ReverseComparator<T>(pOriginal);
    }

    private static class ReverseComparator<T> implements Comparator<T> {
        private Comparator<T> mComparator;

        public ReverseComparator(Comparator<T> pComparator) {
            mComparator = pComparator;
        }


        public int compare(T pLeft, T pRight) {
            int result = mComparator.compare(pLeft, pRight);

            // We can't simply return -result, as -Integer.MIN_VALUE == Integer.MIN_VALUE.
            return -(result | (result >>> 1));
        }
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    static <T extends Iterator<? super E>, E> T generify(final Iterator<?> pIterator, final Class<E> pElementType) {
        return (T) pIterator;
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    static <T extends Collection<? super E>, E> T generify(final Collection<?> pCollection, final Class<E> pElementType) {
        return (T) pCollection;
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    static <T extends Map<? super K, ? super V>, K, V> T generify(final Map<?, ?> pMap, final Class<K> pKeyType, final Class<V> pValueType) {
        return (T) pMap;
    }

    @SuppressWarnings({"unchecked"})
    static <T extends Collection<? super E>, E> T generify2(Collection<?> pCollection) {
        return (T) pCollection;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    static void test() {
        List list = Collections.singletonList("foo");
        @SuppressWarnings({"unchecked"})
        Set set = new HashSet(list);

        List<String> strs0 = CollectionUtil.generify(list, String.class);
        List<Object> objs0 = CollectionUtil.generify(list, String.class);
//        List<String> strs01 = CollectionUtil.generify(list, Object.class); // Not okay
        try {
            List<String> strs1 = CollectionUtil.generify(set, String.class); // Not ok, runtime CCE unless set is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        try {
            ArrayList<String> strs01 = CollectionUtil.generify(list, String.class); // Not ok, runtime CCE unless list is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        Set<String> setstr1 = CollectionUtil.generify(set, String.class);
        Set<Object> setobj1 = CollectionUtil.generify(set, String.class);
        try {
            Set<Object> setobj44 = CollectionUtil.generify(list, String.class);  // Not ok, runtime CCE unless list is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }

        List<String> strs2 = CollectionUtil.<List<String>, String>generify2(list);
        List<Object> objs2 = CollectionUtil.<List<Object>, String>generify2(list);
//        List<String> morestrs = CollectionUtil.<List<Object>, String>generify2(list); // Not ok
        try {
            List<String> strs3 = CollectionUtil.<List<String>, String>generify2(set); // Not ok, runtime CCE unless set is null
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static class ArrayIterator<E> implements ListIterator<E> {
        private int mIndex;
        private final int mStart;
        private final int mLength;
        private final E[] mArray;

        public ArrayIterator(E[] pArray, int pStart, int pLength) {
            if (pArray == null) {
                throw new IllegalArgumentException("array == null");
            }
            if (pStart < 0) {
                throw new IllegalArgumentException("start < 0");
            }
            if (pLength > pArray.length - pStart) {
                throw new IllegalArgumentException("length > array.length - start");
            }
            mArray = pArray;
            mStart = pStart;
            mLength = pLength;
            mIndex = mStart;
        }

        public boolean hasNext() {
            return mIndex < mLength + mStart;
        }

        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                return mArray[mIndex++];
            }
            catch (ArrayIndexOutOfBoundsException e) {
                NoSuchElementException nse = new NoSuchElementException(e.getMessage());
                nse.initCause(e);
                throw nse;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void add(E pElement) {
            throw new UnsupportedOperationException();
        }

        public boolean hasPrevious() {
            return mIndex > mStart;
        }

        public int nextIndex() {
            return mIndex + 1;
        }

        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }

            try {
                return mArray[mIndex--];
            }
            catch (ArrayIndexOutOfBoundsException e) {
                NoSuchElementException nse = new NoSuchElementException(e.getMessage());
                nse.initCause(e);
                throw nse;
            }
        }

        public int previousIndex() {
            return mIndex - 1;
        }

        public void set(E pElement) {
            mArray[mIndex] = pElement;
        }
    }
}