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

package com.twelvemonkeys.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps (decorates) an {@code Iterator} with extra functionality, to allow
 * element filtering. Each
 * element is filtered against the given {@code Filter}, and only elements
 * that are {@code accept}ed are returned by the {@code next} method.
 * <p>
 * The optional {@code remove} operation is implemented, but may throw
 * {@code UnsupportedOperationException} if the underlying iterator does not
 * support the remove operation.
 * </p>
 *
 * @see FilterIterator.Filter
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/FilterIterator.java#1 $
 */
public class FilterIterator<E> implements Iterator<E> {

    protected final Filter<E> filter;
    protected final Iterator<E> iterator;

    private E next = null;
    private E current = null;

    /**
     * Creates a {@code FilterIterator} that wraps the {@code Iterator}. Each
     * element is filtered against the given {@code Filter}, and only elements
     * that are {@code accept}ed are returned by the {@code next} method.
     *
     * @param pIterator the iterator to filter
     * @param pFilter the filter
     * @see FilterIterator.Filter
     */
    public FilterIterator(final Iterator<E> pIterator, final Filter<E> pFilter) {
        if (pIterator == null) {
            throw new IllegalArgumentException("iterator == null");
        }
        if (pFilter == null) {
            throw new IllegalArgumentException("filter == null");
        }

        iterator = pIterator;
        filter = pFilter;
    }

    /**
     * Returns {@code true} if the iteration has more elements. (In other
     * words, returns {@code true} if {@code next} would return an element
     * rather than throwing an exception.)
     *
     * @return {@code true} if the iterator has more elements.
     * @see FilterIterator.Filter#accept
     */
    public boolean hasNext() {
        while (next == null && iterator.hasNext()) {
            E element = iterator.next();

            if (filter.accept(element)) {
                next = element;
                break;
            }
        }

        return next != null;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @see FilterIterator.Filter#accept
     */
    public E next() {
        if (hasNext()) {
            current = next;

            // Make sure we advance next time
            next = null;
            return current;
        }
        else {
            throw new NoSuchElementException("Iteration has no more elements.");
        }
    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to {@code next}.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     */
    public void remove() {
        if (current != null) {
            iterator.remove();
        }
        else {
            throw new IllegalStateException("Iteration has no current element.");
        }
    }

    /**
     * Used to tests whether or not an element fulfills certain criteria, and
     * hence should be accepted by the FilterIterator instance.
     */
    public static interface Filter<E> {

        /**
         * Tests whether or not the element fulfills certain criteria, and hence
         * should be accepted.
         *
         * @param pElement the element to test
         * @return {@code true} if the object is accepted, otherwise
         *         {@code false}
         */
        public boolean accept(E pElement);
    }
}