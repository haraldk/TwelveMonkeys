package com.twelvemonkeys.lang;

import java.lang.reflect.UndeclaredThrowableException;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ExceptionUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/ExceptionUtil.java#2 $
 */
public final class ExceptionUtil {

    /**
     * Re-throws an exception, either as-is if the exception was already unchecked, otherwise wrapped in
     * a {@link RuntimeException} subtype.
     * "Expected" exception types are wrapped in {@link RuntimeException}s directly, while
     * "unexpected" exception types are wrapped in {@link java.lang.reflect.UndeclaredThrowableException}s.
     *
     * @param pThrowable the exception to launder
     * @param pExpectedTypes the types of exception the code is expected to throw
     */
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

    /*@SafeVarargs*/
    @SuppressWarnings({"unchecked", "varargs"})
    /*public*/ static  void handle(final Throwable pThrowable, final ThrowableHandler<? extends Throwable>... pHandlers) {
        handleImpl(pThrowable, (ThrowableHandler<Throwable>[]) pHandlers);
    }

    private static void handleImpl(final Throwable pThrowable, final ThrowableHandler<Throwable>... pHandlers) {
        // TODO: Sort more specific throwable handlers before less specific?
        for (ThrowableHandler<Throwable> handler : pHandlers) {
            if (handler.handles(pThrowable)) {
                handler.handle(pThrowable);
                return;
            }
        }

        // Not handled, re-throw
        throwUnchecked(pThrowable);
    }

    public static abstract class ThrowableHandler<T extends Throwable> {
        private final Class<? extends T>[] throwables;

        protected ThrowableHandler(final Class<? extends T>... pThrowables) {
            throwables = notNull(pThrowables).clone();
        }

        final public boolean handles(final Throwable pThrowable) {
            for (Class<? extends T> throwable : throwables) {
                if (throwable.isAssignableFrom(pThrowable.getClass())) {
                    return true;
                }
            }

            return false;
        }

        public abstract void handle(T pThrowable);
    }
}
