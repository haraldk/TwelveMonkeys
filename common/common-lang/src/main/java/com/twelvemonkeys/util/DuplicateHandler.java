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

/**
 * DuplicateHandler
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/DuplicateHandler.java#2 $
 */
public interface DuplicateHandler<T> {

    /**
     * Resolves duplicates according to a certain strategy.
     *
     * @param pOld the old value
     * @param pNew the new value
     *
     * @return the resolved value.
     *
     * @throws IllegalArgumentException is the arguments cannot be resolved for
     * some reason.
     */
    public T resolve(T pOld, T pNew);

    /**
     * Will use the first (old) value. Any new values will be discarded.
     *
     * @see CollectionUtil#invert(java.util.Map, java.util.Map, DuplicateHandler)
     */
    public final static DuplicateHandler<?> USE_FIRST_VALUE = new DuplicateHandler() {
        /**
         * Returns {@code pOld}.
         *
         * @param pOld the old value
         * @param pNew the new value
         *
         * @return {@code pOld}
         */
        public Object resolve(Object pOld, Object pNew) {
            return pOld;
        }
    };

    /**
     * Will use the last (new) value. Any old values will be discarded
     * (overwritten).
     *
     * @see CollectionUtil#invert(java.util.Map, java.util.Map, DuplicateHandler)
     */
    public final static DuplicateHandler<?> USE_LAST_VALUE = new DuplicateHandler() {
        /**
         * Returns {@code pNew}.
         *
         * @param pOld the old value
         * @param pNew the new value
         *
         * @return {@code pNew}
         */
        public Object resolve(Object pOld, Object pNew) {
            return pNew;
        }
    };

    /**
     * Converts duplicats to an {@code Object} array.
     *
     * @see CollectionUtil#invert(java.util.Map, java.util.Map, DuplicateHandler)
     */
    public final static DuplicateHandler<?> DUPLICATES_AS_ARRAY = new DuplicateHandler() {
        /**
         * Returns an {@code Object} array, containing {@code pNew} as its
         * last element.
         *
         * @param pOld the old value
         * @param pNew the new value
         *
         * @return an {@code Object} array, containing {@code pNew} as its
         * last element.
         */
        public Object resolve(Object pOld, Object pNew) {
            Object[] result;

            if (pOld instanceof Object[]) {
                Object[] old = ((Object[]) pOld);
                result = new Object[old.length + 1];
                System.arraycopy(old, 0, result, 0, old.length);
                result[old.length] = pNew;
            }
            else {
                result = new Object[] {pOld, pNew};
            }

            return result;
        }
    };

    /**
     * Converts duplicates to a comma-separated {@code String}.
     * Note that all values should allready be {@code String}s if using this
     * handler.
     *
     * @see CollectionUtil#invert(java.util.Map, java.util.Map, DuplicateHandler)
     */
    public final static DuplicateHandler<String> DUPLICATES_AS_CSV = new DuplicateHandler<String>() {
        /**
         * Returns a comma-separated {@code String}, with the string
         * representation of {@code pNew} as the last element.
         *
         * @param pOld the old value
         * @param pNew the new value
         *
         * @return a comma-separated {@code String}, with the string
         * representation of {@code pNew} as the last element.
         */
        public String resolve(String pOld, String pNew) {
            StringBuilder result = new StringBuilder(String.valueOf(pOld));
            result.append(',');
            result.append(pNew);

            return result.toString();
        }
    };
}
