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

package com.twelvemonkeys.util.convert;

/**
 * This exception may be thrown by PropertyConverters, when an attempted 
 * conversion fails.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/convert/ConversionException.java#1 $
 */
public class ConversionException extends IllegalArgumentException {
    protected Throwable mCause = this;

    /**
     * Creates a {@code ConversionException} with the given error message.
     *
     * @param pMessage the error message
     */ 
    public ConversionException(String pMessage) {
        super(pMessage);
    }

    /**
     * Creates a {@code ConversionException} with the given cause.
     *
     * @param pCause The Throwable that caused this exception
     */ 
    public ConversionException(Throwable pCause) {
        super(pCause == null ? null : pCause.getMessage());
        initCause(pCause);
    }

    /**
     * Returns the cause of this {@code Throwable} or {@code null} if the
     * cause is nonexistent or unknown.
     *
     * @return the cause of this {@code Throwable} or {@code null} if the
     * cause is nonexistent or unknown (the cause is the throwable that caused
     * this throwable to get thrown).
     */
    public Throwable getCause() {
        if (mCause == this) {
            return null;
        }
        return mCause;
    }

    /**
     * Initializes this ConversionException with the given cause.
     *
     * @param pCause The Throwable that caused this exception
     *
     * @throws IllegalStateException if cause is allready set 
     * @throws IllegalArgumentException if {@code pCause == this}
     */
    public Throwable initCause(Throwable pCause) {
        if (mCause != this) {
            throw new IllegalStateException("Can't overwrite cause");
        }
        if (pCause == this) {
            throw new IllegalArgumentException("Can't be caused by self");
        }

        mCause = pCause;

        return this;
    }
}
