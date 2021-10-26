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

package com.twelvemonkeys.servlet;

import com.twelvemonkeys.io.NullOutputStream;
import org.junit.Test;

import java.io.PrintWriter;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * ServletConfigExceptionTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/ServletConfigExceptionTestCase.java#2 $
 */
public class ServletConfigExceptionTest {
    @Test
    public void testThrowCatchPrintStacktrace() {
        try {
            throw new ServletConfigException("FooBar!");
        }
        catch (ServletConfigException e) {
            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    @Test
    public void testThrowCatchGetNoCause() {
        try {
            throw new ServletConfigException("FooBar!");
        }
        catch (ServletConfigException e) {
            assertNull(e.getRootCause()); // Old API
            assertNull(e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    @Test
    public void testThrowCatchInitCauseNull() {
        try {
            ServletConfigException e = new ServletConfigException("FooBar!");
            e.initCause(null);
            throw e;
        }
        catch (ServletConfigException e) {
            assertNull(e.getRootCause()); // Old API
            assertNull(e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    @Test
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

    @Test
    public void testThrowCatchGetNullCause() {
        try {
            throw new ServletConfigException("FooBar!", null);
        }
        catch (ServletConfigException e) {
            assertNull(e.getRootCause()); // Old API
            assertNull(e.getCause());

            e.printStackTrace(new PrintWriter(new NullOutputStream()));
        }
    }

    @Test
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
