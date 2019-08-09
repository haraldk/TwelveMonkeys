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

/**
 * Abstract base class for {@code TokenIterator}s to extend.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/AbstractTokenIterator.java#1 $
 */
public abstract class AbstractTokenIterator implements TokenIterator {

    /**
     * Not supported.
     *
     * @throws UnsupportedOperationException {@code remove} is not supported by
     * this Iterator.
     */
    public void remove() {
        // TODO: This is not difficult:
        // - Convert String to StringBuilder in constructor
        // - delete(pos, next.lenght())
        // - Add toString() method
        // BUT: Would it ever be useful? :-)

        throw new UnsupportedOperationException("remove");
    }

    public final boolean hasMoreTokens() {
        return hasNext();
    }
  
    /**
     * Returns the next element in the iteration as a {@code String}.
     * This implementation simply returns {@code (String) next()}.
     *
     * @return the next element in the iteration.
     * @exception java.util.NoSuchElementException iteration has no more elements.
     * @see #next()
     */
    public final String nextToken() {
        return next();
    }

    /**
     * This implementation simply returns {@code hasNext()}.
     * @see #hasNext()
     */
    public final boolean hasMoreElements() {
        return hasNext();
    }

    /**
     * This implementation simply returns {@code next()}.
     * @see #next()
     */
    public final String nextElement() {
        return next();
    }
}
