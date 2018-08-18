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

/**
 * Contains various image-outputting filters, that should run under any
 * servlet engine.
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
 * If you cannot use JRE 1.4 or later, or do not want to use the X
 * libraries, one possibility is to use the
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