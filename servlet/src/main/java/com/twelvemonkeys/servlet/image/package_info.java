/**
 * Contains various image-outputting servlets, that should run under any servlet engine. To create your own image servlet, simply subclass the servlet
 * {@code ImageServlet}. Optionally implement the interface
 * {@code ImagePainterServlet}, if you want to do painting.
 * <P>
 * Some of these methods may require use of the native graphics libraries
 * supported by the JVM, like the X libraries on Unix systems, and should be
 * run with JRE <STRONG>1.4</STRONG> or later, and with the option:
 * <DL>
 * <DD>{@code -Djawa.awt.headless=true}</DD>
 * </DL>
 * See the document
 * <A href="http://java.sun.com/j2se/1.4/docs/guide/awt/AWTChanges.html#headless">AWT Enhancements</A> and bugtraq report
 * <A href="http://developer.java.sun.com/developer/bugParade/bugs/4281163.html">4281163</A> for more information on this issue.
 * <P>
 * If you cannot use JRE 1.4 for any reason, or do not want to use the X
 * libraries, a possibilty is to use the
 * <A href="http://www.eteks.com/pja/en/">PJA package</A> (com.eteks.pja),
 * and start the JVM with the following options:
 * <DL>
 * <DD>{@code -Xbootclasspath/a:&lt;path to pja.jar&gt;}</DD>
 * <DD>{@code -Dawt.toolkit=com.eteks.awt.PJAToolkit}</DD>
 * <DD>{@code -Djava.awt.graphicsenv=com.eteks.java2d.PJAGraphicsEnvironment}</DD>
 * <DD>{@code -Djava.awt.fonts=&lt;path where True Type fonts files will be loaded from&gt;}</DD>
 * </DL>
 * <P>
 * Please note that creation of PNG images (from bytes or URL's) are only
 * supported in JRE 1.3 and later, trying to load them from an earlier version,
 * will result in errors.
 *
 * @see com.twelvemonkeys.servlet.image.ImageServlet
 * @see com.twelvemonkeys.servlet.image.ImagePainterServlet
 */
package com.twelvemonkeys.servlet.image;