package com.twelvemonkeys.lang;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;

/**
 * ExceptionUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/ExceptionUtil.java#2 $
 */
public final class ExceptionUtil {

    /*public*/ static void launder(final Throwable pThrowable, Class<? extends Throwable>... pExpectedTypes) {
        if (pThrowable instanceof Error) {
            throw (Error) pThrowable;
        }
        if (pThrowable instanceof RuntimeException) {
            throw (RuntimeException) pThrowable;
        }

        for (Class<? extends Throwable> expectedType : pExpectedTypes) {
            if (expectedType.isInstance(pThrowable)) {
                throw new RuntimeException(pThrowable);
            }
        }

        throw new UndeclaredThrowableException(pThrowable);
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    static <T extends Throwable> void throwAs(final Class<T> pType, final Throwable pThrowable) throws T {
        throw (T) pThrowable;
    }

    public static void throwUnchecked(final Throwable pThrowable)  {
        throwAs(RuntimeException.class, pThrowable);
    }

    /*public*/ static void handle(final Throwable pThrowable, final ThrowableHandler<? extends Throwable>... pHandler) {
        handleImpl(pThrowable, pHandler);
    }

    @SuppressWarnings({"unchecked"})
    private static <T extends Throwable> void handleImpl(final Throwable pThrowable, final ThrowableHandler<T>... pHandler) {
        // TODO: Sort more specific throwable handlers before less specific?
        for (ThrowableHandler<T> handler : pHandler) {
            if (handler.handles(pThrowable)) {
                handler.handle((T) pThrowable);
                return;
            }
        }
        throwUnchecked(pThrowable);
    }

    public static abstract class ThrowableHandler<T extends Throwable> {
        private Class<? extends T>[] mThrowables;

        protected ThrowableHandler(final Class<? extends T>... pThrowables) {
            // TODO: Assert not null
            mThrowables = pThrowables.clone();
        }

        final public boolean handles(final Throwable pThrowable) {
            for (Class<? extends T> throwable : mThrowables) {
                if (throwable.isAssignableFrom(pThrowable.getClass())) {
                    return true;
                }
            }
            return false;
        }

        public abstract void handle(T pThrowable);
    }

    @SuppressWarnings({"InfiniteLoopStatement"})
    public static void main(String[] pArgs) {
        while (true) {
            foo();
        }
    }

    private static void foo() {
        try {
            bar();
        }
        catch (Throwable t) {
            handle(t,
                    new ThrowableHandler<IOException>(IOException.class) {
                        public void handle(final IOException pThrowable) {
                            System.out.println("IOException: " + pThrowable + " handled");
                        }
                    },
                    new ThrowableHandler<Exception>(SQLException.class, NumberFormatException.class) {
                        public void handle(final Exception pThrowable) {
                            System.out.println("Exception: " + pThrowable + " handled");
                        }
                    },
                    new ThrowableHandler<Throwable>(Throwable.class) {
                        public void handle(final Throwable pThrowable) {
                            System.err.println("Generic throwable: " + pThrowable + " NOT handled");
                            throwUnchecked(pThrowable);
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
        if (random < (1.0 / 3.0)) {
            throwUnchecked(new FileNotFoundException("FNF Boo"));
        }
        if (random < (2.0 / 3.0)) {
            throwUnchecked(new SQLException("SQL Boo"));
        }
        else {
            throwUnchecked(new Exception("Some Boo"));
        }
    }
}
