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

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.xml.XMLSerializer;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Properties subclass, that reads and writes files in a simple XML format.
 * Can be used in-place where ever {@link java.util.Properties}
 * is used. The major differences are that it reads
 * and writes XML files, instead of ".properties" format files, and has
 * support for typed values (where normal Properties only supports Strings).
 * <P/>
 * The greatest advantage of the XML format, is that it
 * supports hierarchial structures or grouping of properties, in addtition to
 * be a more standard way of storing data. The XML format also opens up for
 * allowing for more metadata on
 * the properties, such as type and the format information, specifying how to
 * read and write them.
 * <P/>
 * This class relies on SAX for reading and parsing XML, in
 * addition, it requires DOM for outputting XML. It is possible
 * to configure what (SAX implementing) parser to use, by setting  the system
 * property {@code org.xml.sax.driver} to your favourite SAX parser. The
 * default is the {@code org.apache.xerces.parsers.SAXParser}.
 * <P/>
 * <STRONG><A name="DTD"></A>XML Format (DTD):</STRONG><BR/>
 * <PRE>
 * &lt;!ELEMENT properties (property)*&gt;
 * &lt;!ELEMENT property (value?, property*)&gt;
 * &lt;!ATTLIST property
 *                    name   CDATA #REQUIRED
 *                    value  CDATA #IMPLIED
 *                    type   CDATA "String"
 *                    format CDATA #IMPLIED
 * &gt;
 * &lt;!ELEMENT value (PCDATA)&gt;
 * &lt;!ATTLIST value
 *                    type   CDATA "String"
 *                    format CDATA #IMPLIED
 * &gt;
 * </PRE>
 * See {@link #SYSTEM_DTD_URI}, {@link #DTD}.
 * <BR/><BR/>
 * <STRONG>XML Format eample:</STRONG><BR/>
 * <PRE>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE properties SYSTEM "http://www.twelvemonkeys.com/xml/XMLProperties.dtd"&gt;
 * &lt;!-- A simple XMLProperties example --&gt;
 * &lt;!-- Sat Jan 05 00:16:55 CET 2002 --&gt;
 * &lt;properties&gt;
 *    &lt;property name="one" value="1" type="Integer" /&gt;
 *    &lt;property name="two"&gt;
 *       &lt;property name="a" value="A" /&gt;
 *       &lt;property name="b"&gt;
 *           &lt;value&gt;B is a very long value, that can span several
 * lines
 * &lt;![CDATA[&lt;this&gt;&lt;doesn't ---&gt; really
 * matter&lt;
 * ]]&gt;
 * as it is escaped using CDATA.&lt;/value&gt;
 *       &lt/property&gt;
 *       &lt;property name="c" value="C"&gt;
 *          &lt;property name="i" value="I"/&gt;
 *       &lt;/property&gt;
 *    &lt;/property&gt;
 *    &lt;property name="date" value="16. Mar 2002"
 *              type="java.util.Date" format="dd. MMM yyyy"/&gt;
 *    &lt;property name="none" value="" /&gt;
 * &lt;/properties&gt;
 * </PRE>
 * Results in the properties {@code one=1, two.a=A, two.b=B is a very long...,
 * two.c=C, two.c.i=I, date=Sat Mar 16 00:00:00 CET 2002
 * } and {@code none=}. Note that there is no property named
 * {@code two}.
 *
 * @see java.util.Properties
 * @see #setPropertyValue(String,Object)
 * @see #getPropertyValue(String)
 * @see #load(InputStream)
 * @see #store(OutputStream,String)
 *
 * @author <A href="harald.kuhr@twelvemonkeys.com">Harald Kuhr</A>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/XMLProperties.java#1 $
 *
 */
// TODO: Consider deleting this code.. Look at Properties XML format.
public class XMLProperties extends Properties {

  /** {@code "UTF-8"} */
  public final static String UTF_8_ENCODING = "UTF-8";

  /** {@code "xmlns"} */
  public final static String XMLNS = "xmlns";

  /** {@code "properties"} */
  public final static String PROPERTIES = "properties";

  /** {@code "property"} */
  public final static String PROPERTY = "property";

  /** {@code "name"} */
  public final static String PROPERTY_NAME = "name";

  /** {@code "value"} */
  public final static String PROPERTY_VALUE = "value";

  /** {@code "type"} */
  public final static String PROPERTY_TYPE = "type";

  /** {@code "format"} */
  public final static String PROPERTY_FORMAT = "format";

  /** {@code "String"} ({@link java.lang.String}) */
  public final static String DEFAULT_TYPE = "String";

  /** {@code "yyyy-MM-dd hh:mm:ss.SSS"}
   * ({@link java.sql.Timestamp} format, excpet nanos) */
  public final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS";

  /** This is the DTD */
  public final static String DTD =
    "<!ELEMENT properties (property*)>\n<!ELEMENT property (value?, property*)>\n<!ATTLIST property\n\tname  CDATA #REQUIRED\n\tvalue CDATA #IMPLIED\n\ttype  CDATA \"String\"\n>\n<!ELEMENT value (#PCDATA)>\n<!ATTLIST value\n\ttype  CDATA \"String\"\n>";

  /** {@code "http://www.twelvemonkeys.com/xml/XMLProperties.dtd"} */
  public final static String SYSTEM_DTD_URI = "http://www.twelvemonkeys.com/xml/XMLProperties.dtd";

  /** {@code "http://www.twelvemonkeys.com/xml/XMLProperties"} */
  public final static String NAMESPACE_URI = "http://www.twelvemonkeys.com/xml/XMLProperties";

  /** {@code "http://xml.org/sax/features/validation"} */
  public final static String SAX_VALIDATION_URI = "http://xml.org/sax/features/validation";

  /** debug */
  private boolean  mValidation = true;
  protected Vector mErrors     = null;
  protected Vector mWarnings   = null;

  // protected Hashtable mTypes = new Hashtable();
  protected Hashtable         mFormats       = new Hashtable();
  protected static DateFormat sDefaultFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

  /**
   * Creates an empty property list with no default values.
   */
  public XMLProperties() {}

  /**
   * Creates an empty property list with the specified defaults.
   *
   * @param pDefaults the defaults.
   */
  public XMLProperties(Properties pDefaults) {

    // Sets the protected defaults variable
    super(pDefaults);
  }

  void addXMLError(SAXParseException pException) {

    if (mErrors == null) {
      mErrors = new Vector();
    }
    mErrors.add(pException);
  }

  /**
   * Gets the non-fatal XML errors (SAXParseExceptions) resulting from a
   * load.
   *
   * @return An array of SAXParseExceptions, or null if none occured.
   *
   * @see XMLProperties.PropertiesHandler
   * @see #load(InputStream)
   */
  public SAXParseException[] getXMLErrors() {

    if (mErrors == null) {
      return null;
    }
    return (SAXParseException[]) mErrors.toArray(new SAXParseException[mErrors.size()]);
  }

  void addXMLWarning(SAXParseException pException) {

    if (mWarnings == null) {
      mWarnings = new Vector();
    }
    mWarnings.add(pException);
  }

  /**
   * Gets the XML warnings (SAXParseExceptions) resulting from a load.
   *
   * @return An array of SAXParseExceptions, or null if none occured.
   *
   * @see XMLProperties.PropertiesHandler
   * @see #load(InputStream)
   */
  public SAXParseException[] getXMLWarnings() {

    if (mWarnings == null) {
      return null;
    }
    return (SAXParseException[]) mWarnings.toArray(new SAXParseException[mWarnings.size()]);
  }

  /**
   * Reads a property list (key and element pairs) from the input stream. The
   * stream is assumed to be using the UFT-8 character encoding, and be in
   * valid, well-formed XML format.
   * <P/>
   * After loading, any errors or warnings from the SAX parser, are available
   * as array of SAXParseExceptions from the getXMLErrors and getXMLWarnings
   * methods.
   *
   * @param pInput the input stream to load from.
   *
   * @exception IOException if an error occurred when reading from the input
   *            stream. Any SAXExceptions are also wrapped in IOExceptions.
   *
   * @see Properties#load(InputStream)
   * @see #DTD
   * @see #SYSTEM_DTD_URI
   * @see XMLProperties.PropertiesHandler
   * @see #getXMLErrors
   * @see #getXMLWarnings
   */
  public synchronized void load(InputStream pInput) throws IOException {
    // Get parser instance
    XMLReader parser;

    // Try to instantiate System default parser
    String driver = System.getProperty("org.xml.sax.driver");

    if (driver == null) {

      // Instantiate the org.apache.xerces.parsers.SAXParser as default
      driver = "org.apache.xerces.parsers.SAXParser";
    }
    try {
      parser = XMLReaderFactory.createXMLReader(driver);
      parser.setFeature(SAX_VALIDATION_URI, mValidation);
    } catch (SAXNotRecognizedException saxnre) {

      // It should be okay to throw RuntimeExptions, as you will need an
      // XML parser
      throw new RuntimeException("Error configuring XML parser \"" + driver + "\": " + saxnre.getClass().getName() + ": "
                                 + saxnre.getMessage());
    } catch (SAXException saxe) {
      throw new RuntimeException("Error creating XML parser \"" + driver + "\": " + saxe.getClass().getName() + ": " + saxe.getMessage());
    }

    // Register handler
    PropertiesHandler handler = new PropertiesHandler(this);

    parser.setContentHandler(handler);
    parser.setErrorHandler(handler);
    parser.setDTDHandler(handler);
    parser.setEntityResolver(handler);

    // Read and parse XML
    try {
      parser.parse(new InputSource(pInput));
    } catch (SAXParseException saxpe) {

      // Wrap SAXException in IOException to be consistent
      throw new IOException("Error parsing XML: " + saxpe.getClass().getName() + ": " + saxpe.getMessage() + " Line: "
                            + saxpe.getLineNumber() + " Column: " + saxpe.getColumnNumber());
    } catch (SAXException saxe) {

      // Wrap SAXException in IOException to be consistent
      // Doesn't realy matter, as the SAXExceptions seems to be pretty
      // meaningless themselves...
      throw new IOException("Error parsing XML: " + saxe.getClass().getName() + ": " + saxe.getMessage());
    }
  }

  /**
   * Initializes the value of a property.
   *
   * @todo move init code to the parser?
   *
   * @throws ClassNotFoundException if there is no class found for the given
   *         type
   * @throws IllegalArgumentException if the value given, is not parseable
   *         as the given type
   */
  protected Object initPropertyValue(String pValue, String pType, String pFormat) throws ClassNotFoundException {

    // System.out.println("pValue=" + pValue + " pType=" + pType
    //                 + " pFormat=" + pFormat);
    // No value to convert
    if (pValue == null) {
      return null;
    }

    // No conversion needed for Strings
    if ((pType == null) || pType.equals("String") || pType.equals("java.lang.String")) {
      return pValue;
    }
    Object value;

    if (pType.equals("Date") || pType.equals("java.util.Date")) {

      // Special parser needed
      try {

        // Parse date through StringUtil
        if (pFormat == null) {
          value = StringUtil.toDate(pValue, sDefaultFormat);
        } else {
          value = StringUtil.toDate(pValue, new SimpleDateFormat(pFormat));
        }
      } catch (IllegalArgumentException e) {

        // Not parseable...
        throw e;
      }

      // Return
      return value;
    } else if (pType.equals("java.sql.Timestamp")) {

      // Special parser needed
      try {

        // Parse timestamp through StringUtil
        value = StringUtil.toTimestamp(pValue);
      } catch (IllegalArgumentException e) {

        // Not parseable...
        throw new RuntimeException(e.getMessage());
      }

      // Return
      return value;
    } else {
      int dot = pType.indexOf(".");

      if (dot < 0) {
        pType = "java.lang." + pType;
      }

      // Get class
      Class cl = Class.forName(pType);

      // Try to create instance from <Constructor>(String)
      value = createInstance(cl, pValue);
      if (value == null) {

        // createInstance failed for some reason
        // Try to invoke the static method valueof(String)
        value = invokeStaticMethod(cl, "valueOf", pValue);

        // If the value is still null, well, then I cannot help...
      }
    }

    // Return
    return value;
  }

  /**
   * Creates an object from the given class' single argument constructor.
   *
   * @return The object created from the constructor.
   * If the constructor could not be invoked for any reason, null is
   * returned.
   */
  private Object createInstance(Class pClass, Object pParam) {
    Object value;

    try {

      // Create param and argument arrays
      Class[]  param = { pParam.getClass() };
      Object[] arg   = { pParam };

      // Get constructor
      Constructor constructor = pClass.getDeclaredConstructor(param);

      // Invoke and create instance
      value = constructor.newInstance(arg);
    } catch (Exception e) {
      return null;
    }
    return value;
  }

  /**
   * Creates an object from any given static method, given the parameter
   *
   * @return The object returned by the static method.
   * If the return type of the method is a primitive type, it is wrapped in
   * the corresponding wrapper object (int is wrapped in an Integer).
   * If the return type of the method is void, null is returned.
   * If the method could not be invoked for any reason, null is returned.
   */
  private Object invokeStaticMethod(Class pClass, String pMethod, Object pParam) {
    Object value = null;

    try {

      // Create param and argument arrays
      Class[]  param = { pParam.getClass() };
      Object[] arg   = { pParam };

      // Get method
      // *** If more than one such method is found in the class, and one
      // of these methods has a RETURN TYPE that is more specific than
      // any of the others, that method is reflected; otherwise one of
      // the methods is chosen ARBITRARILY.
      // java/lang/Class.html#getMethod(java.lang.String, java.lang.Class[])
      java.lang.reflect.Method method = pClass.getMethod(pMethod, param);

      // Invoke public static method
      if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
        value = method.invoke(null, arg);
      }
    } catch (Exception e) {
      return null;
    }
    return value;
  }

  /**
   * Gets the format of a property. This value is used for formatting the
   * value before it is stored as xml.
   *
   * @param pKey a key in this hashtable.
   *
   * @return the  format for the specified key or null if it does not
   *         have one.
   */
  public String getPropertyFormat(String pKey) {

    // Get format
    return StringUtil.valueOf(mFormats.get(pKey));
  }

  /**
   * Sets the format of a property. This value is used for formatting the
   * value before it is stored as xml.
   *
   * @param pKey a key in this hashtable.
   * @param pFormat a string representation of the format.
   *
   * @return the previous format for the specified key or null if it did not
   *         have one.
   */
  public synchronized String setPropertyFormat(String pKey, String pFormat) {

    // Insert format
    return StringUtil.valueOf(mFormats.put(pKey, pFormat));
  }

  /**
   * Calls the Hashtable method put. Provided for parallelism with the
   * getPropertyValue method. Enforces use of strings for property keys.
   * The value returned is the result of the Hashtable call to put.
   *
   * @param pKey the key to be placed into this property list.
   * @param pValue the value corresponding to key.
   *
   * @return the previous value of the specified key in this property list,
   *         or null if it did not have one.
   *
   * @see #getPropertyValue(String)
   */
  public synchronized Object setPropertyValue(String pKey, Object pValue) {

    // Insert value
    return put(pKey, pValue);
  }

  /**
   * Searches for the property with the specified key in this property list.
   * If the key is not found in this property list, the default property
   * list, and its defaults, recursively, are then checked. The method
   * returns null if the property is not found.
   *
   * @param pKey the property key.
   *
   * @return the value in this property list with the specified key value.
   *
   * @see #setPropertyValue(String, Object)
   * @see #getPropertyValue(String, Object)
   * @see Properties#defaults
   */
  public synchronized Object getPropertyValue(String pKey) {
    return getPropertyValue(pKey, null);
  }

  /**
   * Searches for the property with the specified key in this property list.
   * If the key is not found in this property list, the default property
   * list, and its defaults, recursively, are then checked. The method
   * returns the default value argument if the property is not found.
   *
   * @param pKey the property key.
   * @param pDefaultValue the default value.
   *
   * @return the value in this property list with the specified key value.
   *
   * @see #getPropertyValue(String)
   * @see Properties#defaults
   */
  public Object getPropertyValue(String pKey, Object pDefaultValue) {

    Object value = super.get(pKey);  // super.get() is EXTREMELEY IMPORTANT

    if (value != null) {
      return value;
    }
    if (defaults instanceof XMLProperties) {
      return (((XMLProperties) defaults).getPropertyValue(pKey));
    }
    return ((defaults != null) ? defaults.getProperty(pKey) : pDefaultValue);
  }

  /**
   * Overloaded get method, that <EM>always returns Strings</EM>.
   * Due to the way the store and list methods of
   * java.util.Properties works (it calls get and casts to String, instead
   * of calling getProperty), this methods returns
   * StringUtil.valueOf(super.get), to avoid ClassCastExcpetions.
   * <P/>
   * <SMALL>If you need the old functionality back,
   * getPropertyValue returns super.get directly.
   * A cleaner approach would be to override the list and store
   * methods, but it's too much work for nothing...</SMALL>
   *
   * @param pKey a key in this hashtable
   *
   * @return the value to which the key is mapped in this hashtable,
   * converted to a string; null if the key is not mapped to any value in
   * this hashtable.
   *
   * @see #getPropertyValue(String)
   * @see Properties#getProperty(String)
   * @see Hashtable#get(Object)
   * @see StringUtil#valueOf(Object)
   */
  public Object get(Object pKey) {

    //System.out.println("get(" + pKey + "): " + super.get(pKey));
    Object value = super.get(pKey);

    // ---
    if ((value != null) && (value instanceof Date)) {  // Hmm.. This is true for subclasses too...

      // Special threatment of Date
      String format = getPropertyFormat(StringUtil.valueOf(pKey));

      if (format != null) {
        value = new SimpleDateFormat(format).format(value);
      } else {
        value = sDefaultFormat.format(value);
      }
      return value;
    }

    // ---
    // Simply return value
    return StringUtil.valueOf(value);
  }

  /**
   * Searches for the property with the specified key in this property list.
   * If the key is not found in this property list, the default property list,
   * and its defaults, recursively, are then checked. The method returns the
   * default value argument if the property is not found.
   *
   * @param   pKey            the hashtable key.
   * @param   pDefaultValue   a default value.
   *
   * @return  the value in this property list with the specified key value.
   * @see     #setProperty
   * @see     #defaults
   */
  public String getProperty(String pKey, String pDefaultValue) {

    // Had to override this because Properties uses super.get()...
    String value = (String) get(pKey);  // Safe cast, see get(Object)

    if (value != null) {
      return value;
    }
    return ((defaults != null)
            ? defaults.getProperty(pKey)
            : pDefaultValue);
  }

  /**
   * Searches for the property with the specified key in this property list.
   * If the key is not found in this property list, the default property list,
   * and its defaults, recursively, are then checked. The method returns
   * {@code null} if the property is not found.
   *
   * @param   pKey   the property key.
   * @return  the value in this property list with the specified key value.
   * @see     #setProperty
   * @see     #defaults
   */
  public String getProperty(String pKey) {

    // Had to override this because Properties uses super.get()...
    return getProperty(pKey, null);
  }

  /**
   * Writes this property list (key and element pairs) in this
   * {@code Properties}
   * table to the output stream in a format suitable for loading into a
   * Properties table using the load method. This implementation writes
   * the list in XML format. The stream is written using the UTF-8 character
   * encoding.
   *
   * @param pOutput the output stream to write to.
   * @param pHeader a description of the property list.
   *
   * @exception IOException if writing this property list to the specified
   *  output stream throws an IOException.
   *
   * @see java.util.Properties#store(OutputStream,String)
   */
  public synchronized void store(OutputStream pOutput, String pHeader) throws IOException {
    storeXML(this, pOutput, pHeader);
  }

  /**
   * Utility method that stores the property list in normal properties
   * format. This method writes the list of Properties (key and element
   * pairs) in the given {@code Properties}
   * table to the output stream in a format suitable for loading into a
   * Properties table using the load method. The stream is written using the
   * ISO 8859-1 character encoding.
   *
   * @param pProperties the Properties table to store
   * @param pOutput the output stream to write to.
   * @param pHeader a description of the property list.
   *
   * @exception IOException if writing this property list to the specified
   *  output stream throws an IOException.
   *
   * @see java.util.Properties#store(OutputStream,String)
   */
  public static void storeProperties(Map pProperties, OutputStream pOutput, String pHeader) throws IOException {
    // Create new properties
    Properties props = new Properties();

    // Copy all elements from the pProperties (shallow)
    Iterator iterator = pProperties.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry) iterator.next();

      props.setProperty((String) entry.getKey(), StringUtil.valueOf(entry.getValue()));
    }

    // Store normal properties
    props.store(pOutput, pHeader);
  }

  /**
   * Utility method that stores the property list in XML format. This method
   * writes the list of Properties (key and element pairs) in the given
   * {@code Properties}
   * table to the output stream in a format suitable for loading into a
   * XMLProperties table using the load method.  Useful for converting
   * Properties into XMLProperties.
   * The stream is written using the UTF-8 character
   * encoding.
   *
   * @param pProperties the Properties table to store.
   * @param pOutput the output stream to write to.
   * @param pHeader a description of the property list.
   *
   * @exception IOException if writing this property list to the specified
   *  output stream throws an IOException.
   *
   * @see #store(OutputStream,String)
   * @see java.util.Properties#store(OutputStream,String)
   *
   * @todo Find correct way of setting namespace URI's
   * @todo Store type and format information
   */
  public static void storeXML(Map pProperties, OutputStream pOutput, String pHeader) throws IOException {
    // Build XML tree (Document) and write
    // Find the implementation
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      throw (IOException) new IOException(e.getMessage()).initCause(e);
    }
    DOMImplementation dom = builder.getDOMImplementation();

    Document document = dom.createDocument(null, PROPERTIES, dom.createDocumentType(PROPERTIES, null, SYSTEM_DTD_URI));
    Element root = document.getDocumentElement();

    // This is probably not the correct way of setting a default namespace
    root.setAttribute(XMLNS, NAMESPACE_URI);

    // Create and insert the normal Properties headers as XML comments
    if (pHeader != null) {
      document.insertBefore(document.createComment(" " + pHeader + " "), root);
    }
    document.insertBefore(document.createComment(" " + new Date() + " "), root);

    // Insert elements from the Properties
    Iterator iterator = pProperties.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry) iterator.next();
      String key = (String) entry.getKey();
      Object value = entry.getValue();
      String format = null;

      if (pProperties instanceof XMLProperties) {
        format = ((XMLProperties) pProperties).getPropertyFormat(key);
      }
      insertElement(document, key, value, format);
    }

    // Create serializer and output document
    //XMLSerializer serializer = new XMLSerializer(pOutput, new OutputFormat(document, UTF_8_ENCODING, true));
    XMLSerializer serializer = new XMLSerializer(pOutput, UTF_8_ENCODING);

    serializer.serialize(document);
  }

  /**
   * Inserts elements to the given document one by one, and creates all its
   * parents if needed.
   *
   * @param pDocument the document to insert to.
   * @param pName the name of the property element.
   * @param pValue the value of the property element.
   * @param pFormat
   *
   * @todo I guess this implementation could use some optimisaztion, as
   * we do a lot of unneccessary looping.
   */
  private static void insertElement(Document pDocument, String pName, Object pValue, String pFormat) {

    // Get names of all elements we need
    String[] names = StringUtil.toStringArray(pName, ".");

    // Get value formatted as string
    String value = null;

    if (pValue != null) {
      // ---
      if (pValue instanceof Date) {

        // Special threatment of Date
        if (pFormat != null) {
          value = new SimpleDateFormat(pFormat).format(pValue);
        }
        else {
          value = sDefaultFormat.format(pValue);
        }
      }
      else {
        value = String.valueOf(pValue);
      }

      // ---
    }

    // Loop through document from root, and insert parents as needed
    Element element = pDocument.getDocumentElement();

    for (int i = 0; i < names.length; i++) {
      boolean found = false;

      // Get children
      NodeList children = element.getElementsByTagName(PROPERTY);
      Element  child    = null;

      // Search for correct name
      for (int j = 0; j < children.getLength(); j++) {
        child = (Element) children.item(j);
        if (names[i].equals(child.getAttribute(PROPERTY_NAME))) {
          // Found
          found   = true;
          element = child;
          break;  // Next name
        }
      }

      // Test if the node was not found, otherwise we need to insert
      if (!found) {
        // Not found
        child = pDocument.createElement(PROPERTY);
        child.setAttribute(PROPERTY_NAME, names[i]);

        // Insert it
        element.appendChild(child);
        element = child;
      }

      // If it's the destination node, set the value
      if ((i + 1) == names.length) {

        // If the value string contains special data,
        // use a CDATA block instead of the "value" attribute
        if (StringUtil.contains(value, "\n") || StringUtil.contains(value, "\t") || StringUtil.contains(value, "\"")
                || StringUtil.contains(value, "&") || StringUtil.contains(value, "<") || StringUtil.contains(value, ">")) {

          // Create value element
          Element valueElement = pDocument.createElement(PROPERTY_VALUE);

          // Set type attribute
          String className = pValue.getClass().getName();

          className = StringUtil.replace(className, "java.lang.", "");
          if (!DEFAULT_TYPE.equals(className)) {
            valueElement.setAttribute(PROPERTY_TYPE, className);
          }

          // Set format attribute
          if (pFormat != null) {
            valueElement.setAttribute(PROPERTY_FORMAT, pFormat);
          }

          // Crate cdata section
          CDATASection cdata = pDocument.createCDATASection(value);

          // Append to document tree
          valueElement.appendChild(cdata);
          child.appendChild(valueElement);
        }
        else {
          // Just set normal attribute value
          child.setAttribute(PROPERTY_VALUE, value);

          // Set type attribute
          String className = pValue.getClass().getName();

          className = StringUtil.replace(className, "java.lang.", "");
          if (!DEFAULT_TYPE.equals(className)) {
            child.setAttribute(PROPERTY_TYPE, className);
          }

          // If format is set, store in attribute
          if (pFormat != null) {
            child.setAttribute(PROPERTY_FORMAT, pFormat);
          }
        }
      }
    }
  }

  /**
   * Gets all properties in a properties group.
   * If no properties exists in the specified group, {@code null} is
   * returned.
   *
   * @param pGroupKey the group key
   *
   * @return a new Properties continaing all properties in the group. Keys in
   *         the new Properties wil not contain the group key.
   * If no properties exists in the specified group, {@code null} is
   * returned.
   */
  public Properties getProperties(String pGroupKey) {
    // Stupid impl...
    XMLProperties props    = new XMLProperties();
    String        groupKey = pGroupKey;

    if (groupKey.charAt(groupKey.length()) != '.') {
      groupKey += ".";
    }

    Iterator iterator = entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry) iterator.next();
      String    key   = (String) entry.getKey();

      if (key.startsWith(groupKey)) {
        String subKey = key.substring(key.indexOf(groupKey));

        props.setPropertyValue(subKey, entry.getValue());
      }
    }

    return ((props.size() > 0) ? props : null);
  }

  /**
   * Sets the properties in the given properties group.
   * Existing properties in the same group, will not be removed, unless they
   * are replaced by new values.
   * Any existing properties in the same group that was replaced, are
   * returned. If no properties are replaced, <CODE>null<CODE> is
   * returned.
   *
   * @param pGroupKey the group key
   * @param pProperties the properties to set into this group
   *
   * @return Any existing properties in the same group that was replaced.
   * If no properties are replaced, <CODE>null<CODE> is
   * returned.
   */
  public Properties setProperties(String pGroupKey, Properties pProperties) {
    XMLProperties old      = new XMLProperties();
    String        groupKey = pGroupKey;

    if (groupKey.charAt(groupKey.length()) != '.') {
      groupKey += ".";
    }
    Iterator iterator = pProperties.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry) iterator.next();
      String    key   = (String) entry.getKey();
      Object    obj   = setPropertyValue(groupKey + key, entry.getValue());

      // Return removed entries
      if (obj != null) {
        old.setPropertyValue(groupKey + key, entry.getValue());
      }
    }
    return ((old.size() > 0) ? old : null);
  }

  /**
   * For testing only.
   */
  public static void main(String[] pArgs) throws Exception {
    // -- Print DTD
    System.out.println("DTD: \n" + DTD);
    System.out.println("--");

    // -- Test load
    System.out.println("Reading properties from \"" + pArgs[0] + "\"...");
    XMLProperties props = new XMLProperties();

    props.load(new FileInputStream(new File(pArgs[0])));
    props.list(System.out);
    System.out.println("--");

    // -- Test recursion
    String     key = "key";
    Object     old = props.setProperty(key, "AAA");
    Properties p1  = new XMLProperties(new XMLProperties(props));
    Properties p2  = new Properties(new Properties(props));

    System.out.println("XMLProperties: " + p1.getProperty(key) + " ==" + " Properties: " + p2.getProperty(key));
    if (old == null) {
      props.remove("key");
    } else {
      props.put("key", old);  // Put old value back, to avoid confusion...
    }
    System.out.println("--");

    // -- Test store
    //props.store(System.out, "XML Properties file written by XMLProperties.");
    File out = new File("copy_of_" + pArgs[0]);

    System.out.println("Writing properties to \"" + out.getName() + "\"");
    if (!out.exists()) {
      props.store(new FileOutputStream(out), "XML Properties file written by XMLProperties.");
    } else {
      System.err.println("File \"" + out.getName() + "\" allready exists, cannot write!");
    }

    // -- Test utility methods
    // Write normal properties from XMLProperties
    out = new File("copy_of_" + pArgs[0].substring(0, pArgs[0].lastIndexOf(".")) + ".properties");
    System.out.println("Writing properties to \"" + out.getName() + "\"");
    if (!out.exists()) {
      storeProperties(props, new FileOutputStream(out), "Properties file written by XMLProperties.");
    } else {
      System.err.println("File \"" + out.getName() + "\" allready exists, cannot write!");
    }
    System.out.println("--");

    // -- Test type attribute
    System.out.println("getPropertyValue(\"one\"): " + props.getPropertyValue("one") + " class: "
                       + props.getPropertyValue("one").getClass());
    System.out.println("setPropertyValue(\"now\", " + new Date() + "): " + props.setPropertyValue("now", new Date()) + " class: "
                       + props.getPropertyValue("now").getClass());
    System.out.println("getPropertyValue(\"date\"): " + props.getPropertyValue("date") + " class: "
                       + props.getPropertyValue("date").getClass());
    System.out.println("getPropertyValue(\"time\"): " + props.getPropertyValue("time") + " class: "
                       + props.getPropertyValue("time").getClass());
  }

  /**
   * ContentHandler, ErrorHandler and EntityResolver implementation for the
   * SAX Parser.
   */
  protected class PropertiesHandler extends DefaultHandler {
    protected Stack mStack = null;

    /** Stores the characters read so far, from the characters callback */
    protected char[]        mReadSoFar  = null;
    protected boolean       mIsValue    = false;
    protected String        mType       = null;
    protected String        mFormat     = null;
    protected XMLProperties mProperties = null;
    protected Locator       mLocator    = null;

    /**
     * Creates a PropertiesHandler for the given XMLProperties.
     */
    PropertiesHandler(XMLProperties pProperties) {
      mProperties = pProperties;
      mStack      = new Stack();
    }

    /**
     * setDocumentLocator implementation.
     */
    public void setDocumentLocator(Locator pLocator) {

      // System.out.println("Setting locator: " + pLocator);
      mLocator = pLocator;
    }

    /**
     * Calls XMLProperties.addXMLError with the given SAXParseException
     * as the argument.
     */
    public void error(SAXParseException pException) throws SAXParseException {

      //throw pException;
      mProperties.addXMLError(pException);

      /*
        System.err.println("error:  " + pException.getMessage());
        System.err.println("line:   " + mLocator.getLineNumber());
        System.err.println("column: " + mLocator.getColumnNumber());
      */
    }

    /**
     * Throws the given SAXParseException (and stops the parsing).
     */
    public void fatalError(SAXParseException pException) throws SAXParseException {

      throw pException;

      /*
        System.err.println("fatal error: " + pException.getMessage());
        System.err.println("line:        " + mLocator.getLineNumber());
        System.err.println("column:      " + mLocator.getColumnNumber());
      */
    }

    /**
     * Calls XMLProperties.addXMLWarning with the given SAXParseException
     * as the argument.
     */
    public void warning(SAXParseException pException) throws SAXParseException {

      // throw pException;
      mProperties.addXMLWarning(pException);

      /*
        System.err.println("warning: " + pException.getMessage());
        System.err.println("line:    " + mLocator.getLineNumber());
        System.err.println("column:  " + mLocator.getColumnNumber());
      */
    }

    /**
     * startElement implementation.
     */
    public void startElement(String pNamespaceURI, String pLocalName, String pQualifiedName, Attributes pAttributes) throws SAXException {

      /*

      String attributes = "";
      for (int i = 0; i < pAttributes.getLength(); i++) {
      attributes += pAttributes.getQName(i) + "=" +  pAttributes.getValue(i) + (i < pAttributes.getLength() ? ", " : "");
      }

      System.out.println("startElement: " + pNamespaceURI
      + "." + pLocalName
      + " (" + pQualifiedName + ") "
      + attributes);
      */
      if (XMLProperties.PROPERTY.equals(pLocalName)) {

        // Get attibute values
        String name   = pAttributes.getValue(XMLProperties.PROPERTY_NAME);
        String value  = pAttributes.getValue(XMLProperties.PROPERTY_VALUE);
        String type   = pAttributes.getValue(XMLProperties.PROPERTY_TYPE);
        String format = pAttributes.getValue(XMLProperties.PROPERTY_FORMAT);

        // Get the full name of the property
        if (!mStack.isEmpty()) {
          name = (String) mStack.peek() + "." + name;
        }

        // Set the property
        if (value != null) {
          mProperties.setProperty(name, value);

          // Store type & format
          if (!XMLProperties.DEFAULT_TYPE.equals(type)) {
            mType   = type;
            mFormat = format;  // Might be null (no format)
          }
        }

        // Push the last name on the stack
        mStack.push(name);
      }                        // /PROPERTY
              else if (XMLProperties.PROPERTY_VALUE.equals(pLocalName)) {

        // Get attibute values
        String name   = (String) mStack.peek();
        String type   = pAttributes.getValue(XMLProperties.PROPERTY_TYPE);
        String format = pAttributes.getValue(XMLProperties.PROPERTY_FORMAT);

        // Store type & format
        if (!XMLProperties.DEFAULT_TYPE.equals(type)) {
          mType   = type;
          mFormat = format;
        }
        mIsValue = true;
      }
    }

    /**
     * endElement implementation.
     */
    public void endElement(String pNamespaceURI, String pLocalName, String pQualifiedName) throws SAXException {

      /*
        System.out.println("endElement: " + pNamespaceURI
        + "." + pLocalName + " (" + pQualifiedName + ")");
      */
      if (XMLProperties.PROPERTY.equals(pLocalName)) {

        // Just remove the last name
        String name = (String) mStack.pop();

        // Init typed values
        try {
          String prop = mProperties.getProperty(name);

          // Value may be null, if so just skip
          if (prop != null) {
            Object value = mProperties.initPropertyValue(prop, mType, mFormat);

            // Store format
            if ((mFormat != null) &&!XMLProperties.DEFAULT_DATE_FORMAT.equals(mFormat)) {
              mProperties.setPropertyFormat(name, mFormat);
            }

            //System.out.println("-->" + prop + "-->" + value);
            mProperties.setPropertyValue(name, value);
          }

          // Clear type & format
          mType   = null;
          mFormat = null;
        } catch (Exception e) {
          e.printStackTrace(System.err);
          throw new SAXException(e);
        }
      } else if (XMLProperties.PROPERTY_VALUE.equals(pLocalName)) {
        if (mStack.isEmpty()) {

          // There can't be any characters here, really
          return;
        }

        // Get the full name of the property
        String name = (String) mStack.peek();

        // Set the property
        String value = new String(mReadSoFar);

        //System.err.println("characters: >" + value+ "<");
        if (!StringUtil.isEmpty(value)) {

          // If there is allready a value, both the value attribute
          // and element have been specified, this is an error
          if (mProperties.containsKey(name)) {
            throw new SAXParseException(
              "Value can only be specified either using the \"value\" attribute, OR the \"value\" element, not both.", mLocator);
          }

          // Finally, set the property
          mProperties.setProperty(name, value);
        }

        // Done value processing
        mIsValue = false;
      }
    }

    /**
     * characters implementation
     */
    public void characters(char[] pChars, int pStart, int pLength) throws SAXException {
        // TODO: Use StringBuilder instead?
      if (mIsValue) {
        // If nothing read so far
        if (mReadSoFar == null) {
          // Create new array and copy into
          mReadSoFar = new char[pLength];
          System.arraycopy(pChars, pStart, mReadSoFar, 0, pLength);
        }
        else {
          // Merge arrays
          mReadSoFar = (char[]) CollectionUtil.mergeArrays(mReadSoFar, 0, mReadSoFar.length, pChars, pStart, pLength);
        }
      }
    }


    /**
     * Intercepts the entity
     * "http://www.twelvemonkeys.com/xml/XMLProperties.dtd", and return
     * an InputSource based on the internal DTD of XMLProperties instead.
     *
     * @todo Maybe intercept a PUBLIC DTD and be able to have SYSTEM DTD
     * override?
     */
    public InputSource resolveEntity(String pPublicId, String pSystemId) {
      // If we are looking for the standard SYSTEM DTD, then
      // Return an InputSource based on the internal DTD.
      if (XMLProperties.SYSTEM_DTD_URI.equals(pSystemId)) {
        return new InputSource(new StringReader(XMLProperties.DTD));
      }

      // use the default behaviour
      return null;
    }
  }
}

