/*
 * Copyright (c) 2009, Harald Kuhr
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

/*
 * Adapted from sample code featured in
 * "Intro to Programming in Java: An Interdisciplinary Approach" (Addison Wesley)
 * by Robert Sedgewick and Kevin Wayne. Permission granted to redistribute under BSD license.
 */

package com.twelvemonkeys.imageio.metadata.tiff;

/**
 * Represents a rational number with a {@code long} numerator and {@code long} denominator.
 * Rational numbers are stored in reduced form with the sign stored with the numerator.
 * Rationals are immutable.
 * <p/>
 * Adapted from sample code featured in
 * <a href="http://www.cs.princeton.edu/introcs/home/">"Intro to Programming in Java: An Interdisciplinary Approach" (Addison Wesley)</a>
 * by Robert Sedgewick and Kevin Wayne. Permission granted to redistribute under BSD license.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="http://www.cs.princeton.edu/introcs/92symbolic/Rational.java.html">Robert Sedgewick and Kevin Wayne (original version)</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Rational.java,v 1.0 Nov 18, 2009 1:12:00 AM haraldk Exp$
 */
public final class Rational extends Number implements Comparable<Rational> {
    // TODO: Document public API
    // TODO: Move to com.tm.lang?
    // Inspired by http://www.cs.princeton.edu/introcs/92symbolic/Rational.java.html and java.lang.Integer
    static final Rational ZERO = new Rational(0, 1);
    static final Rational NaN = new Rational(); // TODO: This field needs thoughts/tests/spec/consistency check, see Float.NaN

    private final long numerator;
    private final long denominator;

    private Rational() {
        numerator = 0;
        denominator = 0;
    }

    public Rational(final long pNumber) {
        this(pNumber, 1);
    }

    public Rational(final long pNumerator, final long pDenominator) {
        if (pDenominator == 0) {
            throw new IllegalArgumentException("denominator == 0");
        }
        if (pNumerator == Long.MIN_VALUE || pDenominator == Long.MIN_VALUE) {
            throw new IllegalArgumentException("value == Long.MIN_VALUE");
        }

        // Reduce fractions
        long gcd = gcd(pNumerator, pDenominator);
        long num = pNumerator / gcd;
        long den = pDenominator / gcd;

        numerator = pDenominator >= 0 ? num : -num;
        denominator = pDenominator >= 0 ? den : -den;
    }

    private static long gcd(final long m, final long n) {
        if (m < 0) {
            return gcd(n, -m);
        }

        return n == 0 ? m : gcd(n, m % n);
    }

    private static long lcm(final long m, final long n) {
        if (m < 0) {
            return lcm(n, -m);
        }

        return m * (n / gcd(m, n));    // parentheses important to avoid overflow
    }

    public long numerator() {
        return numerator;
    }

    public long denominator() {
        return denominator;
    }

    /// Number implementation

    @Override
    public int intValue() {
        return (int) doubleValue();
    }

    @Override
    public long longValue() {
        return (long) doubleValue();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        if (this == NaN) {
            return Double.NaN;
        }

        return numerator / (double) denominator;
    }

    /// Comparable implementation

    public int compareTo(final Rational pOther) {
        double thisVal = doubleValue();
        double otherVal = pOther.doubleValue();

        return thisVal < otherVal ? -1 : thisVal == otherVal ? 0 : 1;
    }

    /// Object overrides

    @Override
    public int hashCode() {
        return Float.floatToIntBits(floatValue());
    }

    @Override
    public boolean equals(final Object pOther) {
        return pOther == this || pOther instanceof Rational && compareTo((Rational) pOther) == 0;
    }

    @Override
    public String toString() {
        if (this == NaN) {
            return "NaN";
        }

        return denominator == 1 ? Long.toString(numerator) : String.format("%s/%s", numerator, denominator);
    }

    /// Operations (adapted from http://www.cs.princeton.edu/introcs/92symbolic/Rational.java.html)
    // TODO: Naming! multiply/divide/add/subtract or times/divides/plus/minus

    // return a * b, staving off overflow as much as possible by cross-cancellation
    public Rational times(final Rational pOther) {
        // special cases
        if (equals(ZERO) || pOther.equals(ZERO)) {
            return ZERO;
        }

        // reduce p1/q2 and p2/q1, then multiply, where a = p1/q1 and b = p2/q2
        Rational c = new Rational(numerator, pOther.denominator);
        Rational d = new Rational(pOther.numerator, denominator);

        return new Rational(c.numerator * d.numerator, c.denominator * d.denominator);
    }

    // return a + b, staving off overflow
    public Rational plus(final Rational pOther) {
        // special cases
        if (equals(ZERO)) {
            return pOther;
        }
        if (pOther.equals(ZERO)) {
            return this;
        }

        // Find gcd of numerators and denominators
        long f = gcd(numerator, pOther.numerator);
        long g = gcd(denominator, pOther.denominator);

        // add cross-product terms for numerator
        // multiply back in
        return new Rational(
                ((numerator / f) * (pOther.denominator / g) + (pOther.numerator / f) * (denominator / g)) * f,
                lcm(denominator, pOther.denominator)
        );
    }

    // return -a
    public Rational negate() {
        return new Rational(-numerator, denominator);
    }

    // return a - b
    public Rational minus(final Rational pOther) {
        return plus(pOther.negate());
    }

    public Rational reciprocal() {
        return new Rational(denominator, numerator);
    }

    // return a / b
    public Rational divides(final Rational pOther) {
        if (pOther.equals(ZERO)) {
            throw new ArithmeticException("/ by zero");
        }
        
        return times(pOther.reciprocal());
    }
}
