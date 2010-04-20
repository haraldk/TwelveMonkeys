package com.twelvemonkeys.servlet;

import com.twelvemonkeys.io.NullOutputStream;

import junit.framework.TestCase;

import java.io.PrintWriter;

/**
 * ServletConfigExceptionTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletConfigExceptionTestCase.java#2 $
 */
public class ServletConfigExceptionTestCase extends TestCase {
    public void testThrowCatchPrintStacktrace() {
        try {
            throw new ServletConfigException("FooBar!");
        }
        catch (ServletConfigException e) {
            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    public void testThrowCatchGetNoCause() {
        try {
            throw new ServletConfigException("FooBar!");
        }
        catch (ServletConfigException e) {
            assertEquals(null, e.getRootCause()); // Old API
            assertEquals(null, e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    public void testThrowCatchInitCauseNull() {
        try {
            ServletConfigException e = new ServletConfigException("FooBar!");
            e.initCause(null);
            throw e;
        }
        catch (ServletConfigException e) {
            assertEquals(null, e.getRootCause()); // Old API
            assertEquals(null, e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    public void testThrowCatchInitCause() {
        //noinspection ThrowableInstanceNeverThrown
        Exception cause = new Exception();
        try {
            ServletConfigException exception = new ServletConfigException("FooBar!");
            exception.initCause(cause);
            throw exception;
        }
        catch (ServletConfigException e) {
            // NOTE: We don't know how the superclass is implemented, so we assume nothing here
            //assertEquals(null, e.getRootCause()); // Old API
            assertSame(cause, e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    public void testThrowCatchGetNullCause() {
        try {
            throw new ServletConfigException("FooBar!", null);
        }
        catch (ServletConfigException e) {
            assertEquals(null, e.getRootCause()); // Old API
            assertEquals(null, e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    public void testThrowCatchGetCause() {
        IllegalStateException cause = new IllegalStateException();
        try {
            throw new ServletConfigException("FooBar caused by stupid API!", cause);
        }
        catch (ServletConfigException e) {
            assertSame(cause, e.getRootCause()); // Old API
            assertSame(cause, e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }
}
