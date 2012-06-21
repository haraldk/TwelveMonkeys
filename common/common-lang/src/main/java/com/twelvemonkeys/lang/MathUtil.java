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

package com.twelvemonkeys.lang;

/**
 * The class MathUtil contains methods for performing basic numeric operations 
 * such as the elementary exponential, logarithm, square root, and 
 * trigonometric functions.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/lang/MathUtil.java#1 $
 */
public final class MathUtil {

    /** */
    private MathUtil() {
    }

    /**
     * Returns the natural logarithm (base <I>e</I>) of a double value.
     * Equivalent to {@code java.lang.Math.log}, just with a proper name.
     *
     * @param pArg a number greater than 0.0. 
     * @return the value ln {@code pArg}, the natural logarithm of
     *         {@code pArg}.
     *
     * @see java.lang.Math#log(double)
     */
    public static double ln(final double pArg) {
        return Math.log(pArg);
    }

    private final static double LN_10 = Math.log(10);

    /**
     * Returns the base 10 logarithm of a double value.
     *
     * @param pArg a number greater than 0.0. 
     * @return the value log {@code pArg}, the base 10 logarithm of
     *         {@code pArg}.
     */
    public static double log(final double pArg) {
        return Math.log(pArg) / LN_10;
    }
    
    private final static double LN_2 = Math.log(10);

    /**
     * Returns the base 2 logarithm of a double value.
     *
     * @param pArg a number greater than 0.0. 
     * @return the value log<SUB>2</SUB> {@code pArg}, the base 2
     *         logarithm of {@code pArg}.
     */
    public static double log2(final double pArg) {
        return Math.log(pArg) / LN_2;
    }

    /**
     * Returns the base <i>N</i> logarithm of a double value, for a given base 
     * <i>N</i>.
     *
     * @param pArg a number greater than 0.0. 
     * @param pBase a number greater than 0.0. 
     *
     * @return the value log<SUB>pBase</SUB> {@code pArg}, the base
     *         {@code pBase} logarithm of {@code pArg}.
     */
    public static double log(final double pArg, final double pBase) {
        return Math.log(pArg) / Math.log(pBase);
    }

    /**
     * A replacement for {@code Math.abs}, that never returns negative values.
     * {@code Math.abs(long)} does this for {@code Long.MIN_VALUE}.
     *
     * @see Math#abs(long)
     * @see Long#MIN_VALUE
     *
     * @param pNumber a number
     * @return the absolute value of {@code pNumber}
     *
     * @throws ArithmeticException if {@code pNumber == Long.MIN_VALUE}
     */
    public static long abs(final long pNumber) {
        if (pNumber == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow: 9223372036854775808");
        }

        return (pNumber < 0) ? -pNumber : pNumber;
    }

    /**
     * A replacement for {@code Math.abs}, that never returns negative values.
     * {@code Math.abs(int)} does this for {@code Integer.MIN_VALUE}.
     *
     * @see Math#abs(int)
     * @see Integer#MIN_VALUE
     *
     * @param pNumber a number
     * @return the absolute value of {@code pNumber}
     *
     * @throws ArithmeticException if {@code pNumber == Integer.MIN_VALUE}
     */
    public static int abs(final int pNumber) {
        if (pNumber == Integer.MIN_VALUE) {
            throw new ArithmeticException("int overflow: 2147483648");
        }

        return (pNumber < 0) ? -pNumber : pNumber;
    }
}
