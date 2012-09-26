/*
 * Copyright (c) 2012, Harald Kuhr
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

import org.junit.Ignore;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * ExceptionUtilTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ExceptionUtilTest.java,v 1.0 11.04.12 16:07 haraldk Exp$
 */
@Ignore("Under development")
public class ExceptionUtilTest {
    @Test(expected = BadException.class)
    @SuppressWarnings({"InfiniteLoopStatement"})
    public void test() {
        while (true) {
            foo();
        }
    }

    @SuppressWarnings({"unchecked", "varargs"})
    private static void foo() {
        try {
            bar();
        }
        catch (Throwable t) {
            ExceptionUtil.handle(t,
                    new ExceptionUtil.ThrowableHandler<IOException>(IOException.class) {
                        public void handle(final IOException pThrowable) {
                            System.out.println("IOException: " + pThrowable + " handled");
                        }
                    },
                    new ExceptionUtil.ThrowableHandler<Exception>(SQLException.class, NumberFormatException.class) {
                        public void handle(final Exception pThrowable) {
                            System.out.println("Exception: " + pThrowable + " handled");
                        }
                    }
            );
        }
    }

    private static void bar() {
        baz();
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private static void baz() {
        double random = Math.random();
        if (random < (2.0 / 3.0)) {
            ExceptionUtil.throwUnchecked(new FileNotFoundException("FNF Boo"));
        }
        if (random < (5.0 / 6.0)) {
            ExceptionUtil.throwUnchecked(new SQLException("SQL Boo"));
        }
        else {
            ExceptionUtil.throwUnchecked(new BadException("Some Boo"));
        }
    }

    static final class BadException extends Exception {
        public BadException(String s) {
            super(s);
        }
    }
}
