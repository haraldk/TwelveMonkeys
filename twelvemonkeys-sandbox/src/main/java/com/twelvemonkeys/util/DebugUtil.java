/****************************************************
 *                                                  *
 *             (c) 2000-2003 TwelveMonkeys                *
 *             All rights reserved                  *
 *             http://www.twelvemonkeys.no                 *
 *                                                  *
 *   $RCSfile: DebugUtil.java,v $                
 *   @version  $Revision: #2 $                            
 *   $Date: 2009/06/19 $                    
 *                                                  * 
 *   @author  Last modified by: $Author: haku $                            
 *                                                  *
 ****************************************************/



/*
 * Produced (p) 2002 TwelveMonkeys
 * Address : Svovelstikka 1, Box 6432 Etterstad, 0605 Oslo, Norway.
 * Phone   : +47 22 57 70 00
 * Fax     : +47 22 57 70 70
 */
package com.twelvemonkeys.util;


import com.twelvemonkeys.lang.StringUtil;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.util.*;


/**
 * A utility class to simplify debugging.
 * This includes viewing generic data structures, printing timestamps, printing object info and more...
 * <i>NB! Only use this class for instrumentation purposes</i>
 * <p>
 * @author <a href="mailto:eirik.torske@twelvemonkeys.no">Eirik Torske</a>
 */
@Deprecated
public class DebugUtil {

  // Constants

  /** Field PRINTSTREAM_IS_NULL_ERROR_MESSAGE           */
  public static final String PRINTSTREAM_IS_NULL_ERROR_MESSAGE = "PrintStream is null";

  /** Field OBJECT_IS_NULL_ERROR_MESSAGE           */
  public static final String OBJECT_IS_NULL_ERROR_MESSAGE = "Object is null";

  /** Field INTARRAY_IS_NULL_ERROR_MESSAGE           */
  public static final String INTARRAY_IS_NULL_ERROR_MESSAGE = "int array is null";

  /** Field STRINGARRAY_IS_NULL_ERROR_MESSAGE           */
  public static final String STRINGARRAY_IS_NULL_ERROR_MESSAGE = "String array is null";

  /** Field ENUMERATION_IS_NULL_ERROR_MESSAGE           */
  public static final String ENUMERATION_IS_NULL_ERROR_MESSAGE = "Enumeration is null";

  /** Field COLLECTION_IS_NULL_ERROR_MESSAGE           */
  public static final String COLLECTION_IS_NULL_ERROR_MESSAGE = "Collection is null";

  /** Field COLLECTION_IS_EMPTY_ERROR_MESSAGE           */
  public static final String COLLECTION_IS_EMPTY_ERROR_MESSAGE = "Collection contains no elements";

  /** Field MAP_IS_NULL_ERROR_MESSAGE           */
  public static final String MAP_IS_NULL_ERROR_MESSAGE = "Map is null";

  /** Field MAP_IS_EMPTY_ERROR_MESSAGE           */
  public static final String MAP_IS_EMPTY_ERROR_MESSAGE = "Map contains no elements";

  /** Field PROPERTIES_IS_NULL_ERROR_MESSAGE           */
  public static final String PROPERTIES_IS_NULL_ERROR_MESSAGE = "Properties is null";

  /** Field PROPERTIES_IS_EMPTY_ERROR_MESSAGE           */
  public static final String PROPERTIES_IS_EMPTY_ERROR_MESSAGE = "Properties contains no elements";

  /** Field CALENDAR_IS_NULL_ERROR_MESSAGE           */
  public static final String CALENDAR_IS_NULL_ERROR_MESSAGE = "Calendar is null";

  /** Field CALENDAR_CAUSATION_ERROR_MESSAGE           */
  public static final String CALENDAR_CAUSATION_ERROR_MESSAGE = "The causation of the calendars is wrong";

  /** Field TIMEDIFFERENCES_IS_NULL_ERROR_MESSAGE           */
  public static final String TIMEDIFFERENCES_IS_NULL_ERROR_MESSAGE = "Inner TimeDifference object is null";

  /** Field TIMEDIFFERENCES_WRONG_DATATYPE_ERROR_MESSAGE           */
  public static final String TIMEDIFFERENCES_WRONG_DATATYPE_ERROR_MESSAGE =
    "Element in TimeDifference collection is not a TimeDifference object";

  /** Field DEBUG           */
  public static final String DEBUG = "**** external debug: ";

  /** Field INFO           */
  public static final String INFO = "**** external info: ";

  /** Field WARNING           */
  public static final String WARNING = "**** external warning: ";

  /** Field ERROR           */
  public static final String ERROR = "**** external error: ";

  /**
   * Builds a prefix message to be used in front of <i>info</i> messages for identification purposes.
   * The message format is:
   * <pre>
   * **** external info: [timestamp] [class name]:
   * </pre>
   * <p>
   * @param pObject the {@code java.lang.Object} to be debugged. If the object ia a {@code java.lang.String} object, it is assumed that it is the class name given directly.
   * @return a prefix for an info message.
   */
  public static String getPrefixInfoMessage(final Object pObject) {

    StringBuilder buffer = new StringBuilder();

    buffer.append(INFO);
    buffer.append(getTimestamp());
    buffer.append(" ");
    if (pObject == null) {
      buffer.append("[unknown class]");
    } else {
      if (pObject instanceof String) {
        buffer.append((String) pObject);
      } else {
        buffer.append(getClassName(pObject));
      }
    }
    buffer.append(":   ");
    return buffer.toString();
  }

  /**
   * Builds a prefix message to be used in front of <i>debug</i> messages for identification purposes.
   * The message format is:
   * <pre>
   * **** external debug: [timestamp] [class name]:
   * </pre>
   * <p>
   * @param pObject the {@code java.lang.Object} to be debugged. If the object ia a {@code java.lang.String} object, it is assumed that it is the class name given directly.
   * @return a prefix for a debug message.
   */
  public static String getPrefixDebugMessage(final Object pObject) {

    StringBuilder buffer = new StringBuilder();

    buffer.append(DEBUG);
    buffer.append(getTimestamp());
    buffer.append(" ");
    if (pObject == null) {
      buffer.append("[unknown class]");
    } else {
      if (pObject instanceof String) {
        buffer.append((String) pObject);
      } else {
        buffer.append(getClassName(pObject));
      }
    }
    buffer.append(":   ");
    return buffer.toString();
  }

  /**
   * Builds a prefix message to be used in front of <i>warning</i> messages for identification purposes.
   * The message format is:
   * <pre>
   * **** external warning: [timestamp] [class name]:
   * </pre>
   * <p>
   * @param pObject the {@code java.lang.Object} to be debugged. If the object ia a {@code java.lang.String} object, it is assumed that it is the class name given directly.
   * @return a prefix for a warning message.
   */
  public static String getPrefixWarningMessage(final Object pObject) {

    StringBuilder buffer = new StringBuilder();

    buffer.append(WARNING);
    buffer.append(getTimestamp());
    buffer.append(" ");
    if (pObject == null) {
      buffer.append("[unknown class]");
    } else {
      if (pObject instanceof String) {
        buffer.append((String) pObject);
      } else {
        buffer.append(getClassName(pObject));
      }
    }
    buffer.append(":   ");
    return buffer.toString();
  }

  /**
   * Builds a prefix message to be used in front of <i>error</i> messages for identification purposes.
   * The message format is:
   * <pre>
   * **** external error: [timestamp] [class name]:
   * </pre>
   * <p>
   * @param pObject the {@code java.lang.Object} to be debugged. If the object ia a {@code java.lang.String} object, it is assumed that it is the class name given directly.
   * @return a prefix for an error message.
   */
  public static String getPrefixErrorMessage(final Object pObject) {

    StringBuilder buffer = new StringBuilder();

    buffer.append(ERROR);
    buffer.append(getTimestamp());
    buffer.append(" ");
    if (pObject == null) {
      buffer.append("[unknown class]");
    } else {
      if (pObject instanceof String) {
        buffer.append((String) pObject);
      } else {
        buffer.append(getClassName(pObject));
      }
    }
    buffer.append(":   ");
    return buffer.toString();
  }

  /**
   * The "default" method that invokes a given method of an object and prints the results to a {@code java.io.PrintStream}.<br>
   * The method for invocation must have no formal parameters. If the invoking method does not exist, the {@code toString()} method is called.
   * The {@code toString()} method of the returning object is called.
   * <p>
   * @param pObject the {@code java.lang.Object} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printDebug(final Object pObject, final String pMethodName, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pObject == null) {
      pPrintStream.println(OBJECT_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (!StringUtil.isEmpty(pMethodName)) {
      try {
        Method objectMethod = pObject.getClass().getMethod(pMethodName, null);
        Object retVal       = objectMethod.invoke(pObject, null);

        if (retVal != null) {
          printDebug(retVal, null, pPrintStream);
        } else {
          throw new Exception();
        }
      } catch (Exception e) {

        // Default
        pPrintStream.println(pObject.toString());
      }
    } else {  // Ultimate default
      pPrintStream.println(pObject.toString());
    }
  }

  /**
   * Prints the object's {@code toString()} method to a {@code java.io.PrintStream}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printDebug(final Object pObject, final PrintStream pPrintStream) {
    printDebug(pObject, null, pPrintStream);
  }

  /**
   * Prints the object's {@code toString()} method to {@code System.out}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be printed.
   */
  public static void printDebug(final Object pObject) {
    printDebug(pObject, System.out);
  }

  /**
   * Prints a line break.
   */
  public static void printDebug() {
    System.out.println();
  }

  /**
   * Prints a primitive {@code boolean} to {@code System.out}.
   * <p>
   * @param pBoolean the {@code boolean} to be printed.
   */
  public static void printDebug(final boolean pBoolean) {
    printDebug(new Boolean(pBoolean).toString());
  }

  /**
   * Prints a primitive {@code int} to {@code System.out}.
   * <p>
   *
   * @param pInt
   */
  public static void printDebug(final int pInt) {
    printDebug(new Integer(pInt).toString());
  }

  /**
   * Prints the content of a {@code int[]} to a {@code java.io.PrintStream}.
   * <p>
   * @param pIntArray the {@code int[]} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printDebug(final int[] pIntArray, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pIntArray == null) {
      pPrintStream.println(INTARRAY_IS_NULL_ERROR_MESSAGE);
      return;
    }
    for (int i = 0; i < pIntArray.length; i++) {
      pPrintStream.println(pIntArray[i]);
    }
  }

  /**
   * Prints the content of a {@code int[]} to {@code System.out}.
   * <p>
   * @param pIntArray the {@code int[]} to be printed.
   */
  public static void printDebug(final int[] pIntArray) {
    printDebug(pIntArray, System.out);
  }

  /**
   * Prints a number of character check methods from the {@code java.lang.Character} class to a {@code java.io.PrintStream}.
   * <p>
   * @param pChar the {@code java.lang.char} to be debugged.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printDebug(final char pChar, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    pPrintStream.println("Character.getNumericValue(pChar): " + Character.getNumericValue(pChar));
    pPrintStream.println("Character.getType(pChar): " + Character.getType(pChar));
    pPrintStream.println("pChar.hashCode(): " + new Character(pChar).hashCode());
    pPrintStream.println("Character.isDefined(pChar): " + Character.isDefined(pChar));
    pPrintStream.println("Character.isDigit(pChar): " + Character.isDigit(pChar));
    pPrintStream.println("Character.isIdentifierIgnorable(pChar): " + Character.isIdentifierIgnorable(pChar));
    pPrintStream.println("Character.isISOControl(pChar): " + Character.isISOControl(pChar));
    pPrintStream.println("Character.isJavaIdentifierPart(pChar): " + Character.isJavaIdentifierPart(pChar));
    pPrintStream.println("Character.isJavaIdentifierStart(pChar): " + Character.isJavaIdentifierStart(pChar));
    pPrintStream.println("Character.isLetter(pChar): " + Character.isLetter(pChar));
    pPrintStream.println("Character.isLetterOrDigit(pChar): " + Character.isLetterOrDigit(pChar));
    pPrintStream.println("Character.isLowerCase(pChar): " + Character.isLowerCase(pChar));
    pPrintStream.println("Character.isSpaceChar(pChar): " + Character.isSpaceChar(pChar));
    pPrintStream.println("Character.isTitleCase(pChar): " + Character.isTitleCase(pChar));
    pPrintStream.println("Character.isUnicodeIdentifierPart(pChar): " + Character.isUnicodeIdentifierPart(pChar));
    pPrintStream.println("Character.isUnicodeIdentifierStart(pChar): " + Character.isUnicodeIdentifierStart(pChar));
    pPrintStream.println("Character.isUpperCase(pChar): " + Character.isUpperCase(pChar));
    pPrintStream.println("Character.isWhitespace(pChar): " + Character.isWhitespace(pChar));
    pPrintStream.println("pChar.toString(): " + new Character(pChar).toString());
  }

  /**
   * Prints a number of character check methods from the {@code java.lang.Character} class to {@code System.out}.
   * <p>
   * @param pChar the {@code java.lang.char} to be debugged.
   */
  public static void printDebug(final char pChar) {
    printDebug(pChar, System.out);
  }

  /**
   * Prints the content of a {@code java.lang.String[]} to a {@code java.io.PrintStream}.
   * <p>
   * @param pStringArray the {@code java.lang.String[]} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printDebug(final String[] pStringArray, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pStringArray == null) {
      pPrintStream.println(STRINGARRAY_IS_NULL_ERROR_MESSAGE);
      return;
    }
    for (int i = 0; i < pStringArray.length; i++) {
      pPrintStream.println(pStringArray[i]);
    }
  }

  /**
   * Prints the content of a {@code java.lang.String[]} to {@code System.out}.
   * <p>
   * @param pStringArray the {@code java.lang.String[]} to be printed.
   */
  public static void printDebug(final String[] pStringArray) {
    printDebug(pStringArray, System.out);
  }

  /**
   * Invokes a given method of every element in a {@code java.util.Enumeration} and prints the results to a {@code java.io.PrintStream}.
   * The method to be invoked must have no formal parameters.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pEnumeration the {@code java.util.Enumeration} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked on each collection element.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Enumeration.html">{@code java.util.Enumeration}</a>
   */
  public static void printDebug(final Enumeration pEnumeration, final String pMethodName, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pEnumeration == null) {
      pPrintStream.println(ENUMERATION_IS_NULL_ERROR_MESSAGE);
      return;
    }
    while (pEnumeration.hasMoreElements()) {
      printDebug(pEnumeration.nextElement(), pMethodName, pPrintStream);
    }
  }

  /**
   * Invokes a given method of every element in a {@code java.util.Enumeration} and prints the results to a {@code java.io.PrintStream}.
   * The method to be invoked must have no formal parameters.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pEnumeration the {@code java.util.Enumeration} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked on each collection element.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Enumeration.html">{@code java.util.Enumeration}</a>
   */
  public static void printDebug(final Enumeration pEnumeration, final String pMethodName) {
    printDebug(pEnumeration, pMethodName, System.out);
  }

  /**
   * Invokes a given method of every element in a {@code java.util.Enumeration} and prints the results to a {@code java.io.PrintStream}.
   * The method to be invoked must have no formal parameters. The default is calling an element's {@code toString()} method.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pEnumeration the {@code java.util.Enumeration} to be printed.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Enumeration.html">{@code java.util.Enumeration}</a>
   */
  public static void printDebug(final Enumeration pEnumeration) {
    printDebug(pEnumeration, null, System.out);
  }

  /**
   * Invokes a given method of every element in a {@code java.util.Collection} and prints the results to a {@code java.io.PrintStream}.
   * The method to be invoked must have no formal parameters.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * Be aware that the {@code Collection} interface embraces a large portion of the bulk data types in the {@code java.util} package,
   * e.g. {@code List}, {@code Set}, {@code Vector} and {@code HashSet}.
   * <p>
   * For debugging of arrays, use the method <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Arrays.html#asList(java.lang.Object[])">{@code java.util.Arrays.asList(Object[])}</a> method for converting the object array to a list before calling this method.
   * <p>
   * @param pCollection the {@code java.util.Collection} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked on each collection element.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Collection.html">{@code java.util.Collection}</a>
   */
  public static void printDebug(final Collection pCollection, final String pMethodName, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pCollection == null) {
      pPrintStream.println(COLLECTION_IS_NULL_ERROR_MESSAGE);
      return;
    } else if (pCollection.isEmpty()) {
      pPrintStream.println(COLLECTION_IS_EMPTY_ERROR_MESSAGE);
      return;
    }
    for (Iterator i = pCollection.iterator(); i.hasNext(); ) {
      printDebug(i.next(), pMethodName, pPrintStream);
    }
  }

  /**
   * Invokes a given method of every element in a {@code java.util.Collection} and prints the results to {@code System.out}.
   * The method to be invoked must have no formal parameters.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * Be aware that the {@code Collection} interface embraces a large portion of the bulk data types in the {@code java.util} package,
   * e.g. {@code List}, {@code Set}, {@code Vector} and {@code HashSet}.
   * <p>
   * For debugging of arrays, use the method <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Arrays.html#asList(java.lang.Object[])">{@code java.util.Arrays.asList(Object[])}</a> method for converting the object array to a list before calling this method.
   * <p>
   * @param pCollection the {@code java.util.Collection} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked on each collection element.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Collection.html">{@code java.util.Collection}</a>
   */
  public static void printDebug(final Collection pCollection, final String pMethodName) {
    printDebug(pCollection, pMethodName, System.out);
  }

  /**
   * Prints the content of a {@code java.util.Collection} to a {@code java.io.PrintStream}.
   * <p>
   * Not all data types are supported so far. The default is calling an element's {@code toString()} method.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * Be aware that the {@code Collection} interface embraces a large portion of the bulk data types in the {@code java.util} package,
   * e.g. {@code List}, {@code Set}, {@code Vector} and {@code HashSet}.
   * <p>
   * For debugging of arrays, use the method <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Arrays.html#asList(java.lang.Object[])">{@code java.util.Arrays.asList(Object[])}</a> method for converting the object array to a list before calling this method.
   * <p>
   * @param pCollection the {@code java.util.Collection} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Collection.html">{@code java.util.Collection}</a>
   */
  public static void printDebug(final Collection pCollection, final PrintStream pPrintStream) {
    printDebug(pCollection, null, pPrintStream);
  }

  /**
   * Prints the content of a {@code java.util.Collection} to {@code System.out}.
   * <p>
   * Not all data types are supported so far. The default is calling an element's {@code toString()} method.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * Be aware that the {@code Collection} interface embraces a large portion of the bulk data types in the {@code java.util} package,
   * e.g. {@code List}, {@code Set}, {@code Vector} and {@code HashSet}.
   * <p>
   * For debugging of arrays, use the method <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Arrays.html#asList(java.lang.Object[])">{@code java.util.Arrays.asList(Object[])}</a> method for converting the object array to a list before calling this method.
   * <p>
   * @param pCollection the {@code java.util.Collection} to be printed.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Collection.html">{@code java.util.Collection}</a>
   */
  public static void printDebug(final Collection pCollection) {
    printDebug(pCollection, System.out);
  }

  /**
   * Invokes a given method of every object in a {@code java.util.Map} and prints the results to a {@code java.io.PrintStream}.
   * The method called must have no formal parameters.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pMap the {@code java.util.Map} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked on each mapped object.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Map.html">{@code java.util.Map}</a>
   */
  public static void printDebug(final Map pMap, final String pMethodName, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pMap == null) {
      pPrintStream.println(MAP_IS_NULL_ERROR_MESSAGE);
      return;
    } else if (pMap.isEmpty()) {
      pPrintStream.println(MAP_IS_EMPTY_ERROR_MESSAGE);
      return;
    }
    Object mKeyObject;
    Object mEntryObject;

    for (Iterator i = pMap.keySet().iterator(); i.hasNext(); ) {
      mKeyObject   = i.next();
      mEntryObject = pMap.get(mKeyObject);
      if ((mKeyObject instanceof String) && (mEntryObject instanceof String)) {
        pPrintStream.println((String) mKeyObject + ": " + mEntryObject);
      } else if ((mKeyObject instanceof String) && (mEntryObject instanceof List)) {
        printDebug((List) mEntryObject, pPrintStream);
      } else if ((mKeyObject instanceof String) && (mEntryObject instanceof Set)) {
        printDebug((Set) mEntryObject, pPrintStream);
      } else if (mKeyObject instanceof String) {
        if (!StringUtil.isEmpty(pMethodName)) {
          try {
            Method objectMethod = mEntryObject.getClass().getMethod(pMethodName, null);
            Object retVal       = objectMethod.invoke(mEntryObject, null);

            if (retVal != null) {
              pPrintStream.println((String) mKeyObject + ": " + retVal.toString());
            } else {  // Default execution
              throw new Exception();
            }
          } catch (Exception e) {

            // Default
            pPrintStream.println((String) mKeyObject + ": " + mEntryObject.toString());
          }
        } else {      // Default
          pPrintStream.println((String) mKeyObject + ": " + mEntryObject.toString());
        }
      } else if ((mKeyObject instanceof Integer) && (mEntryObject instanceof String)) {
        pPrintStream.println((Integer) mKeyObject + ": " + mEntryObject);
      } else if ((mKeyObject instanceof Integer) && (mEntryObject instanceof List)) {
        printDebug((List) mEntryObject, pPrintStream);
      } else if ((mKeyObject instanceof String) && (mEntryObject instanceof Set)) {
        printDebug((Set) mEntryObject, pPrintStream);
      } else if (mKeyObject instanceof Integer) {
        if (!StringUtil.isEmpty(pMethodName)) {
          try {
            Method objectMethod = mEntryObject.getClass().getMethod(pMethodName, null);
            Object retVal       = objectMethod.invoke(mEntryObject, null);

            if (retVal != null) {
              pPrintStream.println((Integer) mKeyObject + ": " + retVal.toString());
            } else {  // Default execution
              throw new Exception();
            }
          } catch (Exception e) {

            // Default
            pPrintStream.println((Integer) mKeyObject + ": " + mEntryObject.toString());
          }
        } else {      // Default
          pPrintStream.println((Integer) mKeyObject + ": " + mEntryObject.toString());
        }
      }

      // More..
      //else if
    }
  }

  /**
   * Invokes a given method of every object in a {@code java.util.Map} to {@code System.out}.
   * The method called must have no formal parameters.
   * <p>
   * If an exception is throwed during the method invocation, the element's {@code toString()} method is called.<br>
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pMap the {@code java.util.Map} to be printed.
   * @param pMethodName a {@code java.lang.String} holding the name of the method to be invoked on each mapped object.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Map.html">{@code java.util.Map}</a>
   */
  public static void printDebug(final Map pMap, final String pMethodName) {
    printDebug(pMap, pMethodName, System.out);
  }

  /**
   * Prints the content of a {@code java.util.Map} to a {@code java.io.PrintStream}.
   * <p>
   * Not all data types are supported so far. The default is calling an element's {@code toString()} method.<br>
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pMap the {@code java.util.Map} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Map.html">{@code java.util.Map}</a>
   */
  public static void printDebug(final Map pMap, final PrintStream pPrintStream) {
    printDebug(pMap, null, pPrintStream);
  }

  /**
   * Prints the content of a {@code java.util.Map} to {@code System.out}.
   * <p>
   * Not all data types are supported so far. The default is calling an element's {@code toString()} method.<br>
   * For bulk data types, recursive invocations and invocations of other methods in this class, are used.
   * <p>
   * @param pMap the {@code java.util.Map} to be printed.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Map.html">{@code java.util.Map}</a>
   */
  public static void printDebug(final Map pMap) {
    printDebug(pMap, System.out);
  }

  /**
   * Prints the content of a {@code java.util.Properties} to a {@code java.io.PrintStream}.
   * <p>
   * @param pProperties the {@code java.util.Properties} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Properties.html">{@code java.util.Properties}</a>
   */
  public static void printDebug(final Properties pProperties, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pProperties == null) {
      pPrintStream.println(PROPERTIES_IS_NULL_ERROR_MESSAGE);
      return;
    } else if (pProperties.isEmpty()) {
      pPrintStream.println(PROPERTIES_IS_EMPTY_ERROR_MESSAGE);
      return;
    }
    for (Enumeration e = pProperties.propertyNames(); e.hasMoreElements(); ) {
      String key = (String) e.nextElement();

      pPrintStream.println(key + ": " + pProperties.getProperty(key));
    }
  }

  /**
   * Prints the content of a {@code java.util.Properties} to {@code System.out}.
   * <p>
   * @param pProperties the {@code java.util.Properties} to be printed.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Properties.html">{@code java.util.Properties}</a>
   */
  public static void printDebug(final Properties pProperties) {
    printDebug(pProperties, System.out);
  }

  // Timestamp utilities

  /**
   * Prints out the calendar time.
   * <p>
   * @param pCalendar the {@code java.util.Calendar} object from which to extract the date information.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static void printTimestamp(final Calendar pCalendar, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    pPrintStream.println(getTimestamp(pCalendar));
  }

  /**
   * Prints out the system time.
   * <p>
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/GregorianCalendar.html">{@code java.util.GregorianCalendar}</a>
   */
  public static void printTimestamp(final PrintStream pPrintStream) {

    GregorianCalendar cal = new GregorianCalendar();

    printTimestamp(cal, pPrintStream);
  }

  /**
   * Prints out the system time to {@code System.out}.
   */
  public static void printTimestamp() {
    printTimestamp(System.out);
  }

  /**
   * Returns a presentation of the date based on the given milliseconds.
   * <p>
   * @param pMilliseconds The specified number of milliseconds since the standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
   * @return a presentation of the calendar time.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static String getTimestamp(final String pMilliseconds) {
    return getTimestamp(Long.parseLong(pMilliseconds));
  }

  /**
   * Returns a presentation of the date based on the given milliseconds.
   * <p>
   * @param pMilliseconds The specified number of milliseconds since the standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT.
   * @return a presentation of the calendar time.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static String getTimestamp(final long pMilliseconds) {

    java.util.Date     date     = new java.util.Date(pMilliseconds);
    java.util.Calendar calendar = new GregorianCalendar();

    calendar.setTime(date);
    return getTimestamp(calendar);
  }

  /**
   * Returns a presentation of the given calendar's time.
   * <p>
   * @param pCalendar the {@code java.util.Calendar} object from which to extract the date information.
   * @return a presentation of the calendar time.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static String getTimestamp(final Calendar pCalendar) {
    return buildTimestamp(pCalendar);
  }

  /**
   * @return a presentation of the system time.
   */
  public static String getTimestamp() {

    GregorianCalendar cal = new GregorianCalendar();

    return getTimestamp(cal);
  }

  /**
   * Builds a presentation of the given calendar's time. This method contains the common timestamp format used in this class.
   * @return a presentation of the calendar time.
   */
  protected static String buildTimestamp(final Calendar pCalendar) {

    if (pCalendar == null) {
      return CALENDAR_IS_NULL_ERROR_MESSAGE;
    }

    // The timestamp format
    StringBuilder timestamp = new StringBuilder();

    //timestamp.append(DateUtil.getMonthName(new Integer(pCalendar.get(Calendar.MONTH)).toString(), "0", "us", "MEDIUM", false) + " ");
    timestamp.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(pCalendar.getTime()));

    //timestamp.append(pCalendar.get(Calendar.DAY_OF_MONTH) + " ");
    timestamp.append(" ");
    timestamp.append(StringUtil.pad(new Integer(pCalendar.get(Calendar.HOUR_OF_DAY)).toString(), 2, "0", true) + ":");
    timestamp.append(StringUtil.pad(new Integer(pCalendar.get(Calendar.MINUTE)).toString(), 2, "0", true) + ":");
    timestamp.append(StringUtil.pad(new Integer(pCalendar.get(Calendar.SECOND)).toString(), 2, "0", true) + ":");
    timestamp.append(StringUtil.pad(new Integer(pCalendar.get(Calendar.MILLISECOND)).toString(), 3, "0", true));
    return timestamp.toString();
  }

  /**
   * Builds the time difference between two millisecond representations.
   * <p>
   * This method is to be used with small time intervals between 0 ms up to a couple of minutes.
   * <p>
   * @param pStartTime   the start time.
   * @param pEndTime     the end time.
   * @return the time difference in milliseconds.
   */
  public static String buildTimeDifference(final long pStartTime, final long pEndTime) {

    //return pEndTime - pStartTime;
    StringBuilder retVal = new StringBuilder();

    // The time difference in milliseconds
    long timeDifference = pEndTime - pStartTime;

    if (timeDifference < 1000) {
      retVal.append(timeDifference);
      retVal.append(" ms");
    } else {
      long seconds = timeDifference / 1000;

      timeDifference = timeDifference % 1000;
      retVal.append(seconds);
      retVal.append("s ");
      retVal.append(timeDifference);
      retVal.append("ms");
    }

    //return retVal.toString() + "   (original timeDifference: " + new String(new Long(pEndTime - pStartTime).toString()) + ")";
    return retVal.toString();
  }

  /**
   * Builds the time difference between the given time and present time.
   * <p>
   * This method is to be used with small time intervals between 0 ms up to a couple of minutes.
   * <p>
   * @param pStartTime the start time.
   * @return the time difference in milliseconds.
   */
  public static String buildTimeDifference(final long pStartTime) {

    long presentTime = System.currentTimeMillis();

    return buildTimeDifference(pStartTime, presentTime);
  }

  /**
   * Prints out the difference between two millisecond representations.
   * The start time is subtracted from the end time.
   * <p>
   * @param pStartTime the start time.
   * @param pEndTime the end time.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printTimeDifference(final long pStartTime, final long pEndTime, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    pPrintStream.println(buildTimeDifference(pStartTime, pEndTime));
  }

  /**
   * Prints out the difference between two millisecond representations.
   * The start time is subtracted from the end time.
   * <p>
   * @param pStartTime the start time.
   * @param pEndTime the end time.
   */
  public static void printTimeDifference(final long pStartTime, final long pEndTime) {
    printTimeDifference(pStartTime, pEndTime, System.out);
  }

  /**
   * Prints out the difference between the given time and present time.
   * The start time is subtracted from the present time.
   * <p>
   * @param pStartTime the start time.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printTimeDifference(final long pStartTime, final PrintStream pPrintStream) {
    printTimeDifference(pStartTime, System.currentTimeMillis(), pPrintStream);
  }

  /**
   * Prints out the difference between the given time and present time to {@code System.out}.
   * The start time is subtracted from the present time.
   * <p>
   * usage:
   * <pre>
   * long startTime = System.currentTimeMillis();
   * ...
   * com.iml.oslo.eito.util.DebugUtil.printTimeDifference(startTime);
   * </pre>
   * <p>
   * @param pStartTime the start time.
   */
  public static void printTimeDifference(final long pStartTime) {
    printTimeDifference(pStartTime, System.out);
  }

  /**
   * Builds a string representing the difference between two calendar times.
   * The first calendar object is subtracted from the second one.
   * <p>
   * This method is to be used with time intervals between 0 ms up to several hours.
   * <p>
   * @param pStartCalendar the first {@code java.util.Calendar}.
   * @param pEndCalendar the second {@code java.util.Calendar}.
   * @return a string representation of the time difference.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static String buildTimeDifference(final Calendar pStartCalendar, final Calendar pEndCalendar) {

    if (pStartCalendar == null) {
      return CALENDAR_IS_NULL_ERROR_MESSAGE;
    }
    if (pEndCalendar == null) {
      return CALENDAR_IS_NULL_ERROR_MESSAGE;
    }
    if (pEndCalendar.before(pStartCalendar)) {
      return CALENDAR_CAUSATION_ERROR_MESSAGE;
    }
    int dateDiff        = pEndCalendar.get(Calendar.DATE) - pStartCalendar.get(Calendar.DATE);
    int hourDiff        = pEndCalendar.get(Calendar.HOUR_OF_DAY) - pStartCalendar.get(Calendar.HOUR_OF_DAY);
    int minuteDiff      = pEndCalendar.get(Calendar.MINUTE) - pStartCalendar.get(Calendar.MINUTE);
    int secondDiff      = pEndCalendar.get(Calendar.SECOND) - pStartCalendar.get(Calendar.SECOND);
    int milliSecondDiff = pEndCalendar.get(Calendar.MILLISECOND) - pStartCalendar.get(Calendar.MILLISECOND);

    if (milliSecondDiff < 0) {
      secondDiff--;
      milliSecondDiff += 1000;
    }
    if (secondDiff < 0) {
      minuteDiff--;
      secondDiff += 60;
    }
    if (minuteDiff < 0) {
      hourDiff--;
      minuteDiff += 60;
    }
    while (dateDiff > 0) {
      dateDiff--;
      hourDiff += 24;
    }

    // Time difference presentation format
    StringBuilder buffer = new StringBuilder();

    if ((hourDiff == 0) && (minuteDiff == 0) && (secondDiff == 0)) {
      buffer.append(milliSecondDiff);
      buffer.append("ms");
    } else if ((hourDiff == 0) && (minuteDiff == 0)) {
      buffer.append(secondDiff);
      buffer.append("s ");
      buffer.append(milliSecondDiff);
      buffer.append("ms");
    } else if (hourDiff == 0) {
      buffer.append(minuteDiff);
      buffer.append("m ");
      buffer.append(secondDiff);
      buffer.append(",");
      buffer.append(milliSecondDiff);
      buffer.append("s");
    } else {
      buffer.append(hourDiff);
      buffer.append("h ");
      buffer.append(minuteDiff);
      buffer.append("m ");
      buffer.append(secondDiff);
      buffer.append(",");
      buffer.append(milliSecondDiff);
      buffer.append("s");
    }
    return buffer.toString();
  }

  /**
   * Prints out the difference between to calendar times.
   * The first calendar object is subtracted from the second one.
   * <p>
   * @param pStartCalendar the first {@code java.util.Calendar}.
   * @param pEndCalendar the second {@code java.util.Calendar}.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static void printTimeDifference(final Calendar pStartCalendar, final Calendar pEndCalendar, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    pPrintStream.println(buildTimeDifference(pStartCalendar, pEndCalendar));
  }

  /**
   * Prints out the difference between to calendar times two {@code System.out}.
   * The first calendar object is subtracted from the second one.
   * <p>
   * @param pStartCalendar the first {@code java.util.Calendar}.
   * @param pEndCalendar the second {@code java.util.Calendar}.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static void printTimeDifference(final Calendar pStartCalendar, final Calendar pEndCalendar) {
    printTimeDifference(pStartCalendar, pEndCalendar, System.out);
  }

  /**
   * Prints out the difference between the given calendar time and present time.
   * <p>
   * @param pStartCalendar the {@code java.util.Calendar} to compare with present time.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static void printTimeDifference(final Calendar pStartCalendar, final PrintStream pPrintStream) {

    GregorianCalendar endCalendar = new GregorianCalendar();

    printTimeDifference(pStartCalendar, endCalendar, pPrintStream);
  }

  /**
   * Prints out the difference between the given calendar time and present time to {@code System.out}.
   * <p>
   * usage:
   * <pre>
   * GregorianCalendar startTime = new GregorianCalendar();
   * ...
   * com.iml.oslo.eito.util.DebugUtil.printTimeDifference(startTime);
   * </pre>
   * <p>
   * @param pStartCalendar the {@code java.util.Calendar} to compare with present time.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/util/Calendar.html">{@code java.util.Calendar}</a>
   */
  public static void printTimeDifference(final Calendar pStartCalendar) {

    GregorianCalendar endCalendar = new GregorianCalendar();

    printTimeDifference(pStartCalendar, endCalendar);
  }

  /**
   * Prints out a {@code com.iml.oslo.eito.util.DebugUtil.TimeDifference} object.
   * <p>
   * @param pTimeDifference the {@code com.twelvemonkeys.util.DebugUtil.TimeDifference} to investigate.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printTimeDifference(final TimeDifference pTimeDifference, final PrintStream pPrintStream) {
    printTimeDifference(pTimeDifference.getStartCalendar(), pTimeDifference.getEndCalendar(), pPrintStream);
  }

  /**
   * Prints out a {@code com.iml.oslo.eito.util.DebugUtil.TimeDifference} object to {@code System.out}.
   * <p>
   * @param pTimeDifference the {@code com.twelvemonkeys.util.DebugUtil.TimeDifference} to investigate.
   */
  public static void printTimeDifference(final TimeDifference pTimeDifference) {
    printTimeDifference(pTimeDifference.getStartCalendar(), pTimeDifference.getEndCalendar(), System.out);
  }

  /**
   * A convenience class for embracing two {@code java.util.Calendar} objects.
   * The class is used for building a collection of time differences according to the {@code printTimeAverage} method.
   */
  public static class TimeDifference {

    Calendar mStartCalendar;
    Calendar mEndCalendar;

    /**
     * Constructor TimeDifference
     *
     *
     */
    public TimeDifference() {}

    /**
     * Constructor TimeDifference
     *
     *
     * @param pStartCalendar
     * @param pEndCalendar
     *
     */
    public TimeDifference(final Calendar pStartCalendar, final Calendar pEndCalendar) {
      this.mStartCalendar = pStartCalendar;
      this.mEndCalendar   = pEndCalendar;
    }

    /**
     * Method setStartCalendar
     *
     *
     * @param pStartCalendar
     *
     */
    public void setStartCalendar(Calendar pStartCalendar) {
      this.mStartCalendar = pStartCalendar;
    }

    /**
     * Method getStartCalendar
     *
     *
     * @return
     *
     */
    public Calendar getStartCalendar() {
      return this.mStartCalendar;
    }

    /**
     * Method setEndCalendar
     *
     *
     * @param pEndCalendar
     *
     */
    public void setEndCalendar(Calendar pEndCalendar) {
      this.mEndCalendar = pEndCalendar;
    }

    /**
     * Method getEndCalendar
     *
     *
     * @return
     *
     */
    public Calendar getEndCalendar() {
      return this.mEndCalendar;
    }
  }

  /**
   * Prints out the average time difference from a collection of {@code com.twelvemonkeys.util.DebugUtil.TimeDifference} objects.
   * <p>
   *
   * @param pTimeDifferences
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   */
  public static void printTimeAverage(final Collection pTimeDifferences, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    if (pTimeDifferences == null) {
      pPrintStream.println(TIMEDIFFERENCES_IS_NULL_ERROR_MESSAGE);
      return;
    }
    Object         o;
    TimeDifference timeDifference;
    Calendar       startCalendar      = null;
    Calendar       endCalendar        = null;
    Calendar       totalStartCalendar = null;
    Calendar       totalEndCalendar   = null;
    long           startCalendarMilliSeconds, endCalendarMilliSeconds;
    List           timeDifferenceList = new Vector();
    Iterator       i                  = pTimeDifferences.iterator();

    if (i.hasNext()) {
      o = i.next();
      if (!(o instanceof TimeDifference)) {
        pPrintStream.println(TIMEDIFFERENCES_WRONG_DATATYPE_ERROR_MESSAGE);
        return;
      }
      timeDifference            = (TimeDifference) o;
      startCalendar             = timeDifference.getStartCalendar();
      totalStartCalendar        = startCalendar;
      endCalendar               = timeDifference.getEndCalendar();
      startCalendarMilliSeconds = startCalendar.getTime().getTime();
      endCalendarMilliSeconds   = endCalendar.getTime().getTime();
      timeDifferenceList.add(new Long(endCalendarMilliSeconds - startCalendarMilliSeconds));
    }
    while (i.hasNext()) {
      o = i.next();
      if (!(o instanceof TimeDifference)) {
        pPrintStream.println(TIMEDIFFERENCES_WRONG_DATATYPE_ERROR_MESSAGE);
        return;
      }
      timeDifference            = (TimeDifference) o;
      startCalendar             = timeDifference.getStartCalendar();
      endCalendar               = timeDifference.getEndCalendar();
      startCalendarMilliSeconds = startCalendar.getTime().getTime();
      endCalendarMilliSeconds   = endCalendar.getTime().getTime();
      timeDifferenceList.add(new Long(endCalendarMilliSeconds - startCalendarMilliSeconds));
    }
    totalEndCalendar = endCalendar;
    int  numberOfElements = timeDifferenceList.size();
    long timeDifferenceElement;
    long timeDifferenceSum = 0;

    for (Iterator i2 = timeDifferenceList.iterator(); i2.hasNext(); ) {
      timeDifferenceElement = ((Long) i2.next()).longValue();
      timeDifferenceSum     += timeDifferenceElement;
    }

    // Total elapsed time
    String totalElapsedTime = buildTimeDifference(totalStartCalendar, totalEndCalendar);

    // Time average presentation format
    pPrintStream.println("Average time difference: " + timeDifferenceSum / numberOfElements + "ms (" + numberOfElements
                         + " elements, total elapsed time: " + totalElapsedTime + ")");
  }

  /**
   * Prints out the average time difference from a collection of {@code com.twelvemonkeys.util.DebugUtil.TimeDifference} objects to {@code System.out}.
   * <p>
   *
   * @param pTimeDifferences
   */
  public static void printTimeAverage(final Collection pTimeDifferences) {
    printTimeAverage(pTimeDifferences, System.out);
  }

  // Reflective methods

  /**
   * Prints the top-wrapped class name of a {@code java.lang.Object} to a {@code java.io.PrintStream}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be printed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   */
  public static void printClassName(final Object pObject, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    pPrintStream.println(getClassName(pObject));
  }

  /**
   * Prints the top-wrapped class name of a {@code java.lang.Object} to {@code System.out}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be printed.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   */
  public static void printClassName(final Object pObject) {
    printClassName(pObject, System.out);
  }

  /**
   * Builds the top-wrapped class name of a {@code java.lang.Object}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @return the object's class name.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   */
  public static String getClassName(final Object pObject) {

    if (pObject == null) {
      return OBJECT_IS_NULL_ERROR_MESSAGE;
    }
    return pObject.getClass().getName();
  }

  /**
   * Prints javadoc-like, the top wrapped class fields and methods of a {@code java.lang.Object} to a {@code java.io.PrintStream}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @param pObjectName the name of the object instance, for identification purposes.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Modifier.html">{@code java.lang.reflect.Modifier}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Field.html">{@code java.lang.reflect.Field}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Constructor.html">{@code java.lang.reflect.Constructor}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Method.html">{@code java.lang.reflect.Method}</a>
   */
  public static void printClassDetails(final Object pObject, final String pObjectName, final PrintStream pPrintStream) {

    if (pPrintStream == null) {
      System.err.println(PRINTSTREAM_IS_NULL_ERROR_MESSAGE);
      return;
    }
    pPrintStream.println(getClassDetails(pObject, pObjectName));
  }

  /**
   * Prints javadoc-like, the top wrapped class fields and methods of a {@code java.lang.Object} to {@code System.out}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @param pObjectName the name of the object instance, for identification purposes.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Modifier.html">{@code java.lang.reflect.Modifier}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Field.html">{@code java.lang.reflect.Field}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Constructor.html">{@code java.lang.reflect.Constructor}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Method.html">{@code java.lang.reflect.Method}</a>
   */
  public static void printClassDetails(final Object pObject, final String pObjectName) {
    printClassDetails(pObject, pObjectName, System.out);
  }

  /**
   * Prints javadoc-like, the top wrapped class fields and methods of a {@code java.lang.Object} to {@code System.out}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Modifier.html">{@code java.lang.reflect.Modifier}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Field.html">{@code java.lang.reflect.Field}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Constructor.html">{@code java.lang.reflect.Constructor}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Method.html">{@code java.lang.reflect.Method}</a>
   */
  public static void printClassDetails(final Object pObject) {
    printClassDetails(pObject, null, System.out);
  }

  /**
   * Prints javadoc-like, the top wrapped class fields and methods of a {@code java.lang.Object} to a {@code java.io.PrintStream}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @param pPrintStream the {@code java.io.PrintStream} for flushing the results.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Modifier.html">{@code java.lang.reflect.Modifier}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Field.html">{@code java.lang.reflect.Field}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Constructor.html">{@code java.lang.reflect.Constructor}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Method.html">{@code java.lang.reflect.Method}</a>
   */
  public static void printClassDetails(final Object pObject, final PrintStream pPrintStream) {
    printClassDetails(pObject, null, pPrintStream);
  }

  /**
   * Builds a javadoc-like presentation of the top wrapped class fields and methods of a {@code java.lang.Object}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @return a listing of the object's class details.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Modifier.html">{@code java.lang.reflect.Modifier}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Field.html">{@code java.lang.reflect.Field}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Constructor.html">{@code java.lang.reflect.Constructor}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Method.html">{@code java.lang.reflect.Method}</a>
   */
  public static String getClassDetails(final Object pObject) {
    return getClassDetails(pObject, null);
  }

  /**
   * Builds a javadoc-like presentation of the top wrapped class fields and methods of a {@code java.lang.Object}.
   * <p>
   * @param pObject the {@code java.lang.Object} to be analysed.
   * @param pObjectName the name of the object instance, for identification purposes.
   * @return a listing of the object's class details.
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/Class.html">{@code java.lang.Class}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Modifier.html">{@code java.lang.reflect.Modifier}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Field.html">{@code java.lang.reflect.Field}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Constructor.html">{@code java.lang.reflect.Constructor}</a>
   * @see <a href="http://java.sun.com/products/jdk/1.3/docs/api/java/lang/reflect/Method.html">{@code java.lang.reflect.Method}</a>
   */
  public static String getClassDetails(final Object pObject, final String pObjectName) {

    if (pObject == null) {
      return OBJECT_IS_NULL_ERROR_MESSAGE;
    }
    final String    endOfLine   = System.getProperty("line.separator");
    final String    dividerLine = "---------------------------------------------------------";
    Class           c           = pObject.getClass();
    StringTokenizer tokenizedString;
    String          str;
    String          className      = new String();
    String          superClassName = new String();
    StringBuilder    buffer         = new StringBuilder();

    // Heading
    buffer.append(endOfLine);
    buffer.append("**** class details");
    if (!StringUtil.isEmpty(pObjectName)) {
      buffer.append(" for \"" + pObjectName + "\"");
    }
    buffer.append(" ****");
    buffer.append(endOfLine);

    // Package
    Package p = c.getPackage();

    if (p != null) {
      buffer.append(p.getName());
    }
    buffer.append(endOfLine);

    // Class or Interface
    if (c.isInterface()) {
      buffer.append("I n t e r f a c e   ");
    } else {
      buffer.append("C l a s s   ");
    }
    str             = c.getName();
    tokenizedString = new StringTokenizer(str, ".");
    while (tokenizedString.hasMoreTokens()) {
      className = tokenizedString.nextToken().trim();
    }
    str = new String();
    char[] charArray = className.toCharArray();

    for (int i = 0; i < charArray.length; i++) {
      str += charArray[i] + " ";
    }
    buffer.append(str);
    buffer.append(endOfLine);
    buffer.append(endOfLine);

    // Class Hierarch
    List classNameList = new Vector();

    classNameList.add(c.getName());
    Class superclass = c.getSuperclass();

    while (superclass != null) {
      classNameList.add(superclass.getName());
      superclass = superclass.getSuperclass();
    }
    Object[] classNameArray = classNameList.toArray();
    int      counter        = 0;

    for (int i = classNameArray.length - 1; i >= 0; i--) {
      for (int j = 0; j < counter; j++) {
        buffer.append("   ");
      }
      if (counter > 0) {
        buffer.append("|");
        buffer.append(endOfLine);
      }
      for (int j = 0; j < counter; j++) {
        buffer.append("   ");
      }
      if (counter > 0) {
        buffer.append("+-");
      }
      buffer.append((String) classNameArray[i]);
      buffer.append(endOfLine);
      counter++;
    }

    // Divider
    buffer.append(endOfLine);
    buffer.append(dividerLine);
    buffer.append(endOfLine);
    buffer.append(endOfLine);

    // Profile
    int classModifier = c.getModifiers();

    buffer.append(Modifier.toString(classModifier) + " ");
    if (c.isInterface()) {
      buffer.append("Interface ");
    } else {
      buffer.append("Class ");
    }
    buffer.append(className);
    buffer.append(endOfLine);
    if ((classNameArray != null) && (classNameArray[classNameArray.length - 2] != null)) {
      str             = (String) classNameArray[classNameArray.length - 2];
      tokenizedString = new StringTokenizer(str, ".");
      while (tokenizedString.hasMoreTokens()) {
        superClassName = tokenizedString.nextToken().trim();
      }
      buffer.append("extends " + superClassName);
      buffer.append(endOfLine);
    }
    if (!c.isInterface()) {
      Class[] interfaces = c.getInterfaces();

      if ((interfaces != null) && (interfaces.length > 0)) {
        buffer.append("implements ");
        str             = interfaces[0].getName();
        tokenizedString = new StringTokenizer(str, ".");
        while (tokenizedString.hasMoreTokens()) {
          str = tokenizedString.nextToken().trim();
        }
        buffer.append(str);
        for (int i = 1; i < interfaces.length; i++) {
          str             = interfaces[i].getName();
          tokenizedString = new StringTokenizer(str, ".");
          while (tokenizedString.hasMoreTokens()) {
            str = tokenizedString.nextToken().trim();
          }
          buffer.append(", " + str);
        }
        buffer.append(endOfLine);
      }
    }

    // Divider
    buffer.append(endOfLine);
    buffer.append(dividerLine);
    buffer.append(endOfLine);
    buffer.append(endOfLine);

    // Fields
    buffer.append("F I E L D   S U M M A R Y");
    buffer.append(endOfLine);
    Field[] fields = c.getFields();

    if (fields != null) {
      for (int i = 0; i < fields.length; i++) {
        buffer.append(Modifier.toString(fields[i].getType().getModifiers()) + " ");
        str             = fields[i].getType().getName();
        tokenizedString = new StringTokenizer(str, ".");
        while (tokenizedString.hasMoreTokens()) {
          str = tokenizedString.nextToken().trim();
        }
        buffer.append(str + " ");
        buffer.append(fields[i].getName());
        buffer.append(endOfLine);
      }
    }
    buffer.append(endOfLine);

    // Constructors        
    buffer.append("C O N S T R U C T O R   S U M M A R Y");
    buffer.append(endOfLine);
    Constructor[] constructors = c.getConstructors();

    if (constructors != null) {
      for (int i = 0; i < constructors.length; i++) {
        buffer.append(className + "(");
        Class[] parameterTypes = constructors[i].getParameterTypes();

        if (parameterTypes != null) {
          if (parameterTypes.length > 0) {
            str             = parameterTypes[0].getName();
            tokenizedString = new StringTokenizer(str, ".");
            while (tokenizedString.hasMoreTokens()) {
              str = tokenizedString.nextToken().trim();
            }
            buffer.append(str);
            for (int j = 1; j < parameterTypes.length; j++) {
              str             = parameterTypes[j].getName();
              tokenizedString = new StringTokenizer(str, ".");
              while (tokenizedString.hasMoreTokens()) {
                str = tokenizedString.nextToken().trim();
              }
              buffer.append(", " + str);
            }
          }
        }
        buffer.append(")");
        buffer.append(endOfLine);
      }
    }
    buffer.append(endOfLine);

    // Methods
    buffer.append("M E T H O D   S U M M A R Y");
    buffer.append(endOfLine);
    Method[] methods = c.getMethods();

    if (methods != null) {
      for (int i = 0; i < methods.length; i++) {
        buffer.append(Modifier.toString(methods[i].getModifiers()) + " ");
        str             = methods[i].getReturnType().getName();
        tokenizedString = new StringTokenizer(str, ".");
        while (tokenizedString.hasMoreTokens()) {
          str = tokenizedString.nextToken().trim();
        }
        buffer.append(str + " ");
        buffer.append(methods[i].getName() + "(");
        Class[] parameterTypes = methods[i].getParameterTypes();

        if ((parameterTypes != null) && (parameterTypes.length > 0)) {
          if (parameterTypes[0] != null) {
            str             = parameterTypes[0].getName();
            tokenizedString = new StringTokenizer(str, ".");
            while (tokenizedString.hasMoreTokens()) {
              str = tokenizedString.nextToken().trim();
            }

            // array bugfix
            if (str.charAt(str.length() - 1) == ';') {
              str = str.substring(0, str.length() - 1) + "[]";
            }
            buffer.append(str);
            for (int j = 1; j < parameterTypes.length; j++) {
              str             = parameterTypes[j].getName();
              tokenizedString = new StringTokenizer(str, ".");
              while (tokenizedString.hasMoreTokens()) {
                str = tokenizedString.nextToken().trim();
              }
              buffer.append(", " + str);
            }
          }
        }
        buffer.append(")");
        buffer.append(endOfLine);
      }
    }
    buffer.append(endOfLine);

    // Ending        
    buffer.append("**** class details");
    if (!StringUtil.isEmpty(pObjectName)) {
      buffer.append(" for \"" + pObjectName + "\"");
    }
    buffer.append(" end ****");
    buffer.append(endOfLine);
    return buffer.toString();
  }

  /**
   * Prettyprints a large number.
   * <p>
   *
   * @param pBigNumber
   * @return prettyprinted number with dot-separation each 10e3.
   */
  public static String getLargeNumber(final long pBigNumber) {

    StringBuilder buffer       = new StringBuilder(new Long(pBigNumber).toString());
    char[]       number       = new Long(pBigNumber).toString().toCharArray();
    int          reverseIndex = 0;

    for (int i = number.length; i >= 0; i--) {
      reverseIndex++;
      if ((reverseIndex % 3 == 0) && (i > 1)) {
        buffer = buffer.insert(i - 1, '.');
      }
    }
    return buffer.toString();
  }

  /**
   * Prettyprints milliseconds to ?day(s) ?h ?m ?s ?ms.
   * <p>
   *
   * @param pMilliseconds
   * @return prettyprinted time duration.
   */
  public static String getTimeInterval(final long pMilliseconds) {

    long         timeIntervalMilliseconds = pMilliseconds;
    long         timeIntervalSeconds      = 0;
    long         timeIntervalMinutes      = 0;
    long         timeIntervalHours        = 0;
    long         timeIntervalDays         = 0;
    boolean      printMilliseconds        = true;
    boolean      printSeconds             = false;
    boolean      printMinutes             = false;
    boolean      printHours               = false;
    boolean      printDays                = false;
    final long   MILLISECONDS_IN_SECOND   = 1000;
    final long   MILLISECONDS_IN_MINUTE   = 60 * MILLISECONDS_IN_SECOND;  // 60000
    final long   MILLISECONDS_IN_HOUR     = 60 * MILLISECONDS_IN_MINUTE;  // 3600000
    final long   MILLISECONDS_IN_DAY      = 24 * MILLISECONDS_IN_HOUR;    // 86400000
    StringBuilder timeIntervalBuffer       = new StringBuilder();

    // Days
    if (timeIntervalMilliseconds >= MILLISECONDS_IN_DAY) {
      timeIntervalDays         = timeIntervalMilliseconds / MILLISECONDS_IN_DAY;
      timeIntervalMilliseconds = timeIntervalMilliseconds % MILLISECONDS_IN_DAY;
      printDays                = true;
      printHours               = true;
      printMinutes             = true;
      printSeconds             = true;
    }

    // Hours
    if (timeIntervalMilliseconds >= MILLISECONDS_IN_HOUR) {
      timeIntervalHours        = timeIntervalMilliseconds / MILLISECONDS_IN_HOUR;
      timeIntervalMilliseconds = timeIntervalMilliseconds % MILLISECONDS_IN_HOUR;
      printHours               = true;
      printMinutes             = true;
      printSeconds             = true;
    }

    // Minutes
    if (timeIntervalMilliseconds >= MILLISECONDS_IN_MINUTE) {
      timeIntervalMinutes      = timeIntervalMilliseconds / MILLISECONDS_IN_MINUTE;
      timeIntervalMilliseconds = timeIntervalMilliseconds % MILLISECONDS_IN_MINUTE;
      printMinutes             = true;
      printSeconds             = true;
    }

    // Seconds
    if (timeIntervalMilliseconds >= MILLISECONDS_IN_SECOND) {
      timeIntervalSeconds      = timeIntervalMilliseconds / MILLISECONDS_IN_SECOND;
      timeIntervalMilliseconds = timeIntervalMilliseconds % MILLISECONDS_IN_SECOND;
      printSeconds             = true;
    }

    // Prettyprint    
    if (printDays) {
      timeIntervalBuffer.append(timeIntervalDays);
      if (timeIntervalDays > 1) {
        timeIntervalBuffer.append("days ");
      } else {
        timeIntervalBuffer.append("day ");
      }
    }
    if (printHours) {
      timeIntervalBuffer.append(timeIntervalHours);
      timeIntervalBuffer.append("h ");
    }
    if (printMinutes) {
      timeIntervalBuffer.append(timeIntervalMinutes);
      timeIntervalBuffer.append("m ");
    }
    if (printSeconds) {
      timeIntervalBuffer.append(timeIntervalSeconds);
      timeIntervalBuffer.append("s ");
    }
    if (printMilliseconds) {
      timeIntervalBuffer.append(timeIntervalMilliseconds);
      timeIntervalBuffer.append("ms");
    }
    return timeIntervalBuffer.toString();
  }
}


/*--- Formatted in Sun Java Convention Style on ma, des 1, '03 ---*/


/*------ Formatted by Jindent 3.23 Basic 1.0 --- http://www.jindent.de ------*/
