package com.twelvemonkeys.util;

/**
 * A generic function, since Java 7 lacks this interface.
 *
 * @param <T>
 * @param <R>
 */
public interface Function<T, R> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	R apply(T t);
}
